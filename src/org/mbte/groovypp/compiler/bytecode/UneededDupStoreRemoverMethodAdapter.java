package org.mbte.groovypp.compiler.bytecode;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.mbte.groovypp.compiler.CompilerStack;

public class UneededDupStoreRemoverMethodAdapter extends UneededBoxingRemoverMethodAdapter {
    private int dupCode;

    private Store store;

    abstract class Store {
        abstract void execute (MethodVisitor mv);
    }

    class StoreVar extends Store {
        final int storeCode, storeIndex;

        public StoreVar(int storeCode, int storeIndex) {
            this.storeCode = storeCode;
            this.storeIndex = storeIndex;
        }

        void execute (MethodVisitor mv) {
            UneededDupStoreRemoverMethodAdapter.super.visitVarInsn(storeCode, storeIndex);
        }
    }

    class StoreStaticField extends Store {
        final int opcode;
        final String owner, name, desc;

        public StoreStaticField(int opcode, String owner, String name, String desc) {
            this.opcode = opcode;
            this.desc = desc;
            this.name = name;
            this.owner = owner;
        }

        void execute (MethodVisitor mv) {
            UneededDupStoreRemoverMethodAdapter.super.visitFieldInsn(opcode, owner, name, desc);
        }
    }

    public UneededDupStoreRemoverMethodAdapter(MethodVisitor mv) {
        super(mv);
    }

    private void dropDupStore() {
        if (dupCode != 0) {
            super.visitInsn(dupCode);
            dupCode = 0;
            if (store != null) {
                store.execute(mv);
                store = null;
            }
        }
    }

    public void visitInsn(int opcode) {
        if (store != null && (opcode == POP || opcode == POP2)) {
            dupCode = 0;
            store.execute(mv);
            store = null;
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
        if (dupCode != 0 && store == null) {
            switch (opcode) {
                case ISTORE:
                case LSTORE:
                case FSTORE:
                case DSTORE:
                case ASTORE:
                    store = new StoreVar(opcode, var);
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
        if (dupCode != 0 && store == null) {
            switch (opcode) {
                case PUTSTATIC:
                    store = new StoreStaticField(opcode, owner, name, desc);
                    break;

                default:
                    super.visitInsn(dupCode);
                    super.visitFieldInsn(opcode, owner, name, desc);
                    dupCode = 0;
            }
        }
        else {
            super.visitFieldInsn(opcode, owner, name, desc);
        }
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