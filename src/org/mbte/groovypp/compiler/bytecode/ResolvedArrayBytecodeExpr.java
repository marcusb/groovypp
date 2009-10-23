package org.mbte.groovypp.compiler.bytecode;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.objectweb.asm.MethodVisitor;

public class ResolvedArrayBytecodeExpr extends ResolvedLeftExpr {
    private final BytecodeExpr array;
    private final BytecodeExpr index;

    public ResolvedArrayBytecodeExpr(ASTNode parent, BytecodeExpr array, BytecodeExpr index, CompilerTransformer compiler) {
        super(parent, array.getType().getComponentType());
        this.array = array;
        this.index = compiler.cast(index, ClassHelper.int_TYPE);
    }

    protected void compile(MethodVisitor mv) {
        array.visit(mv);
        index.visit(mv);
        if (ClassHelper.isPrimitiveType(getType()))
            mv.visitMethodInsn(INVOKESTATIC, "org/mbte/groovypp/runtime/ArraysMethods", "getAt", "("+BytecodeHelper.getTypeDescription(array.getType()) + "I)" + BytecodeHelper.getTypeDescription(getType()));
        else {
            mv.visitMethodInsn(INVOKESTATIC, "org/mbte/groovypp/runtime/ArraysMethods", "getAt", "([Ljava/lang/Object;I)Ljava/lang/Object;");
        }
    }

    public BytecodeExpr createAssign(ASTNode parent, BytecodeExpr right0, CompilerTransformer compiler) {
        final BytecodeExpr right = compiler.cast(right0, getType());
        return new BytecodeExpr(parent, getType()) {
            protected void compile(MethodVisitor mv) {
                array.visit(mv);
                index.visit(mv);
                right.visit(mv);
                dup_x2(getType(), mv);
                if (ClassHelper.isPrimitiveType(getType()))
                    mv.visitMethodInsn(INVOKESTATIC, "org/mbte/groovypp/runtime/ArraysMethods", "putAt", "("+BytecodeHelper.getTypeDescription(array.getType()) + "I" + BytecodeHelper.getTypeDescription(getType())+ ")V");
                else
                    mv.visitMethodInsn(INVOKESTATIC, "org/mbte/groovypp/runtime/ArraysMethods", "putAt", "([Ljava/lang/Object;ILjava/lang/Object;)V");
            }
        };
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
                array.visit(mv);
                index.visit(mv);
                mv.visitInsn(DUP2);

                if (ClassHelper.isPrimitiveType(getType()))
                    mv.visitMethodInsn(INVOKESTATIC, "org/mbte/groovypp/runtime/ArraysMethods", "getAt", "("+BytecodeHelper.getTypeDescription(array.getType()) + "I)" + BytecodeHelper.getTypeDescription(getType()));
                else {
                    mv.visitMethodInsn(INVOKESTATIC, "org/mbte/groovypp/runtime/ArraysMethods", "getAt", "([Ljava/lang/Object;I)Ljava/lang/Object;");
                }

                transformedOp.visit(mv);

                dup_x2(getType(), mv);

                if (ClassHelper.isPrimitiveType(getType()))
                    mv.visitMethodInsn(INVOKESTATIC, "org/mbte/groovypp/runtime/ArraysMethods", "putAt", "("+BytecodeHelper.getTypeDescription(array.getType()) + "I" + BytecodeHelper.getTypeDescription(getType())+ ")V");
                else
                    mv.visitMethodInsn(INVOKESTATIC, "org/mbte/groovypp/runtime/ArraysMethods", "putAt", "([Ljava/lang/Object;ILjava/lang/Object;)V");
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
