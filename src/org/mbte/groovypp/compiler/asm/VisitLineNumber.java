package org.mbte.groovypp.compiler.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class VisitLineNumber extends AsmInstr {
    public final int line;
    public final Label label;

    public VisitLineNumber(int line, Label label) {
        this.line = line;
        this.label = label;
    }

    public void visit(MethodVisitor mv) {
        mv.visitLineNumber(line, label);
    }
}
