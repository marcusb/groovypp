package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.mbte.groovypp.compiler.AccessibilityCheck;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.mbte.groovypp.compiler.bytecode.PropertyUtil;
import org.mbte.groovypp.compiler.bytecode.ResolvedMethodBytecodeExpr;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.text.MessageFormat;

public class PropertyExpressionTransformer extends ExprTransformer<PropertyExpression> {
    public Expression transform(PropertyExpression exp, CompilerTransformer compiler) {
        Expression originalProperty = exp.getProperty();
        if (exp.isSpreadSafe()) {
            Parameter param = new Parameter(ClassHelper.OBJECT_TYPE, "$it");
            VariableExpression ve = new VariableExpression(param);
            ve.setSourcePosition(originalProperty);
            PropertyExpression prop = new PropertyExpression(ve, originalProperty);
            prop.setSourcePosition(originalProperty);
            ReturnStatement retStat = new ReturnStatement(prop);
            retStat.setSourcePosition(originalProperty);
            ClosureExpression ce = new ClosureExpression(new Parameter[]{param}, retStat);
            ce.setVariableScope(new VariableScope(compiler.compileStack.getScope()));
            MethodCallExpression mce = new MethodCallExpression(exp.getObjectExpression(), "map", new ArgumentListExpression(ce));
            mce.setSourcePosition(exp);
            return compiler.transform(mce);
        }

        if (exp.isSafe()) {
            return transformSafe(exp, compiler);
        }

        String propName;
        if (!(originalProperty instanceof ConstantExpression) || !(((ConstantExpression) originalProperty).getValue() instanceof String)) {
            compiler.addError("Non-static property name", exp);
            return null;
        } else {
            propName = (String) ((ConstantExpression) originalProperty).getValue();
        }

        BytecodeExpr object;
        final ClassNode type;

        if (exp.getObjectExpression() instanceof ClassExpression) {
            Object prop = PropertyUtil.resolveGetProperty(ClassHelper.CLASS_Type, propName, compiler, false);
            if (prop != null) {
                type = ClassHelper.CLASS_Type;
                if (!checkAccessible(prop, exp, type, compiler)) return null;
                object = (BytecodeExpr) compiler.transform(exp.getObjectExpression());
                return PropertyUtil.createGetProperty(exp, compiler, propName, object, prop, true);
            }
            else {
                type = TypeUtil.wrapSafely(exp.getObjectExpression().getType());
                prop = PropertyUtil.resolveGetProperty(type, propName, compiler, true);
                if (!checkAccessible(prop, exp, type, compiler)) return null;
                object = null;
                return PropertyUtil.createGetProperty(exp, compiler, propName, object, prop, true);
            }
        } else {
            if (exp.getObjectExpression().equals(VariableExpression.THIS_EXPRESSION)) {
                if ((compiler.classNode instanceof InnerClassNode) && !compiler.classNode.isStaticClass()) {
                    return inCaseOfInner(exp, compiler, propName);
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
                        prop = PropertyUtil.resolveGetProperty(type, propName, compiler, false);
                    if (!checkAccessible(prop, exp, type, compiler)) return null;
                    return PropertyUtil.createGetProperty(exp, compiler, propName, object, prop, true);
                }
            } else {
                object = (BytecodeExpr) compiler.transform(exp.getObjectExpression());
                type = object.getType();

                Object prop = null;
                if (exp.isImplicitThis()) {
                    prop = compiler.findField(type, propName);
                }

                if (prop == null)
                    prop = PropertyUtil.resolveGetProperty(type, propName, compiler, false);
                if (prop == null) {
                    MethodNode unboxing = TypeUtil.getReferenceUnboxingMethod(type);
                        if (unboxing != null) {
                            ClassNode t = TypeUtil.getSubstitutedType(unboxing.getReturnType(), unboxing.getDeclaringClass(), type);
                            prop = PropertyUtil.resolveGetProperty(t, propName, compiler, false);
                            if (prop != null) {
                                object = ResolvedMethodBytecodeExpr.create(exp, unboxing, object,
                                        new ArgumentListExpression(), compiler);
                            }
                        }
                }
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

    private Expression inCaseOfInner(final PropertyExpression exp, final CompilerTransformer compiler, String propName) {
        BytecodeExpr object;

        ClassNode thisType = compiler.classNode;
        while (thisType != null) {
            Object prop = PropertyUtil.resolveGetProperty(thisType, propName, compiler, false);
            if (prop != null) {
                if (!checkAccessible(prop, exp, thisType, compiler)) return null;
                boolean isStatic = PropertyUtil.isStatic(prop);
                if (!isStatic && exp.isStatic()) return null;
                final ClassNode thisTypeFinal = thisType;
                object = isStatic ? null : new BytecodeExpr(exp.getObjectExpression(), thisTypeFinal) {
                    protected void compile(MethodVisitor mv) {
                        mv.visitVarInsn(ALOAD, 0);
                        ClassNode curThis = compiler.methodNode.getDeclaringClass();
                        while (curThis != thisTypeFinal) {
                            compiler.context.setOuterClassInstanceUsed(curThis);
                            ClassNode next = curThis.getField("this$0").getType();
                            mv.visitFieldInsn(GETFIELD, BytecodeHelper.getClassInternalName(curThis), "this$0", BytecodeHelper.getTypeDescription(next));
                            curThis = next;
                        }
                    }
                };

                return PropertyUtil.createGetProperty(exp, compiler, propName, object, prop, false);
            }

            thisType = thisType.getOuterClass();
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

        if (ClassHelper.isPrimitiveType(call.getType())) {
            return new BytecodeExpr(exp,call.getType()) {
                protected void compile(MethodVisitor mv) {
                    Label nullLabel = new Label(), endLabel = new Label ();

                    object.visit(mv);
                    mv.visitInsn(DUP);
                    mv.visitJumpInsn(IFNULL, nullLabel);

                    call.visit(mv);
                    mv.visitJumpInsn(GOTO, endLabel);

                    mv.visitLabel(nullLabel);
                    mv.visitInsn(POP);

                    if (call.getType() == ClassHelper.long_TYPE) {
                        mv.visitInsn(LCONST_0);
                    } else
                    if (call.getType() == ClassHelper.float_TYPE) {
                        mv.visitInsn(FCONST_0);
                    } else
                    if (call.getType() == ClassHelper.double_TYPE) {
                        mv.visitInsn(DCONST_0);
                    } else
                        mv.visitInsn(ICONST_0);

                    mv.visitLabel(endLabel);
                }
            };
        }
        else {
            return new BytecodeExpr(exp,call.getType()) {
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
}
