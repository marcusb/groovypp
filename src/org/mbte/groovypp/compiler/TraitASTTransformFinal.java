package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.*;
import static org.codehaus.groovy.ast.ClassHelper.*;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.VariableExpression;
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
            if (classNode instanceof InnerClassNode && classNode.getName().endsWith("$TraitImpl")) {
                staticifyImplementationMethods(classNode, source);
            }
        }

        for (ClassNode classNode : module.getClasses()) {
            if (classNode instanceof InnerClassNode && classNode.getName().endsWith("$TraitImpl")) {
                continue;
            }

            improveAbstractMethods(classNode);
        }
    }

    private void staticifyImplementationMethods(ClassNode classNode, final SourceUnit source) {
        for (MethodNode methodNode : classNode.getMethods()) {
            if (methodNode.isStatic() || methodNode.isSynthetic())
                continue;

            Parameter[] parameters = methodNode.getParameters();
            Parameter[] newParameters = new Parameter[parameters.length + 1];
            final Parameter self = new Parameter(classNode.getInterfaces()[0], "$self");
            newParameters[0] = self;
            System.arraycopy(parameters, 0, newParameters, 1, parameters.length);

            methodNode.setParameters(newParameters);
            methodNode.setModifiers(methodNode.getModifiers() | Opcodes.ACC_STATIC);
            methodNode.getVariableScope().setInStaticContext(true);

            ClassCodeExpressionTransformer thisToSelf = new ClassCodeExpressionTransformer() {
                protected SourceUnit getSourceUnit() {
                    return source;
                }

                public Expression transform(Expression exp) {
                    if (exp instanceof VariableExpression && "this".equals(((VariableExpression) exp).getName()))
                        return new VariableExpression(self);
                    else
                        return super.transform(exp);
                }
            };
            thisToSelf.visitMethod(methodNode);
        }
    }

    public static void improveAbstractMethods(final ClassNode classNode) {
        List<MethodNode> abstractMethods = getAbstractMethods(classNode);
        if (abstractMethods != null) {
            for (final MethodNode method : abstractMethods) {
                List<AnnotationNode> list = method.getAnnotations(TypeUtil.HAS_DEFAULT_IMPLEMENTATION);
                if (list != null && !list.isEmpty()) {
                    Expression klazz = list.get(0).getMember("value");
                    Expression field = list.get(0).getMember("fieldName");
                    if (field == null || (field instanceof ConstantExpression) && (((ConstantExpression)field).getValue() == null || "".equals((((ConstantExpression)field).getValue())))) {
                        final Parameter[] oldParams = method.getParameters();
                        final Parameter[] params = new Parameter[oldParams.length + 1];
                        params[0] = new Parameter(method.getDeclaringClass(), "$self");
                        System.arraycopy(oldParams, 0, params, 1, oldParams.length);
                        final MethodNode found = klazz.getType().getMethod(method.getName(), params);
                        if (found != null) {
                            addImplMethod(classNode, method, oldParams, found);
                        }
                    } else {
                        if ((classNode.getModifiers() & Opcodes.ACC_ABSTRACT) != 0)
                            continue;

                        final ClassNode fieldType;
                        final boolean getter;
                        if (method.getName().startsWith("get")) {
                            fieldType = method.getReturnType();
                            getter = true;
                        } else {
                            fieldType = method.getParameters()[0].getType();
                            getter = false;
                        }

                        final String fieldName = (String) ((ConstantExpression) field).getValue();
                        FieldNode klazzField = classNode.getField(fieldName);
                        if (klazzField == null)
                            klazzField = classNode.addField(fieldName, ACC_PRIVATE, fieldType, null);

                        classNode.addMethod(method.getName(), ACC_PUBLIC, getter ? method.getReturnType() : ClassHelper.VOID_TYPE, method.getParameters(), ClassNode.EMPTY_ARRAY,
                                new BytecodeSequence(new BytecodeInstruction() {
                                    public void visit(MethodVisitor mv) {
                                        if (getter) {
                                            mv.visitVarInsn(ALOAD, 0);
                                            mv.visitFieldInsn(GETFIELD, BytecodeHelper.getClassInternalName(classNode), fieldName, BytecodeHelper.getTypeDescription(fieldType));
                                            BytecodeExpr.doReturn(mv, fieldType);
                                        } else {
                                            mv.visitVarInsn(ALOAD, 0);
                                            if (fieldType == double_TYPE) {
                                                mv.visitVarInsn(Opcodes.DLOAD, 1);
                                            } else if (fieldType == float_TYPE) {
                                                mv.visitVarInsn(Opcodes.FLOAD, 1);
                                            } else if (fieldType == long_TYPE) {
                                                mv.visitVarInsn(Opcodes.LLOAD, 1);
                                            } else if (
                                                    fieldType == boolean_TYPE
                                                            || fieldType == char_TYPE
                                                            || fieldType == byte_TYPE
                                                            || fieldType == int_TYPE
                                                            || fieldType == short_TYPE) {
                                                mv.visitVarInsn(Opcodes.ILOAD, 1);
                                            } else {
                                                mv.visitVarInsn(Opcodes.ALOAD, 1);
                                            }
                                            mv.visitFieldInsn(PUTFIELD, BytecodeHelper.getClassInternalName(classNode), fieldName, BytecodeHelper.getTypeDescription(fieldType));
                                            mv.visitInsn(RETURN);
                                        }
                                    }
                                }));
                    }
                }
            }
        }
    }

    private static void addImplMethod(ClassNode classNode, MethodNode method, final Parameter[] oldParams, final MethodNode found) {
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