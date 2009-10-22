package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.CastExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.ClosureUtil;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.objectweb.asm.MethodVisitor;

import java.util.List;

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

        if (exp.getExpression() instanceof ListExpression) {
            if (exp.getType().isArray()) {
                ClassNode componentType = exp.getType().getComponentType();
                ListExpression le = (ListExpression) exp.getExpression();

                List<Expression> list = le.getExpressions();
                int count = list.size();
                for (int i = 0; i != count; ++i) {
                    Expression el = list.get(i);
                    CastExpression castExpression = new CastExpression(componentType, el);
                    castExpression.setSourcePosition(el);
                    list.set(i, castExpression);
                }
            }
        }

        final BytecodeExpr expr = (BytecodeExpr) compiler.transform(exp.getExpression());

        if (expr.getType().implementsInterface(TypeUtil.TCLOSURE)) {
            List<MethodNode> one = ClosureUtil.isOneMethodAbstract(exp.getType());
            MethodNode doCall = one == null ? null : ClosureUtil.isMatch(one, expr.getType());
            if (one != null && doCall != null) {
                ClosureUtil.makeOneMethodClass(expr.getType(), exp.getType(), one, doCall);

                if (exp.getExpression() instanceof VariableExpression) {
                    VariableExpression ve = (VariableExpression) exp.getExpression();
                    if (ve.isDynamicTyped()) {
                        compiler.getLocalVarInferenceTypes().addWeak(ve, expr.getType());
                    }
                }

                return expr;
            }
        }

        if (exp.isCoerce()) {
            // a)
            final ClassNode type = TypeUtil.wrapSafely(exp.getType());
            Expression arg = ClassExpressionTransformer.newExpr(exp, type);
            return new AsType(exp, type, expr, (BytecodeExpr) arg);
        } else {
            if (TypeUtil.isNumericalType(exp.getType()) && TypeUtil.isNumericalType(expr.getType())) {
                // b)
                return new Cast(exp.getType(), expr);
            } else {
                ClassNode rtype = TypeUtil.wrapSafely(expr.getType());
                if (TypeUtil.isDirectlyAssignableFrom(exp.getType(), rtype)) {
                    // c)
                    if (rtype.equals(exp.getType()))
                        return expr;
                    else {
                        return new BytecodeExpr(expr, rtype) {
                            protected void compile(MethodVisitor mv) {
                                expr.visit(mv);
                                box(expr.getType(), mv);
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

        protected void compile(MethodVisitor mv) {
            expr.visit(mv);
            box(expr.getType(), mv);
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

        protected void compile(MethodVisitor mv) {
            expr.visit(mv);
            box(expr.getType(), mv);
            cast(TypeUtil.wrapSafely(expr.getType()), TypeUtil.wrapSafely(getType()), mv);
            unbox(getType(), mv);
        }
    }
}
