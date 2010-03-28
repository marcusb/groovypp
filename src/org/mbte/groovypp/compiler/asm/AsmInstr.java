package org.mbte.groovypp.compiler.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.MethodVisitor;

public abstract class AsmInstr implements Opcodes {
    public abstract void visit(MethodVisitor mv);

}
