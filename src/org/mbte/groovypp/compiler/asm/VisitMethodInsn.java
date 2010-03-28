package org.mbte.groovypp.compiler.asm;

import org.objectweb.asm.MethodVisitor;

public class VisitMethodInsn extends AsmInstr {
    public final int opcode;
    public final String owner, name, descr;

    public VisitMethodInsn(int opcode, String owner, String name, String descr) {
        this.opcode = opcode;
        this.owner = owner;
        this.name = name;
        this.descr = descr;
    }

    public void visit(MethodVisitor mv) {
        mv.visitMethodInsn(opcode, owner, name, descr);
    }
}
