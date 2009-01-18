package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.mbte.groovypp.compiler.BytecodeSpreadExpr;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;

import java.util.List;

public class ListExpressionTransformer extends ExprTransformer<ListExpression> {
    public Expression transform(final ListExpression exp, CompilerTransformer compiler) {
        final List list = exp.getExpressions();
        for (int i = 0; i != list.size(); ++i) {
            list.set(i, compiler.transform((Expression) list.get(i)));
        }

        return new MyBytecodeExpr(exp);
    }

    private static class MyBytecodeExpr extends BytecodeExpr {
        private final ListExpression exp;

        public MyBytecodeExpr(ListExpression exp) {
            super(exp, TypeUtil.ARRAY_LIST_TYPE);
            this.exp = exp;
        }

        protected void compile() {
            final List list = exp.getExpressions();
            mv.visitTypeInsn(NEW, "java/util/ArrayList");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL,"java/util/ArrayList","<init>","()V");
            for (int i = 0; i != list.size(); ++i) {
                final BytecodeExpr be = (BytecodeExpr) list.get(i);
                mv.visitInsn(DUP);
                be.visit(mv);
                if (be instanceof BytecodeSpreadExpr)
                    mv.visitMethodInsn(INVOKEVIRTUAL,"java/util/ArrayList","addAll","(Ljava/util/Collection;)Z");
                else {
                    box(be.getType());
                    mv.visitMethodInsn(INVOKEVIRTUAL,"java/util/ArrayList","add","(Ljava/lang/Object;)Z");
                }
                mv.visitInsn(POP);
            }
        }
    }
}
