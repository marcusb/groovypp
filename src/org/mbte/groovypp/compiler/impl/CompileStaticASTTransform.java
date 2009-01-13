package org.mbte.groovypp.compiler.impl;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.codehaus.groovy.syntax.SyntaxException;
import org.codehaus.groovy.classgen.BytecodeSequence;
import org.objectweb.asm.*;

import java.util.LinkedList;

/**
 * Handles generation of code for the @CompileStatic annotation
 *
 * @author 2008-2009 Copyright (C) MBTE Sweden AB. All Rights Reserved.
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class CompileStaticASTTransform implements ASTTransformation, Opcodes {

    public void visit(ASTNode[] nodes, final SourceUnit source) {
        if (!(nodes[0] instanceof AnnotationNode) || !(nodes[1] instanceof AnnotatedNode)) {
            throw new RuntimeException("Internal error: wrong types: $node.class / $parent.class");
        }

        AnnotatedNode parent = (AnnotatedNode) nodes[1];

        LinkedList<MethodNode> toProcess = new LinkedList<MethodNode> ();
        final ClassNode classNode;
        if (parent instanceof MethodNode) {
            final MethodNode mn = (MethodNode) parent;
            classNode = mn.getDeclaringClass();
            toProcess.addLast(mn);
        }
        else {
            if (parent instanceof ClassNode) {
                classNode = (ClassNode) parent;
                for (MethodNode mn : classNode.getMethods() ) {
                    toProcess.addLast(mn);
                }
            }
            else {

                int line = parent.getLineNumber();
                int col = parent.getColumnNumber();
                source.getErrorCollector().addError(
                        new SyntaxErrorMessage(new SyntaxException("@CompileStatic applicable only to classes or methods" + '\n', line, col), source), true
                );
                return;
            }
        }

        new OVerifier().addDefaultParameterMethods(classNode);
        while (toProcess.size() > 0) {
            final MethodNode mn = toProcess.removeFirst();
            final Statement code = mn.getCode();
            if (!(code instanceof BytecodeSequence)) {
                code.visit(new ClosureExtractor(source, toProcess, mn, classNode));
                StaticMethodBytecode.replaceMethodCode(source, mn, new CompilerStack(null));
            }
            else {
                continue;
            }
        }
    }
}