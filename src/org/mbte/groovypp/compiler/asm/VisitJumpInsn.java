package org.mbte.groovypp.compiler.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class VisitJumpInsn extends AsmInstr {
    public final int opcode;
    public final Label label;

    public VisitJumpInsn(int opcode, Label label) {
        this.opcode = opcode;
        this.label = label;
    }

    public void visit(MethodVisitor mv) {
        mv.visitJumpInsn(opcode, label);
    }
}
