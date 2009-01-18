package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.GroovyCodeVisitor;
import org.codehaus.groovy.classgen.BytecodeInstruction;
import org.codehaus.groovy.classgen.BytecodeSequence;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class NullReturnStatement extends BytecodeSequence implements Opcodes {
    @Override
    public void visit(GroovyCodeVisitor visitor) {
        if (visitor instanceof StaticCompiler)
          ((StaticCompiler)visitor).visitBytecodeSequence (this);
        else
          super.visit(visitor);
    }

    public NullReturnStatement(final ClassNode returnType) {
        super(new BytecodeInstruction(){
            public void visit(MethodVisitor mv) {
                if (returnType == ClassHelper.double_TYPE) {
                    mv.visitLdcInsn((double)0);
                    mv.visitInsn(DRETURN);
                } else if (returnType == ClassHelper.float_TYPE) {
                    mv.visitLdcInsn((float)0);
                    mv.visitInsn(FRETURN);
                } else if (returnType == ClassHelper.long_TYPE) {
                    mv.visitLdcInsn(0L);
                    mv.visitInsn(LRETURN);
                } else if (
                       returnType == ClassHelper.boolean_TYPE
                    || returnType == ClassHelper.char_TYPE
                    || returnType == ClassHelper.byte_TYPE
                    || returnType == ClassHelper.int_TYPE
                    || returnType == ClassHelper.short_TYPE) {
                    //byte,short,boolean,int are all IRETURN
                    mv.visitInsn(ICONST_0);
                    mv.visitInsn(IRETURN);
                } else if (returnType == ClassHelper.VOID_TYPE) {
                    mv.visitInsn(RETURN);
                } else {
                    mv.visitInsn(ACONST_NULL);
                    mv.visitInsn(ARETURN);
                }
            }
        });
    }
}
