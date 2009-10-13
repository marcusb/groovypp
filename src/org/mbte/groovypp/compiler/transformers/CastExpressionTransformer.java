package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.CastExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;

/**
 * Cast processing rules:
 * a) as operator always go via asType method
 * b) both numerical types always goes via primitive types
 * c) if we cast staticly to lower type we keep upper (except if upper NullType)
 * d) otherwise we do direct cast
 * e) primitive types goes via boxing and Number
 */
public class CastExpressionTransformer extends ExprTransformer<CastExpression> {
    public BytecodeExpr transform(final CastExpression exp, CompilerTransformer compiler) {

        final BytecodeExpr expr = (BytecodeExpr) compiler.transform(exp.getExpression());

        if (exp.isCoerce()) {
            // a)
            final ClassNode type = ClassHelper.getWrapper(exp.getType());
            Expression arg = new ClassExpression(type);
            arg = compiler.transform(arg);
            arg.setType(ClassHelper.CLASS_Type);

            final BytecodeExpr arg1 = (BytecodeExpr) arg;
            return new AsType(exp, type, expr, arg1);
        } else {
            if (TypeUtil.isNumericalType(exp.getType()) && TypeUtil.isNumericalType(expr.getType())) {
                // b)
                return new Cast(exp.getType(), expr);
            } else {
                ClassNode rtype = ClassHelper.getWrapper(expr.getType());
                if (TypeUtil.isDirectlyAssignableFrom(exp.getType(), rtype)) {
                    // c)
                    if (rtype.equals(exp.getType()))
                        return expr;
                    else {
                        return new BytecodeExpr(expr, rtype) {
                            protected void compile() {
                                expr.visit(mv);
                                box(expr.getType());
                            }
                        };
                    }
                } else {
                    // d
                    if (exp.getExpression() instanceof VariableExpression) {
                        VariableExpression ve = (VariableExpression) exp.getExpression();
                        if (ve.isDynamicTyped()) {
                            compiler.getLocalVarInferenceTypes().addWeak(ve, expr.getType());
                        }
                    }

                    return new Cast(exp.getType(), expr);
                }
            }
        }
    }

    private static class AsType extends BytecodeExpr {
        private final BytecodeExpr expr;
        private final BytecodeExpr arg1;

        public AsType(CastExpression exp, ClassNode type, BytecodeExpr expr, BytecodeExpr arg1) {
            super(exp, type);
            this.expr = expr;
            this.arg1 = arg1;
        }

        protected void compile() {
            expr.visit(mv);
            box(expr.getType());
            arg1.visit(mv);
            mv.visitMethodInsn(INVOKESTATIC, "org/codehaus/groovy/runtime/ScriptBytecodeAdapter", "asType", "(Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;");
            mv.visitTypeInsn(CHECKCAST, BytecodeHelper.getClassInternalName(getType()));
        }
    }

    public static class Cast extends BytecodeExpr {
        private final BytecodeExpr expr;

        public Cast(ClassNode type, BytecodeExpr expr) {
            super(expr, type);
            this.expr = expr;
        }

        protected void compile() {
            expr.visit(mv);
            box(expr.getType());
            expr.cast(ClassHelper.getWrapper(expr.getType()), ClassHelper.getWrapper(getType()));
            unbox(getType());
        }
    }
}
