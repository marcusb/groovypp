package org.mbte.groovypp.compiler.impl.expressions;

import org.codehaus.groovy.ast.expr.GStringExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.ClassHelper;
import org.mbte.groovypp.compiler.impl.CompilerTransformer;
import org.mbte.groovypp.compiler.impl.bytecode.BytecodeExpr;

import java.util.List;

public class GStringExpressionTransformer extends ExprTransformer<GStringExpression> {
    public Expression transform(final GStringExpression exp, CompilerTransformer compiler) {
        final List list = exp.getValues();
        for (int i = 0; i != list.size(); i++)
           list.set(i, compiler.transform((Expression) list.get(i)));
        return new BytecodeExpr (exp, ClassHelper.GSTRING_TYPE) {
            protected void compile() {
                mv.visitTypeInsn(NEW, "org/codehaus/groovy/runtime/GStringImpl");
                mv.visitInsn(DUP);

                int size = exp.getValues().size();
                mv.visitLdcInsn(size);
                mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");

                for (int i = 0; i < size; i++) {
                    mv.visitInsn(DUP);
                    mv.visitLdcInsn(i);
                    final BytecodeExpr el = (BytecodeExpr) exp.getValue(i);
                    el.visit(mv);
                    box(el.getType());
                    mv.visitInsn(AASTORE);
                }

                List strings = exp.getStrings();
                size = strings.size();
                mv.visitLdcInsn(size);
                mv.visitTypeInsn(ANEWARRAY, "java/lang/String");

                for (int i = 0; i < size; i++) {
                    mv.visitInsn(DUP);
                    mv.visitLdcInsn(i);
                    mv.visitLdcInsn(((ConstantExpression) strings.get(i)).getValue());
                    mv.visitInsn(AASTORE);
                }

                mv.visitMethodInsn(INVOKESPECIAL, "org/codehaus/groovy/runtime/GStringImpl", "<init>", "([Ljava/lang/Object;[Ljava/lang/String;)V");
            }
        };
    }
}
