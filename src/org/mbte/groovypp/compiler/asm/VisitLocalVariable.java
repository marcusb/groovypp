package org.mbte.groovypp.compiler.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class VisitLocalVariable extends AsmInstr {
    public final String name, desc;
    public final Label start, end;
    public final int index;

    public VisitLocalVariable(String name, String desc, Label start, Label end, int index) {
        this.name = name;
        this.desc = desc;
        this.start = start;
        this.end = end;
        this.index = index;
    }

    public void visit(MethodVisitor mv) {
        mv.visitLocalVariable(name, desc, null, start, end, index);
    }
}
