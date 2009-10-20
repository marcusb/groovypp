package org.mbte.groovypp.compiler.bytecode;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.syntax.Token;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.objectweb.asm.MethodVisitor;

public class ResolvedGetterBytecodeExpr extends ResolvedLeftExpr {
    private final MethodNode methodNode;
    private final BytecodeExpr object;
    private final boolean needsObjectIfStatic;
    private final BytecodeExpr getter;

    public ResolvedGetterBytecodeExpr(ASTNode parent, MethodNode methodNode, BytecodeExpr object, boolean needsObjectIfStatic, CompilerTransformer compiler) {
        super(parent, methodNode.getReturnType());
        this.methodNode = methodNode;
        this.object = object;
        this.needsObjectIfStatic = needsObjectIfStatic;
        getter = new ResolvedMethodBytecodeExpr(
                parent,
                methodNode,
                methodNode.isStatic() && !needsObjectIfStatic ? null : object,
                new ArgumentListExpression(), compiler);
    }

    protected void compile(MethodVisitor mv) {
        getter.visit(mv);
    }

    public BytecodeExpr createAssign(ASTNode parent, BytecodeExpr right, CompilerTransformer compiler) {
        String name = methodNode.getName().substring(3);
        name = name.substring(0, 1).toLowerCase() + name.substring(1);
        Object prop = PropertyUtil.resolveSetProperty(object.getType(), name, right.getType(), compiler);
        return PropertyUtil.createSetProperty(parent, compiler, name, object, right, prop, needsObjectIfStatic);
    }

    public BytecodeExpr createBinopAssign(ASTNode parent, Token method, BytecodeExpr right, CompilerTransformer compiler) {
        String name = methodNode.getName().substring(3);
        name = name.substring(0, 1).toLowerCase() + name.substring(1);

        final BytecodeExpr fakeObject = new BytecodeExpr(object, object.getType()) {
            @Override
            protected void compile(MethodVisitor mv) {
            }
        };

        BytecodeExpr get = new ResolvedMethodBytecodeExpr(
                parent,
                methodNode,
                methodNode.isStatic() && !needsObjectIfStatic ? null : fakeObject,
                new ArgumentListExpression(), compiler);

        final BinaryExpression op = new BinaryExpression(get, method, right);
        op.setSourcePosition(parent);
        final BytecodeExpr transformedOp = (BytecodeExpr) compiler.transform(op);

        Object prop = PropertyUtil.resolveSetProperty(object.getType(), name, transformedOp.getType(), compiler);
        return PropertyUtil.createSetProperty(parent, compiler, name, fakeObject, transformedOp, prop, needsObjectIfStatic);
    }

    public BytecodeExpr createPrefixOp(ASTNode parent, int type, CompilerTransformer compiler) {
        return null;
    }

    public BytecodeExpr createPostfixOp(ASTNode parent, int type, CompilerTransformer compiler) {
        return null;
    }
}