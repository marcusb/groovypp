package org.mbte.groovypp.compiler.transformers;

import groovy.lang.TypePolicy;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.mbte.groovypp.compiler.*;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.mbte.groovypp.compiler.bytecode.ResolvedMethodBytecodeExpr;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.util.List;

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
                return dynamicOrError(exp, compiler, methodName, type, argTypes, "Can't find static method ");
            }

            return createCall(exp, compiler, args, null, foundMethod);
        } else {
            if (exp.getObjectExpression().equals(VariableExpression.THIS_EXPRESSION) && compiler.methodNode instanceof ClosureMethodNode) {
                int level = 0;
                for (ClosureMethodNode cmn = (ClosureMethodNode) compiler.methodNode; cmn != null; cmn = cmn.getOwner(), level++) {
                    ClassNode thisType = cmn.getParameters()[0].getType();
                    foundMethod = findMethodWithClosureCoercion(thisType, methodName, argTypes, compiler);
                    if (foundMethod != null) {
                        final int level1 = level;
                        object = new BytecodeExpr(exp.getObjectExpression(), thisType) {
                            protected void compile(MethodVisitor mv) {
                                mv.visitVarInsn(ALOAD, 0);
                                for (int i = 0; i != level1; ++i) {
                                    mv.visitTypeInsn(CHECKCAST, "groovy/lang/OwnerAware");
                                    mv.visitMethodInsn(INVOKEINTERFACE, "groovy/lang/OwnerAware", "getOwner", "()Ljava/lang/Object;");
                                }
                                BytecodeExpr.checkCast(getType(), mv);
                            }
                        };
                        return createCall(exp, compiler, args, object, foundMethod);
                    }

                    // checkDelegate
                    if (thisType.implementsInterface(TypeUtil.TCLOSURE)) {
                        final ClassNode tclosure = thisType.getInterfaces()[0];
                        final GenericsType[] genericsTypes = tclosure.getGenericsTypes();
                        if (genericsTypes != null) {
                            final ClassNode delegateType = genericsTypes[0].getType();
                            foundMethod = compiler.findMethod(delegateType, methodName, argTypes);
                            if (foundMethod != null) {
                                final int level3 = level;
                                object = new BytecodeExpr(exp.getObjectExpression(), delegateType) {
                                    protected void compile(MethodVisitor mv) {
                                        mv.visitVarInsn(ALOAD, 0);
                                        for (int i = 0; i != level3; ++i) {
                                            mv.visitTypeInsn(CHECKCAST, "groovy/lang/OwnerAware");
                                            mv.visitMethodInsn(INVOKEINTERFACE, "groovy/lang/OwnerAware", "getOwner", "()Ljava/lang/Object;");
                                        }
                                        mv.visitTypeInsn(CHECKCAST, "groovy/lang/Closure");
                                        mv.visitMethodInsn(INVOKEVIRTUAL, "groovy/lang/Closure", "getDelegate", "()Ljava/lang/Object;");
                                        BytecodeExpr.checkCast(getType(), mv);
                                    }
                                };
                                return createCall(exp, compiler, args, object, foundMethod);
                            }
                        }
                    }
                }

                foundMethod = findMethodWithClosureCoercion(compiler.classNode, methodName, argTypes, compiler);
                if (foundMethod != null) {
                    final int level2 = level;
                    object = new BytecodeExpr(exp.getObjectExpression(), compiler.classNode) {
                        protected void compile(MethodVisitor mv) {
                            mv.visitVarInsn(ALOAD, 0);
                            for (int i = 0; i != level2; ++i) {
                                mv.visitTypeInsn(CHECKCAST, "groovy/lang/OwnerAware");
                                mv.visitMethodInsn(INVOKEINTERFACE, "groovy/lang/OwnerAware", "getOwner", "()Ljava/lang/Object;");
                            }
                            BytecodeExpr.checkCast(getType(), mv);
                        }
                    };
                    return createCall(exp, compiler, args, object, foundMethod);
                }

                return dynamicOrError(exp, compiler, methodName, compiler.classNode, argTypes, "Can't find method ");
            } else {
                object = (BytecodeExpr) compiler.transform(exp.getObjectExpression());
                type = TypeUtil.wrapSafely(object.getType());

                foundMethod = findMethodWithClosureCoercion(type, methodName, argTypes, compiler);

                if (foundMethod == null) {
                    return dynamicOrError(exp, compiler, methodName, type, argTypes, "Can't find method ");
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
            compiler.addError(msg + getMethodDescr(type, methodName, argTypes), exp);
            return null;
        } else
            return createDynamicCall(exp, compiler);
    }

    private String getMethodDescr(ClassNode type, String methodName, ClassNode[] argTypes) {
        StringBuilder sb = new StringBuilder(type.getName())
                .append(".")
                .append(methodName)
                .append("(");
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

                            List<MethodNode> one = ClosureUtil.isOneMethodAbstract(argType);
                            MethodNode doCall = one == null ? null : ClosureUtil.isMatch(one, oarg);
                            if (one == null || doCall == null) {
                                foundMethod = null;
                            } else {
                                ClosureUtil.makeOneMethodClass(oarg, argType, one, doCall);
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

    private MethodNode tryCoerceMap(ClassNode type, String methodName, ClassNode[] argTypes, CompilerTransformer compiler, int i, ClassNode oarg) {
        MethodNode foundMethod;
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
                    ClassNode argType = p[p.length - 1].getType();

                    List<MethodNode> one = ClosureUtil.isOneMethodAbstract(argType);
                    MethodNode doCall = one == null ? null : ClosureUtil.isMatch(one, oarg);
                    if (one == null || doCall == null) {
                        return null;
                    } else {
                        ClosureUtil.makeOneMethodClass(oarg, argType, one, doCall);
                    }
                }
            }
        argTypes[i] = oarg;

        return foundMethod;
    }

    private MethodNode findMethodWithClosureCoercion(ClassNode type, String methodName, ClassNode[] argTypes, CompilerTransformer compiler) {
        return findMethodVariatingArgs(type, methodName, argTypes, compiler, -1);
    }
}
