package org.mbte.groovypp.compiler.impl;

import org.codehaus.groovy.classgen.BytecodeSequence;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.GroovyCodeVisitor;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.SourceUnit;
import org.objectweb.asm.MethodVisitor;

class StaticMethodBytecode extends StoredBytecodeInstruction {
    final MethodNode methodNode;
    final SourceUnit su;
    Statement code;
    final StaticCompiler compiler;

    public StaticMethodBytecode(MethodNode methodNode, SourceUnit su, Statement code, CompilerStack compileStack, boolean debug) {
        this.methodNode = methodNode;
        this.su = su;
        this.code = code;

        MethodVisitor mv = createStorage();
        if (debug)
            mv = DebugMethodAdapter.create(mv);
        mv = new BytecodeImproverMethodAdapter(mv);
        compiler = new StaticCompiler(
                su,
                this,
                mv,
                compileStack);
//
        if (debug)
           System.out.println("-----> " + methodNode.getDeclaringClass().getName() + "#" + methodNode.getName());
        compiler.execute();
        if (debug)
           System.out.println("------------");
    }

    public static void replaceMethodCode(SourceUnit source, MethodNode methodNode, CompilerStack compileStack, boolean debug) {
        final Statement code = methodNode.getCode();
        if (!(code instanceof BytecodeSequence)) {
            final StaticMethodBytecode methodBytecode = new StaticMethodBytecode(methodNode, source, code, compileStack, debug);
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
