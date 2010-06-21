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

package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import static org.codehaus.groovy.ast.ClassHelper.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.classgen.BytecodeInstruction;
import org.codehaus.groovy.classgen.BytecodeSequence;
import org.codehaus.groovy.classgen.Verifier;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.codehaus.groovy.syntax.SyntaxException;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@GroovyASTTransformation(phase = CompilePhase.INSTRUCTION_SELECTION)
public class TraitASTTransformFinal implements ASTTransformation, Opcodes {
    public void visit(ASTNode[] nodes, final SourceUnit source) {
        ModuleNode module = (ModuleNode) nodes[0];
        for (ClassNode classNode : module.getClasses()) {
            if (classNode instanceof InnerClassNode && classNode.getName().endsWith("$TraitImpl")) {
                makeImplementationMethodsStatic(classNode, source);
            }
        }

        for (ClassNode classNode : module.getClasses()) {
            if (classNode instanceof InnerClassNode && classNode.getName().endsWith("$TraitImpl")) {
                continue;
            }

            VolatileFieldUpdaterTransform.addUpdaterForVolatileFields(classNode);
            try {
                new OpenVerifier().visitClass(classNode);
             }
             catch (MultipleCompilationErrorsException err) {
                 throw err;
             }
             catch (Throwable t) {
                 int line = classNode.getLineNumber();
                 int col = classNode.getColumnNumber();
                 t.printStackTrace();
                 source.getErrorCollector().addError(new SyntaxErrorMessage(new SyntaxException(t.getMessage() + '\n', line, col), source), true);
             }

            improveAbstractMethods(classNode);
        }
    }

    private void makeImplementationMethodsStatic(final ClassNode classNode, final SourceUnit source) {
        for (final MethodNode methodNode : classNode.getMethods()) {
            if (methodNode.isStatic() || methodNode.isSynthetic() || methodNode.isAbstract())
                continue;

            final Parameter[] parameters = methodNode.getParameters();

            methodNode.setModifiers(methodNode.getModifiers() | Opcodes.ACC_STATIC);
            methodNode.getVariableScope().setInStaticContext(true);
            final VariableExpression self = new VariableExpression(parameters[0]);
            final String propNameCapitalized = getPropertyNameCapitalized(methodNode);

            ClassCodeExpressionTransformer thisToSelf = new ClassCodeExpressionTransformer() {
                protected SourceUnit getSourceUnit() {
                    return source;
                }

                public Expression transform(Expression exp) {
                    if (exp instanceof VariableExpression && isExplicitThis((VariableExpression) exp))
                        return self;
                    else if (exp instanceof PropertyExpression) {
                        final PropertyExpression propExp = (PropertyExpression) exp;
                        if (propExp.isImplicitThis() || (propExp.getObjectExpression() instanceof VariableExpression &&
                        isExplicitThis((VariableExpression) propExp.getObjectExpression()))) {
                            final String name = propExp.getPropertyAsString();
                            final FieldNode field = classNode.getField(name);
                            if (field != null && Verifier.capitalize(field.getName()).equals(propNameCapitalized))
                                return new PropertyExpression(self, "$" + name);
                        }
                    }
                    return super.transform(exp);
                }

                private boolean isExplicitThis(VariableExpression exp) {
                    return "this".equals(exp.getName());
                }
            };
            thisToSelf.visitMethod(methodNode);
        }
    }


