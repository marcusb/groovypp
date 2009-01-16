package org.mbte.groovypp.compiler;

import org.codehaus.groovy.classgen.BytecodeSequence;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.GroovyCodeVisitor;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.SourceUnit;
import org.objectweb.asm.MethodVisitor;
import org.mbte.groovypp.compiler.bytecode.BytecodeImproverMethodAdapter;
import groovy.lang.CompilePolicy;

class StaticMethodBytecode extends StoredBytecodeInstruction {
    final MethodNode methodNode;
    final SourceUnit su;
    Statement code;
    private final CompilePolicy policy;
    final StaticCompiler compiler;

    public StaticMethodBytecode(MethodNode methodNode, SourceUnit su, Statement code, CompilerStack compileStack, boolean debug, CompilePolicy policy) {
        this.methodNode = methodNode;
        this.su = su;
        this.code = code;
        this.policy = policy;

        MethodVisitor mv = createStorage();
        if (debug)
            mv = DebugMethodAdapter.create(mv);
        mv = new BytecodeImproverMethodAdapter(mv);
        compiler = new StaticCompiler(
                su,
                this,
                mv,
                compileStack,
                policy);
//
        if (debug)
           System.out.println("-----> " + methodNode.getDeclaringClass().getName() + "#" + methodNode.getName());
        compiler.execute();
        if (debug)
           System.out.println("------------");
    }

    public static void replaceMethodCode(SourceUnit source, MethodNode methodNode, CompilerStack compileStack, boolean debug, CompilePolicy policy) {
        final Statement code = methodNode.getCode();
        if (!(code instanceof BytecodeSequence)) {
            final StaticMethodBytecode methodBytecode = new StaticMethodBytecode(methodNode, source, code, compileStack, debug, policy);
            methodNode.setCode(new MyBytecodeSequence(methodBytecode){});
        }
    }

    private static class MyBytecodeSequence extends BytecodeSequence {
        public MyBytecodeSequence(StaticMethodBytecode instruction) {
            super(instruction);
        }

        @Override
        public void visit(GroovyCodeVisitor visitor) {
            ((StaticMethodBytecode)getInstructions().get(0)).code.visit(visitor);
        }
    }
}
