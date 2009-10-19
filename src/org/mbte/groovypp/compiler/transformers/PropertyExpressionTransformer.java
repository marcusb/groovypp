package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.GenericsType;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.mbte.groovypp.compiler.ClosureMethodNode;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.mbte.groovypp.compiler.bytecode.PropertyUtil;
import org.objectweb.asm.Label;

public class PropertyExpressionTransformer extends ExprTransformer<PropertyExpression> {
    public Expression transform(PropertyExpression exp, CompilerTransformer compiler) {
        if (exp.isSpreadSafe()) {
            compiler.addError("Spread operator is not supported yet by static compiler", exp);
            return null;
        }

        if (exp.isSafe()) {
            return transformSafe(exp, compiler);
        }

        Object property = exp.getProperty();
        String propName = null;
        if (!(property instanceof ConstantExpression) || !(((ConstantExpression) property).getValue() instanceof String)) {
            compiler.addError("Non-static property name", exp);
            return null;
        } else {
            propName = (String) ((ConstantExpression) property).getValue();
        }

        final BytecodeExpr object;
        final ClassNode type;

        if (exp.getObjectExpression() instanceof ClassExpression) {
            object = null;
            type = ClassHelper.getWrapper(exp.getObjectExpression().getType());

            Object prop = PropertyUtil.resolveGetProperty(type, propName, compiler);

            return PropertyUtil.createGetProperty(exp, compiler, propName, object, prop, true);
        } else {
            if (exp.getObjectExpression().equals(VariableExpression.THIS_EXPRESSION)) {
                if (compiler.methodNode instanceof ClosureMethodNode) {
                    return inCaseOfClosure(exp, compiler, propName);
                } else {
                    if (compiler.methodNode.isStatic()) {
                        Parameter[] pp = compiler.methodNode.getParameters();
                        if (pp.length > 0 && "$self".equals(pp[0].getName())) {
                            object = new BytecodeExpr(exp, pp[0].getType()) {
                                protected void compile() {
                                    mv.visitVarInsn(ALOAD, 0);
                                }
                            };
                            type = object.getType();
                        } else {
                            object = null;
                            type = compiler.classNode;
                        }
                    } else {
                        object = (BytecodeExpr) compiler.transform(exp.getObjectExpression());
                        type = ClassHelper.getWrapper(object.getType());
                    }

                    Object prop = null;
                    if (exp.isImplicitThis()) {
                        prop = compiler.findField(type, propName);
                    }

                    if (prop == null)
                        prop = PropertyUtil.resolveGetProperty(type, propName, compiler);
                    return PropertyUtil.createGetProperty(exp, compiler, propName, object, prop, true);
                }
            } else {
                object = (BytecodeExpr) compiler.transform(exp.getObjectExpression());
                type = ClassHelper.getWrapper(object.getType());

                Object prop = null;
                if (exp.isImplicitThis()) {
                    prop = compiler.findField(type, propName);
                }

                if (prop == null)
                    prop = PropertyUtil.resolveGetProperty(type, propName, compiler);
                return PropertyUtil.createGetProperty(exp, compiler, propName, object, prop, true);
            }
        }
    }

    private Expression inCaseOfClosure(final PropertyExpression exp, final CompilerTransformer compiler, String propName) {
        BytecodeExpr object;
        int level = 0;
        for (ClosureMethodNode cmn = (ClosureMethodNode) compiler.methodNode; cmn != null; cmn = cmn.getOwner(), level++) {
            ClassNode thisType = cmn.getParameters()[0].getType();

            Object prop = PropertyUtil.resolveGetProperty(thisType, propName, compiler);
            if (prop != null) {
                final int level1 = level;
                object = new BytecodeExpr(exp.getObjectExpression(), thisType) {
                    protected void compile() {
                        mv.visitVarInsn(ALOAD, 0);
                        for (int i = 0; i != level1; ++i) {
                            mv.visitTypeInsn(CHECKCAST, "groovy/lang/OwnerAware");
                            mv.visitMethodInsn(INVOKEINTERFACE, "groovy/lang/OwnerAware", "getOwner", "()Ljava/lang/Object;");
                        }
                        mv.visitTypeInsn(CHECKCAST, BytecodeHelper.getClassInternalName(getType()));
                    }
                };

                return PropertyUtil.createGetProperty(exp, compiler, propName, object, prop, false);
            }

            // checkDelegate
            if (thisType.implementsInterface(TypeUtil.TCLOSURE)) {
                final ClassNode tclosure = thisType.getInterfaces()[0];
                final GenericsType[] genericsTypes = tclosure.getGenericsTypes();
                if (genericsTypes != null) {
                    final ClassNode delegateType = genericsTypes[0].getType();
                    prop = PropertyUtil.resolveGetProperty(delegateType, propName, compiler);
                    if (prop != null) {
                        final int level3 = level;
                        object = new BytecodeExpr(exp.getObjectExpression(), delegateType) {
                            protected void compile() {
                                mv.visitVarInsn(ALOAD, 0);
                                for (int i = 0; i != level3; ++i) {
                                    mv.visitTypeInsn(CHECKCAST, "groovy/lang/OwnerAware");
                                    mv.visitMethodInsn(INVOKEINTERFACE, "groovy/lang/OwnerAware", "getOwner", "()Ljava/lang/Object;");
                                }
                                mv.visitTypeInsn(CHECKCAST, "groovy/lang/Closure");
                                mv.visitMethodInsn(INVOKEVIRTUAL, "groovy/lang/Closure", "getDelegate", "()Ljava/lang/Object;");
                                mv.visitTypeInsn(CHECKCAST, BytecodeHelper.getClassInternalName(getType()));
                            }
                        };
                        return PropertyUtil.createGetProperty(exp, compiler, propName, object, prop, false);
                    }
                }
            }
        }

        Object prop = PropertyUtil.resolveGetProperty(compiler.classNode, propName, compiler);
        if (prop != null) {
            final int level2 = level;
            object = new BytecodeExpr(exp.getObjectExpression(), compiler.classNode) {
                protected void compile() {
                    mv.visitVarInsn(ALOAD, 0);
                    for (int i = 0; i != level2; ++i) {
                        mv.visitTypeInsn(CHECKCAST, "groovy/lang/OwnerAware");
                        mv.visitMethodInsn(INVOKEINTERFACE, "groovy/lang/OwnerAware", "getOwner", "()Ljava/lang/Object;");
                    }
                    mv.visitTypeInsn(CHECKCAST, BytecodeHelper.getClassInternalName(getType()));
                }
            };
            return PropertyUtil.createGetProperty(exp, compiler, propName, object, prop, false);
        }

        compiler.addError("Can't resolve property " + propName, exp);
        return null;
    }

    private Expression transformSafe(final PropertyExpression exp, CompilerTransformer compiler) {
        final BytecodeExpr object = (BytecodeExpr) compiler.transform(exp.getObjectExpression());
        ClassNode type = ClassHelper.getWrapper(object.getType());

        final BytecodeExpr call = (BytecodeExpr) compiler.transform(new PropertyExpression(new BytecodeExpr(object, type) {
            protected void compile() {
                // nothing to do
                // expect parent on stack
            }
        }, exp.getProperty()));

        return new BytecodeExpr(exp, ClassHelper.getWrapper(call.getType())) {
            protected void compile() {
                object.visit(mv);
                Label nullLabel = new Label();
                mv.visitInsn(DUP);
                mv.visitJumpInsn(IFNULL, nullLabel);
                call.visit(mv);
                box(call.getType());
                mv.visitLabel(nullLabel);
            }
        };
    }
}
