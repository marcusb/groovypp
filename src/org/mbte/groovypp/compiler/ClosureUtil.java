package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.classgen.BytecodeSequence;
import org.codehaus.groovy.classgen.BytecodeInstruction;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.MethodVisitor;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;

import java.util.List;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

public class ClosureUtil {
    private static final LinkedList<MethodNode> NONE = new LinkedList<MethodNode> ();

    public static boolean likeGetter(MethodNode method) {
        return method.getName().startsWith("get")
                && ClassHelper.VOID_TYPE != method.getReturnType()
                && method.getParameters().length == 0;
    }

    public static boolean likeSetter(MethodNode method) {
        return method.getName().startsWith("set")
                && ClassHelper.VOID_TYPE == method.getReturnType()
                && method.getParameters().length == 1;
    }

    public synchronized static List<MethodNode> isOneMethodAbstract (ClassNode node) {
        if ((node.getModifiers() & Opcodes.ACC_ABSTRACT) == 0 && !node.isInterface())
            return null;

        final ClassNodeCache.ClassNodeInfo info = ClassNodeCache.getClassNodeInfo(node);
        if (info.isOneMethodAbstract == null) {
            List<MethodNode> am = node.getAbstractMethods();

            if (am == null) {
                am = (List<MethodNode>) Collections.EMPTY_LIST;
            }

            MethodNode one = null;
            for (Iterator it = am.iterator(); it.hasNext();) {
                MethodNode mn = (MethodNode) it.next();
                if (!likeGetter(mn) && !likeSetter(mn) && !traitMethod(mn)) {
                    if (one != null) {
                        info.isOneMethodAbstract = NONE;
                        return null;
                    }
                    one = mn;
                    it.remove();
                }
            }

            if (one != null)
                am.add(0, one);
            else
                if (am.size() != 1) {
                    info.isOneMethodAbstract = NONE;
                    return null;
                }

            info.isOneMethodAbstract = am;
        }

        if (info.isOneMethodAbstract == NONE)
            return null;

        return info.isOneMethodAbstract;
    }

    private static boolean traitMethod(MethodNode mn) {
        return !mn.getAnnotations(TypeUtil.HAS_DEFAULT_IMPLEMENTATION).isEmpty();
    }

    public static MethodNode isMatch(List<MethodNode> one, ClosureClassNode closureType, CompilerTransformer compiler, ClassNode baseType) {
        class Mutation {
            final Parameter p;
            final ClassNode t;

            public Mutation(ClassNode t, Parameter p) {
                this.t = t;
                this.p = p;
            }

            void mutate () {
                p.setType(t);
            }
        }

        List<Mutation> mutations = null;

        MethodNode missing = one.get(0);
        Parameter[] missingMethodParameters = missing.getParameters();
        List<MethodNode> methods = closureType.getDeclaredMethods("doCall");

//if (missing.getName().equals("call"))
//        System.err.println("MISSING " + missing.getDeclaringClass() + " " + missing.toString());

        for (MethodNode method : methods) {
            
//if (missing.getName().equals("call"))
//        System.err.println(method.getDeclaringClass() + " " + method.toString());

            Parameter[] closureParameters = method.getParameters();

            if (closureParameters.length != missingMethodParameters.length)
                continue;

            boolean match = true;
            for (int i = 0; i < closureParameters.length; i++) {
                Parameter closureParameter = closureParameters[i];
                Parameter missingMethodParameter = missingMethodParameters[i];

                ClassNode parameterType = missingMethodParameter.getType();
                parameterType = TypeUtil.getSubstitutedType(parameterType, baseType.redirect(), baseType);
                if (!TypeUtil.isAssignableFrom(parameterType, closureParameter.getType())) {
                    if (closureParameter.getType() == ClassHelper.DYNAMIC_TYPE) {
                        if (mutations == null)
                            mutations = new LinkedList<Mutation> ();
                        mutations.add(new Mutation(parameterType, closureParameter));
//if (missing.getName().equals("call"))
//        System.err.println("MUTATE: " + missingMethodParameter.getType() + " " + closureParameter.getName());
                        continue;
                    }
//if (missing.getName().equals("call"))
//        System.err.println("FAIL: " + missingMethodParameter.getType() + " " + closureParameter.getType());

                    match = false;
                    break;
                }
            }

            if (match) {
                if (mutations != null)
                    for (Mutation mutation : mutations) {
                        mutation.mutate();
                    }

                improveClosureType(closureType, baseType);
                StaticMethodBytecode.replaceMethodCode(compiler.su, method, compiler.compileStack, compiler.debug == -1 ? -1 : compiler.debug+1, compiler.policy, compiler.classNode.getName());
                return method;
            }
        }
        return null;
    }

