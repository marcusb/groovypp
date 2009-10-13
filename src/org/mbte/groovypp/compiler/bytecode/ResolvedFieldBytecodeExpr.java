package org.mbte.groovypp.compiler.bytecode;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.syntax.Token;
import org.mbte.groovypp.compiler.CompilerTransformer;

public class ResolvedFieldBytecodeExpr extends ResolvedLeftExpr {
    private final FieldNode fieldNode;
    private final BytecodeExpr object;
    private final BytecodeExpr value;

    public ResolvedFieldBytecodeExpr(ASTNode parent, FieldNode fieldNode, BytecodeExpr object, BytecodeExpr value) {
        super(parent, fieldNode.getType());
        this.fieldNode = fieldNode;
        this.object = object;
        this.value = value;
    }

    public void compile() {
        int op;
        if (object != null) {
            object.visit(mv);
            if (fieldNode.isStatic()) {
                pop(object.getType());
            } else {
                object.box(object.getType());
            }
        }

        if (value == null) {
            op = fieldNode.isStatic() ? GETSTATIC : GETFIELD;
        } else {
            op = fieldNode.isStatic() ? PUTSTATIC : PUTFIELD;
            value.visit(mv);

            if (object != null)
                dup_x1(value.getType());
            else
                dup(value.getType());

            box(value.getType());
            cast(ClassHelper.getWrapper(value.getType()), ClassHelper.getWrapper(fieldNode.getType()));
            unbox(fieldNode.getType());
        }
        mv.visitFieldInsn(op, BytecodeHelper.getClassInternalName(fieldNode.getDeclaringClass()), fieldNode.getName(), BytecodeHelper.getTypeDescription(fieldNode.getType()));
    }

    public BytecodeExpr createAssign(ASTNode parent, BytecodeExpr right, CompilerTransformer compiler) {
        return new ResolvedFieldBytecodeExpr(parent, fieldNode, object, right);
    }

    public BytecodeExpr createBinopAssign(ASTNode parent, Token method, final BytecodeExpr right, CompilerTransformer compiler) {
        final BytecodeExpr opLeft = new BytecodeExpr(this, getType()) {
            @Override
            protected void compile() {
            }
        };
        final BinaryExpression op = new BinaryExpression(opLeft, method, right);
        op.setSourcePosition(parent);
        final BytecodeExpr transformedOp = compiler.cast((BytecodeExpr) compiler.transform(op), getType());
        return new BytecodeExpr(parent, getType()) {
            @Override
            protected void compile() {
                if (object != null) {
                    object.visit(mv);
                    if (fieldNode.isStatic()) {
                        pop(object.getType());
                    } else {
                        object.box(object.getType());
                        mv.visitInsn(DUP);
                    }
                }

                mv.visitFieldInsn(fieldNode.isStatic() ? GETSTATIC : GETFIELD, BytecodeHelper.getClassInternalName(fieldNode.getDeclaringClass()), fieldNode.getName(), BytecodeHelper.getTypeDescription(fieldNode.getType()));

                transformedOp.visit(mv);

                if (object != null)
                    dup_x1(getType());
                else
                    dup(getType());

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