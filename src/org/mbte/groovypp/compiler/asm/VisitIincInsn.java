package org.mbte.groovypp.compiler.asm;

import org.objectweb.asm.MethodVisitor;

public class VisitIincInsn extends AsmInstr {
    public final int var;
    public final int increment;

    public VisitIincInsn(int var, int increment) {
        this.var = var;
        this.increment = increment;
    }

    public void visit(MethodVisitor mv) {
        mv.visitIincInsn(var, increment);
    }
}
