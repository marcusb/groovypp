package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.IfStatement;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.objectweb.asm.Opcodes;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Handles generation of code for the @IDef annotation
 *
 * @author 2008-2009 Copyright (C) MBTE Sweden AB. All Rights Reserved.
 */
@GroovyASTTransformation(phase = CompilePhase.CONVERSION)
public class IDefASTTransform implements ASTTransformation, Opcodes {

    public void visit(ASTNode[] nodes, final SourceUnit source) {
        ModuleNode module = (ModuleNode) nodes[0];
        List<MethodNode> toProcess = new LinkedList<MethodNode> ();
        for (Iterator it = module.getClasses().iterator(); it.hasNext(); ) {
            ClassNode classNode = (ClassNode) it.next();
            for (MethodNode mn : classNode.getMethods()) {
                for (Object o : mn.getAnnotations()) {
                    AnnotationNode ann = (AnnotationNode) o;
                    if (ann.getClassNode().getNameWithoutPackage().equals("IDef")) {
                        toProcess.add(mn);
                    }
                }
            }
        }

        for (MethodNode mn : toProcess) {
            final String name = mn.getDeclaringClass().getName() + "_I" + Character.toUpperCase(mn.getName().charAt(0)) + mn.getName().substring(1);
            ClassNode newType = new ClassNode(name, ACC_PUBLIC|ACC_INTERFACE|ACC_ABSTRACT|ACC_SYNTHETIC, ClassHelper.OBJECT_TYPE);
            module.addClass(newType);

            newType.addMethod(mn.getName(), ACC_PUBLIC|ACC_ABSTRACT, mn.getReturnType(), mn.getParameters(), ClassNode.EMPTY_ARRAY, null);

            final PropertyNode propertyNode = mn.getDeclaringClass().addProperty(mn.getName(), ACC_PUBLIC, newType, null, null, null);
            new OpenVerifier().addPropertyMethods(propertyNode);

            ArgumentListExpression args = new ArgumentListExpression();
            for(Parameter p : mn.getParameters()) {
                args.addExpression(new VariableExpression(p.getName()));
            }
            mn.setCode(new IfStatement(
                    new BooleanExpression(new PropertyExpression(VariableExpression.THIS_EXPRESSION, mn.getName())),
                    new ExpressionStatement(
                        new MethodCallExpression(
                            new PropertyExpression(VariableExpression.THIS_EXPRESSION, mn.getName()),
                            mn.getName(),
                            args
                        )
                    ), mn.getCode()));
        }
    }
}