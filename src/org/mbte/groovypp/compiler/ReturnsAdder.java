package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.ClassCodeExpressionTransformer;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.VariableScope;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.TernaryExpression;
import org.codehaus.groovy.ast.stmt.*;
import org.codehaus.groovy.classgen.BytecodeSequence;
import org.codehaus.groovy.control.SourceUnit;

import java.util.ArrayList;
import java.util.Iterator;
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
        if (statement instanceof ReturnStatement
           || statement instanceof BytecodeSequence
           || statement instanceof ThrowStatement)
        {
            return statement;
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
            if (expr.getClass() == TernaryExpression.class) {
                TernaryExpression t = (TernaryExpression)expr;
                ExpressionStatement trueExpr = new ExpressionStatement(t.getTrueExpression());
                trueExpr.setSourcePosition(t.getTrueExpression());
                ExpressionStatement falseExpr = new ExpressionStatement(t.getFalseExpression());
                falseExpr.setSourcePosition(t.getFalseExpression());
                ret = new IfStatement(t.getBooleanExpression(), trueExpr, falseExpr);
                ret.setSourcePosition(expr);
                ret = addReturnsIfNeeded(ret, scope);
            }
            else {
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
            }
            else {
                ReturnStatement ret = new ReturnStatement(ConstantExpression.NULL);
                ret.setSourcePosition(block);
                return ret;
            }

            return new BlockStatement(filterStatements(list), block.getVariableScope());
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
        returnStatement.setLineNumber(methodNode.getLastLineNumber());
        returnStatement.setColumnNumber(methodNode.getLastColumnNumber());
        returnStatement.setLastLineNumber(methodNode.getLastLineNumber());
        returnStatement.setLastColumnNumber(methodNode.getLastColumnNumber());
        return returnStatement;
    }

    protected List<Statement> filterStatements(List<Statement> list) {
        List<Statement> answer = new ArrayList<Statement>(list.size());
        for (Iterator<Statement> iter = list.iterator(); iter.hasNext();) {
            answer.add(filterStatement(iter.next()));
        }
        return answer;
    }

    protected Statement filterStatement(Statement statement) {
        if (statement instanceof ExpressionStatement) {
            ExpressionStatement expStmt = (ExpressionStatement) statement;
            Expression expression = expStmt.getExpression();
            if (expression instanceof ClosureExpression) {
                ClosureExpression closureExp = (ClosureExpression) expression;
                if (!closureExp.isParameterSpecified()) {
                    return closureExp.getCode();
                }
            }
        }
        return statement;
    }

    protected SourceUnit getSourceUnit() {
        return su;
    }
}
