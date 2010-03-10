package org.mbte.groovypp.compiler.bytecode;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.syntax.Token;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;
import org.objectweb.asm.MethodVisitor;

public class ResolvedLeftMapExpr extends ResolvedLeftExpr {
    private final BytecodeExpr object;
    private final String propName;

    public ResolvedLeftMapExpr(ASTNode parent, BytecodeExpr object, String propName) {
        super(parent, TypeUtil.getSubstitutedType(ClassHelper.MAP_TYPE, ClassHelper.MAP_TYPE, object.getType()).getGenericsTypes()[1].getType());
        this.object = object;
        this.propName = propName;
    }

    public BytecodeExpr createAssign(ASTNode parent, BytecodeExpr right, CompilerTransformer compiler) {
        return null;
    }

    public BytecodeExpr createBinopAssign(ASTNode parent, Token method, BytecodeExpr right, CompilerTransformer compiler) {
        return null;
    }

    protected void compile(MethodVisitor mv) {
        object.visit(mv);
        mv.visitLdcInsn(propName);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
        checkCast(getType(), mv);
    }
}
