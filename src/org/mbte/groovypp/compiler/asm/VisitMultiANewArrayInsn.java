package org.mbte.groovypp.compiler.asm;

import org.objectweb.asm.MethodVisitor;

public class VisitMultiANewArrayInsn extends AsmInstr {
    public final String desc;
    public final int dims;

    public VisitMultiANewArrayInsn(String desc, int dims) {
        this.desc = desc;
        this.dims = dims;
    }

    public void visit(MethodVisitor mv) {
        mv.visitMultiANewArrayInsn(desc, dims);
    }
}
