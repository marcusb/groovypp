package org.mbte.groovypp.compiler.bytecode;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.mbte.groovypp.compiler.CompilerStack;

public class UneededDupStoreRemoverMethodAdapter extends UneededBoxingRemoverMethodAdapter {
    private int dupCode, storeCode, storeIndex = -1;

    public UneededDupStoreRemoverMethodAdapter(MethodVisitor mv) {
        super(mv);
    }

    private void dropDupStore() {
        if (dupCode != 0) {
            super.visitInsn(dupCode);
            dupCode = 0;
            if (storeCode != 0) {
                super.visitVarInsn(storeCode, storeIndex);
                storeCode = 0;
                storeIndex = -1;
            }
        }
    }

    public void visitInsn(int opcode) {
        if (storeIndex != -1 && (opcode == POP || opcode == POP2)) {
            super.visitVarInsn(storeCode, storeIndex);
            dupCode = 0;
            storeCode = 0;
            storeIndex = -1;
        }
        else {
            if (dupCode == 0 && (opcode == DUP || opcode == DUP2)) {
                dupCode = opcode;
            }
            else {
                dropDupStore();
                super.visitInsn(opcode);
            }
        }
    }

    public void visitIntInsn(int opcode, int operand) {
        dropDupStore();
        super.visitIntInsn(opcode, operand);
    }

    public void visitVarInsn(int opcode, int var) {
        if (dupCode != 0 && storeIndex == -1) {
            switch (opcode) {
                case ISTORE:
                case LSTORE:
                case FSTORE:
                case DSTORE:
                case ASTORE:
                    storeCode = opcode;
                    storeIndex = var;
                    break;

                default:
                    super.visitInsn(dupCode);
                    super.visitVarInsn(opcode, var);
                    dupCode = 0;
            }
        }
        else {
            super.visitVarInsn(opcode, var);
        }
    }

    public void visitTypeInsn(int opcode, String desc) {
        dropDupStore();
        super.visitTypeInsn(opcode, desc);
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        dropDupStore();
        super.visitFieldInsn(opcode, owner, name, desc);
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        dropDupStore();
        super.visitMethodInsn(opcode, owner, name, desc);
    }

    public void visitJumpInsn(int opcode, Label label) {
        dropDupStore();
        super.visitJumpInsn(opcode, label);
    }

    public void visitLabel(Label label) {
        dropDupStore();
        super.visitLabel(label);
    }

    public void visitLdcInsn(Object cst) {
        dropDupStore();
        super.visitLdcInsn(cst);
    }

    public void visitIincInsn(int var, int increment) {
        dropDupStore();
        super.visitIincInsn(var, increment);
    }

    public void visitTableSwitchInsn(int min, int max, Label dflt, Label labels[]) {
        dropDupStore();
        super.visitTableSwitchInsn(min, max, dflt, labels);
    }

    public void visitLookupSwitchInsn(Label dflt, int keys[], Label labels[]) {
        dropDupStore();
        super.visitLookupSwitchInsn(dflt, keys, labels);
    }

    public void visitMultiANewArrayInsn(String desc, int dims) {
        dropDupStore();
        super.visitMultiANewArrayInsn(desc, dims);
    }

    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        dropDupStore();
        super.visitTryCatchBlock(start, end, handler, type);
    }
}