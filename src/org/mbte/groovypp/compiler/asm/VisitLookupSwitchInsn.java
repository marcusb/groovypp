package org.mbte.groovypp.compiler.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class VisitLookupSwitchInsn extends AsmInstr {
    public final Label dflt;
    public final int[] keys;
    public final Label[] labels;

    public VisitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        this.dflt = dflt;
        this.keys = keys;
        this.labels = labels;
    }

    public void visit(MethodVisitor mv) {
        mv.visitLookupSwitchInsn(dflt, keys, labels);
    }
}
