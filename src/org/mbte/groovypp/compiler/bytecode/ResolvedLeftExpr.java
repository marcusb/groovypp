package org.mbte.groovypp.compiler.bytecode;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.syntax.Token;
import org.mbte.groovypp.compiler.CompilerTransformer;

public abstract class ResolvedLeftExpr extends BytecodeExpr {

    public ResolvedLeftExpr(ASTNode parent, ClassNode type) {
        super(parent, type);
    }

    public abstract BytecodeExpr createAssign(ASTNode parent, BytecodeExpr right, CompilerTransformer compiler);

    public abstract BytecodeExpr createBinopAssign(ASTNode parent, Token method, BytecodeExpr right, CompilerTransformer compiler);
}
