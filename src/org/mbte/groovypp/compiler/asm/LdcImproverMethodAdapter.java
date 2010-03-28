package org.mbte.groovypp.compiler.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.mbte.groovypp.compiler.bytecode.StackAwareMethodAdapter;

import java.math.BigDecimal;
import java.math.BigInteger;

public class LdcImproverMethodAdapter extends StackAwareMethodAdapter implements Opcodes {
    public LdcImproverMethodAdapter(MethodVisitor methodVisitor) {
        super(methodVisitor);
    }

    public void visitLdcInsn(Object cst) {
        if (cst instanceof Integer) {
            Integer value = (Integer) cst;
            switch (value) {
                case -1:
                    super.visitInsn(Opcodes.ICONST_M1);
                    break;
                case 0:
                    super.visitInsn(Opcodes.ICONST_0);
                    break;
                case 1:
                    super.visitInsn(Opcodes.ICONST_1);
                    break;
                case 2:
                    super.visitInsn(Opcodes.ICONST_2);
                    break;
                case 3:
                    super.visitInsn(Opcodes.ICONST_3);
                    break;
                case 4:
                    super.visitInsn(Opcodes.ICONST_4);
                    break;
                case 5:
                    super.visitInsn(Opcodes.ICONST_5);
                    break;
                default:
                    if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
                        super.visitIntInsn(Opcodes.BIPUSH, value);
                    } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
                        super.visitIntInsn(Opcodes.SIPUSH, value);
                    } else {
                        super.visitLdcInsn(Integer.valueOf(value));
                    }
            }
        } else if (cst instanceof BigDecimal) {
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
        else if (cst instanceof Double) {
            Double aDouble = (Double) cst;
            if (aDouble == 1.0d)
                super.visitInsn(DCONST_1);
            else
                super.visitLdcInsn(cst);
        }
        else if (cst instanceof Long) {
            Long aLong = (Long) cst;
            if (aLong == 0L)
                super.visitInsn(LCONST_0);
            else
                if (aLong == 1L)
                    super.visitInsn(LCONST_1);
                else
                    super.visitLdcInsn(cst);
        }
        else if (cst instanceof Float) {
            Float aFloat = (Float) cst;
            if (aFloat == 1.0f)
                super.visitInsn(FCONST_1);
            else
                if (aFloat == 2.0f)
                    super.visitInsn(FCONST_2);
                else
                    super.visitLdcInsn(cst);
        }
        else if (cst == null) {
            super.visitInsn(ACONST_NULL);
        }
        else
            super.visitLdcInsn(cst);
    }
}
