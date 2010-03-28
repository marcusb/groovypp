package org.mbte.groovypp.compiler.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;

import java.util.List;
import java.util.ArrayList;

public class StoringMethodVisitor implements MethodVisitor {
    public final List<AsmInstr> operations = new ArrayList<AsmInstr>();

    public void visitInsn(int opcode) {
        operations.add(new VisitInsn(opcode));
    }

    public void visitIntInsn(int opcode, int operand) {
        operations.add(new VisitIntInsn(opcode, operand));
    }

    public void visitVarInsn(int opcode, int var) {
        operations.add(new VisitVarInsn(opcode, var));
    }

    public void visitTypeInsn(int opcode, String type) {
        operations.add(new VisitTypeInsn(opcode, type));
    }

    public void visitFieldInsn(int opcode, String owner, String name, String type) {
        operations.add(new VisitFieldInsn(opcode, owner, name, type));
    }

    public void visitMethodInsn(int opcode, String owner, String name, String descr) {
        operations.add(new VisitMethodInsn(opcode, owner, name, descr));
    }

    public void visitJumpInsn(int opcode, Label label) {
        operations.add(new VisitJumpInsn(opcode, label));
    }

    public void visitLabel(Label label) {
        operations.add(new VisitLabel(label));
    }

    public void visitLdcInsn(Object value) {
        operations.add(new VisitLdcInsn(value));
    }

    public void visitIincInsn(int var, int increment) {
        operations.add(new VisitIincInsn(var, increment));
    }

    public void visitTableSwitchInsn(int i, int i1, Label label, Label[] labels) {
        operations.add(new VisitTableSwitchInsn(i, i1, label, labels));
    }

    public void visitLookupSwitchInsn(Label label, int[] ints, Label[] labels) {
        operations.add(new VisitLookupSwitchInsn(label, ints, labels));
    }

    public void visitMultiANewArrayInsn(String s, int i) {
        operations.add(new VisitMultiANewArrayInsn(s, i));
    }

    public void visitTryCatchBlock(Label label, Label label1, Label label2, String s) {
        operations.add(new VisitTryCatchBlock(label, label1, label2, s));
    }

    public void visitLocalVariable(String s, String s1, String s2, Label label, Label label1, int i) {
        operations.add(new VisitLocalVariable(s, s1, label, label1, i));
    }

    public void visitLineNumber(int line, Label label) {
        operations.add(new VisitLineNumber(line, label));
    }

    public void visitMaxs(int i, int i1) {
        throw new UnsupportedOperationException();
    }

    public void visitEnd() {
        throw new UnsupportedOperationException();
    }

    public AnnotationVisitor visitAnnotationDefault() {
        throw new UnsupportedOperationException();
    }

    public AnnotationVisitor visitAnnotation(String s, boolean b) {
        throw new UnsupportedOperationException();
    }

    public AnnotationVisitor visitParameterAnnotation(int i, String s, boolean b) {
        throw new UnsupportedOperationException();
    }

    public void visitAttribute(Attribute attribute) {
        throw new UnsupportedOperationException();
    }

    public void visitCode() {
        throw new UnsupportedOperationException();
    }

    public void visitFrame(int i, int i1, Object[] objects, int i2, Object[] objects1) {
        throw new UnsupportedOperationException();
    }
}
