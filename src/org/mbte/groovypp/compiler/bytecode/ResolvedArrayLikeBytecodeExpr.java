package org.mbte.groovypp.compiler.bytecode;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.syntax.Token;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.objectweb.asm.MethodVisitor;

public class ResolvedArrayLikeBytecodeExpr extends ResolvedLeftExpr {
    private final BytecodeExpr array;
    private final BytecodeExpr index;
    private final BytecodeExpr getter;
    private final MethodNode setter;

    public ResolvedArrayLikeBytecodeExpr(ASTNode parent, BytecodeExpr array, BytecodeExpr index, MethodNode getter, MethodNode setter, CompilerTransformer compiler) {
        super(parent, getter.getReturnType());
        this.array = array;
        this.index = index;
        this.getter = new ResolvedMethodBytecodeExpr(parent, getter, array, new ArgumentListExpression(index), compiler);
        this.setter = setter;
    }

    protected void compile(MethodVisitor mv) {
        getter.visit(mv);
    }

    public BytecodeExpr createAssign(ASTNode parent, BytecodeExpr right, CompilerTransformer compiler) {
        if (setter == null) {
            compiler.addError("Can't find method 'putAt' for type: " + getType().getName(), parent);
            return null;
        }

        return new ResolvedMethodBytecodeExpr(parent, setter, array, new ArgumentListExpression(index, right), compiler);
    }

    public BytecodeExpr createBinopAssign(ASTNode parent, Token right, BytecodeExpr type, CompilerTransformer compiler) {
        return null;
    }

    public BytecodeExpr createPrefixOp(ASTNode parent, int type, CompilerTransformer compiler) {
        return null;
    }

    public BytecodeExpr createPostfixOp(ASTNode parent, int type, CompilerTransformer compiler) {
        return null;
    }
}
