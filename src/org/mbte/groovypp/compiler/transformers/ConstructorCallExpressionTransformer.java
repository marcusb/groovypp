package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.mbte.groovypp.compiler.*;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.mbte.groovypp.compiler.bytecode.ResolvedMethodBytecodeExpr;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayList;
import java.util.List;

public class ConstructorCallExpressionTransformer extends ExprTransformer<ConstructorCallExpression> {
    private static final ClassNode[] MAP_ARGS = new ClassNode[]{TypeUtil.LINKED_HASH_MAP_TYPE};

    public Expression transform(ConstructorCallExpression exp, final CompilerTransformer compiler) {

        if (exp.isSuperCall() || exp.isThisCall())
            return transformSpecial (exp, compiler);

        MethodNode constructor;
        ClassNode type = exp.getType();
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
            // Improve type.
            GenericsType[] generics = type.redirect().getGenericsTypes();
            // We don't support inference if the method itself is parameterized.
            if (generics != null && constructor.getGenericsTypes() == null) {
                Parameter[] params = constructor.getParameters();
                ClassNode[] paramTypes = new ClassNode[params.length];
                for (int i = 0; i < paramTypes.length; i++) {
                    paramTypes[i] = params[i].getType();
                }
                ClassNode[] unified = TypeUnification.inferTypeArguments(generics, paramTypes, argTypes);
                if (TypeUnification.totalInference(unified)) {
                    type = TypeUtil.withGenericTypes(type, unified);
                }
            }
            int first = 0;
            if ((type.getModifiers() & ACC_STATIC) == 0 && type.redirect() instanceof InnerClassNode) {
                first = 1;
            }
            for (int i = 0; i != newArgs.getExpressions().size(); ++i)
                newArgs.getExpressions().set(i, compiler.cast(newArgs.getExpressions().get(i), constructor.getParameters()[i+first].getType()));
            
            final MethodNode constructor1 = constructor;
            final ClassNode compilerClass = compiler.classNode;
            return new BytecodeExpr(exp, type) {
                protected void compile(MethodVisitor mv) {
                    final String classInternalName = BytecodeHelper.getClassInternalName(getType());
                    mv.visitTypeInsn(NEW, classInternalName);
                    mv.visitInsn(DUP);

                    int first = 0;
                    if ((getType().getModifiers() & ACC_STATIC) == 0 && getType().redirect() instanceof InnerClassNode) {
                        mv.visitVarInsn(ALOAD, 0);
                        for (ClassNode tp = compilerClass ; tp != getType().redirect().getOuterClass(); ) {
                            final ClassNode outerTp = tp.redirect().getOuterClass();
                            mv.visitFieldInsn(GETFIELD, BytecodeHelper.getClassInternalName(tp), "this$0", BytecodeHelper.getTypeDescription(outerTp));
                            tp = outerTp;
                        }
                        first = 1;
                    }

                    ArgumentListExpression bargs = (ArgumentListExpression) newArgs;
                    for (int i = 0; i != bargs.getExpressions().size(); ++i) {
                        BytecodeExpr be = (BytecodeExpr) bargs.getExpressions().get(i);
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

        compiler.addError("Can't find constructor", exp);
        return null;
    }

    private Expression transformSpecial(ConstructorCallExpression exp, CompilerTransformer compiler) {
        final Expression newArgs = compiler.transform(exp.getArguments());
        final ClassNode[] argTypes = compiler.exprToTypeArray(newArgs);

        MethodNode constructor = compiler.findConstructor(exp.isSuperCall() ? compiler.classNode.getSuperClass() : compiler.classNode, argTypes);
        if (constructor != null) {
            return new ResolvedMethodBytecodeExpr(exp, constructor,
                    exp.isSuperCall() ?
                        new VariableExpressionTransformer.Super(VariableExpression.SUPER_EXPRESSION, compiler)
                      : new VariableExpressionTransformer.This(VariableExpression.THIS_EXPRESSION, compiler), 
                    (ArgumentListExpression) newArgs, compiler);
        }

        compiler.addError("Can't find constructor", exp);
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
                                StaticMethodBytecode.replaceMethodCode(compiler.su, compiler.context, ((ClosureClassNode)oarg).getDoCallMethod(), compiler.compileStack, compiler.debug == -1 ? -1 : compiler.debug+1, compiler.policy, compiler.classNode.getName());
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
