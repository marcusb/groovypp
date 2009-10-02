package org.mbte.groovypp.compiler.bytecode;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.syntax.Token;
import org.mbte.groovypp.compiler.CompilerTransformer;

class UnresolvedLeftExpr extends ResolvedLeftExpr {
    private final BytecodeExpr value;
    private final BytecodeExpr object;
    private final String propName;

    public UnresolvedLeftExpr(ASTNode exp, BytecodeExpr value, BytecodeExpr object, String propName) {
        super(exp, value != null ? ClassHelper.OBJECT_TYPE : ClassHelper.VOID_TYPE);
        this.value = value;
        this.object = object;
        this.propName = propName;
    }

    protected void compile() {
        object.visit(mv);
        if (value == null) {
            // getter
            mv.visitLdcInsn(propName);
            mv.visitMethodInsn(INVOKESTATIC, "org/codehaus/groovy/runtime/InvokerHelper", "getProperty", "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;");
        } else {
            // setter
            mv.visitLdcInsn(propName);
            value.visit(mv);
            mv.visitMethodInsn(INVOKESTATIC, "org/codehaus/groovy/runtime/InvokerHelper", "setProperty", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V");
        }
    }

    public BytecodeExpr createAssign(ASTNode parent, final BytecodeExpr right, CompilerTransformer compiler) {
        return new BytecodeExpr(parent, ClassHelper.VOID_TYPE) {
            protected void compile() {
                object.visit(mv);
                mv.visitLdcInsn(propName);
                right.visit(mv);
                mv.visitMethodInsn(INVOKESTATIC, "org/codehaus/groovy/runtime/InvokerHelper", "setProperty", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V");
            }
        };
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
