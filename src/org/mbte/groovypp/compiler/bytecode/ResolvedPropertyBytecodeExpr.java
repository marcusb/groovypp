package org.mbte.groovypp.compiler.bytecode;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.classgen.Verifier;
import org.codehaus.groovy.syntax.Token;
import org.mbte.groovypp.compiler.CompilerTransformer;

public class ResolvedPropertyBytecodeExpr extends ResolvedLeftExpr {
    private final PropertyNode propertyNode;
    private final BytecodeExpr object;
    private final String methodName;
    private final BytecodeExpr bargs;
    private final boolean needsObjectIfStatic;

    public ResolvedPropertyBytecodeExpr(ASTNode parent, PropertyNode propertyNode, BytecodeExpr object, BytecodeExpr bargs, boolean needsObjectIfStatic) {
        super(parent, propertyNode.getType());
        this.propertyNode = propertyNode;
        this.object = object;
        this.bargs = bargs;
        this.needsObjectIfStatic = needsObjectIfStatic;

        if (bargs != null) {
            methodName = "set" + Verifier.capitalize(propertyNode.getName());
        } else {
            methodName = "get" + Verifier.capitalize(propertyNode.getName());
        }
    }

    public void compile() {
        final String classInternalName;
        final String methodDescriptor;

        int op = INVOKEVIRTUAL;

        if (propertyNode.getDeclaringClass().isInterface())
            op = INVOKEINTERFACE;

        if (propertyNode.isStatic())
            op = INVOKESTATIC;

        if (object != null && !(propertyNode.isStatic() && !needsObjectIfStatic)) {
            object.visit(mv);
            box(object.getType());
        }

        if (op == INVOKESTATIC && object != null && needsObjectIfStatic) {
            if (ClassHelper.long_TYPE == object.getType() || ClassHelper.double_TYPE == object.getType())
                mv.visitInsn(POP2);
            else
                mv.visitInsn(POP);
        }
        classInternalName = BytecodeHelper.getClassInternalName(propertyNode.getDeclaringClass());

        if (methodName.startsWith("set")) {
            bargs.visit(mv);
            final ClassNode paramType = propertyNode.getType();
            final ClassNode type = bargs.getType();
            box(type);
            bargs.cast(ClassHelper.getWrapper(type), ClassHelper.getWrapper(paramType));
            bargs.unbox(paramType);
            if (object != null)
                dup_x1(paramType);
            else
                dup(paramType);
            methodDescriptor = BytecodeHelper.getMethodDescriptor(ClassHelper.VOID_TYPE, new Parameter[]{new Parameter(paramType, "")});
            mv.visitMethodInsn(op, classInternalName, methodName, methodDescriptor);
        } else {
            methodDescriptor = BytecodeHelper.getMethodDescriptor(propertyNode.getType(), Parameter.EMPTY_ARRAY);
            mv.visitMethodInsn(op, classInternalName, methodName, methodDescriptor);
        }
    }

    public BytecodeExpr createAssign(ASTNode parent, final BytecodeExpr right, CompilerTransformer compiler) {
        return new ResolvedPropertyBytecodeExpr(parent, propertyNode, object, right, needsObjectIfStatic);
    }

    public BytecodeExpr createBinopAssign(ASTNode parent, Token method, BytecodeExpr right, CompilerTransformer compiler) {
        final BytecodeExpr fakeObject = new BytecodeExpr(object, object.getType()) {
            @Override
            protected void compile() {
            }
        };

        final BytecodeExpr dupObject = new BytecodeExpr(object, object.getType()) {
            @Override
            protected void compile() {
                object.visit(mv);
                dup(object.getType());
            }
        };

        BytecodeExpr get = new ResolvedPropertyBytecodeExpr(
                parent,
                propertyNode,
                dupObject,
                null,
                needsObjectIfStatic);

        final BinaryExpression op = new BinaryExpression(get, method, right);
        op.setSourcePosition(parent);
        final BytecodeExpr transformedOp = (BytecodeExpr) compiler.transform(op);

        return new ResolvedPropertyBytecodeExpr(
                parent,
                propertyNode,
                fakeObject,
                transformedOp,
                needsObjectIfStatic);
    }

    public BytecodeExpr createPrefixOp(ASTNode parent, int type, CompilerTransformer compiler) {
        return null;
    }

    public BytecodeExpr createPostfixOp(ASTNode parent, int type, CompilerTransformer compiler) {
        return null;
    }
}