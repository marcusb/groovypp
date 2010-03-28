package org.mbte.groovypp.compiler.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class VisitTryCatchBlock extends AsmInstr {
    public final Label start, end, handler;
    public final String type;

    public VisitTryCatchBlock(Label start, Label end, Label handler, String type) {
        this.start = start;
        this.end = end;
        this.handler = handler;
        this.type = type;
    }

    public void visit(MethodVisitor mv) {
        mv.visitTryCatchBlock(start, end, handler, type);
    }
}
