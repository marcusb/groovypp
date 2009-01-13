package org.mbte.groovypp.compiler.impl;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;

import java.math.BigDecimal;
import java.math.BigInteger;

class BytecodeImproverMethodAdapter extends StackAwareMethodAdapter implements Opcodes {
    private String boxingDesc = null;
    private static final String DTT = BytecodeHelper.getClassInternalName(DefaultTypeTransformation.class.getName());

    public BytecodeImproverMethodAdapter(MethodVisitor mv) {
        super(mv);
    }

    private void dropBoxing () {
        if (boxingDesc != null) {
            super.visitMethodInsn(Opcodes.INVOKESTATIC, DTT, "box", boxingDesc);
            boxingDesc = null;
        }
    }

    public void visitInsn(int opcode) {
        if (boxingDesc != null && (opcode == POP || opcode == POP2)) {
            boxingDesc = null;
        }

        dropBoxing ();
        super.visitInsn(opcode);
    }

    public void visitIntInsn(int opcode, int operand) {
        dropBoxing ();
        super.visitIntInsn(opcode, operand);
    }

    public void visitVarInsn(int opcode, int var) {
        dropBoxing ();
        super.visitVarInsn(opcode, var);
    }

    public void visitTypeInsn(int opcode, String desc) {
        if (opcode == CHECKCAST && desc.equals("java/lang/Object"))
           return;

        dropBoxing ();
        super.visitTypeInsn(opcode, desc);
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        dropBoxing ();
        super.visitFieldInsn(opcode, owner, name, desc);
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        if (boxing(opcode,owner,name)) {
            dropBoxing();
            boxingDesc = desc;
        }
        else {
          if (unboxing(opcode, owner, name)) {
              if (boxingDesc != null)
                boxingDesc = null;
              else
                super.visitMethodInsn(opcode, owner, name, desc);
          }
          else {
            dropBoxing();
            super.visitMethodInsn(opcode, owner, name, desc);
          }
        }
    }

    private boolean boxing(int opcode, String owner, String name) {
        return opcode == Opcodes.INVOKESTATIC && owner.equals(DTT) && name.equals("box");
    }

    private boolean unboxing(int opcode, String owner, String name) {
        return opcode == Opcodes.INVOKESTATIC && owner.equals(DTT) && name.endsWith("Unbox");
    }

    public void visitJumpInsn(int opcode, Label label) {
        dropBoxing ();
        super.visitJumpInsn(opcode, label);
    }

    public void visitLabel(Label label) {
        dropBoxing ();
        super.visitLabel(label);
    }

    public void visitLdcInsn(Object cst) {
        dropBoxing ();
        if (cst instanceof BigDecimal) {
            super.visitTypeInsn(NEW, "java/math/BigDecimal");
            super.visitInsn(DUP);
            super.visitLdcInsn(cst.toString());
            super.visitMethodInsn(INVOKESPECIAL, "java/math/BigDecimal", "<init>", "(Ljava/lang/String;)V");
        }
        else if (cst instanceof BigInteger) {
            super.visitTypeInsn(NEW, "java/math/BigInteger");
            super.visitInsn(DUP);
            super.visitLdcInsn(cst.toString());
            super.visitMethodInsn(INVOKESPECIAL, "java/math/BigInteger", "<init>", "(Ljava/lang/String;)V");
        }
        else
            super.visitLdcInsn(cst);
    }

    public void visitIincInsn(int var, int increment) {
        dropBoxing ();
        super.visitIincInsn(var, increment);
    }

    public void visitTableSwitchInsn(int min, int max, Label dflt, Label labels[]) {
        dropBoxing ();
        super.visitTableSwitchInsn(min, max, dflt, labels);
    }

    public void visitLookupSwitchInsn(Label dflt, int keys[], Label labels[]) {
        dropBoxing ();
        super.visitLookupSwitchInsn(dflt, keys, labels);
    }

    public void visitMultiANewArrayInsn(String desc, int dims) {
        dropBoxing ();
        super.visitMultiANewArrayInsn(desc, dims);
    }

    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        dropBoxing ();
        super.visitTryCatchBlock(start, end, handler, type);
    }

}
