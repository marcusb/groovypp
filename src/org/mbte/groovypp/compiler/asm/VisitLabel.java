package org.mbte.groovypp.compiler.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class VisitLabel extends AsmInstr {
    public final Label label;

    public VisitLabel(Label label) {
        this.label = label;
    }

    public void visit(MethodVisitor mv) {
        mv.visitLabel(label);
    }
}
