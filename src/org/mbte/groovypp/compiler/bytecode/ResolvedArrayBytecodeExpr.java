package org.mbte.groovypp.compiler.bytecode;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.syntax.Token;
import org.mbte.groovypp.compiler.CompilerTransformer;

public class ResolvedArrayBytecodeExpr extends ResolvedLeftExpr {
    private final BytecodeExpr array;
    private final BytecodeExpr index;

    public ResolvedArrayBytecodeExpr(ASTNode parent, BytecodeExpr array, BytecodeExpr index, CompilerTransformer compiler) {
        super(parent, array.getType().getComponentType());
        this.array = array;
        this.index = compiler.cast(index, ClassHelper.int_TYPE);
    }

    protected void compile() {
        array.visit(mv);
        index.visit(mv);
        loadArray(getType());
    }

    public BytecodeExpr createAssign(ASTNode parent, BytecodeExpr right0, CompilerTransformer compiler) {
        final BytecodeExpr right = compiler.cast(right0, getType());
        return new BytecodeExpr(parent, getType()) {
            protected void compile() {
                array.visit(mv);
                index.visit(mv);
                right.visit(mv);
                dup_x2(getType());
                storeArray(getType());
            }
        };
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
                array.visit(mv);
                index.visit(mv);
                mv.visitInsn(DUP2);
                loadArray(getType());

                transformedOp.visit(mv);

                dup_x2(getType());
                storeArray(getType());
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
