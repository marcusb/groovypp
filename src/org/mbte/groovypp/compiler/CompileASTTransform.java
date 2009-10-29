package org.mbte.groovypp.compiler;

import groovy.lang.TypePolicy;
import groovy.lang.Typed;
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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Handles generation of code for the @Typed annotation
 *
 * @author 2008-2009 Copyright (C) MBTE Sweden AB. All Rights Reserved.
 */
@GroovyASTTransformation(phase = CompilePhase.INSTRUCTION_SELECTION)
public class CompileASTTransform implements ASTTransformation, Opcodes {
    private static final ClassNode COMPILE_TYPE = ClassHelper.make(Typed.class);

    public void visit(ASTNode[] nodes, final SourceUnit source) {
        if (!(nodes[0] instanceof AnnotationNode) || !(nodes[1] instanceof AnnotatedNode)) {
            throw new RuntimeException("Internal error: wrong types: $node.class / $parent.class");
        }

        AnnotatedNode parent = (AnnotatedNode) nodes[1];

        LinkedList toProcess = new LinkedList();
        final ClassNode classNode;

        if (parent instanceof MethodNode) {
            TypePolicy classPolicy = getPolicy(parent.getDeclaringClass(), source, TypePolicy.DYNAMIC);
            TypePolicy methodPolicy = getPolicy(parent, source, classPolicy);

            final MethodNode mn = (MethodNode) parent;
            classNode = mn.getDeclaringClass();
            if (methodPolicy != TypePolicy.DYNAMIC) {
                toProcess.addLast(mn);
                toProcess.addLast(methodPolicy);
            }

            new OpenVerifier().addDefaultParameterMethods(classNode);
        } else {
            if (parent instanceof ClassNode) {
                classNode = (ClassNode) parent;
                TypePolicy classPolicy = getPolicy(classNode, source, TypePolicy.DYNAMIC);
                for (MethodNode mn : classNode.getMethods()) {
                    if (!mn.isAbstract() && (mn.getModifiers() & ACC_SYNTHETIC) == 0) {
                        TypePolicy methodPolicy = getPolicy(mn, source, classPolicy);
                        if (methodPolicy != TypePolicy.DYNAMIC) {
                            toProcess.addLast(mn);
                            toProcess.addLast(methodPolicy);
                        }
                    }
                }

                new OpenVerifier().addDefaultParameterMethods(classNode);
            } else {
                if (parent instanceof PackageNode) {
                    TypePolicy modulePolicy = getPolicy(parent, source, TypePolicy.DYNAMIC);
                    for (ClassNode node : source.getAST().getClasses()) {
                        TypePolicy classPolicy = getPolicy(node, source, modulePolicy);
                        for (MethodNode mn : node.getMethods()) {
                            if (!mn.isAbstract() && (mn.getModifiers() & ACC_SYNTHETIC) == 0) {
                                TypePolicy methodPolicy = getPolicy(mn, source, classPolicy);
                                if (methodPolicy != TypePolicy.DYNAMIC) {
                                    toProcess.addLast(mn);
                                    toProcess.addLast(methodPolicy);
                                }
                            }
                        }

                        new OpenVerifier().addDefaultParameterMethods(node);
                    }
                } else {
                    int line = parent.getLineNumber();
                    int col = parent.getColumnNumber();
                    source.getErrorCollector().addError(
                            new SyntaxErrorMessage(new SyntaxException("@Typed applicable only to classes or methods or package declaration" + '\n', line, col), source), true
                    );
                    return;
                }
            }
        }

        final Expression member = ((AnnotationNode) nodes[0]).getMember("debug");
        boolean debug = member != null && member instanceof ConstantExpression && ((ConstantExpression) member).getValue().equals(Boolean.TRUE);

        final HashMap<String, Integer> usedNames = new HashMap<String, Integer>();
        while (toProcess.size() > 0) {
            final MethodNode mn = (MethodNode) toProcess.removeFirst();
            final TypePolicy policy = (TypePolicy) toProcess.removeFirst();

            final List<AnnotationNode> anns = mn.getAnnotations(COMPILE_TYPE);
            boolean localDebug = debug;
            if (!anns.isEmpty()) {
                final AnnotationNode ann = anns.get(0);
                final Expression localMember = ((AnnotationNode) ann).getMember("debug");
                localDebug = localMember != null && localMember instanceof ConstantExpression && ((ConstantExpression) localMember).getValue().equals(Boolean.TRUE);
            }

            if ((mn.getModifiers() & Opcodes.ACC_BRIDGE) != 0 || mn.isAbstract())
                continue;

            final Statement code = mn.getCode();
            if (!(code instanceof BytecodeSequence)) {
                final ClosureExtractor extractor = new ClosureExtractor(source, toProcess, mn, mn.getDeclaringClass(), policy);
                Integer integer = usedNames.get(mn.getName());
                if (integer == null)
                    integer = 0;
                integer = integer + 1;
                usedNames.put(mn.getName(), integer);

                extractor.extract(code, mn.getName() + (integer == 1 ? "" : integer.toString()));
                if (!mn.getName().equals("$doCall"))
                    StaticMethodBytecode.replaceMethodCode(source, mn, new CompilerStack(null), localDebug ? 0 : -1, policy);
            }
        }

        return;
    }

    private TypePolicy getPolicy(AnnotatedNode ann, SourceUnit source, TypePolicy def) {
        final List<AnnotationNode> list = ann.getAnnotations(COMPILE_TYPE);
        if (list.isEmpty())
            return def;

        for (AnnotationNode an : list) {
            final Expression member = an.getMember("value");
            if (member instanceof PropertyExpression) {
                PropertyExpression pe = (PropertyExpression) member;
                if (pe.getObjectExpression() instanceof ClassExpression) {
                    ClassExpression ce = (ClassExpression) pe.getObjectExpression();

                    if (ce.getType().getName().equals("groovy.lang.TypePolicy")) {
                        if ("DYNAMIC".equals(pe.getPropertyAsString())) {
                            return TypePolicy.DYNAMIC;
                        } else {
                            if ("MIXED".equals(pe.getPropertyAsString())) {
                                return TypePolicy.MIXED;
                            } else {
                                if ("STATIC".equals(pe.getPropertyAsString())) {
                                    return TypePolicy.STATIC;
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
                    new SyntaxErrorMessage(new SyntaxException("Wrong 'value' for @Typed annotation" + '\n', line, col), source), true
            );
            return null;
        }
        return TypePolicy.STATIC;
    }
}