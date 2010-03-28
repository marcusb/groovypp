package org.mbte.groovypp.compiler.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class VisitTableSwitchInsn extends AsmInstr {
    public final int min, max;
    public final Label dflt, labels [];

    public VisitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {
        this.min = min;
        this.max = max;
        this.dflt = dflt;
        this.labels = labels;
    }

    public void visit(MethodVisitor mv) {
        mv.visitTableSwitchInsn(min, max, dflt, labels);
    }
}
