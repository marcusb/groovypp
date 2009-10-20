package org.mbte.groovypp.compiler.bytecode;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.syntax.Token;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;
import org.objectweb.asm.MethodVisitor;

public class ResolvedFieldBytecodeExpr extends ResolvedLeftExpr {
    private final FieldNode fieldNode;
    private final BytecodeExpr object;
    private final BytecodeExpr value;

    public ResolvedFieldBytecodeExpr(ASTNode parent, FieldNode fieldNode, BytecodeExpr object, BytecodeExpr value, CompilerTransformer compiler) {
        super(parent, getType(object, fieldNode));
        this.fieldNode = fieldNode;
        this.object = object;
        this.value = value;

        if (fieldNode.isFinal() && value != null) {
            compiler.addError("Can't modify final field " + formatFieldName(), parent);
        }

        checkFieldAccess(parent, fieldNode, compiler);
    }

    private void checkFieldAccess(ASTNode parent, FieldNode fieldNode, CompilerTransformer compiler) {
        if ((fieldNode.getModifiers() & ACC_PUBLIC) == 0) {
            if ((fieldNode.getModifiers() & ACC_PRIVATE) != 0) {
                if (!compiler.classNode.equals(fieldNode.getDeclaringClass())) {
                    compiler.addError("Can't access private field " + formatFieldName(), parent);
                }
            }

            if ((fieldNode.getModifiers() & ACC_PROTECTED) != 0) {
                if (!compiler.classNode.isDerivedFrom(fieldNode.getDeclaringClass())
                        && !compiler.samePackage(fieldNode)) {
                    compiler.addError("Can't access protected field " + formatFieldName(), parent);
                }
            }

            if ((fieldNode.getModifiers() & (ACC_PUBLIC | ACC_PROTECTED | ACC_PRIVATE)) == 0) {
                if (!compiler.samePackage(fieldNode)) {
                    compiler.addError("Can't access package scoped field " + formatFieldName(), parent);
                }
            }
        }
    }

    private String formatFieldName() {
        return fieldNode.getDeclaringClass().getName() + "." + fieldNode.getName();
    }

    private static ClassNode getType(BytecodeExpr object, FieldNode fieldNode) {
        ClassNode type = fieldNode.getType();
        return object != null ? TypeUtil.getSubstitutedType(type,
                fieldNode.getDeclaringClass(), object.getType()) : type;
    }

    public void compile(MethodVisitor mv) {
        int op;
        if (object != null) {
            object.visit(mv);
            if (fieldNode.isStatic()) {
                pop(object.getType(), mv);
            } else {
                object.box(object.getType(), mv);
            }
        }

        if (value == null) {
            op = fieldNode.isStatic() ? GETSTATIC : GETFIELD;
        } else {
            op = fieldNode.isStatic() ? PUTSTATIC : PUTFIELD;
            value.visit(mv);

            if (object != null)
                dup_x1(value.getType(), mv);
            else
                dup(value.getType(), mv);

            box(value.getType(), mv);
            unbox(fieldNode.getType(), mv);
        }
        mv.visitFieldInsn(op, BytecodeHelper.getClassInternalName(fieldNode.getDeclaringClass()), fieldNode.getName(), BytecodeHelper.getTypeDescription(fieldNode.getType()));
        cast(ClassHelper.getWrapper(fieldNode.getType()), ClassHelper.getWrapper(getType()), mv);
    }

    public BytecodeExpr createAssign(ASTNode parent, BytecodeExpr right, CompilerTransformer compiler) {
        return new ResolvedFieldBytecodeExpr(parent, fieldNode, object, right, compiler);
    }

    public BytecodeExpr createBinopAssign(ASTNode parent, Token method, final BytecodeExpr right, CompilerTransformer compiler) {
        final BytecodeExpr opLeft = new BytecodeExpr(this, getType()) {
            @Override
            protected void compile(MethodVisitor mv) {
            }
        };
        final BinaryExpression op = new BinaryExpression(opLeft, method, right);
        op.setSourcePosition(parent);
        final BytecodeExpr transformedOp = compiler.cast((BytecodeExpr) compiler.transform(op), getType());
        return new BytecodeExpr(parent, getType()) {
            @Override
            protected void compile(MethodVisitor mv) {
                if (object != null) {
                    object.visit(mv);
                    if (fieldNode.isStatic()) {
                        pop(object.getType(), mv);
                    } else {
                        object.box(object.getType(), mv);
                        mv.visitInsn(DUP);
                    }
                }

                mv.visitFieldInsn(fieldNode.isStatic() ? GETSTATIC : GETFIELD, BytecodeHelper.getClassInternalName(fieldNode.getDeclaringClass()), fieldNode.getName(), BytecodeHelper.getTypeDescription(fieldNode.getType()));

                transformedOp.visit(mv);

                if (object != null)
                    dup_x1(getType(), mv);
                else
                    dup(getType(), mv);

                mv.visitFieldInsn(fieldNode.isStatic() ? PUTSTATIC : PUTFIELD, BytecodeHelper.getClassInternalName(fieldNode.getDeclaringClass()), fieldNode.getName(), BytecodeHelper.getTypeDescription(fieldNode.getType()));
            }
        };
    }

    public BytecodeExpr createPrefixOp(ASTNode parent, int type, CompilerTransformer compiler) {
        return null;
    }

    public BytecodeExpr createPostfixOp(ASTNode parent, int type, CompilerTransformer compiler) {
        return null;
    }
}