    public static void makeOneMethodClass(final ClassNode closureType, ClassNode baseType, List<MethodNode> abstractMethods, final MethodNode doCall) {
        boolean traitMethods = false;
        int k = 0;
        for (final MethodNode missed : abstractMethods) {
            if (k == 0) {
               closureType.addMethod(
                    missed.getName(),
                    Opcodes.ACC_PUBLIC,
                    getSubstitutedReturnType(doCall, missed, closureType, baseType),
                    missed.getParameters(),
                    ClassNode.EMPTY_ARRAY,
                    new BytecodeSequence(
                            new BytecodeInstruction() {
                                public void visit(MethodVisitor mv) {
                                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                                    Parameter pp[] = missed.getParameters();
                                    for (int i = 0, k = 1; i != pp.length; ++i) {
                                        final ClassNode type = pp[i].getType();
                                        ClassNode expectedType = doCall.getParameters()[i].getType();
                                        if (ClassHelper.isPrimitiveType(type)) {
                                            if (type == ClassHelper.long_TYPE) {
                                                mv.visitVarInsn(Opcodes.LLOAD, k++);
                                                k++;
                                            } else if (type == ClassHelper.double_TYPE) {
                                                mv.visitVarInsn(Opcodes.DLOAD, k++);
                                                k++;
                                            } else if (type == ClassHelper.float_TYPE) {
                                                mv.visitVarInsn(Opcodes.FLOAD, k++);
                                            } else {
                                                mv.visitVarInsn(Opcodes.ILOAD, k++);
                                            }
                                        } else {
                                            mv.visitVarInsn(Opcodes.ALOAD, k++);
                                        }
                                        BytecodeExpr.box(type, mv);
                                        BytecodeExpr.cast(TypeUtil.wrapSafely(type), TypeUtil.wrapSafely(expectedType), mv);
                                        BytecodeExpr.unbox(expectedType, mv);
                                    }
                                    mv.visitMethodInsn(
                                            Opcodes.INVOKEVIRTUAL,
                                            BytecodeHelper.getClassInternalName(doCall.getDeclaringClass()),
                                            doCall.getName(),
                                            BytecodeHelper.getMethodDescriptor(doCall.getReturnType(), doCall.getParameters())
                                    );

                                    if (missed.getReturnType() != ClassHelper.VOID_TYPE) {
                                        BytecodeExpr.box(doCall.getReturnType(), mv);
                                        BytecodeExpr.checkCast(TypeUtil.wrapSafely(doCall.getReturnType()), mv);
                                        BytecodeExpr.unbox(missed.getReturnType(), mv);
                                    }
                                    BytecodeExpr.doReturn(mv, missed.getReturnType());
                                }
                            }
                    ));
            }
            else {
                if (ClosureUtil.likeGetter(missed)) {
                    String pname = missed.getName().substring(3);
                    pname = Character.toLowerCase(pname.charAt(0)) + pname.substring(1);
                    closureType.addProperty(pname, Opcodes.ACC_PUBLIC, missed.getReturnType(), null, null, null);
                }
                else {
                    if (ClosureUtil.likeSetter(missed)) {
                        String pname = missed.getName().substring(3);
                        pname = Character.toLowerCase(pname.charAt(0)) + pname.substring(1);
                        closureType.addProperty(pname, Opcodes.ACC_PUBLIC, missed.getParameters()[0].getType(), null, null, null);
                    }
                    else {
                        if (ClosureUtil.traitMethod(missed)) {
                            traitMethods = true;
                        }
                    }
                }
            }
            k++;
        }

        if (traitMethods)
            TraitASTTransformFinal.improveAbstractMethods(closureType);
    }

    private static ClassNode getSubstitutedReturnType(MethodNode doCall, MethodNode missed, ClassNode closureType,
                                                      ClassNode baseType) {
        ClassNode returnType = missed.getReturnType();
        if (missed.getParameters().length == doCall.getParameters().length) {
            int nParams = missed.getParameters().length;
            ClassNode declaringClass = missed.getDeclaringClass();
            GenericsType[] typeVars = declaringClass.getGenericsTypes();
            if (typeVars != null && typeVars.length > 0) {
                ClassNode[] formals = new ClassNode[nParams + 1];
                ClassNode[] actuals = new ClassNode[nParams + 1];
                for (int i = 0; i < nParams; i++) {
                    actuals[i] = doCall.getParameters()[i].getType();
                    formals[i] = missed.getParameters()[i].getType();
                }
                actuals[actuals.length - 1] = doCall.getReturnType();
                formals[formals.length - 1] = missed.getReturnType();
                ClassNode[] unified = TypeUnification.inferTypeArguments(typeVars, formals, actuals);
                if (totalInference(unified)) {
                    GenericsType[] genericTypes = new GenericsType[unified.length];
                    for (int i = 0; i < genericTypes.length; i++) {
                        genericTypes[i] = new GenericsType(unified[i]);
                    }
                    ClassNode newBase = TypeUtil.withGenericTypes(baseType, genericTypes);
                    improveClosureType(closureType, newBase);
                    returnType = TypeUtil.getSubstitutedType(returnType, declaringClass, baseType);
                }
            }
        }
        return returnType;
    }

    private static boolean totalInference(ClassNode[] nodes) {
        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i] == null) return false;
        }
        return true;
    }

    public static void improveClosureType(final ClassNode closureType, ClassNode baseType) {
        if (baseType.isInterface()) {
            closureType.setInterfaces(new ClassNode[]{baseType});
            closureType.setSuperClass(ClassHelper.OBJECT_TYPE);
        } else {
            closureType.setInterfaces(ClassNode.EMPTY_ARRAY);
            closureType.setSuperClass(baseType);
        }
    }
}
