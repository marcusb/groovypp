package org.mbte.groovypp.compiler;

import org.codehaus.groovy.classgen.BytecodeInstruction;
import org.objectweb.asm.MethodVisitor;
import org.mbte.groovypp.compiler.asm.*;

public class StoredBytecodeInstruction extends BytecodeInstruction {

    private final StoringMethodVisitor storage = new StoringMethodVisitor ();

    public void visit(MethodVisitor mv) {
        for(AsmInstr op : storage.operations)
            op.visit(mv);
        storage.operations.clear();
    }

    public MethodVisitor createStorage() {
        return storage;
    }

    void clear () {
        storage.operations.clear();
    }
}