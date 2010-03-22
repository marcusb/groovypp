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
            foundMethod = findMethodWithClosureCoercion(type, methodName, argTypes, compiler, true);
            if (foundMethod == null || !foundMethod.isStatic()) {
                // Try methods from java.lang.Class
                ClassNode clazz = TypeUtil.withGenericTypes(ClassHelper.CLASS_Type, type);
                foundMethod = findMethodWithClosureCoercion(clazz, methodName, argTypes, compiler, false);
                if (foundMethod == null) {
                    // Try some property with 'call' method.
                    final Object prop = resolveCallableProperty(compiler, methodName, type, true);
                    if (prop != null) {
                        final MethodNode callMethod = resolveCallMethod(compiler, argTypes, prop);
                        if (callMethod != null) {
                            return createCallMethodCall(exp, compiler, methodName, args, null, prop, callMethod, type);
                        }
                    }
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
            if (exp.getObjectExpression() instanceof VariableExpression &&
                (((VariableExpression) exp.getObjectExpression()).isThisExpression() ||
                ((VariableExpression) exp.getObjectExpression()).isSuperExpression())) {
                ClassNode thisType = compiler.methodNode.getDeclaringClass();
                if (compiler.methodNode.isStatic() && !(foundMethod instanceof ClassNodeCache.DGM)) {
                    foundMethod = findMethodWithClosureCoercion(thisType, methodName, argTypes, compiler, true);

                    if (foundMethod != null) {
                        object = null;
                        if (!AccessibilityCheck.isAccessible(foundMethod.getModifiers(),
                                foundMethod.getDeclaringClass(), compiler.classNode, thisType)) {
                            return dynamicOrError(exp, compiler, methodName, thisType, argTypes, "Cannot access method ");
                        }

                        return createCall(exp, compiler, args, object, foundMethod);
                    }
                    else {
                        final Object prop = resolveCallableProperty(compiler, methodName, thisType, false);
                        if (prop != null) {
                            final MethodNode callMethod = resolveCallMethod(compiler, argTypes, prop);
                            if (callMethod != null) {
                                return createCallMethodCall(exp, compiler, methodName, args, createThisFetchingObject(exp, compiler, thisType), prop, callMethod, thisType);
                            }
                        }

                        foundMethod = findMethodWithClosureCoercion(ClassHelper.CLASS_Type, methodName, argTypes, compiler, false);
                        if (foundMethod != null) {
                            object = (BytecodeExpr) compiler.transform(new ClassExpression(compiler.classNode));
                            return createCall(exp, compiler, args, object, foundMethod);
                        }
                    }
                } else {
                    if (thisType instanceof ClosureClassNode &&
                        thisType.isDerivedFrom(ClassHelper.CLOSURE_TYPE) &&
                        methodName.equals("call"))
                    // We have a closure recursive call,
                    // let's forget about 'call' and deal with 'doCall' instead.
                    methodName = "doCall";

                    boolean isSuper = ((VariableExpression) exp.getObjectExpression()).isSuperExpression();

                    while (thisType != null) {
                        ClassNode declaringType = isSuper ? thisType.getSuperClass() : thisType;
                        foundMethod = findMethodWithClosureCoercion(declaringType, methodName, argTypes, compiler, false);
                        if (foundMethod == null) {
                            // Groovy does not allow to call 'this.closure()' for closure fields: issue 143.
                            if (exp.isImplicitThis() || !(thisType instanceof ClosureClassNode) || isSuper) {
                                // Try some property with 'call' method.
                                final Object prop = resolveCallableProperty(compiler, methodName, declaringType, false);
                                if (prop != null) {
                                    final MethodNode callMethod = resolveCallMethod(compiler, argTypes, prop);
                                    if (callMethod != null) {
                                        return createCallMethodCall(exp, compiler, methodName, args, createThisFetchingObject(exp, compiler, thisType), prop, callMethod, declaringType);
                                    }
                                }
                            }

                            if (thisType.implementsInterface(TypeUtil.DELEGATING)) {
                                final MethodNode gd = compiler.findMethod(thisType, "getDelegate", ClassNode.EMPTY_ARRAY, false);
                                if (gd != null) {
                                    final InnerThisBytecodeExpr innerThis = new InnerThisBytecodeExpr(exp, thisType, compiler);
                                    final ResolvedMethodBytecodeExpr delegate = ResolvedMethodBytecodeExpr.create(exp, gd, innerThis, ArgumentListExpression.EMPTY_ARGUMENTS, compiler);
                                    foundMethod = findMethodWithClosureCoercion(delegate.getType(), methodName, argTypes, compiler, false);

                                    if (foundMethod != null) {
                                        if (!AccessibilityCheck.isAccessible(foundMethod.getModifiers(),
                                                foundMethod.getDeclaringClass(), compiler.classNode, delegate.getType())) {
                                            return dynamicOrError(exp, compiler, methodName, delegate.getType(), argTypes, "Cannot access method ");
                                        }

                                        return createCall(exp, compiler, args, delegate, foundMethod);
                                    }
                                }
                            }
                        }

                        if (foundMethod != null) {
                            // 'super' access is always permitted.
                            final ClassNode accessType = isSuper ? null : declaringType;
                            if (!AccessibilityCheck.isAccessible(foundMethod.getModifiers(),
                                    foundMethod.getDeclaringClass(), compiler.classNode, accessType)) {
                                return dynamicOrError(exp, compiler, methodName, declaringType, argTypes, "Cannot access method ");
                            }


                            if (foundMethod.isStatic())
                                object = null;
                            else {
                                if (compiler.methodNode.isStatic() && !(foundMethod instanceof ClassNodeCache.DGM)) {
                                    compiler.addError("Cannot reference an instance method from static context", exp);
                                    return null;
                                }

                                if (isSuper) {
                                    if (thisType != compiler.classNode) {
                                        // super call to outer class' super method needs to be proxied.
                                        foundMethod = compiler.context.getSuperMethodDelegate(foundMethod, thisType);
                                        object = createThisFetchingObject(exp, compiler, thisType);
                                    } else {
                                        object = (BytecodeExpr) compiler.transform(exp.getObjectExpression());
                                    }
                                } else {
                                    if (thisType != compiler.classNode && thisType != foundMethod.getDeclaringClass() &&
                                            !foundMethod.isPublic()) {
                                        // super call to outer class' super method needs to be proxied.
                                        foundMethod = compiler.context.getSuperMethodDelegate(foundMethod, thisType);
                                    }
                                    object = createThisFetchingObject(exp, compiler, thisType);
                                }
                            }

                            return createCall(exp, compiler, args, object, foundMethod);
                        }

                        compiler.context.setOuterClassInstanceUsed(thisType);
                        FieldNode ownerField = thisType.getField("this$0");
                        thisType = ownerField == null ? null : ownerField.getType();
                    }
                }

                return dynamicOrError(exp, compiler, methodName, compiler.classNode, argTypes, "Cannot find method ");
            } else {
                object = (BytecodeExpr) compiler.transformToGround(exp.getObjectExpression());
                type = object.getType();

                if (type instanceof ClosureClassNode && methodName.equals("call"))
                    // Since ClosureClassNode can't (at least currently) escape its scope we have a typed closure,
                    // so let's forget about 'call' and deal with 'doCall' instead.
                    methodName = "doCall";

                if (foundMethod == null)
                    foundMethod = findMethodWithClosureCoercion(type, methodName, argTypes, compiler, false);

                if (foundMethod == null) {
                    // Try some property with 'call' method.
                    final Object prop = resolveCallableProperty(compiler, methodName, type, false);
                    if (prop != null) {
                        final MethodNode callMethod = resolveCallMethod(compiler, argTypes, prop);
                        if (callMethod != null) {
                            return createCallMethodCall(exp, compiler, methodName, args, object, prop, callMethod, type);
                        }
                    }
                }

                if (foundMethod == null) {
                    if (TypeUtil.isAssignableFrom(TypeUtil.TCLOSURE, object.getType())) {
                        foundMethod = findMethodWithClosureCoercion(ClassHelper.CLOSURE_TYPE, methodName, argTypes, compiler, false);
                        if (foundMethod != null) {
                            ClosureUtil.improveClosureType(object.getType(), ClassHelper.CLOSURE_TYPE);
                            return createCall(exp, compiler, args, object, foundMethod);
                        }
                    } else {
                        MethodNode unboxing = TypeUtil.getReferenceUnboxingMethod(type);
                        if (unboxing != null) {
                            ClassNode t = TypeUtil.getSubstitutedType(unboxing.getReturnType(), unboxing.getDeclaringClass(), type);
                            foundMethod = findMethodWithClosureCoercion(t, methodName, argTypes, compiler, false);
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
                                MethodNode updaterMethod = findMethodWithClosureCoercion(updater.getType(), methodName, newArgs, compiler, false);
                                if (updaterMethod != null) {
                                    ResolvedFieldBytecodeExpr updaterInstance = new ResolvedFieldBytecodeExpr(exp, updater, null, null, compiler);
                                    ((TupleExpression)args).getExpressions().add(0, obj.getObject());
                                    return createCall(exp, compiler, args, updaterInstance, updaterMethod);
                                }
                            }
                        }
                    } else if (object instanceof ResolvedGetterBytecodeExpr) {
                        ResolvedGetterBytecodeExpr obj = (ResolvedGetterBytecodeExpr) object;
                        FieldNode fieldNode = obj.getFieldNode();
                        if (fieldNode != null && (fieldNode.getModifiers() & Opcodes.ACC_VOLATILE) != 0) {
                            FieldNode updater = fieldNode.getDeclaringClass().getDeclaredField(fieldNode.getName() + "$updater");
                            if (updater != null) {
                                ClassNode[] newArgs = new ClassNode [argTypes.length+1];
                                System.arraycopy(argTypes, 0, newArgs, 1, argTypes.length);
                                newArgs [0] = obj.getObject().getType();
                                MethodNode updaterMethod = compiler.findMethod(updater.getType(), methodName, newArgs, false);
                                if (updaterMethod != null) {
                                    ResolvedFieldBytecodeExpr updaterInstance = new ResolvedFieldBytecodeExpr(exp, updater, null, null, compiler);
                                    ((TupleExpression)args).getExpressions().add(0, obj.getObject());
                                    return createCall(exp, compiler, args, updaterInstance, updaterMethod);
                                }
                            }
                        }
                    }

                    return dynamicOrError(exp, compiler, methodName, type, argTypes, "Cannot find method ");
                }

                // 'super' access is always permitted.
                final ClassNode accessType = object instanceof VariableExpressionTransformer.Super ? null : type;
                if (!AccessibilityCheck.isAccessible(foundMethod.getModifiers(),
                        foundMethod.getDeclaringClass(), compiler.classNode, accessType)) {
                    return dynamicOrError(exp, compiler, methodName, type, argTypes, "Cannot access method ");
                }

                return createCall(exp, compiler, args, object, foundMethod);
            }
        }
    }

    private Expression createCallMethodCall(MethodCallExpression exp, CompilerTransformer compiler, String methodName, Expression args, BytecodeExpr object, Object prop, MethodNode callMethod, ClassNode type) {
        PropertyExpression propertyExpression = new PropertyExpression(
                exp.getObjectExpression(), methodName);
        object = PropertyUtil.createGetProperty(propertyExpression, compiler, methodName,
                type, object, prop);
        return createCall(exp, compiler, args, object, callMethod);
    }

    private MethodNode resolveCallMethod(CompilerTransformer compiler, ClassNode[] argTypes, Object prop) {
        final ClassNode propType = PropertyUtil.getPropertyType(prop);
        return findMethodWithClosureCoercion(propType, "call", argTypes, compiler, false);
    }

    private Object resolveCallableProperty(CompilerTransformer compiler, String methodName, ClassNode thisType,
                                           boolean onlyStatic) {
        return PropertyUtil.resolveGetProperty(thisType, methodName, compiler, onlyStatic, false);
    }

    private BytecodeExpr createThisFetchingObject(final MethodCallExpression exp, final CompilerTransformer compiler, final ClassNode thisTypeFinal) {
        return new BytecodeExpr(exp.getObjectExpression(), thisTypeFinal) {
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

    private Expression createCall(MethodCallExpression exp, CompilerTransformer compiler, Expression args, BytecodeExpr object, MethodNode foundMethod) {
        if (foundMethod.getReturnType().equals(ClassHelper.VOID_TYPE)) {
            final ResolvedMethodBytecodeExpr call = ResolvedMethodBytecodeExpr.create(exp, foundMethod, object, (TupleExpression) args, compiler);
            return new BytecodeExpr(exp, TypeUtil.NULL_TYPE) {
                protected void compile(MethodVisitor mv) {
                    call.visit(mv);
                    mv.visitInsn(ACONST_NULL);
                }
            };
        }
        else
            return ResolvedMethodBytecodeExpr.create(exp, foundMethod, object, (TupleExpression) args, compiler);
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
        final List<Expression> args = ((TupleExpression) exp.getArguments()).getExpressions();

        for (int i = 0; i != args.size(); ++i) {
            BytecodeExpr arg = compiler.transformSynthetic((BytecodeExpr) args.get(i));
            if (arg instanceof CompiledClosureBytecodeExpr) {
                compiler.processPendingClosure((CompiledClosureBytecodeExpr) arg);
            }
            args.set(i, arg);
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

    private MethodNode findMethodVariatingArgs(ClassNode type, String methodName, ClassNode[] argTypes, CompilerTransformer compiler, boolean staticOnly) {

        MethodNode foundMethod;
        List<Changed> changed  = null;

        ClassNode[] argTypesCopy = new ClassNode[argTypes.length];
        System.arraycopy(argTypes, 0, argTypesCopy, 0, argTypes.length);
        for (int i = 0; i < argTypesCopy.length+1; ++i) {
            foundMethod = compiler.findMethod(type, methodName, argTypesCopy, staticOnly);
            if (foundMethod != null) {
                return foundMethodInference(type, foundMethod, changed, argTypesCopy, compiler);
            }

            if (i == argTypesCopy.length)
                return null;

            final ClassNode oarg = argTypesCopy[i];
            if (oarg == null)
                continue;

            if (oarg.implementsInterface(TypeUtil.TCLOSURE) ||
                oarg.implementsInterface(TypeUtil.TLIST) ||
                oarg.implementsInterface(TypeUtil.TMAP) ||
                oarg.implementsInterface(TypeUtil.TTHIS)) {

                if (changed == null)
                    changed = new ArrayList<Changed> ();

                Changed change = new Changed();
                change.index = i;
                change.original = argTypesCopy[i];
                changed.add(change);
                argTypesCopy[i] = oarg.implementsInterface(TypeUtil.TCLOSURE) ?
                        TypeUtil.withGenericTypes(TypeUtil.TCLOSURE_NULL,change.original) 
                        : null;
            }
        }

        return null;
    }

    private MethodNode foundMethodInference(ClassNode type, MethodNode foundMethod, List<Changed> changed, ClassNode [] argTypes, CompilerTransformer compiler) {
        if (changed == null)
            return foundMethod;

        Parameter parameters[] = foundMethod.getParameters();
        ClassNode[] paramTypes = new ClassNode[argTypes.length];
        for (int i = 0; i < parameters.length - 1; i++) {
            paramTypes[i] = parameters[i].getType();
        }
        ClassNode lastType = parameters[parameters.length - 1].getType();
        if (parameters.length == argTypes.length) {
            paramTypes[paramTypes.length -1] = lastType;
        } else {
            if (!lastType.isArray()) return null;
            for (int i = parameters.length -1 ; i < paramTypes.length; i++) {
                paramTypes[i] = lastType.getComponentType();
            }
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
            ClassNode paramType = paramTypes[change.index];

            if (!change.original.implementsInterface(TypeUtil.TCLOSURE)) {
                it.remove();
                // nothing special needs to be done for list & maps
            }
            else {
                if (paramType.equals(ClassHelper.CLOSURE_TYPE)) {
                    ClosureUtil.improveClosureType(change.original, ClassHelper.CLOSURE_TYPE);
                    StaticMethodBytecode.replaceMethodCode(compiler.su, compiler.context, ((ClosureClassNode)change.original).getDoCallMethod(), compiler.compileStack, compiler.debug == -1 ? -1 : compiler.debug+1, compiler.policy, change.original.getName());
                    argTypes [change.index] = change.original;
                    it.remove();
                }
                else {
                    List<MethodNode> one = ClosureUtil.isOneMethodAbstract(paramType);
                    if (one == null) {
                        return null;
                    }

                    change.oneMethodAbstract = one;
                    if (!hasGenerics) {
                        it.remove();

                        MethodNode doCall = ClosureUtil.isMatch(one, (ClosureClassNode) change.original, compiler, paramType);
                        if (doCall == null) {
                            return null;
                        } else {
                            ClosureUtil.makeOneMethodClass(change.original, paramType, one, doCall, compiler);
                        }
                    } else {
                        boolean[] used = new boolean[typeVars.length];
                        extractUsedVariables(one.get(0), indices, used, paramType);
                        inTypeVars.put(change, used);
                    }
                }
            }
        }

        if (changed.size() == 0) {
            return foundMethod;
        }

        if (changed.size() == 1) {
            ClassNode[] bindings = obtainInitialBindings(type, foundMethod, argTypes, paramTypes, typeVars);
            return inferTypesForClosure(type, foundMethod, compiler, paramTypes, changed.get(0), bindings, typeVars) ? foundMethod : null;
        }

        ClassNode[] bindings = obtainInitialBindings(type, foundMethod, argTypes, paramTypes, typeVars);
        Next:
        while (true) {
            if (changed.isEmpty()) return foundMethod;
            for (Iterator<Changed> it = changed.iterator(); it.hasNext();) {
                Changed change = it.next();
                if (isBound(bindings, inTypeVars.get(change))) {
                    if (!inferTypesForClosure(type, foundMethod, compiler, paramTypes, change, bindings, typeVars)) return null;
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
                                         CompilerTransformer compiler, ClassNode[] paramTypes,
                                         Changed info, ClassNode[] bindings, GenericsType[] typeVars) {
        ClassNode paramType = paramTypes[info.index];
        paramType = TypeUtil.getSubstitutedType(paramType, foundMethod, bindings);

        if (type != null) {
            paramType = TypeUtil.getSubstitutedType(paramType, foundMethod.getDeclaringClass(), type);
        }

        List<MethodNode> one = info.oneMethodAbstract;
        MethodNode doCall = one == null ? null : ClosureUtil.isMatch(one, (ClosureClassNode) info.original, compiler, paramType);
        if (doCall == null) {
            return false;
        } else {
            ClosureUtil.makeOneMethodClass(info.original, paramType, one, doCall, compiler);
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

    private ClassNode[] obtainInitialBindings(ClassNode type, MethodNode foundMethod, ClassNode[] argTypes, ClassNode[] paramTypes, GenericsType[] methodTypeVars) {
        ArrayList<ClassNode> formals = new ArrayList<ClassNode> (2);
        ArrayList<ClassNode> instantiateds = new ArrayList<ClassNode> (2);

        if (!foundMethod.isStatic()) {
            formals.add(foundMethod.getDeclaringClass());
            instantiateds.add(type);
        }

        for (int j = 0; j != argTypes.length; j++) {
            formals.add(paramTypes[j]);
            instantiateds.add(argTypes[j]);
        }

        return TypeUnification.inferTypeArguments(methodTypeVars, formals.toArray(new ClassNode[formals.size()]),
                instantiateds.toArray(new ClassNode[instantiateds.size()]));
    }

    private MethodNode findMethodWithClosureCoercion(ClassNode type, String methodName, ClassNode[] argTypes, CompilerTransformer compiler, boolean staticOnly) {
        return findMethodVariatingArgs(type, methodName, argTypes, compiler, staticOnly);
    }
}
