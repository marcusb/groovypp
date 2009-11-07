package org.mbte.groovypp.compiler.transformers;

import groovy.lang.TypePolicy;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.mbte.groovypp.compiler.*;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.mbte.groovypp.compiler.bytecode.ResolvedMethodBytecodeExpr;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.util.List;
import java.util.ArrayList;

public class MethodCallExpressionTransformer extends ExprTransformer<MethodCallExpression> {
    public Expression transform(final MethodCallExpression exp, final CompilerTransformer compiler) {
        Expression args = compiler.transform(exp.getArguments());
        exp.setArguments(args);

        if (exp.isSpreadSafe()) {
            compiler.addError("Spread operator is not supported by static compiler", exp);
            return null;
        }

        if (exp.isSafe()) {
            return transformSafe(exp, compiler);
        }

        Object method = exp.getMethod();
        String methodName;
        if (!(method instanceof ConstantExpression) || !(((ConstantExpression) method).getValue() instanceof String)) {
            if (compiler.policy == TypePolicy.STATIC) {
                compiler.addError("Non-static method name", exp);
                return null;
            } else {
                return createDynamicCall(exp, compiler);
            }
        } else {
            methodName = (String) ((ConstantExpression) method).getValue();
        }

        BytecodeExpr object;
        ClassNode type;
        MethodNode foundMethod;
        final ClassNode[] argTypes = compiler.exprToTypeArray(args);

        if (exp.getObjectExpression() instanceof ClassExpression) {
            type = TypeUtil.wrapSafely(exp.getObjectExpression().getType());
            foundMethod = findMethodWithClosureCoercion(type, methodName, argTypes, compiler);
            if (foundMethod == null || !foundMethod.isStatic()) {
                return dynamicOrError(exp, compiler, methodName, type, argTypes, "Cannot find static method ");
            }
            if (!AccessibilityCheck.isAccessible(foundMethod.getModifiers(),
                    foundMethod.getDeclaringClass(), compiler.classNode, type)) {
                return dynamicOrError(exp, compiler, methodName, type, argTypes, "Cannot access method ");
            }
            return createCall(exp, compiler, args, null, foundMethod);
        } else {
            if (exp.getObjectExpression().equals(VariableExpression.THIS_EXPRESSION)) {
                ClassNode thisType = compiler.methodNode.getDeclaringClass();
                while (thisType != null) {
                    foundMethod = findMethodWithClosureCoercion(thisType, methodName, argTypes, compiler);

                    if (foundMethod != null) {
                        final ClassNode thisTypeFinal = thisType;
                        object = new BytecodeExpr(exp.getObjectExpression(), thisTypeFinal) {
                            protected void compile(MethodVisitor mv) {
                                mv.visitVarInsn(ALOAD, 0);
                                ClassNode curThis = compiler.methodNode.getDeclaringClass();
                                while (curThis != thisTypeFinal) {
                                    ClassNode next = curThis.getField("$owner").getType();
                                    mv.visitFieldInsn(GETFIELD, BytecodeHelper.getClassInternalName(curThis), "$owner", BytecodeHelper.getTypeDescription(next));
                                    curThis = next;
                                }
                            }
                        };

                        if (!AccessibilityCheck.isAccessible(foundMethod.getModifiers(),
                                foundMethod.getDeclaringClass(), compiler.classNode, thisType)) {
                            return dynamicOrError(exp, compiler, methodName, thisType, argTypes, "Cannot access method ");
                        }

                        return createCall(exp, compiler, args, object, foundMethod);
                    }

                    FieldNode ownerField = thisType.getField("$owner");
                    thisType = ownerField == null ? null : ownerField.getType();
                }

                return dynamicOrError(exp, compiler, methodName, compiler.classNode, argTypes, "Cannot find method ");
            } else {
                object = (BytecodeExpr) compiler.transform(exp.getObjectExpression());
                type = TypeUtil.wrapSafely(object.getType());

                foundMethod = findMethodWithClosureCoercion(type, methodName, argTypes, compiler);

                if (foundMethod == null) {
                    if (TypeUtil.isAssignableFrom(TypeUtil.TCLOSURE, object.getType())) {
                        foundMethod = findMethodWithClosureCoercion(ClassHelper.CLOSURE_TYPE, methodName, argTypes, compiler);
                        if (foundMethod != null) {
                            ClosureUtil.improveClosureType(object.getType(), ClassHelper.CLOSURE_TYPE);
                            return createCall(exp, compiler, args, object, foundMethod);
                        }
                    }

                    return dynamicOrError(exp, compiler, methodName, type, argTypes, "Cannot find method ");
                }

                if (!AccessibilityCheck.isAccessible(foundMethod.getModifiers(),
                        foundMethod.getDeclaringClass(), compiler.classNode, type)) {
                    return dynamicOrError(exp, compiler, methodName, type, argTypes, "Cannot access method ");
                }

                return createCall(exp, compiler, args, object, foundMethod);
            }
        }
    }

