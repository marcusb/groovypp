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
import org.codehaus.groovy.ast.stmt.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.mbte.groovypp.compiler.*;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.mbte.groovypp.compiler.bytecode.ResolvedMethodBytecodeExpr;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

public class ConstructorCallExpressionTransformer extends ExprTransformer<ConstructorCallExpression> {
    private static final ClassNode[] MAP_ARGS = new ClassNode[]{TypeUtil.LINKED_HASH_MAP_TYPE};

    public Expression transform(ConstructorCallExpression exp, final CompilerTransformer compiler) {

        if (exp.isSuperCall() || exp.isThisCall())
            return transformSpecial (exp, compiler);

        MethodNode constructor;
        ClassNode type = exp.getType();

        if (type instanceof InnerClassNode) {
            InnerClassNode innerClassNode = (InnerClassNode) type;

            if (innerClassNode.isAnonymous()) {
                if(!improveWhatInnerClassVisitorDid(innerClassNode, exp, compiler))
                    return null;
            }
        }

        rewriteThis0 (exp, compiler);

        if (exp.getArguments() instanceof TupleExpression && ((TupleExpression)exp.getArguments()).getExpressions().size() == 1 && ((TupleExpression)exp.getArguments()).getExpressions().get(0) instanceof MapExpression) {
            MapExpression me = (MapExpression) ((TupleExpression)exp.getArguments()).getExpressions().get(0);

            constructor = compiler.findConstructor(type, MAP_ARGS);
            if (constructor == null) {
                final ArrayList<BytecodeExpr> propSetters = new ArrayList<BytecodeExpr> ();

                constructor = compiler.findConstructor(type, ClassNode.EMPTY_ARRAY);
                if (constructor != null) {
                    for (MapEntryExpression mee : me.getMapEntryExpressions()) {

                        BytecodeExpr obj = new BytecodeExpr(mee, type) {
                            protected void compile(MethodVisitor mv) {
                                mv.visitInsn(DUP);
                            }
                        };

                        propSetters.add(
                                (BytecodeExpr) compiler.transform(
                                        new BinaryExpression(
                                                new PropertyExpression(
                                                        obj,
                                                        mee.getKeyExpression()
                                                ),
                                                Token.newSymbol(Types.ASSIGN, -1, -1),
                                                mee.getValueExpression()
                                        )
                                )
                        );
                    }

                    return new BytecodeExpr(exp, type) {
                        protected void compile(MethodVisitor mv) {
                            final String classInternalName = BytecodeHelper.getClassInternalName(getType());
                            mv.visitTypeInsn(NEW, classInternalName);
                            mv.visitInsn(DUP);
                            mv.visitMethodInsn(INVOKESPECIAL, classInternalName, "<init>", "()V");

                            for (BytecodeExpr prop : propSetters) {
                                prop.visit(mv);
                                if (!ClassHelper.VOID_TYPE.equals(prop.getType()))
                                    pop(prop.getType(), mv);
                            }
                        }
                    };
                }
            }
        }

        final TupleExpression newArgs = (TupleExpression) compiler.transform(exp.getArguments());
        final ClassNode[] argTypes = compiler.exprToTypeArray(newArgs);

        constructor = findConstructorWithClosureCoercion(type, argTypes, compiler);

        if (constructor != null) {
            if (!AccessibilityCheck.isAccessible(constructor.getModifiers(), constructor.getDeclaringClass(), compiler.classNode, null)) {
                compiler.addError("Cannot access constructor", exp);
                return null;
            }

            final Parameter[] params = constructor.getParameters();
            int base = 0;
//            if ((type.getModifiers() & ACC_STATIC) == 0 && type.redirect() instanceof InnerClassNode) {
//                base = 1;
//            }
            final ArgumentListExpression finalArgs = wrapArgumentsForVarargs(newArgs, params, base);

            if ((constructor.getModifiers() & Opcodes.ACC_PRIVATE) != 0 && constructor.getDeclaringClass() != compiler.classNode) {
                MethodNode delegate = compiler.context.getConstructorDelegate(constructor);
                return ResolvedMethodBytecodeExpr.create(exp, delegate, null, finalArgs, compiler);
            }

            // Improve type.
            GenericsType[] generics = type.redirect().getGenericsTypes();
            // We don't support inference if the method itself is parameterized.
            if (generics != null && constructor.getGenericsTypes() == null) {
                ClassNode[] paramTypes = new ClassNode[params.length];
                for (int i = 0; i < paramTypes.length; i++) {
                    paramTypes[i] = params[i].getType();
                }
                ClassNode[] unified = TypeUnification.inferTypeArguments(generics, paramTypes, argTypes);
                if (TypeUnification.totalInference(unified)) {
                    type = TypeUtil.withGenericTypes(type, unified);
                }
            }
            for (int i = 0; i != finalArgs.getExpressions().size(); ++i)
                finalArgs.getExpressions().set(i, compiler.cast(finalArgs.getExpressions().get(i), constructor.getParameters()[i+base].getType()));
            
            final MethodNode constructor1 = constructor;
            final ClassNode compilerClass = compiler.classNode;
            return new BytecodeExpr(exp, type) {
                protected void compile(MethodVisitor mv) {
                    final String classInternalName = BytecodeHelper.getClassInternalName(getType());
                    mv.visitTypeInsn(NEW, classInternalName);
                    mv.visitInsn(DUP);

                    int first = 0;
//                    if ((getType().getModifiers() & ACC_STATIC) == 0 && getType().redirect() instanceof InnerClassNode) {
//                        mv.visitVarInsn(ALOAD, 0);
//                        for (ClassNode tp = compilerClass ; tp != getType().redirect().getOuterClass(); ) {
//                            compiler.context.setOuterClassInstanceUsed(tp);
//                            final ClassNode outerTp = tp.redirect().getOuterClass();
//                            mv.visitFieldInsn(GETFIELD, BytecodeHelper.getClassInternalName(tp), "this$0", BytecodeHelper.getTypeDescription(outerTp));
//                            tp = outerTp;
//                        }
//                        first = 1;
//                    }

                    for (int i = 0; i != finalArgs.getExpressions().size(); ++i) {
                        BytecodeExpr be = (BytecodeExpr) finalArgs.getExpressions().get(i);
                        be.visit(mv);
                        final ClassNode paramType = constructor1.getParameters()[i+first].getType();
                        final ClassNode type = be.getType();
                        box(type, mv);
                        cast(TypeUtil.wrapSafely(type), TypeUtil.wrapSafely(paramType), mv);
                        unbox(paramType, mv);
                    }

                    mv.visitMethodInsn(INVOKESPECIAL, classInternalName, "<init>", BytecodeHelper.getMethodDescriptor(ClassHelper.VOID_TYPE, constructor1.getParameters()));
                }
            };
        }

        if (compiler.policy == TypePolicy.STATIC) {
            compiler.addError("Cannot find constructor", exp);
            return null;
        } else
            return createDynamicCall(exp, compiler, newArgs);
    }

