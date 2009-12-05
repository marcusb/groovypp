package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.UnaryMinusExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.objectweb.asm.MethodVisitor;

public class UnaryMinusExpressionTransformer extends ExprTransformer<UnaryMinusExpression> {
    public Expression transform(UnaryMinusExpression exp, CompilerTransformer compiler) {
        final BytecodeExpr inner = (BytecodeExpr) compiler.transform(exp.getExpression());
        final ClassNode type = ClassHelper.getUnwrapper(inner.getType());
        if(type == ClassHelper.float_TYPE) {
            return new BytecodeExpr(exp, ClassHelper.double_TYPE) {
                protected void compile(MethodVisitor mv) {
                    inner.visit(mv);
                    if (!ClassHelper.isPrimitiveType(inner.getType()))
                        unbox(getType(), mv);

                    mv.visitInsn(F2D);
                    mv.visitInsn(DNEG);
                }
            };
        }
        else {
            if (   type == ClassHelper.byte_TYPE
                || type == ClassHelper.short_TYPE
                || type == ClassHelper.int_TYPE
                || type == ClassHelper.long_TYPE
                || type == ClassHelper.double_TYPE
                || type.equals(ClassHelper.Byte_TYPE)
                || type.equals(ClassHelper.Short_TYPE)
                || type.equals(ClassHelper.Integer_TYPE)
                || type.equals(ClassHelper.Long_TYPE)
                || type.equals(ClassHelper.Double_TYPE)) {
                return new BytecodeExpr(exp, type) {
                    protected void compile(MethodVisitor mv) {
                        inner.visit(mv);
                        if (!ClassHelper.isPrimitiveType(inner.getType()))
                            unbox(getType(), mv);

                        if (type == ClassHelper.int_TYPE || type == ClassHelper.short_TYPE || type == ClassHelper.byte_TYPE)
                            mv.visitInsn(INEG);
                        else
                            if (type == ClassHelper.long_TYPE)
                                mv.visitInsn(LNEG);
                            else
                                if (type == ClassHelper.float_TYPE)
                                    mv.visitInsn(FNEG);
                                else
                                    if (type == ClassHelper.double_TYPE)
                                        mv.visitInsn(DNEG);
                    }
                };
            }
            else {
                return compiler.transform(new MethodCallExpression(inner, "negative", new ArgumentListExpression()));
            }
        }
    }
}
