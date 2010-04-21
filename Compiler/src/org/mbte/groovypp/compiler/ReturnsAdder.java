/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.ClassCodeExpressionTransformer;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.VariableScope;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.*;
import org.codehaus.groovy.classgen.BytecodeSequence;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.syntax.Types;

import java.util.ArrayList;
import java.util.List;

public abstract class ReturnsAdder extends ClassCodeExpressionTransformer  {
    public SourceUnit su;
    public final MethodNode methodNode;

    public ReturnsAdder(SourceUnit su, MethodNode methodNode) {
        this.su = su;
        this.methodNode = methodNode;
    }

    public void addReturnIfNeeded() {
        Statement statement = getCode();
        if (!methodNode.isVoidMethod()) {
            if (statement != null) // it happens with @interface methods
              setCode(addReturnsIfNeeded(statement, methodNode.getVariableScope()));
        }
        else if (!methodNode.isAbstract()) {
//            BlockStatement newBlock = new BlockStatement();
//            if (statement instanceof BlockStatement) {
//                newBlock.addStatements(filterStatements(((BlockStatement)statement).getStatements()));
//            } else {
//                newBlock.addStatement(filterStatement(statement));
//            }
//            newBlock.addStatement(ReturnStatement.RETURN_NULL_OR_VOID);
//            newBlock.setSourcePosition(statement);
//            setClosureExpression(newBlock);
            setCode(addReturnsIfNeeded(statement, methodNode.getVariableScope()));
        }
    }

    protected abstract void setCode(Statement newBlock);

    protected abstract Statement getCode();

    private Statement addReturnsIfNeeded(Statement statement, VariableScope scope) {
        if (statement instanceof BytecodeSequence
           || statement instanceof ThrowStatement)
        {
            return statement;
        }

        if (statement instanceof ReturnStatement) {
            final Expression expr = ((ReturnStatement) statement).getExpression();
            if (!(expr instanceof TernaryExpression || expr instanceof BinaryExpression)) return statement;
            statement = new ExpressionStatement(expr);
        }

        if (statement instanceof EmptyStatement) {
            ReturnStatement ret = new ReturnStatement(ConstantExpression.NULL);
            ret.setSourcePosition(statement);
            return ret;
        }

        if (statement instanceof ExpressionStatement) {
            ExpressionStatement expStmt = (ExpressionStatement) statement;
            Expression expr = expStmt.getExpression();
            Statement ret;
            if (expr instanceof TernaryExpression) {
                TernaryExpression t = (TernaryExpression)expr;
                ExpressionStatement trueExpr = new ExpressionStatement(t.getTrueExpression());
                trueExpr.setSourcePosition(t.getTrueExpression());
                ExpressionStatement falseExpr = new ExpressionStatement(t.getFalseExpression());
                falseExpr.setSourcePosition(t.getFalseExpression());
                ret = new IfStatement(t.getBooleanExpression(), trueExpr, falseExpr);
                ret.setSourcePosition(expr);
                return addReturnsIfNeeded(ret, scope);
            }
            else {
                if (expr instanceof BinaryExpression) {
                    BinaryExpression be = (BinaryExpression) expr;
                    switch(be.getOperation().getType()) {
                        case Types.LOGICAL_AND: {
                            TernaryExpression t = new TernaryExpression(new BooleanExpression(be.getLeftExpression()), be.getRightExpression(), ConstantExpression.FALSE);
                            t.setSourcePosition(be);
                            ExpressionStatement es = new ExpressionStatement(t);
                            es.setSourcePosition(t);
                            return addReturnsIfNeeded(es, scope);
                        }

                        case Types.LOGICAL_OR: {
                            TernaryExpression t = new TernaryExpression(new BooleanExpression(be.getLeftExpression()), ConstantExpression.TRUE, be.getRightExpression());
                            t.setSourcePosition(be);
                            ExpressionStatement es = new ExpressionStatement(t);
                            es.setSourcePosition(t);
                            return addReturnsIfNeeded(es, scope);
                        }
                    }
                }
                ret = new ReturnStatement(expr);
                ret.setSourcePosition(expr);
            }
            return ret;
        }

        if (statement instanceof SynchronizedStatement) {
            SynchronizedStatement sync = (SynchronizedStatement) statement;
            sync.setCode(addReturnsIfNeeded(sync.getCode(), scope));
            return sync;
        }

        if (statement instanceof IfStatement) {
            IfStatement ifs = (IfStatement) statement;
            ifs.setIfBlock(addReturnsIfNeeded(ifs.getIfBlock(), scope));
            ifs.setElseBlock(addReturnsIfNeeded(ifs.getElseBlock(), scope));

            BlockStatement block = new BlockStatement();
            block.addStatement(ifs);
            block.addStatement(ifs.getElseBlock());
            ifs.setElseBlock(EmptyStatement.INSTANCE);
            return block;
        }

        if (statement instanceof TryCatchStatement) {
            TryCatchStatement trys = (TryCatchStatement) statement;
            trys.setTryStatement(addReturnsIfNeeded(trys.getTryStatement(), scope));
            final int len = trys.getCatchStatements().size();
            for (int i = 0; i != len; ++i) {
                final CatchStatement catchStatement = trys.getCatchStatement(i);
                catchStatement.setCode(addReturnsIfNeeded(catchStatement.getCode(), scope));
            }
            return trys;
        }

        if (statement instanceof BlockStatement) {
            BlockStatement block = (BlockStatement) statement;

            final List<Statement> list = block.getStatements();
            if (!list.isEmpty()) {
                int idx = list.size() - 1;
                list.set(idx, addReturnsIfNeeded(list.get(idx), block.getVariableScope()));
                return new BlockStatement(list, block.getVariableScope());
            }
            else {
                ReturnStatement ret = new ReturnStatement(ConstantExpression.NULL);
                ret.setSourcePosition(block);
                return ret;
            }
        }

        if (statement instanceof SwitchStatement) {
            SwitchStatement swi = (SwitchStatement) statement;
            for (CaseStatement caseStatement : swi.getCaseStatements()) {
                caseStatement.setCode(adjustSwitchCaseCode(caseStatement.getCode(), scope, false));
            }
            swi.setDefaultStatement(adjustSwitchCaseCode(swi.getDefaultStatement(), scope, true));
            return swi;
        }

        if (statement == null)
            return createSyntheticReturnStatement();
        else {
            final List<Statement> list = new ArrayList<Statement>();
            list.add(statement);
            list.add(createSyntheticReturnStatement());
            return new BlockStatement(list,new VariableScope(scope));
        }
    }

    // This creates a return statement which has its source position after the last statement in the method.
    private ReturnStatement createSyntheticReturnStatement() {
        ReturnStatement returnStatement = new ReturnStatement(ConstantExpression.NULL);
        returnStatement.setSourcePosition(methodNode);
        return returnStatement;
    }

    private Statement adjustSwitchCaseCode(Statement statement, VariableScope scope, boolean defaultCase) {
        if(statement instanceof BlockStatement) {
            final List list = ((BlockStatement)statement).getStatements();
            if (!list.isEmpty()) {
                int idx = list.size() - 1;
                Statement last = (Statement) list.get(idx);
                if(last instanceof BreakStatement) {
                    list.remove(idx);
                    return addReturnsIfNeeded(statement, scope);
                } else if(defaultCase) {
                    return addReturnsIfNeeded(statement, scope);
                }
            }
        }
        else {
            if (defaultCase && statement instanceof EmptyStatement) {
                return addReturnsIfNeeded(statement, scope);
            }
        }
        return statement;
    }

    protected SourceUnit getSourceUnit() {
        return su;
    }
}
