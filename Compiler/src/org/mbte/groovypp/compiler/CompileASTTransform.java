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

import groovy.lang.TypePolicy;
import groovy.lang.Typed;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.classgen.BytecodeSequence;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.syntax.SyntaxException;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.objectweb.asm.Opcodes;

import java.util.*;

@GroovyASTTransformation(phase = CompilePhase.INSTRUCTION_SELECTION)
public class CompileASTTransform implements ASTTransformation, Opcodes {
    private static final ClassNode COMPILE_TYPE = ClassHelper.make(Typed.class);

    public void visit(ASTNode[] nodes, final SourceUnit source) {
        if (!(nodes[0] instanceof AnnotationNode) || !(nodes[1] instanceof AnnotatedNode)) {
            throw new RuntimeException("Internal error: wrong types: $node.class / $parent.class");
        }

        AnnotatedNode parent = (AnnotatedNode) nodes[1];

        Map<MethodNode, TypePolicy> toProcess = new LinkedHashMap<MethodNode, TypePolicy>();
        final ClassNode classNode;

        if (parent instanceof MethodNode) {
            TypePolicy classPolicy = getPolicy(parent.getDeclaringClass(), source, TypePolicy.DYNAMIC);
            TypePolicy methodPolicy = getPolicy(parent, source, classPolicy);

            classNode = parent.getDeclaringClass();
            if (methodPolicy != TypePolicy.DYNAMIC) {
                final MethodNode mn = (MethodNode) parent;
                addMethodToProcessingQueue(source, toProcess, methodPolicy, mn);
            }

        } else if (parent instanceof ClassNode) {
            classNode = (ClassNode) parent;
            TypePolicy classPolicy = getPolicy(classNode, source, TypePolicy.DYNAMIC);

            allMethods(source, toProcess, classNode, classPolicy);
        } else if (parent instanceof PackageNode) {
            TypePolicy modulePolicy = getPolicy(parent, source, TypePolicy.DYNAMIC);
            for (ClassNode clazz : source.getAST().getClasses()) {
                if (clazz instanceof InnerClassNode) continue;

                allMethods(source, toProcess, clazz, modulePolicy);
            }
        } else {
            int line = parent.getLineNumber();
            int col = parent.getColumnNumber();
            source.getErrorCollector().addError(
                    new SyntaxErrorMessage(new SyntaxException("@Typed applicable only to classes or methods or package declaration" + '\n', line, col), source), true
            );
            return;
        }

        final Expression member = ((AnnotationNode) nodes[0]).getMember("debug");
        boolean debug = member != null && member instanceof ConstantExpression && ((ConstantExpression) member).getValue().equals(Boolean.TRUE);

        SourceUnitContext context = new SourceUnitContext();
        for (Map.Entry<MethodNode, TypePolicy> entry : toProcess.entrySet()) {
            final MethodNode mn = entry.getKey();
            final TypePolicy policy = entry.getValue();

            final List<AnnotationNode> anns = mn.getAnnotations(COMPILE_TYPE);
            boolean localDebug = debug;
            if (!anns.isEmpty()) {
                final AnnotationNode ann = anns.get(0);
                final Expression localMember = ann.getMember("debug");
                if (localMember != null)
                    localDebug = localMember instanceof ConstantExpression && ((ConstantExpression) localMember).getValue().equals(Boolean.TRUE);
            }

            if ((mn.getModifiers() & Opcodes.ACC_BRIDGE) != 0 || mn.isAbstract())
                continue;

            final Statement code = mn.getCode();
            if (!(code instanceof BytecodeSequence)) {
                if (!mn.getName().equals("$doCall")) {
                    String name = mn.getName().equals("<init>") ? "_init_" :
                            mn.getName().equals("<clinit>") ? "_clinit_" : mn.getName();
                    StaticMethodBytecode.replaceMethodCode(source, context, mn, new CompilerStack(null), localDebug ? 0 : -1, policy, mn.getDeclaringClass().getName() + "$" + name);
                }
            }
        }

        for (MethodNode node : context.generatedFieldGetters.values()) {
            StaticMethodBytecode.replaceMethodCode(source, context, node, new CompilerStack(null), -1, TypePolicy.STATIC, "Neverused");
        }
        for (MethodNode node : context.generatedFieldSetters.values()) {
            StaticMethodBytecode.replaceMethodCode(source, context, node, new CompilerStack(null), -1, TypePolicy.STATIC, "Neverused");
        }
        for (MethodNode node : context.generatedMethodDelegates.values()) {
            StaticMethodBytecode.replaceMethodCode(source, context, node, new CompilerStack(null), -1, TypePolicy.STATIC, "Neverused");
        }
    }

    private void addMethodToProcessingQueue(final SourceUnit source, final Map<MethodNode, TypePolicy> toProcess, final TypePolicy methodPolicy, MethodNode mn) {
        final Statement code = mn.getCode();
        if (code == null)
            return;

        toProcess.put(mn, methodPolicy);
        code.visit(new CodeVisitorSupport(){
            @Override
            public void visitConstructorCallExpression(ConstructorCallExpression call) {
                final ClassNode type = call.getType();
                if (type instanceof InnerClassNode && ((InnerClassNode)type).isAnonymous()) {
                    allMethods(source, toProcess, type, methodPolicy);
                }
                super.visitConstructorCallExpression(call);
            }
        });
    }

    private void allMethods(SourceUnit source, Map<MethodNode, TypePolicy> toProcess, ClassNode classNode, TypePolicy classPolicy) {
        for (MethodNode mn : classNode.getMethods()) {
            if (!mn.isAbstract() && (mn.getModifiers() & ACC_SYNTHETIC) == 0) {
                TypePolicy methodPolicy = getPolicy(mn, source, classPolicy);
                if (methodPolicy != TypePolicy.DYNAMIC) {
                    addMethodToProcessingQueue(source, toProcess, methodPolicy, mn);
                }
            }
        }

        for (MethodNode mn : classNode.getDeclaredConstructors()) {
            TypePolicy methodPolicy = getPolicy(mn, source, classPolicy);
            if (methodPolicy != TypePolicy.DYNAMIC) {
                addMethodToProcessingQueue(source, toProcess, methodPolicy, mn);
            }
        }

        Iterator<InnerClassNode> inners = classNode.getInnerClasses();
        while (inners.hasNext()) {
            InnerClassNode node = inners.next();

            if (node.isAnonymous()) // method compilation will take care
                continue;

            TypePolicy innerClassPolicy = getPolicy(node, source, classPolicy);

            allMethods(source, toProcess, node, innerClassPolicy);        }
    }

    private TypePolicy getPolicy(AnnotatedNode ann, SourceUnit source, TypePolicy def) {
        final List<AnnotationNode> list = ann.getAnnotations(COMPILE_TYPE);
        if (list.isEmpty())
            return def;
        
        if(checkDuplicateTypedAnn(list, source)) return null;
        
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
    
    private boolean checkDuplicateTypedAnn(List<AnnotationNode> list, SourceUnit source) {
    	if(list.size() > 1) {
    		AnnotationNode secondAnn = list.get(1);
            int line = secondAnn.getLineNumber();
            int col = secondAnn.getColumnNumber();
            source.getErrorCollector().addError(
                    new SyntaxErrorMessage(new SyntaxException("Duplicate @Typed annotation found" + '\n', line, col), source), true
            );
            return true;
    	}
    	return false;
    }
}