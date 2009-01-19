package org.mbte.groovypp.compiler.bytecode;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.mbte.groovypp.compiler.CompilerTransformer;

public class ResolvedArrayLikeBytecodeExpr extends ResolvedLeftExpr {
    private final BytecodeExpr array;
    private final BytecodeExpr index;
    private final BytecodeExpr getter;
    private final MethodNode   setter;

    public ResolvedArrayLikeBytecodeExpr(ASTNode parent, BytecodeExpr array, BytecodeExpr index, MethodNode getter, MethodNode setter) {
        super(parent, getter.getReturnType());
        this.array = array;
        this.index = index;
        this.getter = new ResolvedMethodBytecodeExpr(parent, getter, array, new ArgumentListExpression(index));
        this.setter = setter;
    }

    protected void compile() {
        getter.compile();
    }

    public BytecodeExpr createAssign(ASTNode parent, BytecodeExpr right, CompilerTransformer compiler) {
        return new ResolvedMethodBytecodeExpr(parent, setter, array, new ArgumentListExpression(index, right));
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
