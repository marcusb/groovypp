package org.mbte.groovypp.compiler.bytecode;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;

public abstract class ResolvedLeftExpr extends BytecodeExpr{

    public ResolvedLeftExpr(ASTNode parent, ClassNode type) {
        super(parent, type);
    }

    public abstract BytecodeExpr createAssign(ASTNode parent, BytecodeExpr right, CompilerTransformer compiler);

    public abstract BytecodeExpr createBinopAssign(ASTNode parent, BytecodeExpr right, int type, CompilerTransformer compiler);

    public abstract BytecodeExpr createPrefixOp(ASTNode parent, int type, CompilerTransformer compiler);

    public abstract BytecodeExpr createPostfixOp(ASTNode parent, int type, CompilerTransformer compiler);
}
