/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbte.groovypp.compiler.transformers;

import groovy.lang.TypePolicy;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.PresentationUtil;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.bytecode.*;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

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
            if (originalProperty instanceof ClosureExpression) {
                final MethodCallExpression mce = new MethodCallExpression(exp.getObjectExpression(), "apply", new ArgumentListExpression(originalProperty));
                mce.setSourcePosition(exp);
                return compiler.transform(mce);
            }
            else {
                compiler.addError("Non-static property name", originalProperty);
                return null;
            }
        } else {
            propName = (String) ((ConstantExpression) originalProperty).getValue();
        }

        BytecodeExpr object;
        final ClassNode type;

        if (exp.getObjectExpression() instanceof ClassExpression) {
            Object prop = PropertyUtil.resolveGetProperty(ClassHelper.CLASS_Type, propName, compiler, false, false);
            if (prop != null) {
                type = ClassHelper.CLASS_Type;
                object = (BytecodeExpr) compiler.transform(exp.getObjectExpression());
                return PropertyUtil.createGetProperty(exp, compiler, propName, type, object, prop);
            }
            else {
                type = TypeUtil.wrapSafely(exp.getObjectExpression().getType());
                prop = PropertyUtil.resolveGetProperty(type, propName, compiler, true, false);
                object = null;
                return PropertyUtil.createGetProperty(exp, compiler, propName, type, object, prop);
            }
        } else {
            if (exp.getObjectExpression() instanceof VariableExpression &&
                    ((VariableExpression) exp.getObjectExpression()).getName().equals("this")) {
                if ((compiler.classNode instanceof InnerClassNode)) {
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

                    Object prop = PropertyUtil.resolveGetProperty(type, propName, compiler, false, object != null && object.isThis());
                    return PropertyUtil.createGetProperty(exp, compiler, propName, type, object, prop);
                }
            } else {
                object = (BytecodeExpr) compiler.transformToGround(exp.getObjectExpression());
                type = object.getType();

                Object prop = PropertyUtil.resolveGetProperty(type, propName, compiler, false,
                        object.isThis());
                if (prop == null) {
                    MethodNode unboxing = TypeUtil.getReferenceUnboxingMethod(type);
                        if (unboxing != null) {
                            ClassNode t = TypeUtil.getSubstitutedType(unboxing.getReturnType(), unboxing.getDeclaringClass(), type);
                            prop = PropertyUtil.resolveGetProperty(t, propName, compiler, false, false);
                            if (prop != null) {
                                object = ResolvedMethodBytecodeExpr.create(exp, unboxing, object,
                                        new ArgumentListExpression(), compiler);
                            }
                        }
                }
                return PropertyUtil.createGetProperty(exp, compiler, propName, type, object, prop);
            }
        }
    }


    private Expression inCaseOfInner(final PropertyExpression exp, final CompilerTransformer compiler, String propName) {
        BytecodeExpr object;

        ClassNode thisType = compiler.classNode;
        boolean isThis = true;
        boolean onlyStatic = false;
        while (thisType != null) {
            Object prop = PropertyUtil.resolveGetProperty(thisType, propName, compiler, onlyStatic, isThis);
            if (prop != null) {
                boolean isStatic = PropertyUtil.isStatic(prop);
                if (!isStatic && exp.isStatic()) return null;
                object = isStatic ? null : new InnerThisBytecodeExpr(exp, thisType, compiler);

                return PropertyUtil.createGetProperty(exp, compiler, propName, thisType, object, prop);
            }

            isThis = false;

            if (thisType.implementsInterface(TypeUtil.DELEGATING)) {
                final MethodNode gd = compiler.findMethod(thisType, "getDelegate", ClassNode.EMPTY_ARRAY, false);
                if (gd != null) {
                    final InnerThisBytecodeExpr innerThis = new InnerThisBytecodeExpr(exp, thisType, compiler);
                    final ResolvedMethodBytecodeExpr delegate = ResolvedMethodBytecodeExpr.create(exp, gd, innerThis, ArgumentListExpression.EMPTY_ARGUMENTS, compiler);
                    prop = PropertyUtil.resolveGetProperty(delegate.getType(), propName, compiler, onlyStatic, false);
                    if (prop != null) {
                        boolean isStatic = PropertyUtil.isStatic(prop);
                        if (!isStatic && exp.isStatic()) return null;
                        object = isStatic ? null : delegate;

                        return PropertyUtil.createGetProperty(exp, compiler, propName, delegate.getType(), object, prop);
                    }
                }
            }

            onlyStatic |= (thisType.getModifiers() & Opcodes.ACC_STATIC) != 0;
            thisType = thisType.getOuterClass();
        }

        if (compiler.policy == TypePolicy.STATIC) {
            compiler.addError(MessageFormat.format("Cannot resolve property {0}.{1}",
                    PresentationUtil.getText(compiler.classNode),
                    propName), exp);
            return null;
        }
        else {
            object = new InnerThisBytecodeExpr(exp, compiler.classNode, compiler);
            return new UnresolvedLeftExpr(exp, null, object, propName);
        }
    }

    private Expression transformSafe(final PropertyExpression exp, CompilerTransformer compiler) {
        final BytecodeExpr object = (BytecodeExpr) compiler.transform(exp.getObjectExpression());
        ClassNode type = TypeUtil.wrapSafely(object.getType());

        final PropertyExpression newExp = new PropertyExpression(new BytecodeExpr(object, type) {
            protected void compile(MethodVisitor mv) {
                // nothing to do
                // expect parent on stack
            }
        }, exp.getProperty());
        newExp.setSourcePosition(exp);
        final BytecodeExpr call = (BytecodeExpr) compiler.transform(newExp);

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
