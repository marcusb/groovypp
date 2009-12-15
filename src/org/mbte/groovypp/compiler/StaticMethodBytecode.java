package org.mbte.groovypp.compiler;

import groovy.lang.TypePolicy;
import org.codehaus.groovy.ast.GroovyCodeVisitor;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.classgen.BytecodeSequence;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.control.SourceUnit;
import org.mbte.groovypp.compiler.bytecode.BytecodeImproverMethodAdapter;
import org.objectweb.asm.MethodVisitor;

import java.util.List;

public class StaticMethodBytecode extends StoredBytecodeInstruction {
    final MethodNode methodNode;
    final SourceUnit su;
    Statement code;
    private final TypePolicy policy;
    final StaticCompiler compiler;

    public StaticMethodBytecode(MethodNode methodNode, SourceUnitContext context, SourceUnit su, Statement code, CompilerStack compileStack, int debug, TypePolicy policy, String baseClosureName) {
        this.methodNode = methodNode;
        this.su = su;
        this.code = code;
        this.policy = policy;

        MethodVisitor mv = createStorage();
        if (debug != -1) {
            try {
                mv = DebugMethodAdapter.create(mv, debug);
            }
            catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
        mv = new BytecodeImproverMethodAdapter(mv);
        compiler = new StaticCompiler(
                su,
                context,
                this,
                mv,
                compileStack,
                debug,
                policy, baseClosureName);
//
        if (debug != -1)
            System.out.println("-----> " + methodNode.getDeclaringClass().getName() + "#" + methodNode.getName() + "(" + BytecodeHelper.getMethodDescriptor(methodNode.getReturnType(), methodNode.getParameters()) + ")");
        compiler.execute();
        if (debug != -1)
            System.out.println("------------");
    }

    public static void replaceMethodCode(SourceUnit source, SourceUnitContext context, MethodNode methodNode, CompilerStack compileStack, int debug, TypePolicy policy, String baseClosureName) {
        if (methodNode instanceof ClosureMethodNode.Dependent)
            methodNode = ((ClosureMethodNode.Dependent)methodNode).getMaster();
        
        final Statement code = methodNode.getCode();
        if (!(code instanceof BytecodeSequence)) {
            final StaticMethodBytecode methodBytecode = new StaticMethodBytecode(methodNode, context, source, code, compileStack, debug, policy, baseClosureName);
            methodNode.setCode(new MyBytecodeSequence(methodBytecode));
            if (methodBytecode.compiler.shouldImproveReturnType && !TypeUtil.NULL_TYPE.equals(methodBytecode.compiler.calculatedReturnType))
                methodNode.setReturnType(methodBytecode.compiler.calculatedReturnType);
        }

        if (methodNode instanceof ClosureMethodNode) {
            ClosureMethodNode closureMethodNode = (ClosureMethodNode) methodNode;
            List<ClosureMethodNode.Dependent> dependentMethods = closureMethodNode.getDependentMethods();
            if (dependentMethods != null)
                for (ClosureMethodNode.Dependent dependent : dependentMethods) {
                    final Statement mCode = dependent.getCode();
                    if (!(mCode instanceof BytecodeSequence)) {
                        final StaticMethodBytecode methodBytecode = new StaticMethodBytecode(dependent, context, source, mCode, compileStack, debug, policy, baseClosureName);
                        dependent.setCode(new MyBytecodeSequence(methodBytecode));
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
