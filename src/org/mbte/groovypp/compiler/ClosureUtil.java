package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.classgen.BytecodeSequence;
import org.codehaus.groovy.classgen.BytecodeInstruction;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;
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
                if (!likeGetter(mn) && !likeSetter(mn)) {
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

        ClassNode outmost = closureType;
        while (outmost instanceof ClosureClassNode) {
            outmost = ((ClosureClassNode)outmost).getOwner();
        }

        List<Mutation> mutations = null;

        MethodNode missing = one.get(0);
        Parameter[] missingMethodParameters = missing.getParameters();
        List<MethodNode> methods = outmost.getDeclaredMethods("$doCall");
        for (MethodNode method : methods) {
            Parameter[] closureParameters = method.getParameters();
            if (closureParameters.length == 0 || closureParameters[0].getType() != closureType)
                continue;

            if (closureParameters.length != missingMethodParameters.length+1)
                continue;

            boolean match = true;
            for (int i = 1; i < closureParameters.length; i++) {
                Parameter closureParameter = closureParameters[i];
                Parameter missingMethodParameter = missingMethodParameters[i-1];

                if (!TypeUtil.isAssignableFrom(missingMethodParameter.getType(), closureParameter.getType())) {
                    if (closureParameter.getType() == ClassHelper.DYNAMIC_TYPE) {
                        if (mutations == null)
                            mutations = new LinkedList<Mutation> ();
                        mutations.add(new Mutation(missingMethodParameter.getType(), closureParameter));
                        continue;
                    }
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
                StaticMethodBytecode.replaceMethodCode(compiler.su, method, compiler.compileStack, compiler.debug == -1 ? -1 : compiler.debug+1, compiler.policy);
                return method;
            }
        }
        return null;
    }

    public static void makeOneMethodClass(final ClassNode closureType, ClassNode baseType, List<MethodNode> abstractMethods, final MethodNode doCall) {
        int k = 0;
        for (final MethodNode missed : abstractMethods) {
            if (k == 0) {
                closureType.addMethod(
                missed.getName(),
                Opcodes.ACC_PUBLIC,
                getSubstitutedReturnType(doCall, missed, baseType),
                missed.getParameters(),
                ClassNode.EMPTY_ARRAY,
                new BytecodeSequence(
                        new BytecodeInstruction() {
                            public void visit(MethodVisitor mv) {
                                mv.visitVarInsn(Opcodes.ALOAD, 0);
                                Parameter pp[] = missed.getParameters();
                                for (int i = 0, k = 1; i != pp.length; ++i) {
                                    final ClassNode type = pp[i].getType();
                                    ClassNode expectedType = doCall.getParameters()[i + 1].getType();
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
                                        Opcodes.INVOKESTATIC,
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

                if (ClosureUtil.likeSetter(missed)) {
                    String pname = missed.getName().substring(3);
                    pname = Character.toLowerCase(pname.charAt(0)) + pname.substring(1);
                    closureType.addProperty(pname, Opcodes.ACC_PUBLIC, missed.getParameters()[0].getType(), null, null, null);
                }
            }
            k++;
        }

        if (abstractMethods.size() == 1) {
            final MethodNode missed = abstractMethods.get(0);
            closureType.addMethod(
                    missed.getName(),
                    Opcodes.ACC_PUBLIC,
                    missed.getReturnType(),
                    missed.getParameters(),
                    ClassNode.EMPTY_ARRAY,
                    new BytecodeSequence(
                            new BytecodeInstruction() {
                                public void visit(MethodVisitor mv) {
                                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                                    Parameter pp[] = missed.getParameters();
                                    for (int i = 0, k = 1; i != pp.length; ++i) {
                                        final ClassNode type = pp[i].getType();
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
                                    }
                                    mv.visitMethodInsn(
                                            Opcodes.INVOKEVIRTUAL,
                                            BytecodeHelper.getClassInternalName(closureType),
                                            "doCall",
                                            BytecodeHelper.getMethodDescriptor(ClassHelper.OBJECT_TYPE, pp)
                                    );

                                    if (missed.getReturnType() != ClassHelper.VOID_TYPE) {
                                        if (ClassHelper.isPrimitiveType(missed.getReturnType())) {
                                            String returnString = "(Ljava/lang/Object;)" + BytecodeHelper.getTypeDescription(missed.getReturnType());
                                            mv.visitMethodInsn(
                                                    Opcodes.INVOKESTATIC,
                                                    BytecodeHelper.getClassInternalName(DefaultTypeTransformation.class.getName()),
                                                    missed.getReturnType().getName() + "Unbox",
                                                    returnString);
                                        } else {
                                            BytecodeExpr.checkCast(missed.getReturnType(), mv);
                                        }
                                    }
                                    BytecodeExpr.doReturn(mv, missed.getReturnType());
                                }
                            }
                    ));
        }
    }

    private static ClassNode getSubstitutedReturnType(MethodNode doCall, MethodNode missed, ClassNode baseType) {
        ClassNode returnType = missed.getReturnType();
        if (missed.getParameters().length + 1 == doCall.getParameters().length) {
            int nParams = missed.getParameters().length;
            ClassNode declaringClass = missed.getDeclaringClass();
            GenericsType[] typeVars = declaringClass.getGenericsTypes();
            if (typeVars != null && typeVars.length > 0) {
                ClassNode[] formals = new ClassNode[nParams + 1];
                ClassNode[] actuals = new ClassNode[nParams + 1];
                for (int i = 0; i < nParams; i++) {
                    actuals[i] = doCall.getParameters()[i + 1].getType();
                    formals[i] = missed.getParameters()[i].getType();
                }
                actuals[actuals.length - 1] = doCall.getReturnType();
                formals[formals.length - 1] = missed.getReturnType();
                ClassNode[] unified = TypeUnification.inferTypeArguments(typeVars, formals, actuals);
                if (unified != null) {
                    GenericsType[] genericTypes = new GenericsType[unified.length];
                    for (int i = 0; i < genericTypes.length; i++) {
                        genericTypes[i] = new GenericsType(unified[i]);
                    }
                    //baseType.setGenericsTypes(genericTypes);
                    returnType = TypeUtil.getSubstitutedType(returnType, declaringClass, baseType);
                }
            }
        }
        return returnType;
    }

    public static void improveClosureType(final ClassNode closureType, ClassNode baseType) {
        if (baseType.isInterface()) {
            closureType.setInterfaces(new ClassNode[]{baseType});
        } else {
            closureType.setInterfaces(ClassNode.EMPTY_ARRAY);
            closureType.setSuperClass(baseType);
        }
    }
}
