package org.mbte.groovypp.compiler.bytecode;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.mbte.groovypp.compiler.CompilerTransformer;

public class ResolvedFieldBytecodeExpr extends ResolvedLeftExpr {
    private final FieldNode fieldNode;
    private final BytecodeExpr object;
    private final BytecodeExpr value;

    public ResolvedFieldBytecodeExpr(ASTNode parent, FieldNode fieldNode, BytecodeExpr object, BytecodeExpr value) {
        super (parent, fieldNode.getType());
        this.fieldNode = fieldNode;
        this.object = object;
        this.value = value;
    }

    public void compile() {
        int op;
        if (object != null) {
            object.visit(mv);
            if (fieldNode.isStatic()) {
                pop(object.getType());
            }
            else {
                object.box(object.getType());
            }
        }

        if (value == null) {
            op = fieldNode.isStatic() ? GETSTATIC : GETFIELD;
        }
        else {
            op = fieldNode.isStatic() ? PUTSTATIC : PUTFIELD;
            value.visit(mv);

            if (object != null)
                dup_x1(value.getType());
            else
                dup(value.getType());

            box (value.getType());
            cast(ClassHelper.getWrapper(value.getType()), ClassHelper.getWrapper(fieldNode.getType()));
            unbox(fieldNode.getType());
        }
        mv.visitFieldInsn(op, BytecodeHelper.getClassInternalName(fieldNode.getDeclaringClass()), fieldNode.getName(), BytecodeHelper.getTypeDescription(fieldNode.getType()));
    }

    public BytecodeExpr createAssign(ASTNode parent, BytecodeExpr right, CompilerTransformer compiler) {
        return new ResolvedFieldBytecodeExpr(parent, fieldNode, object, right);
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