package org.mbte.groovypp.compiler.transformers;

import groovy.lang.TypePolicy;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.mbte.groovypp.compiler.*;
import org.mbte.groovypp.compiler.bytecode.*;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.*;

public class MethodCallExpressionTransformer extends ExprTransformer<MethodCallExpression> {
    public Expression transform(final MethodCallExpression exp, final CompilerTransformer compiler) {
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

        if (exp.isSpreadSafe()) {
            Parameter param = new Parameter(ClassHelper.OBJECT_TYPE, "$it");
            VariableExpression ve = new VariableExpression(param);
            Expression originalMethod = exp.getMethod();
            ve.setSourcePosition(originalMethod);
            MethodCallExpression prop = new MethodCallExpression(ve, originalMethod, exp.getArguments());
            prop.setSourcePosition(originalMethod);
            ReturnStatement retStat = new ReturnStatement(prop);
            retStat.setSourcePosition(originalMethod);
            ClosureExpression ce = new ClosureExpression(new Parameter[]{param}, retStat);
            ce.setVariableScope(new VariableScope(compiler.compileStack.getScope()));
            MethodCallExpression mce = new MethodCallExpression(exp.getObjectExpression(), "map", new ArgumentListExpression(ce));
            mce.setSourcePosition(exp);
            return compiler.transform(mce);
        }

        Expression args = compiler.transform(exp.getArguments());
        exp.setArguments(args);

        if (exp.isSafe()) {
            return transformSafe(exp, compiler);
        }

        BytecodeExpr object;
        ClassNode type;
        MethodNode foundMethod = null;
        final ClassNode[] argTypes = compiler.exprToTypeArray(args);

        if (exp.getObjectExpression() instanceof ClassExpression) {
            type = TypeUtil.wrapSafely(exp.getObjectExpression().getType());
            foundMethod = findMethodWithClosureCoercion(type, methodName, argTypes, compiler);
            if (foundMethod == null || !foundMethod.isStatic()) {
                // Try methods from java.lang.Class
                ClassNode clazz = TypeUtil.withGenericTypes(ClassHelper.CLASS_Type, type);
                foundMethod = findMethodWithClosureCoercion(clazz, methodName, argTypes, compiler);
                if (foundMethod == null) {
                    return dynamicOrError(exp, compiler, methodName, type, argTypes, "Cannot find static method ");
                }
                object = (BytecodeExpr) compiler.transform(exp.getObjectExpression());
                return createCall(exp, compiler, args, object, foundMethod);
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
                        if (foundMethod.isStatic())
                            object = null;
                        else {
                            if (compiler.methodNode.isStatic() && !(foundMethod instanceof ClassNodeCache.DGM)) {
                                compiler.addError("Cannot reference an instance method from static context", exp);
                                return null;
                            }
                            object = new BytecodeExpr(exp.getObjectExpression(), thisTypeFinal) {
                                protected void compile(MethodVisitor mv) {
                                    mv.visitVarInsn(ALOAD, 0);
                                    ClassNode curThis = compiler.methodNode.getDeclaringClass();
                                    while (curThis != thisTypeFinal) {
                                        ClassNode next = curThis.getField("this$0").getType();
                                        mv.visitFieldInsn(GETFIELD, BytecodeHelper.getClassInternalName(curThis), "this$0", BytecodeHelper.getTypeDescription(next));
                                        curThis = next;
                                    }
                                }

                                @Override
                                public boolean isThis() {
                                    return thisTypeFinal.equals(compiler.classNode);
                                }
                            };
                        }

                        if (!AccessibilityCheck.isAccessible(foundMethod.getModifiers(),
                                foundMethod.getDeclaringClass(), compiler.classNode, thisType)) {
                            return dynamicOrError(exp, compiler, methodName, thisType, argTypes, "Cannot access method ");
                        }

                        return createCall(exp, compiler, args, object, foundMethod);
                    }

                    FieldNode ownerField = thisType.getField("this$0");
                    thisType = ownerField == null ? null : ownerField.getType();
                }

                return dynamicOrError(exp, compiler, methodName, compiler.classNode, argTypes, "Cannot find method ");
            } else {
                object = (BytecodeExpr) compiler.transform(exp.getObjectExpression());
                if (object instanceof ListExpressionTransformer.UntransformedListExpr)
                    object = new ListExpressionTransformer.TransformedListExpr(((ListExpressionTransformer.UntransformedListExpr)object).exp, TypeUtil.ARRAY_LIST_TYPE, compiler);
                else if (object instanceof MapExpressionTransformer.UntransformedMapExpr)
                    object = new MapExpressionTransformer.TransformedMapExpr(((MapExpressionTransformer.UntransformedMapExpr)object).exp, compiler);
                type = object.getType();

                if (type.isDerivedFrom(ClassHelper.CLOSURE_TYPE) && methodName.equals("call"))
                    foundMethod = findMethodWithClosureCoercion(type, "doCall", argTypes, compiler);

                if (foundMethod == null)
                    foundMethod = findMethodWithClosureCoercion(type, methodName, argTypes, compiler);

                if (foundMethod == null) {
                    if (TypeUtil.isAssignableFrom(TypeUtil.TCLOSURE, object.getType())) {
                        foundMethod = findMethodWithClosureCoercion(ClassHelper.CLOSURE_TYPE, methodName, argTypes, compiler);
                        if (foundMethod != null) {
                            ClosureUtil.improveClosureType(object.getType(), ClassHelper.CLOSURE_TYPE);
                            return createCall(exp, compiler, args, object, foundMethod);
                        }
                    } else {
                        MethodNode unboxing = TypeUtil.getReferenceUnboxingMethod(type);
                        if (unboxing != null) {
                            ClassNode t = TypeUtil.getSubstitutedType(unboxing.getReturnType(), unboxing.getDeclaringClass(), type);
                            foundMethod = findMethodWithClosureCoercion(t, methodName, argTypes, compiler);
                            if (foundMethod != null) {
                                object = ResolvedMethodBytecodeExpr.create(exp, unboxing, object,
                                        new ArgumentListExpression(), compiler);
                                return createCall(exp, compiler, args, object, foundMethod);
                            }
                        }
                    }

                    if (object instanceof ResolvedFieldBytecodeExpr) {
                        ResolvedFieldBytecodeExpr obj = (ResolvedFieldBytecodeExpr) object;
                        FieldNode fieldNode = obj.getFieldNode();
                        if ((fieldNode.getModifiers() & Opcodes.ACC_VOLATILE) != 0) {
                            FieldNode updater = fieldNode.getDeclaringClass().getDeclaredField(fieldNode.getName() + "$updater");
                            if (updater != null) {
                                ClassNode [] newArgs = new ClassNode [argTypes.length+1];
                                System.arraycopy(argTypes, 0, newArgs, 1, argTypes.length);
                                newArgs [0] = obj.getObject().getType();
                                MethodNode updaterMethod = compiler.findMethod(updater.getType(), methodName, newArgs);
                                if (updaterMethod != null) {
                                    ResolvedFieldBytecodeExpr updaterInstance = new ResolvedFieldBytecodeExpr(exp, updater, null, null, compiler);
                                    ((ArgumentListExpression)args).getExpressions().add(0, obj.getObject());
                                    return createCall(exp, compiler, args, updaterInstance, updaterMethod);
                                }
                            }
                        }
                    } else if (object instanceof ResolvedGetterBytecodeExpr.Accessor) {
                        ResolvedGetterBytecodeExpr.Accessor obj = (ResolvedGetterBytecodeExpr.Accessor) object;
                        FieldNode fieldNode = obj.getFieldNode();
                        if ((fieldNode.getModifiers() & Opcodes.ACC_VOLATILE) != 0) {
                            FieldNode updater = fieldNode.getDeclaringClass().getDeclaredField(fieldNode.getName() + "$updater");
                            if (updater != null) {
                                ClassNode[] newArgs = new ClassNode [argTypes.length+1];
                                System.arraycopy(argTypes, 0, newArgs, 1, argTypes.length);
                                newArgs [0] = obj.getObject().getType();
                                MethodNode updaterMethod = compiler.findMethod(updater.getType(), methodName, newArgs);
                                if (updaterMethod != null) {
                                    ResolvedFieldBytecodeExpr updaterInstance = new ResolvedFieldBytecodeExpr(exp, updater, null, null, compiler);
                                    ((ArgumentListExpression)args).getExpressions().add(0, obj.getObject());
                                    return createCall(exp, compiler, args, updaterInstance, updaterMethod);
                                }
                            }
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
            final ResolvedMethodBytecodeExpr call = ResolvedMethodBytecodeExpr.create(exp, foundMethod, object, (ArgumentListExpression) args, compiler);
            return new BytecodeExpr(object, TypeUtil.NULL_TYPE) {
                protected void compile(MethodVisitor mv) {
                    call.visit(mv);
                    mv.visitInsn(ACONST_NULL);
                }
            };
        }
        else
            return ResolvedMethodBytecodeExpr.create(exp, foundMethod, object, (ArgumentListExpression) args, compiler);
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
            return new BytecodeExpr(exp, call.getType()) {
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

    private Expression dynamicOrError(MethodCallExpression exp, CompilerTransformer compiler, String methodName, ClassNode type, ClassNode[] argTypes, final String msg) {
        if (compiler.policy == TypePolicy.STATIC) {
            compiler.addError(msg + getMethodDescr(type, methodName, argTypes), exp.getMethod());
            return null;
        } else
            return createDynamicCall(exp, compiler);
    }

    private String getMethodDescr(ClassNode type, String methodName, ClassNode[] argTypes) {
        StringBuilder sb = new StringBuilder();
        sb.append(PresentationUtil.getText(type));
        sb.append(".");
        sb.append(methodName);
        sb.append("(");
        for (int i = 0; i != argTypes.length; i++) {
            if (i != 0)
                sb.append(", ");
            if (argTypes[i] != null)
                sb.append(PresentationUtil.getText(argTypes[i]));
            else
                sb.append("null");
        }
        sb.append(")");
        return sb.toString();
    }

    private Expression createDynamicCall(final MethodCallExpression exp, CompilerTransformer compiler) {
        final List<Expression> args = ((ArgumentListExpression) exp.getArguments()).getExpressions();

        for (int i = 0; i != args.size(); ++i) {
            Expression arg = args.get(i);
            if (arg instanceof CompiledClosureBytecodeExpr) {
                compiler.processPendingClosure((CompiledClosureBytecodeExpr) arg);
            }
            if (arg instanceof ListExpressionTransformer.UntransformedListExpr) {
                arg = new ListExpressionTransformer.TransformedListExpr(((ListExpressionTransformer.UntransformedListExpr)arg).exp, TypeUtil.ARRAY_LIST_TYPE, compiler);
                args.set(i, arg);
            }
            if (arg instanceof MapExpressionTransformer.UntransformedMapExpr) {
                arg = new MapExpressionTransformer.TransformedMapExpr(((MapExpressionTransformer.UntransformedMapExpr)arg).exp, compiler);
                args.set(i, arg);
            }
        }

        final BytecodeExpr methodExpr = (BytecodeExpr) compiler.transform(exp.getMethod());
        final BytecodeExpr object = (BytecodeExpr) compiler.transform(exp.getObjectExpression());

        return new BytecodeExpr(exp, ClassHelper.OBJECT_TYPE) {
            protected void compile(MethodVisitor mv) {
                object.visit(mv);
                box(object.getType(), mv);

                methodExpr.visit(mv);

                mv.visitLdcInsn(args.size());
                mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
                for (int j = 0; j != args.size(); ++j) {
                    mv.visitInsn(DUP);
                    mv.visitLdcInsn(j);
                    BytecodeExpr arg = (BytecodeExpr) args.get(j);
                    arg.visit(mv);
                    box(arg.getType(), mv);
                    mv.visitInsn(AASTORE);
                }
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/codehaus/groovy/runtime/InvokerHelper", "invokeMethod", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;");
            }
        };
    }

    private static class Changed {
        int index;
        ClassNode original;
        List<MethodNode> oneMethodAbstract;
    }

    private MethodNode findMethodVariatingArgs(ClassNode type, String methodName, ClassNode[] argTypes, CompilerTransformer compiler) {

        MethodNode foundMethod;
        List<Changed> changed  = null;

        for (int i = 0; i < argTypes.length+1; ++i) {
            foundMethod = compiler.findMethod(type, methodName, argTypes);
            if (foundMethod != null) {
                return foundMethodInference(type, foundMethod, changed, argTypes, compiler);
            }

            if (i == argTypes.length)
                return null;

            final ClassNode oarg = argTypes[i];
            if (oarg == null)
                continue;

            if (oarg.implementsInterface(TypeUtil.TCLOSURE) ||
                oarg.implementsInterface(TypeUtil.TLIST) ||
                oarg.implementsInterface(TypeUtil.TMAP)) {

                if (changed == null)
                    changed = new ArrayList<Changed> ();

                Changed change = new Changed();
                change.index = i;
                change.original = argTypes[i];
                changed.add(change);
                argTypes[i] = null;
            }
        }

        return null;
    }

    private MethodNode foundMethodInference(ClassNode type, MethodNode foundMethod, List<Changed> changed, ClassNode [] argTypes, CompilerTransformer compiler) {
        if (changed == null)
            return foundMethod;

        Parameter parameters[] = foundMethod.getParameters();
        if (parameters.length != argTypes.length) {
            return null;
        }

        boolean hasGenerics = TypeUtil.hasGenericsTypes(foundMethod);

        GenericsType[] typeVars = foundMethod.getGenericsTypes();
        if (typeVars == null) typeVars = new GenericsType[0];
        Map<String, Integer> indices = new HashMap<String, Integer>();
        
        for (int i = 0; i < typeVars.length; ++i) {
            GenericsType typeVar = typeVars[i];
            indices.put(typeVar.getType().getUnresolvedName(), i);
        }

        Map<Changed, boolean[]> inTypeVars = new HashMap<Changed, boolean[]>();

        for (Iterator<Changed> it = changed.iterator(); it.hasNext(); ) {
            Changed change = it.next();
            ClassNode argType = parameters[change.index].getType();

            if (!change.original.implementsInterface(TypeUtil.TCLOSURE)) {
                it.remove();
                // nothing special needs to be done for list & maps
            }
            else {
                if (argType.equals(ClassHelper.CLOSURE_TYPE)) {
                    ClosureUtil.improveClosureType(change.original, ClassHelper.CLOSURE_TYPE);
                    StaticMethodBytecode.replaceMethodCode(compiler.su, compiler.context, ((ClosureClassNode)change.original).getDoCallMethod(), compiler.compileStack, compiler.debug == -1 ? -1 : compiler.debug+1, compiler.policy, change.original.getName());
                    argTypes [change.index] = change.original;
                    it.remove();
                }
                else {
                    List<MethodNode> one = ClosureUtil.isOneMethodAbstract(argType);
                    if (one == null) {
                        return null;
                    }

                    change.oneMethodAbstract = one;
                    if (!hasGenerics) {
                        it.remove();

                        MethodNode doCall = ClosureUtil.isMatch(one, (ClosureClassNode) change.original, compiler, argType);
                        if (doCall == null) {
                            return null;
                        } else {
                            ClosureUtil.makeOneMethodClass(change.original, argType, one, doCall);
                        }
                    } else {
                        boolean[] used = new boolean[typeVars.length];
                        extractUsedVariables(one.get(0), indices, used, argType);
                        inTypeVars.put(change, used);
                    }
                }
            }
        }

        if (changed.size() == 0) {
            return foundMethod;
        }

        if (changed.size() == 1) {
            ClassNode[] bindings = obtainInitialBindings(type, foundMethod, argTypes, parameters, typeVars);
            return inferTypesForClosure(type, foundMethod, compiler, parameters, changed.get(0), bindings, typeVars) ? foundMethod : null;
        }

        ClassNode[] bindings = obtainInitialBindings(type, foundMethod, argTypes, parameters, typeVars);
        Next:
        while (true) {
            if (changed.isEmpty()) return foundMethod;
            for (Iterator<Changed> it = changed.iterator(); it.hasNext();) {
                Changed change = it.next();
                if (isBound(bindings, inTypeVars.get(change))) {
                    if (!inferTypesForClosure(type, foundMethod, compiler, parameters, change, bindings, typeVars)) return null;
                    it.remove();
                    continue Next;
                }
            }
            return null;
        }
    }

    private boolean isBound(ClassNode[] bindings, boolean[] used) {
        for (int i = 0; i < used.length; i++) {
            if (used[i] && bindings[i] == null) return false;
        }
        return true;
    }

    private void extractUsedVariables(MethodNode methodNode, Map<String, Integer> indices, boolean[] used, ClassNode type) {
        for (Parameter parameter : methodNode.getParameters()) {
            ClassNode t = parameter.getType();
            t = TypeUtil.getSubstitutedType(t, methodNode.getDeclaringClass(), type);
            extractUsedVariables(t, indices, used);
        }
    }

    private void extractUsedVariables(ClassNode type, Map<String, Integer> indices, boolean[] used) {
        if (type.isGenericsPlaceHolder()) {
            Integer idx = indices.get(type.getUnresolvedName());
            if (idx != null) {
                used[idx] = true;
            }
        } else {
            GenericsType[] generics = type.getGenericsTypes();
            if (generics != null) {
                for (GenericsType generic : generics) {
                    extractUsedVariables(generic.getType(), indices, used);
                }
            }
        }
    }

    private boolean inferTypesForClosure(ClassNode type, MethodNode foundMethod,
                                         CompilerTransformer compiler, Parameter[] parameters,
                                         Changed info, ClassNode[] bindings, GenericsType[] typeVars) {
        ClassNode argType = parameters[info.index].getType();
        argType = TypeUtil.getSubstitutedType(argType, foundMethod, bindings);

        if (type != null) {
            argType = TypeUtil.getSubstitutedType(argType, foundMethod.getDeclaringClass(), type);
        }

        List<MethodNode> one = info.oneMethodAbstract;
        MethodNode doCall = one == null ? null : ClosureUtil.isMatch(one, (ClosureClassNode) info.original, compiler, argType);
        if (doCall == null) {
            return false;
        } else {
            ClosureUtil.makeOneMethodClass(info.original, argType, one, doCall);
            ClassNode formal = one.get(0).getReturnType();
            ClassNode instantiated = doCall.getReturnType();
            ClassNode[] addition = TypeUnification.inferTypeArguments(typeVars, new ClassNode[]{formal},
                    new ClassNode[]{instantiated});
            for (int i = 0; i < bindings.length; i++) {
                if (bindings[i] == null) bindings[i] = addition[i];
            }
            return true;
        }
    }

    private ClassNode[] obtainInitialBindings(ClassNode type, MethodNode foundMethod, ClassNode[] argTypes, Parameter[] parameters, GenericsType[] methodTypeVars) {
        ArrayList<ClassNode> formals = new ArrayList<ClassNode> (2);
        ArrayList<ClassNode> instantiateds = new ArrayList<ClassNode> (2);

        if (!foundMethod.isStatic()) {
            formals.add(foundMethod.getDeclaringClass());
            instantiateds.add(type);
        }

        for (int j = 0; j != argTypes.length; j++) {
            formals.add(parameters[j].getType());
            instantiateds.add(argTypes[j]);
        }

        return TypeUnification.inferTypeArguments(methodTypeVars, formals.toArray(new ClassNode[formals.size()]),
                instantiateds.toArray(new ClassNode[instantiateds.size()]));
    }

    private MethodNode findMethodWithClosureCoercion(ClassNode type, String methodName, ClassNode[] argTypes, CompilerTransformer compiler) {
        return findMethodVariatingArgs(type, methodName, argTypes, compiler);
    }
}
