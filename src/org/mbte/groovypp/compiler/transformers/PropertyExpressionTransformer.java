package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.mbte.groovypp.compiler.AccessibilityCheck;
import org.mbte.groovypp.compiler.ClosureMethodNode;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.mbte.groovypp.compiler.bytecode.PropertyUtil;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.text.MessageFormat;

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
            type = TypeUtil.wrapSafely(exp.getObjectExpression().getType());

            Object prop = PropertyUtil.resolveGetProperty(type, propName, compiler);
            if (!checkAccessible(prop, exp, type, compiler)) return null;
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
                                protected void compile(MethodVisitor mv) {
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
                        type = TypeUtil.wrapSafely(object.getType());
                    }

                    Object prop = null;
                    if (exp.isImplicitThis()) {
                        prop = compiler.findField(type, propName);
                    }

                    if (prop == null)
                        prop = PropertyUtil.resolveGetProperty(type, propName, compiler);
                    if (!checkAccessible(prop, exp, type, compiler)) return null;
                    return PropertyUtil.createGetProperty(exp, compiler, propName, object, prop, true);
                }
            } else {
                object = (BytecodeExpr) compiler.transform(exp.getObjectExpression());
                type = TypeUtil.wrapSafely(object.getType());

                Object prop = null;
                if (exp.isImplicitThis()) {
                    prop = compiler.findField(type, propName);
                }

                if (prop == null)
                    prop = PropertyUtil.resolveGetProperty(type, propName, compiler);
                if (!checkAccessible(prop, exp, type, compiler)) return null;
                return PropertyUtil.createGetProperty(exp, compiler, propName, object, prop, true);
            }
        }
    }

    private boolean checkAccessible(Object prop, PropertyExpression exp, ClassNode type, CompilerTransformer compiler) {
        if (prop instanceof MethodNode) {
            MethodNode methodNode = (MethodNode) prop;
            if (!AccessibilityCheck.isAccessible(methodNode.getModifiers(), methodNode.getDeclaringClass(),
                    compiler.classNode, type)) {
                compiler.addError(MessageFormat.format("Cannot access method ''{0}''", methodNode.getName()),
                        exp.getProperty());
                return false;
            }
        } else if (prop instanceof FieldNode) {
            FieldNode fieldNode = (FieldNode) prop;
            if (!AccessibilityCheck.isAccessible(fieldNode.getModifiers(), fieldNode.getDeclaringClass(),
                    compiler.classNode, type)) {
                compiler.addError(MessageFormat.format("Cannot access field ''{0}''", fieldNode.getName()),
                        exp.getProperty());
                return false;
            }
        }
        return true;
    }

    private Expression inCaseOfClosure(final PropertyExpression exp, final CompilerTransformer compiler, String propName) {
        BytecodeExpr object;

        ClosureMethodNode cmn = (ClosureMethodNode) compiler.methodNode;
        ClassNode thisType = cmn.getParameters()[0].getType();
        while (thisType != null) {
            Object prop = PropertyUtil.resolveGetProperty(thisType, propName, compiler);
            if (prop != null) {
                if (!checkAccessible(prop, exp, thisType, compiler)) return null;
                final ClassNode thisTypeFinal = thisType;
                object = new BytecodeExpr(exp.getObjectExpression(), thisTypeFinal) {
                    protected void compile(MethodVisitor mv) {
                        mv.visitVarInsn(ALOAD, 0);
                        ClosureMethodNode cmn2 = (ClosureMethodNode) compiler.methodNode;
                        ClassNode curThis = cmn2.getParameters()[0].getType();
                        while (curThis != thisTypeFinal) {
                            ClassNode next = curThis.getField("$owner").getType();
                            mv.visitFieldInsn(GETFIELD, BytecodeHelper.getClassInternalName(curThis), "$owner", BytecodeHelper.getTypeDescription(next));
                            curThis = next;
                        }
                    }
                };

                return PropertyUtil.createGetProperty(exp, compiler, propName, object, prop, false);
            }

            FieldNode ownerField = thisType.getField("$owner");
            thisType = ownerField == null ? null : ownerField.getType();
        }

        compiler.addError("Can't resolve property " + propName, exp);
        return null;
    }

    private Expression transformSafe(final PropertyExpression exp, CompilerTransformer compiler) {
        final BytecodeExpr object = (BytecodeExpr) compiler.transform(exp.getObjectExpression());
        ClassNode type = TypeUtil.wrapSafely(object.getType());

        final BytecodeExpr call = (BytecodeExpr) compiler.transform(new PropertyExpression(new BytecodeExpr(object, type) {
            protected void compile(MethodVisitor mv) {
                // nothing to do
                // expect parent on stack
            }
        }, exp.getProperty()));

        return new BytecodeExpr(exp,TypeUtil.wrapSafely(call.getType())) {
            protected void compile(MethodVisitor mv) {
                object.visit(mv);
                Label nullLabel = new Label();
                mv.visitInsn(DUP);
                mv.visitJumpInsn(IFNULL, nullLabel);
                call.visit(mv);
                box(call.getType(), mv);
                mv.visitLabel(nullLabel);
                checkCast(getType(), mv);
            }
        };
    }
}
