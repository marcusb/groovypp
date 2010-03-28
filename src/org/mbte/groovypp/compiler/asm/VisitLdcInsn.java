package org.mbte.groovypp.compiler.asm;

import org.objectweb.asm.MethodVisitor;

public class VisitLdcInsn extends AsmInstr {
    public final Object value;

    public VisitLdcInsn(Object value) {
        this.value = value;
    }

    public void visit(MethodVisitor mv) {
        mv.visitLdcInsn(value);
    }
}
