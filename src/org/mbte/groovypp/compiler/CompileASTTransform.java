package org.mbte.groovypp.compiler;

import groovy.lang.Compile;
import groovy.lang.CompilePolicy;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.classgen.BytecodeSequence;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.syntax.SyntaxException;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.objectweb.asm.Opcodes;

import java.util.LinkedList;
import java.util.List;

/**
 * Handles generation of code for the @Compile annotation
 *
 * @author 2008-2009 Copyright (C) MBTE Sweden AB. All Rights Reserved.
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class CompileASTTransform implements ASTTransformation, Opcodes {
    private static final ClassNode COMPILE_TYPE = ClassHelper.make(Compile.class);

    public void visit(ASTNode[] nodes, final SourceUnit source) {
        if (!(nodes[0] instanceof AnnotationNode) || !(nodes[1] instanceof AnnotatedNode)) {
            throw new RuntimeException("Internal error: wrong types: $node.class / $parent.class");
        }

        AnnotatedNode parent = (AnnotatedNode) nodes[1];

        LinkedList toProcess = new LinkedList ();
        final ClassNode classNode;

        if (parent instanceof MethodNode) {
            CompilePolicy classPolicy = getPolicy(parent.getDeclaringClass(), source, CompilePolicy.DYNAMIC);
            CompilePolicy methodPolicy = getPolicy(parent, source, classPolicy);

            final MethodNode mn = (MethodNode) parent;
            classNode = mn.getDeclaringClass();
            if (methodPolicy != CompilePolicy.DYNAMIC) {
                toProcess.addLast(mn);
                toProcess.addLast(methodPolicy);
            }
        }
        else {
            if (parent instanceof ClassNode) {
                classNode = (ClassNode) parent;
                CompilePolicy classPolicy = getPolicy(classNode, source, CompilePolicy.DYNAMIC);
                external:
                for (MethodNode mn : classNode.getMethods() ) {
                    if (!mn.isAbstract()) {
                        CompilePolicy methodPolicy = getPolicy(mn, source, classPolicy);
                        if (methodPolicy != CompilePolicy.DYNAMIC) {
                            toProcess.addLast(mn);
                            toProcess.addLast(methodPolicy);
                        }
                    }
                }
            }
            else {
                int line = parent.getLineNumber();
                int col = parent.getColumnNumber();
                source.getErrorCollector().addError(
                        new SyntaxErrorMessage(new SyntaxException("@Compile applicable only to classes or methods" + '\n', line, col), source), true
                );
                return;
            }
        }

        final Expression member = ((AnnotationNode) nodes[0]).getMember("debug");
        boolean debug = member != null && member instanceof ConstantExpression && ((ConstantExpression)member).getValue().equals(Boolean.TRUE);

        new OpenVerifier().addDefaultParameterMethods(classNode);

        while (toProcess.size() > 0) {
            final MethodNode mn = (MethodNode) toProcess.removeFirst();
            final CompilePolicy policy = (CompilePolicy) toProcess.removeFirst();

            final Statement code = mn.getCode();
            if (!(code instanceof BytecodeSequence)) {
                final ClosureExtractor extractor = new ClosureExtractor(source, toProcess, mn, classNode, policy);
                extractor.extract(code);
                StaticMethodBytecode.replaceMethodCode(source, mn, new CompilerStack(null), debug, policy);
            }
        }
    }

    private CompilePolicy getPolicy (AnnotatedNode ann, SourceUnit source, CompilePolicy def) {
        final List<AnnotationNode> list = ann.getAnnotations(COMPILE_TYPE);
        if (list.isEmpty())
           return def;

        for (AnnotationNode an : list) {
            final Expression member = an.getMember("value");
            if (member instanceof PropertyExpression) {
                PropertyExpression pe = (PropertyExpression) member;
                if (pe.getObjectExpression() instanceof ClassExpression) {
                    ClassExpression ce = (ClassExpression) pe.getObjectExpression();

                    if (ce.getType().getName().equals("groovy.lang.CompilePolicy")) {
                        if ("DYNAMIC".equals(pe.getPropertyAsString())) {
                            return CompilePolicy.DYNAMIC;
                        }
                        else {
                            if ("MIXED".equals(pe.getPropertyAsString())) {
                                return CompilePolicy.MIXED;
                            }
                            else {
                                if ("STATIC".equals(pe.getPropertyAsString())) {
                                    return CompilePolicy.STATIC;
                                }
                            }
                        }
                    }
                }
            }

            if (member == null) {
                continue;
            }

            int line = ann.getLineNumber();
            int col = ann.getColumnNumber();
            source.getErrorCollector().addError(
                    new SyntaxErrorMessage(new SyntaxException("Wrong 'value' for @Compile annotation" + '\n', line, col), source), true
            );
            return null;
        }
        return CompilePolicy.STATIC;
    }
}