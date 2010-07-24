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
import org.codehaus.groovy.ast.GroovyCodeVisitor;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.classgen.BytecodeSequence;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.classgen.BytecodeInstruction;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.mbte.groovypp.compiler.asm.UneededLoadPopRemoverMethodAdapter;
import org.objectweb.asm.MethodVisitor;

import java.util.List;

public class StaticMethodBytecode extends StoredBytecodeInstruction {
    final MethodNode methodNode;
    final SourceUnit su;
    Statement code;
    final StaticCompiler compiler;

    public StaticMethodBytecode(MethodNode methodNode, SourceUnitContext context, SourceUnit su, Statement code, CompilerStack compileStack, int debug, TypePolicy policy, String baseClosureName) {
        this.methodNode = methodNode;
        this.su = su;
        this.code = code;

        MethodVisitor mv = createStorage();
        if (debug != -1) {
            try {
                mv = DebugMethodAdapter.create(mv, debug);
            }
            catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
        compiler = new StaticCompiler(
                su,
                context,
                this,
                new UneededLoadPopRemoverMethodAdapter(mv),
                compileStack,
                debug,
                policy, baseClosureName);
//
        if (debug != -1)
            DebugContext.outputStream.println("-----> " + methodNode.getDeclaringClass().getName() + "#" + methodNode.getName() + "(" + BytecodeHelper.getMethodDescriptor(methodNode.getReturnType(), methodNode.getParameters()) + ") " + BytecodeHelper.getGenericsMethodSignature(methodNode));

        try {
            compiler.execute();
        }
        catch (MultipleCompilationErrorsException me) {
            clear ();
            throw me;
        }
        catch (Throwable t) {
            clear ();
            compiler.addError("Internal Error: " + t.toString(), methodNode);
        }

        if (debug != -1)
            DebugContext.outputStream.println("------------");
    }

    public static void replaceMethodCode(SourceUnit source, SourceUnitContext context, MethodNode methodNode, CompilerStack compileStack, int debug, TypePolicy policy, String baseClosureName) {
        if (methodNode instanceof ClosureMethodNode.Dependent)
            methodNode = ((ClosureMethodNode.Dependent)methodNode).getMaster();
        
        final Statement code = methodNode.getCode();
        if (!(code instanceof BytecodeSequence)) {
            try {
                final StaticMethodBytecode methodBytecode = new StaticMethodBytecode(methodNode, context, source, code, compileStack, debug, policy, baseClosureName);
                methodNode.setCode(new MyBytecodeSequence(methodBytecode));
                if (methodBytecode.compiler.shouldImproveReturnType && !TypeUtil.NULL_TYPE.equals(methodBytecode.compiler.calculatedReturnType))
                    methodNode.setReturnType(methodBytecode.compiler.calculatedReturnType);
            }
            catch (MultipleCompilationErrorsException ce) {
                handleCompilationError(methodNode, ce);
                throw ce;
            }
        }

        if (methodNode instanceof ClosureMethodNode) {
            ClosureMethodNode closureMethodNode = (ClosureMethodNode) methodNode;
            List<ClosureMethodNode.Dependent> dependentMethods = closureMethodNode.getDependentMethods();
            if (dependentMethods != null)
                for (ClosureMethodNode.Dependent dependent : dependentMethods) {
                    final Statement mCode = dependent.getCode();
                    if (!(mCode instanceof BytecodeSequence)) {
                        try {
                            final StaticMethodBytecode methodBytecode = new StaticMethodBytecode(dependent, context, source, mCode, compileStack, debug, policy, baseClosureName);
                            dependent.setCode(new MyBytecodeSequence(methodBytecode));
                        }
                        catch (MultipleCompilationErrorsException ce) {
                            handleCompilationError(methodNode, ce);
                            throw ce;
                        }
                    }
                }
        }
    }

    private static void handleCompilationError(MethodNode methodNode, MultipleCompilationErrorsException ce) {
        methodNode.setCode(new BytecodeSequence(new BytecodeInstruction(){public void visit(MethodVisitor mv) {}}));
        if (methodNode instanceof ClosureMethodNode) {
            ClosureMethodNode closureMethodNode = (ClosureMethodNode) methodNode;
            List<ClosureMethodNode.Dependent> dependentMethods = closureMethodNode.getDependentMethods();
            if (dependentMethods != null)
                for (ClosureMethodNode.Dependent dependent : dependentMethods) {
                    final Statement mCode = dependent.getCode();
                    if (!(mCode instanceof BytecodeSequence)) {
                            dependent.setCode(new BytecodeSequence(new BytecodeInstruction(){public void visit(MethodVisitor mv) {}}));
                    }
                }
        }
    }

    private static class MyBytecodeSequence extends BytecodeSequence {
        public MyBytecodeSequence(StaticMethodBytecode instruction) {
            super(instruction);
        }

        @Override
        public void visit(GroovyCodeVisitor visitor) {
//            ((StaticMethodBytecode) getInstructions().get(0)).code.visit(visitor);
        }
    }
}
