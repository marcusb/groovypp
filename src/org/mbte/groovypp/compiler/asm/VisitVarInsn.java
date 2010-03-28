package org.mbte.groovypp.compiler.asm;

import org.objectweb.asm.MethodVisitor;

public class VisitVarInsn extends AsmInstr {
    public final int opcode;
    public final int var;

    public VisitVarInsn(int opcode, int var) {
        this.opcode = opcode;
        this.var = var;
    }

    public void visit(MethodVisitor mv) {
        mv.visitVarInsn(opcode, var);
    }
}
