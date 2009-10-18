package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.classgen.BytecodeInstruction;
import org.codehaus.groovy.classgen.BytecodeSequence;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles generation of code for the @Trait annotation
 *
 * @author 2008-2009 Copyright (C) MBTE Sweden AB. All Rights Reserved.
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class TraitASTTransformFinal implements ASTTransformation, Opcodes {

    public void visit(ASTNode[] nodes, final SourceUnit source) {
        ModuleNode module = (ModuleNode) nodes[0];
        for (ClassNode classNode : module.getClasses()) {
            List<MethodNode> abstractMethods = getAbstractMethods(classNode);
            if (abstractMethods != null) {
                for (final MethodNode method : abstractMethods) {

                    List<AnnotationNode> list = method.getAnnotations(TypeUtil.HAS_DEFAULT_IMPLEMENTATION);
                    if (list != null && !list.isEmpty()) {
                        Expression member = list.get(0).getMember("value");
                        if (member != null) {
                            final Parameter[] oldParams = method.getParameters();
                            final Parameter[] params = new Parameter[oldParams.length + 1];
                            params[0] = new Parameter(method.getDeclaringClass(), "$self");
                            System.arraycopy(oldParams, 0, params, 1, oldParams.length);
                            final MethodNode found = member.getType().getMethod(method.getName(), params);
                            if (found != null) {
                                addImplMethod(classNode, method, oldParams, found);
                            }
                        }
                    } else {

                    }
                }
            }
        }
    }

    private void addImplMethod(ClassNode classNode, MethodNode method, final Parameter[] oldParams, final MethodNode found) {
        classNode.addMethod(method.getName(), Opcodes.ACC_PUBLIC, method.getReturnType(), oldParams, method.getExceptions(), new BytecodeSequence(new BytecodeInstruction() {
            public void visit(MethodVisitor mv) {
                mv.visitVarInsn(ALOAD, 0);
                int cur = 1;
                for (int i = 0; i != oldParams.length; ++i) {
                    if (!ClassHelper.isPrimitiveType(oldParams[i].getType())) {
                        mv.visitVarInsn(ALOAD, cur);
                        cur++;
                    } else {
                        if (oldParams[i].getType().equals(ClassHelper.long_TYPE)) {
                            mv.visitVarInsn(LLOAD, cur);
                            cur += 2;
                        } else {

                            if (oldParams[i].getType().equals(ClassHelper.double_TYPE)) {
                                mv.visitVarInsn(DLOAD, cur);
                                cur += 2;
                            } else {
                                if (oldParams[i].getType().equals(ClassHelper.float_TYPE)) {
                                    mv.visitVarInsn(FLOAD, cur);
                                    cur++;
                                } else {
                                    mv.visitVarInsn(ILOAD, cur);
                                    cur++;
                                }
                            }
                        }
                    }
                }
                mv.visitMethodInsn(INVOKESTATIC, BytecodeHelper.getClassInternalName(found.getDeclaringClass()), found.getName(), BytecodeHelper.getMethodDescriptor(found.getReturnType(), found.getParameters()));
                BytecodeExpr.doReturn(mv, found.getReturnType());
            }
        }));
    }

    private static Map<String, MethodNode> getDeclaredMethodsMap(ClassNode klazz) {
        // Start off with the methods from the superclass.
        ClassNode parent = klazz.getSuperClass();
        Map<String, MethodNode> result = null;
        if (parent != null) {
            result = getDeclaredMethodsMap(parent);
        } else {
            result = new HashMap<String, MethodNode>();
        }

        // add in unimplemented abstract methods from the interfaces
        for (ClassNode iface : klazz.getInterfaces()) {
            Map<String, MethodNode> ifaceMethodsMap = getDeclaredMethodsMap(iface);
            for (Map.Entry<String, MethodNode> methSig : ifaceMethodsMap.entrySet()) {
                MethodNode methNode = result.get(methSig.getKey());
                if (methNode == null || methNode.isAbstract() && (!hasDefaultImpl(methNode) || hasDefaultImpl(methSig.getValue()) && methSig.getValue().getDeclaringClass().implementsInterface(methNode.getDeclaringClass()))) {
                    result.put(methSig.getKey(), methSig.getValue());
                }
            }
        }

        // And add in the methods implemented in this class.
        for (MethodNode method : klazz.getMethods()) {
            if (method.isSynthetic())
                continue;

            String sig = method.getTypeDescriptor();
            if (!method.isAbstract())
                result.put(sig, method);
            else {
                MethodNode methNode = result.get(sig);
                if (methNode == null || methNode.isAbstract() && (!hasDefaultImpl(methNode) || hasDefaultImpl(method) && method.getDeclaringClass().implementsInterface(methNode.getDeclaringClass()))) {
                    result.put(sig, method);
                }
            }
        }
        return result;
    }

    private static List<MethodNode> getAbstractMethods(ClassNode klazz) {
        List<MethodNode> result = new ArrayList<MethodNode>(3);
        for (MethodNode method : getDeclaredMethodsMap(klazz).values()) {
            if (method.isAbstract()) {
                result.add(method);
            }
        }

        if (result.isEmpty()) {
            return null;
        } else {
            return result;
        }
    }

    private static boolean hasDefaultImpl(MethodNode method) {
        List<AnnotationNode> list = method.getAnnotations(TypeUtil.HAS_DEFAULT_IMPLEMENTATION);
        return (list != null && !list.isEmpty());
    }
}