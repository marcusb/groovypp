package org.mbte.groovypp.compiler.asm;

import org.objectweb.asm.MethodVisitor;

public class VisitFieldInsn extends AsmInstr {
    public final int opcode;
    public final String owner, name, type;

    public VisitFieldInsn(int opcode, String owner, String name, String type) {
        this.opcode = opcode;
        this.owner = owner;
        this.name = name;
        this.type = type;
    }

    public void visit(MethodVisitor mv) {
        mv.visitFieldInsn(opcode, owner, name, type);
    }
}
