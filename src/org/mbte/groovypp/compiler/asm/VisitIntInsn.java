package org.mbte.groovypp.compiler.asm;

import org.objectweb.asm.MethodVisitor;

public class VisitIntInsn extends AsmInstr {
    public final int opcode;
    public final int operand;

    public VisitIntInsn(int opcode, int operand) {
        this.opcode = opcode;
        this.operand = operand;
    }

    public void visit(MethodVisitor mv) {
        mv.visitIntInsn(opcode, operand);
    }
}
