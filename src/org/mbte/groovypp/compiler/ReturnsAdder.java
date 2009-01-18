package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.ClassCodeExpressionTransformer;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.VariableScope;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.stmt.*;
import org.codehaus.groovy.classgen.BytecodeSequence;
import org.codehaus.groovy.control.SourceUnit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class ReturnsAdder extends ClassCodeExpressionTransformer  {
    private SourceUnit su;
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
//            setCode(newBlock);
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
            return new NullReturnStatement(methodNode.getReturnType());
        }

        if (statement instanceof ExpressionStatement) {
            ExpressionStatement expStmt = (ExpressionStatement) statement;
            Expression expr = expStmt.getExpression();
            ReturnStatement ret = new ReturnStatement(expr);
            ret.setSourcePosition(expr);
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

            final List list = block.getStatements();
            if (!list.isEmpty()) {
                int idx = list.size() - 1;
                Statement last = addReturnsIfNeeded((Statement) list.get(idx), block.getVariableScope());
                list.set(idx, last);
                if (!statementReturns(last)) {
                    list.add(new NullReturnStatement(methodNode.getReturnType()));
                }
            }
            else {
                NullReturnStatement ret = new NullReturnStatement(methodNode.getReturnType());
                ret.setSourcePosition(block);
                return ret;
            }

            return new BlockStatement(filterStatements(list),block.getVariableScope());
        }

        if (statement == null)
          return new ReturnStatement(ConstantExpression.NULL);
        else {
            final List list = new ArrayList();
            list.add(statement);
            list.add(new ReturnStatement(ConstantExpression.NULL));
            return new BlockStatement(list,new VariableScope(scope));
        }
    }

    private boolean statementReturns(Statement last) {
        return (
                last instanceof ReturnStatement ||
                last instanceof BlockStatement ||
                last instanceof IfStatement ||
                last instanceof ExpressionStatement ||
                last instanceof EmptyStatement ||
                last instanceof TryCatchStatement ||
                last instanceof BytecodeSequence ||
                last instanceof ThrowStatement ||
                last instanceof SynchronizedStatement
                );
    }

    protected List filterStatements(List list) {
        List answer = new ArrayList(list.size());
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            answer.add(filterStatement((Statement) iter.next()));
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
