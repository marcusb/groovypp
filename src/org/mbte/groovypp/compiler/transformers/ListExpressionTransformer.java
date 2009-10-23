package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.mbte.groovypp.compiler.BytecodeSpreadExpr;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.objectweb.asm.MethodVisitor;

import java.util.List;

public class ListExpressionTransformer extends ExprTransformer<ListExpression> {
    public Expression transform(final ListExpression exp, CompilerTransformer compiler) {
        final List list = exp.getExpressions();
        for (int i = 0; i != list.size(); ++i) {
            list.set(i, compiler.transform((Expression) list.get(i)));
        }

        return new ResolvedListExpression(exp, TypeUtil.ARRAY_LIST_TYPE);
    }

    public static class ResolvedListExpression extends BytecodeExpr {
        private final ListExpression exp;

        public ResolvedListExpression(ListExpression exp, ClassNode type) {
            super(exp, type);
            this.exp = exp;
        }

        protected void compile(MethodVisitor mv) {
            final List list = exp.getExpressions();
            String classInternalName = BytecodeHelper.getClassInternalName(getType());
            mv.visitTypeInsn(NEW, classInternalName);
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL,classInternalName,"<init>","()V");
            for (int i = 0; i != list.size(); ++i) {
                final BytecodeExpr be = (BytecodeExpr) list.get(i);
                mv.visitInsn(DUP);
                be.visit(mv);
                if (be instanceof BytecodeSpreadExpr)
                    mv.visitMethodInsn(INVOKEINTERFACE,"java/util/Collection","addAll","(Ljava/util/Collection;)Z");
                else {
                    box(be.getType(), mv);
                    mv.visitMethodInsn(INVOKEINTERFACE,"java/util/Collection","add","(Ljava/lang/Object;)Z");
                }
                mv.visitInsn(POP);
            }
        }
    }
}
