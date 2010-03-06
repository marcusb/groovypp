package org.mbte.groovypp.compiler.bytecode;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.mbte.groovypp.compiler.CompilerStack;

public class UneededLoadPopRemoverMethodAdapter extends UneededDupStoreRemoverMethodAdapter {
    private Load load;

    static abstract class Load {
        abstract void execute ();
    }

    class VarLabel extends Load {
        private Label label;

        public VarLabel(Label label) {
            this.label = label;
        }

        void execute() {
            UneededLoadPopRemoverMethodAdapter.super.visitLabel(label);
        }
    }

    class LoadVar extends Load {
        private int opcode;
        private int var;

        public LoadVar(int opcode, int var) {
            this.opcode = opcode;
            this.var = var;
        }

        public void execute() {
            UneededLoadPopRemoverMethodAdapter.super.visitVarInsn(opcode, var);
        }
    }

    class Ldc extends Load {
        private Object cst;

        public Ldc(Object cst) {
            this.cst = cst;
        }

        public void execute() {
            UneededLoadPopRemoverMethodAdapter.super.visitLdcInsn(cst);
        }
    }

    class Checkcast extends Load {
        private String descr;

        public Checkcast(String descr) {
            this.descr = descr;
        }

        public void execute() {
            UneededLoadPopRemoverMethodAdapter.super.visitTypeInsn(CHECKCAST, descr);
        }
    }

    public UneededLoadPopRemoverMethodAdapter(MethodVisitor mv) {
        super(mv);
    }

    private void dropLoad() {
        if (load != null) {
            load.execute();
            load = null;
        }
    }

    public void visitInsn(int opcode) {
        if (load != null && (opcode == POP || opcode == POP2)) {
            if (load instanceof VarLabel) {
                super.visitInsn(opcode);
                super.visitLabel(((VarLabel)load).label);
            }
            else
                if (load instanceof Checkcast) {
                    super.visitInsn(opcode);
                }
            load = null;
            return;
        }

        dropLoad();
        switch (opcode) {
            case ACONST_NULL: visitLdcInsn(null); break;
            case ICONST_M1: visitLdcInsn(-1); break;
            case ICONST_0:  visitLdcInsn(0); break;
            case ICONST_1:  visitLdcInsn(1); break;
            case ICONST_2:  visitLdcInsn(2); break;
            case ICONST_3:  visitLdcInsn(3); break;
            case ICONST_4:  visitLdcInsn(4); break;
            case ICONST_5:  visitLdcInsn(5); break;
            case LCONST_0:  visitLdcInsn(0L); break;
            case LCONST_1:  visitLdcInsn(1L); break;
            case FCONST_0:  visitLdcInsn(0.0f); break;
            case FCONST_1:  visitLdcInsn(1.0f); break;
            case FCONST_2:  visitLdcInsn(2.0f); break;
            case DCONST_0:  visitLdcInsn(0.0d); break;
            case DCONST_1:  visitLdcInsn(1.0d); break;

            default:
                super.visitInsn(opcode);
        }
    }

    public void visitIntInsn(int opcode, int operand) {
        dropLoad();
        switch (opcode) {
            case BIPUSH:
            case SIPUSH:
                visitLdcInsn(operand);
                break;

            default:
                super.visitIntInsn(opcode, operand);
        }
    }

    public void visitVarInsn(int opcode, int var) {
        dropLoad();
        switch (opcode) {
            case ILOAD:
            case LLOAD:
            case FLOAD:
            case DLOAD:
            case ALOAD:
                load = new LoadVar(opcode, var);
                break;
            
            default:
                super.visitVarInsn(opcode, var);
        }
    }

    public void visitTypeInsn(int opcode, String desc) {
        dropLoad();
        switch (opcode) {
            case CHECKCAST:
                load = new Checkcast(desc);
                return;

            default:
                super.visitTypeInsn(opcode, desc);
                break;
        }
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        dropLoad();
        super.visitFieldInsn(opcode, owner, name, desc);
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        dropLoad();
        super.visitMethodInsn(opcode, owner, name, desc);
    }

    public void visitJumpInsn(int opcode, Label label) {
        dropLoad();
        super.visitJumpInsn(opcode, label);
    }

    public void visitLabel(Label label) {
        if (label instanceof CompilerStack.VarStartLabel) {
            dropLoad();
            load = new VarLabel(label);
        }
        else {
            dropLoad();
            super.visitLabel(label);
        }
    }

    public void visitLdcInsn(Object cst) {
        dropLoad();
        load = new Ldc(cst);
    }

    public void visitIincInsn(int var, int increment) {
        dropLoad();
        super.visitIincInsn(var, increment);
    }

    public void visitTableSwitchInsn(int min, int max, Label dflt, Label labels[]) {
        dropLoad();
        super.visitTableSwitchInsn(min, max, dflt, labels);
    }

    public void visitLookupSwitchInsn(Label dflt, int keys[], Label labels[]) {
        dropLoad();
        super.visitLookupSwitchInsn(dflt, keys, labels);
    }

    public void visitMultiANewArrayInsn(String desc, int dims) {
        dropLoad();
        super.visitMultiANewArrayInsn(desc, dims);
    }

    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        dropLoad();
        super.visitTryCatchBlock(start, end, handler, type);
    }
}