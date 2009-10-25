package org.mbte.groovypp.compiler.bytecode;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.syntax.Token;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;
import org.objectweb.asm.MethodVisitor;

public class ResolvedArrayLikeBytecodeExpr extends ResolvedLeftExpr {
    private final BytecodeExpr array;
    private final BytecodeExpr index;
    private MethodNode getter;
    private final BytecodeExpr getterExpr;
    private final MethodNode setter;

    public ResolvedArrayLikeBytecodeExpr(ASTNode parent, BytecodeExpr array, BytecodeExpr index, MethodNode getter, MethodNode setter, CompilerTransformer compiler) {
        super(parent, getter.getReturnType());
        this.array = array;
        this.index = index;
        this.getter = getter;
        this.getterExpr = new ResolvedMethodBytecodeExpr(parent, getter, array, new ArgumentListExpression(index), compiler);
        setType(getterExpr.getType());
        this.setter = setter;
    }

    protected void compile(MethodVisitor mv) {
        getterExpr.visit(mv);
    }

    public BytecodeExpr createAssign(ASTNode parent, BytecodeExpr right, CompilerTransformer compiler) {
        if (setter == null) {
            compiler.addError("Can't find method 'putAt' for type: " + getType().getName(), parent);
            return null;
        }

        return new ResolvedMethodBytecodeExpr(parent, setter, array, new ArgumentListExpression(index, right), compiler);
    }

    public BytecodeExpr createBinopAssign(ASTNode parent, Token method, final BytecodeExpr right, CompilerTransformer compiler) {
        final BytecodeExpr loadArr = new BytecodeExpr(this, array.getType()) {
            @Override
            protected void compile(MethodVisitor mv) {
            }
        };

        final BytecodeExpr loadIndex = new BytecodeExpr(this, ClassHelper.int_TYPE) {
            @Override
            protected void compile(MethodVisitor mv) {
            }
        };

        ResolvedMethodBytecodeExpr load  = new ResolvedMethodBytecodeExpr(parent, getter, loadArr, new ArgumentListExpression(loadIndex), compiler);

        final BinaryExpression op = new BinaryExpression(load, method, right);
        op.setSourcePosition(parent);
        final BytecodeExpr transformedOp = compiler.cast((BytecodeExpr) compiler.transform(op), getType());

        final BytecodeExpr result = new BytecodeExpr(this, TypeUtil.wrapSafely(transformedOp.getType())) {
            @Override
            protected void compile(MethodVisitor mv) {
            }
        };

        final ResolvedMethodBytecodeExpr store = new ResolvedMethodBytecodeExpr(parent, setter, loadArr, new ArgumentListExpression(loadIndex, result), compiler);

        return new BytecodeExpr(parent, getType()) {
            @Override
            protected void compile(MethodVisitor mv) {
                array.visit(mv);
                index.visit(mv);
                mv.visitInsn(DUP2);
                transformedOp.visit(mv);
                box(transformedOp.getType(), mv);
                dup_x2(getType(), mv);
                store.visit(mv);
                if (!setter.getReturnType().equals(ClassHelper.VOID_TYPE)) {
                    pop(setter.getReturnType(), mv);
                }
            }
        };
    }

    public BytecodeExpr createPrefixOp(ASTNode parent, int type, CompilerTransformer compiler) {
        return null;
    }

    public BytecodeExpr createPostfixOp(ASTNode parent, int type, CompilerTransformer compiler) {
        return null;
    }
}
