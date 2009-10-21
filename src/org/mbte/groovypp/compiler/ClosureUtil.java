package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.Parameter;
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

    public static MethodNode isMatch(List<MethodNode> one, ClassNode closureType) {
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
        for (MethodNode method : closureType.getDeclaredMethods("doCall")) {
            Parameter[] closureParameters = method.getParameters();
            if (closureParameters.length != missingMethodParameters.length)
                continue;

            boolean match = true;
            for (int i = 0; i < closureParameters.length; i++) {
                Parameter closureParameter = closureParameters[i];
                Parameter missingMethodParameter = missingMethodParameters[i];

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
                return method;
            }
        }
        return null;
    }

    public static void makeOneMethodClass(final ClassNode oarg, ClassNode tp, List<MethodNode> am, final MethodNode doCall) {
        final ClassNode[] oifaces = oarg.getInterfaces();
        ClassNode[] ifaces;
        if (tp.isInterface()) {
            ifaces = new ClassNode[oifaces.length + 1];
            System.arraycopy(oifaces, 0, ifaces, 1, oifaces.length);
            ifaces[0] = tp;
        } else {
            ifaces = new ClassNode[oifaces.length];
            for (int i = 0, k = 0; i != oifaces.length; i++) {
                if (oifaces[i] != TypeUtil.TCLOSURE)
                    ifaces[k++] = oifaces[i];
            }
            ifaces[oifaces.length - 1] = TypeUtil.OWNER_AWARE_SETTER;
            oarg.setSuperClass(tp);
            oarg.addProperty("owner", Opcodes.ACC_PUBLIC, ClassHelper.OBJECT_TYPE, null, null, null);
        }
        oarg.setInterfaces(ifaces);

        int k = 0;
        for (final MethodNode missed : am) {
            if (k == 0) {
                oarg.addMethod(
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
                                    BytecodeExpr.box(type, mv);
                                    ClassNode doCallParamType = doCall.getParameters()[i].getType();
                                    mv.visitTypeInsn(Opcodes.CHECKCAST, BytecodeHelper.getClassInternalName(ClassHelper.getWrapper(doCallParamType)));
                                    BytecodeExpr.unbox(doCallParamType, mv);
                                }
                                mv.visitMethodInsn(
                                        Opcodes.INVOKEVIRTUAL,
                                        BytecodeHelper.getClassInternalName(oarg),
                                        "doCall",
                                        BytecodeHelper.getMethodDescriptor(doCall.getReturnType(), doCall.getParameters())
                                );

                                if (missed.getReturnType() != ClassHelper.VOID_TYPE) {
                                    BytecodeExpr.box(doCall.getReturnType(), mv);
                                    mv.visitTypeInsn(Opcodes.CHECKCAST, BytecodeHelper.getClassInternalName(ClassHelper.getWrapper(doCall.getReturnType())));
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
                    oarg.addProperty(pname, Opcodes.ACC_PUBLIC, missed.getReturnType(), null, null, null);
                }

                if (ClosureUtil.likeSetter(missed)) {
                    String pname = missed.getName().substring(3);
                    pname = Character.toLowerCase(pname.charAt(0)) + pname.substring(1);
                    oarg.addProperty(pname, Opcodes.ACC_PUBLIC, missed.getParameters()[0].getType(), null, null, null);
                }
            }
            k++;
        }

        if (am.size() == 1) {
            final MethodNode missed = am.get(0);
            oarg.addMethod(
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
                                            BytecodeHelper.getClassInternalName(oarg),
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
                                            mv.visitTypeInsn(Opcodes.CHECKCAST, BytecodeHelper.getClassInternalName(missed.getReturnType()));
                                        }
                                    }
                                    BytecodeExpr.doReturn(mv, missed.getReturnType());
                                }
                            }
                    ));
        }
    }
}
