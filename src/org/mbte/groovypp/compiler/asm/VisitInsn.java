package org.mbte.groovypp.compiler.asm;

import org.objectweb.asm.MethodVisitor;

public class VisitInsn extends AsmInstr {
    public final int opcode;

    public VisitInsn(int opcode) {
        this.opcode = opcode;
    }

    public void visit(MethodVisitor mv) {
        mv.visitInsn(opcode);
    }
}
