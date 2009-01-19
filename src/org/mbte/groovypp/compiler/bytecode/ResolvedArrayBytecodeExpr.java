package org.mbte.groovypp.compiler.bytecode;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.mbte.groovypp.compiler.CompilerTransformer;

public class ResolvedArrayBytecodeExpr extends ResolvedLeftExpr {
    private final BytecodeExpr array;
    private final BytecodeExpr index;

    public ResolvedArrayBytecodeExpr(ASTNode parent, BytecodeExpr array, BytecodeExpr index) {
        super(parent, array.getType().getComponentType());
        this.array = array;
        this.index = index;
    }

    protected void compile() {
        array.visit(mv);
        index.visit(mv);
        box(index.getType());
        cast(ClassHelper.getWrapper(index.getType()), ClassHelper.Integer_TYPE);
        unbox(ClassHelper.int_TYPE);
        loadArray(getType());
    }

    public BytecodeExpr createAssign(ASTNode parent, final BytecodeExpr right, CompilerTransformer compiler) {
        return new BytecodeExpr(parent, getType()) {
            protected void compile() {
                array.visit(mv);
                index.visit(mv);
                box(index.getType());
                cast(ClassHelper.getWrapper(index.getType()), ClassHelper.Integer_TYPE);
                unbox(ClassHelper.int_TYPE);
                right.visit(mv);
                box(right.getType());
                cast(ClassHelper.getWrapper(right.getType()), ClassHelper.getWrapper(getType()));
                unbox(getType());
                dup_x2(getType());
                storeArray(getType());
            }
        };
    }

    public BytecodeExpr createBinopAssign(ASTNode parent, BytecodeExpr right, int type, CompilerTransformer compiler) {
        return null;
    }

    public BytecodeExpr createPrefixOp(ASTNode parent, int type, CompilerTransformer compiler) {
        return null;
    }

    public BytecodeExpr createPostfixOp(ASTNode parent, int type, CompilerTransformer compiler) {
        return null;
    }
}
