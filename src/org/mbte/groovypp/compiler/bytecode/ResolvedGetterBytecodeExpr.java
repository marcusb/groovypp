package org.mbte.groovypp.compiler.bytecode;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.mbte.groovypp.compiler.ClassNodeCache;
import org.mbte.groovypp.compiler.CompiledClosureBytecodeExpr;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.CompilerTransformer;

public class ResolvedGetterBytecodeExpr extends ResolvedLeftExpr {
    private final MethodNode methodNode;
    private final BytecodeExpr object;
    private final BytecodeExpr getter;

    public ResolvedGetterBytecodeExpr(ASTNode parent, MethodNode methodNode, BytecodeExpr object) {
        super (parent, methodNode.getReturnType());
        this.methodNode = methodNode;
        this.object = object;
        getter = new ResolvedMethodBytecodeExpr(parent, methodNode, object, new ArgumentListExpression());
    }

    protected void compile() {
        getter.visit (mv);
    }

    public BytecodeExpr createAssign(ASTNode parent, BytecodeExpr right, CompilerTransformer compiler) {
        String name = methodNode.getName().substring(3);
        Object prop = PropertyUtil.resolveSetProperty(methodNode.getDeclaringClass(), name, right.getType(), compiler);
        return PropertyUtil.createSetProperty(parent, compiler, name, object, right, prop); 
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