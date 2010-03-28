package org.mbte.groovypp.compiler.asm;

import org.objectweb.asm.MethodVisitor;

public class VisitTypeInsn extends AsmInstr {
    public final int opcode;
    public final String type;

    public VisitTypeInsn(int opcode, String type) {
        this.opcode = opcode;
        this.type = type;
    }

    public void visit(MethodVisitor mv) {
        mv.visitTypeInsn(opcode, type);
    }
}