    private Expression createDynamicCall(final ConstructorCallExpression exp, CompilerTransformer compiler, TupleExpression newArgs) {
        final List<Expression> args = newArgs.getExpressions();

        for (int i = 0; i != args.size(); ++i) {
            BytecodeExpr arg = compiler.transformSynthetic((BytecodeExpr) args.get(i));
            if (arg instanceof CompiledClosureBytecodeExpr) {
                compiler.processPendingClosure((CompiledClosureBytecodeExpr) arg);
            }
            args.set(i, arg);
        }

        return new BytecodeExpr(exp, exp.getType()) {
            protected void compile(MethodVisitor mv) {
                mv.visitLdcInsn(BytecodeHelper.getClassLoadingTypeDescription(getType()));
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
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
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/codehaus/groovy/runtime/InvokerHelper", "invokeConstructorOf", "(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/Object;");
                mv.visitTypeInsn(Opcodes.CHECKCAST, BytecodeHelper.getClassInternalName(getType()));
            }
        };
    }

    private boolean improveWhatInnerClassVisitorDid(InnerClassNode innerClassNode, ConstructorCallExpression exp, CompilerTransformer compiler) {
        // here we should fix constructor parameter types InnerClassVisitor did with constructor
        // and add compile class itself
        final ClassNode superClass = innerClassNode.getSuperClass();

        final ConstructorNode constructorNode = innerClassNode.getDeclaredConstructors().get(0);
        final VariableScope scope = innerClassNode.getVariableScope();

        final boolean isStatic = !(superClass.redirect() instanceof InnerClassNode) || (superClass.getModifiers() & ACC_STATIC) != 0;
        if (!isStatic && compiler.methodNode.getVariableScope().isInStaticContext()) {
            compiler.addError("Can not instantiate non-static inner class " + superClass.getName() + " in static context", exp);
            return false;
        }

        int additional = 1 + scope.getReferencedLocalVariablesCount();
        TupleExpression te = (TupleExpression) exp.getArguments();

        // remove all added parameters
        for (int i = 0; i != additional; ++i) {
            te.getExpressions().remove(0);
        }

        if (!isStatic) {
            te.getExpressions().add(0, VariableExpression.THIS_EXPRESSION);
        }

        final TupleExpression newArgs = (TupleExpression) compiler.transform(exp.getArguments());
        final ClassNode[] argTypes = compiler.exprToTypeArray(newArgs);

        final MethodNode constr = findConstructorWithClosureCoercion(superClass, argTypes, compiler);
        if (constr == null) {
            compiler.addError("Cannot find constructor", exp);
            return false;
        }

        final ArgumentListExpression superArgs = new ArgumentListExpression();
        if (!isStatic) {
            superArgs.getExpressions().add(new VariableExpression("p$0"));
        }

        final Parameter[] superParams = constr.getParameters();
        for(int i = 0; i != superParams.length; ++i) {
            superArgs.getExpressions().add(new VariableExpression("p$"+(i+additional)));
        }

        BlockStatement code = (BlockStatement) constructorNode.getCode();
        code.getStatements().remove(0);
        code.getStatements().add(0, 
                new ExpressionStatement(
                        new ConstructorCallExpression(ClassNode.SUPER, superArgs)
                )
        );

        final Parameter[] params = constructorNode.getParameters();

        if (isStatic) {
            te.getExpressions().add(0, VariableExpression.THIS_EXPRESSION);
        }

        int pCount = 0;
        for (Iterator it=scope.getReferencedLocalVariablesIterator(); it.hasNext();) {
            pCount++;

            org.codehaus.groovy.ast.Variable var = (org.codehaus.groovy.ast.Variable) it.next();
            final ClassNode vtype = compiler.transform(new VariableExpression(var)).getType();

            params[pCount].setType(vtype);
            innerClassNode.getField(var.getName()).setType(vtype);

            te.getExpressions().add(pCount, new VariableExpression(var));
        }

        for(int i = 0; i != superParams.length; ++i) {
            params[additional+i] = new Parameter(superParams[i].getType(), "p$" + (i+additional));
        }

        ClassNodeCache.clearCache(innerClassNode);
        return true;
    }

    private void rewriteThis0(ConstructorCallExpression exp, CompilerTransformer compiler) {
        if (!(exp.getType().redirect() instanceof InnerClassNode)) return;
        InnerClassNode inner = (InnerClassNode) exp.getType().redirect();
        if ((inner.getModifiers() & ACC_STATIC) != 0) return;

        Expression this0 = VariableExpression.THIS_EXPRESSION;
        ClassNode tp = compiler.classNode;
        for ( ; tp != null && tp != inner.redirect().getOuterClass(); ) {
            compiler.context.setOuterClassInstanceUsed(tp);
            tp = tp.redirect().getOuterClass();
            this0 = new PropertyExpression(this0, "this$0");
        }

        if (tp == null)
           return;

        ((TupleExpression)exp.getArguments()).getExpressions().set(0, this0);
    }

    // Insert array creation for varargs methods.
    // Precondition: isApplicable.
    private static ArgumentListExpression wrapArgumentsForVarargs(TupleExpression args, Parameter[] params, int base) {
        List<Expression> unwrapped = args.getExpressions();
        List<Expression> wrapped = new ArrayList<Expression>();
        int nparams = params.length - base;
        for (int i = 0; i < nparams - 1; i++) {
            wrapped.add(args.getExpression(i));
        }
        int diff = unwrapped.size() - nparams;
        assert diff >= -1;
        if (diff > 0) {
            List<Expression> add = new ArrayList<Expression>(diff);
            for (int i = -1; i < diff; i++) {
                add.add(args.getExpression(nparams + i));
            }
            wrapped.add(new ListExpression(add));
        } else if (diff == 0) {
            if (nparams > 0) {
                wrapped.add(args.getExpression(nparams - 1));
            }
        } else if (diff == -1) {
            wrapped.add(new ListExpression(new ArrayList<Expression>()));
        }
        return new ArgumentListExpression(wrapped);
    }

    private Expression transformSpecial(ConstructorCallExpression exp, CompilerTransformer compiler) {
        final ClassNode type = exp.isSuperCall() ? compiler.classNode.getSuperClass() : compiler.classNode;
        Expression args = exp.getArguments();

//        if (type instanceof InnerClassNode) {
//            if((type.getModifiers() & ACC_STATIC) == 0) {
//                if (!(args instanceof TupleExpression)) {
//                    args = new TupleExpression(args);
//                }
//                ((TupleExpression)args).getExpressions().add(0, new VariableExpression(compiler.methodNode.getParameters()[0]));
//            }
//        }

        final Expression newArgs = compiler.transform(args);
        final ClassNode[] argTypes = compiler.exprToTypeArray(newArgs);

        MethodNode constructor = compiler.findConstructor(type, argTypes);
        if (constructor != null) {
            return ResolvedMethodBytecodeExpr.create(exp, constructor,
                    exp.isSuperCall() ?
                        new VariableExpressionTransformer.Super(VariableExpression.SUPER_EXPRESSION, compiler)
                      : new VariableExpressionTransformer.ThisSpecial(VariableExpression.THIS_EXPRESSION, compiler), 
                    (TupleExpression) newArgs, compiler);
        }

        compiler.addError("Cannot find constructor", exp);
        return null;
    }

    private static MethodNode findConstructorVariatingArgs(ClassNode type, ClassNode[] argTypes, CompilerTransformer compiler, int firstNonVariating) {
        MethodNode foundMethod = compiler.findConstructor(type, argTypes);
        if (foundMethod != null) {
            return foundMethod;
        }

        if (argTypes.length > 0) {
            for (int i=firstNonVariating+1; i < argTypes.length; ++i) {
                final ClassNode oarg = argTypes[i];
                if (oarg == null)
                    continue;
                
                if (oarg.implementsInterface(TypeUtil.TCLOSURE)) {
                    foundMethod = findConstructorVariatingArgs(type, argTypes, compiler, i);

                    if (foundMethod != null) {
                        Parameter p[] = foundMethod.getParameters();
                        if (p.length == argTypes.length) {
                            return foundMethod;
                        }
                    }

                    argTypes[i] = null;
                    foundMethod = findConstructorVariatingArgs(type, argTypes, compiler, i);
                    if (foundMethod != null) {
                        Parameter p[] = foundMethod.getParameters();
                        if (p.length == argTypes.length) {
                            ClassNode argType = p[i].getType();
                            if (argType.equals(ClassHelper.CLOSURE_TYPE)) {
                                ClosureUtil.improveClosureType(oarg, ClassHelper.CLOSURE_TYPE);
                                StaticMethodBytecode.replaceMethodCode(compiler.su, compiler.context, ((ClosureClassNode)oarg).getDoCallMethod(), compiler.compileStack, compiler.debug == -1 ? -1 : compiler.debug+1, compiler.policy, oarg.getName());
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
                                    ClosureUtil.makeOneMethodClass(oarg, argType, one, doCall, compiler);
                                }
                            }
                        }
                    }
                    argTypes[i] = oarg;
                    return foundMethod;
                }
                else {
                    if (oarg.implementsInterface(TypeUtil.TMAP)) {
                        foundMethod = findConstructorVariatingArgs(type, argTypes, compiler, i);

                        if (foundMethod != null) {
                            Parameter p[] = foundMethod.getParameters();
                            if (p.length == argTypes.length) {
                                return foundMethod;
                            }
                        }

                        argTypes[i] = null;
                        foundMethod = findConstructorVariatingArgs(type, argTypes, compiler, i);
                        if (foundMethod != null) {
                            Parameter p[] = foundMethod.getParameters();
                            if (p.length == argTypes.length) {
                                ClassNode argType = p[i].getType();

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
                            }
                        }
                        argTypes[i] = oarg;
                        return foundMethod;
                    }
                    else {
                        if (oarg.implementsInterface(TypeUtil.TLIST)) {
                            foundMethod = findConstructorVariatingArgs(type, argTypes, compiler, i);

                            if (foundMethod != null) {
                                Parameter p[] = foundMethod.getParameters();
                                if (p.length == argTypes.length) {
                                    return foundMethod;
                                }
                            }

                            argTypes[i] = null;
                            foundMethod = findConstructorVariatingArgs(type, argTypes, compiler, i);
                            if (foundMethod != null) {
                                Parameter p[] = foundMethod.getParameters();
                                if (p.length == argTypes.length) {
                                    ClassNode argType = p[i].getType();

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
                                }
                            }
                            argTypes[i] = oarg;
                            return foundMethod;
                        } else if (oarg.implementsInterface(TypeUtil.TTHIS)) {
                            ClassNode run = oarg.getOuterClass();
                            while (run != null) {
                                argTypes[i] = run;
                                foundMethod = findConstructorVariatingArgs(type, argTypes, compiler, i);
                                if (foundMethod != null) return foundMethod;
                                if ((run.getModifiers() & Opcodes.ACC_STATIC) != 0) break;
                                run = run.getOuterClass();
                            }
                            argTypes[i] = oarg.getOuterClass();
                        }
                    }
                }
            }
        }
        return null;
    }

    public static MethodNode findConstructorWithClosureCoercion(ClassNode type, ClassNode[] argTypes, CompilerTransformer compiler) {
        return findConstructorVariatingArgs(type, argTypes, compiler, -1);
    }
}