    public static void improveAbstractMethods(final ClassNode classNode) {
        if ((classNode.getModifiers() & ACC_ABSTRACT) != 0)
            return;

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
                        final Parameter[] parameters;
                        final boolean getter;
                        if (method.getName().startsWith("get")) {
                            fieldType = TypeUtil.mapTypeFromSuper(method.getReturnType(), method.getDeclaringClass(), classNode);
                            parameters = Parameter.EMPTY_ARRAY;
                            getter = true;
                        } else {
                            fieldType = TypeUtil.mapTypeFromSuper(method.getParameters()[0].getType(), method.getDeclaringClass(), classNode);
                            parameters = new Parameter[] {new Parameter(fieldType, method.getParameters()[0].getName())};
                            getter = false;
                        }

                        final String fieldName = (String) ((ConstantExpression) field).getValue();
                        FieldNode klazzField = classNode.getField(fieldName);
                        if (klazzField == null) {
                            klazzField = classNode.addField(fieldName, ACC_PRIVATE, fieldType, null);
                            final MethodNode initMethod = klazz.getType().getMethod("__init_" + fieldName, new Parameter[]{new Parameter(method.getDeclaringClass(), "$self")});
                            if (initMethod != null) {
                                classNode.getObjectInitializerStatements().add(new ExpressionStatement(new StaticMethodCallExpression(klazz.getType(), initMethod.getName(), new ArgumentListExpression(VariableExpression.THIS_EXPRESSION))));
                                klazzField.addAnnotation(new AnnotationNode(TypeUtil.NO_EXTERNAL_INITIALIZATION));
                            }
                        }

                        createGetterSetter(classNode, method, fieldType, parameters, getter, fieldName);
                    }
                }
                else {
                    // very special case of clone ()
                    if (method.getName().equals("clone") && method.getParameters().length == 0) {
                        classNode.addMethod(method.getName(), ACC_PUBLIC, method.getReturnType(), Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY,
                                new BytecodeSequence(new BytecodeInstruction() {
                                    public void visit(MethodVisitor mv) {
                                            mv.visitVarInsn(ALOAD, 0);
                                            mv.visitMethodInsn(INVOKESPECIAL, BytecodeHelper.getClassInternalName(classNode.getSuperClass().getName()), "clone", "()Ljava/lang/Object;");
                                            if (!ClassHelper.OBJECT_TYPE.equals(method.getReturnType()))
                                                mv.visitTypeInsn(CHECKCAST, BytecodeHelper.getClassInternalName(method.getReturnType()));
                                            BytecodeExpr.doReturn(mv, ClassHelper.OBJECT_TYPE);
                                    }
                                }));
                    }
                }
            }
        }
    }

    private static void createGetterSetter(final ClassNode classNode, MethodNode method, final ClassNode fieldType, Parameter[] parameters, final boolean getter, final String fieldName) {
        classNode.addMethod(method.getName(), ACC_PUBLIC, getter ? fieldType : ClassHelper.VOID_TYPE, parameters, ClassNode.EMPTY_ARRAY,
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

    private static void addImplMethod(ClassNode classNode, MethodNode method, final Parameter[] oldParams, final MethodNode found) {
        final ClassNode returnType = TypeUtil.mapTypeFromSuper(method.getReturnType(), method.getDeclaringClass(), classNode);
        Parameter[] newParams = new Parameter[oldParams.length];
        for (int i = 0; i < oldParams.length; i++) {
            ClassNode t = TypeUtil.mapTypeFromSuper(oldParams[i].getType(), method.getDeclaringClass(), classNode);
            newParams[i] = new Parameter(t, oldParams[i].getName());
        }
        ClassNode[] oldExns = method.getExceptions();
        ClassNode[] newExns = new ClassNode[oldExns.length];
        for (int i = 0; i < oldExns.length; i++) {
            newExns[i] = TypeUtil.mapTypeFromSuper(oldExns[i], method.getDeclaringClass(), classNode);
        }
        final MethodNode added = classNode.addMethod(method.getName(), Opcodes.ACC_PUBLIC, returnType, newParams, newExns, new BytecodeSequence(new BytecodeInstruction() {
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
                if (!TypeUtil.isDirectlyAssignableFrom(returnType, found.getReturnType())) {
                    BytecodeExpr.box(found.getReturnType(), mv);
                    BytecodeExpr.cast(TypeUtil.wrapSafely(found.getReturnType()),
                            TypeUtil.wrapSafely(returnType), mv);
                    BytecodeExpr.unbox(returnType, mv);
                }
                BytecodeExpr.doReturn(mv, returnType);
            }
        }));
        added.setGenericsTypes(method.getGenericsTypes());
    }

    private static Map<String, MethodNode> getDeclaredMethodsMap(ClassNode klazz) {
        // Start off with the methods from the superclass.
        ClassNode parent = klazz.getSuperClass();
        Map<String, MethodNode> result;
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
                if (methNode == null || !methNode.isPublic() || methNode.isAbstract() && (!hasDefaultImpl(methNode) || hasDefaultImpl(methSig.getValue()) && methSig.getValue().getDeclaringClass().implementsInterface(methNode.getDeclaringClass()))) {
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

    // Includes synthetic '$self' parameter!
    private static String getPropertyNameCapitalized(MethodNode accessor) {
        final String accessorName = accessor.getName();
        if (accessorName.startsWith("get") && accessorName.length() > 3 && accessor.getParameters().length == 1) return accessorName.substring(3);
        if (accessorName.startsWith("is") && accessorName.length() > 2 && accessor.getParameters().length == 1) return accessorName.substring(2);
        if (accessorName.startsWith("set") && accessorName.length() > 3 && accessor.getParameters().length == 2) return accessorName.substring(3);
        return null;
    }
}
