package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.classgen.Verifier;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.syntax.ASTHelper;
import org.codehaus.groovy.syntax.SyntaxException;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.objectweb.asm.Opcodes;

import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;


/**
 * Handles generation of code for the @Trait annotation
 *
 * @author 2008-2009 Copyright (C) MBTE Sweden AB. All Rights Reserved.
 */
@GroovyASTTransformation(phase = CompilePhase.CONVERSION)
public class TraitASTTransform implements ASTTransformation, Opcodes {

    public void visit(ASTNode[] nodes, final SourceUnit source) {
        ModuleNode module = (ModuleNode) nodes[0];
        List<ClassNode> toProcess = new LinkedList<ClassNode>();
        for (ClassNode classNode : module.getClasses()) {
            boolean process = false;
            boolean typed = false;
            for (AnnotationNode ann : classNode.getAnnotations()) {
                if (ann.getClassNode().getNameWithoutPackage().equals("Trait")) {
                    process = true;
                }
                if (ann.getClassNode().getNameWithoutPackage().equals("Typed")) {
                    typed = true;
                    ann.getClassNode().setRedirect(TypeUtil.TYPED);
                }
            }

            if (process) {
                toProcess.add(classNode);
                if (!typed) {
                    classNode.addAnnotation(new AnnotationNode(TypeUtil.TYPED));
                }
            }
        }

        for (ClassNode classNode : toProcess) {
            if (classNode.isInterface() || classNode.isEnum() || classNode.isAnnotationDefinition()) {
                source.addError(new SyntaxException("@Trait can be applied only to <class>", classNode.getLineNumber(), classNode.getColumnNumber()));
                continue;
            }

            int mod = classNode.getModifiers();
            mod &= ~Opcodes.ACC_FINAL;
            mod |= Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE;
            classNode.setModifiers(mod);

            String name = classNode.getNameWithoutPackage() + "$TraitImpl";
            String fullName = ASTHelper.dot(classNode.getPackageName(), name);

            InnerClassNode innerClassNode = new InnerClassNode(classNode, fullName, ACC_PUBLIC | ACC_STATIC | ACC_ABSTRACT, ClassHelper.OBJECT_TYPE, new ClassNode[]{classNode}, null);
            AnnotationNode typedAnn = new AnnotationNode(TypeUtil.TYPED);
            final Expression member = classNode.getAnnotations(TypeUtil.TYPED).get(0).getMember("debug");
            if (member != null && member instanceof ConstantExpression && ((ConstantExpression)member).getValue().equals(Boolean.TRUE))
                typedAnn.addMember("debug", ConstantExpression.TRUE);
            innerClassNode.addAnnotation(typedAnn);

            innerClassNode.setGenericsTypes(classNode.getGenericsTypes());

            ClassNode superClass = classNode.getSuperClass();
            if (!ClassHelper.OBJECT_TYPE.equals(superClass)) {
                ClassNode[] ifaces = classNode.getInterfaces();
                ClassNode[] newIfaces = new ClassNode[ifaces.length + 1];
                newIfaces[0] = superClass;
                System.arraycopy(ifaces, 0, newIfaces, 1, ifaces.length);
                classNode.setSuperClass(ClassHelper.OBJECT_TYPE);
                classNode.setInterfaces(newIfaces);
            }

            classNode.getModule().addClass(innerClassNode);

            for (FieldNode fieldNode : classNode.getFields()) {
//                if (fieldNode.isStatic())
//                    continue;

                MethodNode getter = classNode.addMethod("get" + Verifier.capitalize(fieldNode.getName()), ACC_PUBLIC | ACC_ABSTRACT, fieldNode.getType(), Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, null);
                AnnotationNode value = new AnnotationNode(TypeUtil.HAS_DEFAULT_IMPLEMENTATION);
                value.addMember("value", new ClassExpression(innerClassNode));
                value.addMember("fieldName", new ConstantExpression(fieldNode.getName()));
                getter.addAnnotation(value);

                Parameter valueParam = new Parameter(fieldNode.getType(), "$value");
                MethodNode setter = classNode.addMethod("set" + Verifier.capitalize(fieldNode.getName()), ACC_PUBLIC | ACC_ABSTRACT, ClassHelper.VOID_TYPE, new Parameter[]{valueParam}, ClassNode.EMPTY_ARRAY, null);
                value = new AnnotationNode(TypeUtil.HAS_DEFAULT_IMPLEMENTATION);
                value.addMember("value", new ClassExpression(innerClassNode));
                value.addMember("fieldName", new ConstantExpression(fieldNode.getName()));
                setter.addAnnotation(value);

                if (fieldNode.hasInitialExpression()) {
                    final Expression initial = fieldNode.getInitialValueExpression();
                    fieldNode.setInitialValueExpression(null);

                    final MethodNode initMethod = innerClassNode.addMethod("__init_" + fieldNode.getName(), ACC_PUBLIC | ACC_STATIC, ClassHelper.VOID_TYPE, new Parameter[]{new Parameter(classNode, "$self")}, ClassNode.EMPTY_ARRAY, new BlockStatement());

                    final PropertyExpression prop = new PropertyExpression(new VariableExpression("$self"), fieldNode.getName());
                    prop.setSourcePosition(fieldNode);
                    final CastExpression cast = new CastExpression(fieldNode.getType(), initial);
                    cast.setSourcePosition(initial);
                    final BinaryExpression assign = new BinaryExpression(prop, Token.newSymbol(Types.ASSIGN, -1, -1), cast);
                    assign.setSourcePosition(initial);
                    final ExpressionStatement assignExpr = new ExpressionStatement(assign);
                    assignExpr.setSourcePosition(initial);
                    ((BlockStatement)initMethod.getCode()).addStatement(assignExpr);
                }

                innerClassNode.addField(fieldNode);
            }

            classNode.getFields().clear();
            classNode.getProperties().clear();

            for (MethodNode methodNode : classNode.getMethods()) {
                if (methodNode.getCode() == null)
                    continue;

                if (methodNode.isStatic()) {
                    source.addError(new SyntaxException("Static methods are not allowed in traits", methodNode.getLineNumber(), methodNode.getColumnNumber()));
                }

                if (!methodNode.isPublic()) {
                    source.addError(new SyntaxException("Non-public methods are not allowed in traits", methodNode.getLineNumber(), methodNode.getColumnNumber()));
                }

                mod = methodNode.getModifiers();
                mod &= ~(Opcodes.ACC_FINAL | Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE);
                mod |= Opcodes.ACC_ABSTRACT | Opcodes.ACC_PUBLIC;
                methodNode.setModifiers(mod);

                Parameter[] parameters = methodNode.getParameters();

                Parameter[] newParameters = new Parameter[parameters.length + 1];
                final Parameter self = new Parameter(classNode, "$self");
                newParameters[0] = self;
                System.arraycopy(parameters, 0, newParameters, 1, parameters.length);

                MethodNode newMethod = innerClassNode.addMethod(methodNode.getName(), ACC_PUBLIC, methodNode.getReturnType(), newParameters, ClassNode.EMPTY_ARRAY, methodNode.getCode());
                ArrayList<GenericsType> gt = new ArrayList<GenericsType>();
                if (classNode.getGenericsTypes() != null)
                    for (int i = 0; i < classNode.getGenericsTypes().length; i++) {
                        GenericsType genericsType = classNode.getGenericsTypes()[i];
                        gt.add(genericsType);
                    }
                if (methodNode.getGenericsTypes() != null)
                    for (int i = 0; i < methodNode.getGenericsTypes().length; i++) {
                        GenericsType genericsType = methodNode.getGenericsTypes()[i];
                        gt.add(genericsType);
                    }

                if (!gt.isEmpty())
                    newMethod.setGenericsTypes(gt.toArray(new GenericsType[gt.size()]));

                AnnotationNode annotationNode = new AnnotationNode(TypeUtil.HAS_DEFAULT_IMPLEMENTATION);
                annotationNode.addMember("value", new ClassExpression(innerClassNode));
                methodNode.addAnnotation(annotationNode);

                methodNode.setCode(null);
            }
        }
    }
}