    private Expression createCall(MethodCallExpression exp, CompilerTransformer compiler, Expression args, BytecodeExpr object, MethodNode foundMethod) {
        if (!foundMethod.isStatic() && foundMethod.getReturnType().equals(ClassHelper.VOID_TYPE)) {
            final ResolvedMethodBytecodeExpr call = new ResolvedMethodBytecodeExpr(exp, foundMethod, object, (ArgumentListExpression) args, compiler);
            return new BytecodeExpr(object, TypeUtil.NULL_TYPE) {
                protected void compile(MethodVisitor mv) {
                    call.visit(mv);
                    mv.visitInsn(ACONST_NULL);
                }
            };
        }
        else
            return new ResolvedMethodBytecodeExpr(exp, foundMethod, object, (ArgumentListExpression) args, compiler);
    }

    private Expression transformSafe(MethodCallExpression exp, CompilerTransformer compiler) {
        final BytecodeExpr object = (BytecodeExpr) compiler.transform(exp.getObjectExpression());
        ClassNode type = TypeUtil.wrapSafely(object.getType());

        MethodCallExpression callExpression = new MethodCallExpression(new BytecodeExpr(object, type) {
            protected void compile(MethodVisitor mv) {
            }
        }, exp.getMethod(), exp.getArguments());
        callExpression.setSourcePosition(exp);
        final BytecodeExpr call = (BytecodeExpr) compiler.transform(callExpression);

        return new BytecodeExpr(exp, TypeUtil.wrapSafely(call.getType())) {
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

    private Expression dynamicOrError(MethodCallExpression exp, CompilerTransformer compiler, String methodName, ClassNode type, ClassNode[] argTypes, final String msg) {
        if (compiler.policy == TypePolicy.STATIC) {
            compiler.addError(msg + getMethodDescr(type, methodName, argTypes), exp.getMethod());
            return null;
        } else
            return createDynamicCall(exp, compiler);
    }

    private String getMethodDescr(ClassNode type, String methodName, ClassNode[] argTypes) {
        StringBuilder sb = new StringBuilder();
        getPresentableText(type, sb);
        sb.append(".");
        sb.append(methodName);
        sb.append("(");
        for (int i = 0; i != argTypes.length; i++) {
            if (i != 0)
                sb.append(", ");
            if (argTypes[i] != null)
                sb.append(argTypes[i].getName());
            else
                sb.append("null");
        }
        sb.append(")");
        return sb.toString();
    }

    private void getPresentableText(ClassNode type, StringBuilder builder) {
        builder.append(type.getName());
        GenericsType[] generics = type.getGenericsTypes();
        if (generics != null && generics.length > 0) {
            builder.append("<");
            for (int i = 0; i < generics.length; i++) {
                if (i > 0) builder.append(",");
                if (generics[i].isWildcard()) {
                    if (TypeUtil.isExtends(generics[i])) {
                        builder.append("? extends ");
                    } else if (TypeUtil.isSuper(generics[i])) {
                        builder.append("? super ");
                    } else {
                        builder.append("?");
                        continue;
                    }
                }
                getPresentableText(generics[i].getType(), builder);
            }
            builder.append(">");
        }
    }

    private Expression createDynamicCall(final MethodCallExpression exp, CompilerTransformer compiler) {
        final BytecodeExpr methodExpr = (BytecodeExpr) compiler.transform(exp.getMethod());
        final BytecodeExpr object = (BytecodeExpr) compiler.transform(exp.getObjectExpression());
        return new BytecodeExpr(exp, ClassHelper.OBJECT_TYPE) {
            protected void compile(MethodVisitor mv) {
                mv.visitInsn(ACONST_NULL);
                object.visit(mv);
                box(object.getType(), mv);

                methodExpr.visit(mv);

                final List args = ((ArgumentListExpression) exp.getArguments()).getExpressions();
                mv.visitLdcInsn(args.size());
                mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
                for (int j = 0; j != args.size(); ++j) {
                    mv.visitInsn(DUP);
                    mv.visitLdcInsn(j);
                    ((BytecodeExpr) args.get(j)).visit(mv);
                    mv.visitInsn(AASTORE);
                }
                mv.visitMethodInsn(INVOKESTATIC, "org/codehaus/groovy/runtime/ScriptBytecodeAdapter", "invokeMethodN", "(Ljava/lang/Class;Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;");
            }
        };
    }

    private MethodNode findMethodVariatingArgs(ClassNode type, String methodName, ClassNode[] argTypes, CompilerTransformer compiler, int firstNonVariating) {
        MethodNode foundMethod = compiler.findMethod(type, methodName, argTypes);
        if (foundMethod != null) {
            return foundMethod;
        }

        if (argTypes.length > 0) {
            for (int i=firstNonVariating+1; i < argTypes.length; ++i) {
                final ClassNode oarg = argTypes[i];
                if (oarg.implementsInterface(TypeUtil.TCLOSURE) || oarg.equals(TypeUtil.EX_LINKED_HASH_MAP_TYPE)) {
                    foundMethod = findMethodVariatingArgs(type, methodName, argTypes, compiler, i);

                    if (foundMethod != null) {
                        Parameter p[] = foundMethod.getParameters();
                        if (p.length == argTypes.length) {
                            return foundMethod;
                        }
                    }

                    argTypes[i] = null;
                    foundMethod = findMethodVariatingArgs(type, methodName, argTypes, compiler, i);
                    if (foundMethod != null) {
                        Parameter p[] = foundMethod.getParameters();
                        if (p.length == argTypes.length) {
                            ClassNode argType = p[i].getType();
                            if (argType.equals(ClassHelper.CLOSURE_TYPE)) {
                                ClosureUtil.improveClosureType(oarg, ClassHelper.CLOSURE_TYPE);
                                StaticMethodBytecode.replaceMethodCode(compiler.su, ((ClosureClassNode)oarg).getDoCallMethod(), compiler.compileStack, compiler.debug == -1 ? -1 : compiler.debug+1, compiler.policy, compiler.classNode.getName());
                            }
                            else {
                                List<MethodNode> one = ClosureUtil.isOneMethodAbstract(argType);
                                GenericsType[] methodTypeVars = foundMethod.getGenericsTypes();
                                if (methodTypeVars != null && methodTypeVars.length > 0) {
                                    ArrayList<ClassNode> formals = new ArrayList<ClassNode> (2);
                                    ArrayList<ClassNode> instantiateds = new ArrayList<ClassNode> (2);

                                    if (!foundMethod.isStatic()) {
                                        formals.add(foundMethod.getDeclaringClass());
                                        instantiateds.add(type);
                                    }

                                    for (int j = 0; j != i; j++) {
                                        formals.add(foundMethod.getParameters()[j].getType());
                                        instantiateds.add(argTypes[j]);
                                    }
                                    
                                    ClassNode[] unified = TypeUnification.inferTypeArguments(methodTypeVars,
                                            formals.toArray(new ClassNode[formals.size()]),
                                            instantiateds.toArray(new ClassNode[instantiateds.size()]));
                                    argType = TypeUtil.getSubstitutedType(argType, foundMethod, unified);
                                }
                                MethodNode doCall = one == null ? null : ClosureUtil.isMatch(one, (ClosureClassNode) oarg, compiler, argType);
                                if (one == null || doCall == null) {
                                    foundMethod = null;
                                } else {
                                    ClosureUtil.makeOneMethodClass(oarg, argType, one, doCall);
                                }
                            }
                        }
                    }
                    argTypes[i] = oarg;
                    return foundMethod;
                }
            }
        }
        return null;
    }

    private MethodNode findMethodWithClosureCoercion(ClassNode type, String methodName, ClassNode[] argTypes, CompilerTransformer compiler) {
        return findMethodVariatingArgs(type, methodName, argTypes, compiler, -1);
    }
}
