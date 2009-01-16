package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.*;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.classgen.BytecodeSequence;
import org.codehaus.groovy.classgen.BytecodeInstruction;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Label;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.mbte.groovypp.compiler.bytecode.BytecodeImproverMethodAdapter;
import org.mbte.groovypp.compiler.bytecode.LocalVarInferenceTypes;
import groovy.lang.CompilePolicy;

public class StaticCompiler extends CompilerTransformer implements Opcodes {
    private StaticMethodBytecode methodBytecode;

    public StaticCompiler(SourceUnit su, StaticMethodBytecode methodBytecode, MethodVisitor mv, CompilerStack compileStack, CompilePolicy policy) {
        super(su, methodBytecode.methodNode.getDeclaringClass(), methodBytecode.methodNode, mv, compileStack, policy);
        this.methodBytecode = methodBytecode;
    }

    protected Statement getCode() {
        return methodBytecode.code;
    }

    protected void setCode(Statement statement) {
        methodBytecode.code = statement;
    }

    protected SourceUnit getSourceUnit() {
        return methodBytecode.su;
    }

    @Override
    protected void visitStatement(Statement statement) {
        super.visitStatement(statement);
    }

    @Override
    public void visitAssertStatement(AssertStatement statement) {
        super.visitAssertStatement(statement);

        final BytecodeExpr be = (BytecodeExpr) statement.getBooleanExpression().getExpression();
        be.visit(mv);
        Label noError = new Label();
        branch(be, IFNE, noError, mv);
        mv.visitTypeInsn(NEW, "java/lang/AssertionError");
        mv.visitInsn(DUP);
        final BytecodeExpr msgExpr = (BytecodeExpr) statement.getMessageExpression();
        if (msgExpr != null)
          msgExpr.visit(mv);
        else
          mv.visitLdcInsn("<no message>");
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/AssertionError", "<init>", "(Ljava/lang/Object;)V");
        mv.visitInsn(ATHROW);
        mv.visitLabel(noError);
    }

    private static final String DTT = BytecodeHelper.getClassInternalName(DefaultTypeTransformation.class.getName());

    public static void branch(BytecodeExpr be, int op, Label label, MethodVisitor mv) {
        // type non-primitive
        final ClassNode type = be.getType();

        if (type == ClassHelper.Boolean_TYPE) {
            be.unbox(ClassHelper.boolean_TYPE);
        }
        else {
            if (ClassHelper.isPrimitiveType(type)) {
                // unwrapper - primitive
                if (type == ClassHelper.byte_TYPE
                 || type == ClassHelper.short_TYPE
                 || type == ClassHelper.char_TYPE
                 || type == ClassHelper.int_TYPE) {
                } else if (type == ClassHelper.long_TYPE) {
                    mv.visitInsn(L2I);
                } else if (type == ClassHelper.float_TYPE) {
                    mv.visitInsn(F2I);
                } else if (type == ClassHelper.double_TYPE) {
                    mv.visitInsn(D2I);
                }
            }
            else {
                mv.visitMethodInsn(INVOKESTATIC, DTT, "castToBoolean", "(Ljava/lang/Object;)Z");
            }
        }
        mv.visitJumpInsn(op, label);
    }

    @Override
    public void visitBlockStatement(BlockStatement block) {
        compileStack.pushVariableScope(block.getVariableScope());
        super.visitBlockStatement(block);
        compileStack.pop();
    }

    @Override
    public void visitBreakStatement(BreakStatement statement) {
        visitStatement(statement);

        String name = statement.getLabel();
        Label breakLabel = compileStack.getNamedBreakLabel(name);
        compileStack.applyFinallyBlocks(breakLabel, true);

        mv.visitJumpInsn(GOTO, breakLabel);
    }

    @Override
    public void visitExpressionStatement(ExpressionStatement statement) {
        super.visitExpressionStatement(statement);

        final BytecodeExpr be = (BytecodeExpr) statement.getExpression();
        be.visit(mv);
        final ClassNode type = be.getType();
        if (type != ClassHelper.VOID_TYPE && type != ClassHelper.void_WRAPPER_TYPE) {
            if (type == ClassHelper.long_TYPE || type == ClassHelper.double_TYPE) {
                mv.visitInsn(POP2);
            }
            else {
                mv.visitInsn(POP);
            }
        }
    }

    @Override
    public void visitForLoop(ForStatement forLoop) {
        visitStatement(forLoop);
        throw new UnsupportedOperationException();
    }

    @Override
    public void visitIfElse(IfStatement ifElse) {
        final BooleanExpression ifExpr = ifElse.getBooleanExpression();
        final BytecodeExpr be = (BytecodeExpr) transform(ifExpr.getExpression());
        be.visit(mv);

        Label elseLabel = new Label();
        branch(be, IFEQ, elseLabel, mv);

        compileStack.pushBooleanExpression();
        ifElse.getIfBlock().visit(this);
        compileStack.pop();

        Label endLabel = new Label();
        if (ifElse.getElseBlock() != EmptyStatement.INSTANCE) {
            mv.visitJumpInsn(GOTO, endLabel);
        }

        mv.visitLabel(elseLabel);

        if (ifElse.getElseBlock() != EmptyStatement.INSTANCE) {
            compileStack.pushBooleanExpression();
            ifElse.getElseBlock().visit(this);
            compileStack.pop();

            mv.visitLabel(endLabel);
        }
    }

    @Override
    public void visitReturnStatement(ReturnStatement statement) {
        super.visitReturnStatement(statement);

        final BytecodeExpr bytecodeExpr = (BytecodeExpr) statement.getExpression();
        bytecodeExpr.visit(mv);
        final ClassNode exprType = bytecodeExpr.getType();
        final ClassNode returnType = methodNode.getReturnType();
        if (exprType.equals(ClassHelper.VOID_TYPE))
            mv.visitInsn(ACONST_NULL);
        else {
            bytecodeExpr.box(exprType);
            bytecodeExpr.cast(ClassHelper.getWrapper(exprType), ClassHelper.getWrapper(returnType));
            bytecodeExpr.unbox(returnType);
        }
        bytecodeExpr.doReturn(returnType);
    }

    @Override
    public void visitWhileLoop(WhileStatement loop) {
        loop.setBooleanExpression((BooleanExpression) this.transform(loop.getBooleanExpression()));

        compileStack.pushLoop(loop.getStatementLabel());
        Label continueLabel = compileStack.getContinueLabel();
        Label breakLabel = compileStack.getBreakLabel();

        mv.visitLabel(continueLabel);
        final BytecodeExpr be = (BytecodeExpr) loop.getBooleanExpression().getExpression();
        be.visit(mv);
        branch(be,IFEQ, breakLabel,mv);

        loop.getLoopBlock().visit(this);

        mv.visitJumpInsn(GOTO, continueLabel);
        mv.visitLabel(breakLabel);

        compileStack.pop();
    }

    @Override
    public void visitCaseStatement(CaseStatement statement) {
        super.visitCaseStatement(statement);
        throw new UnsupportedOperationException();
    }

    @Override
    public void visitDoWhileLoop(DoWhileStatement loop) {
        super.visitDoWhileLoop(loop);
        throw new UnsupportedOperationException();
    }

    @Override
    public void visitSynchronizedStatement(SynchronizedStatement sync) {
        super.visitSynchronizedStatement(sync);
        throw new UnsupportedOperationException();
    }

    @Override
    public void visitThrowStatement(ThrowStatement ts) {
        super.visitThrowStatement(ts);
        throw new UnsupportedOperationException();
    }

    public void execute() {
        addReturnIfNeeded();
        compileStack.init(methodNode.getVariableScope(), methodNode.getParameters(), mv, methodNode.getDeclaringClass());
        getCode().visit(this);
        compileStack.clear();
    }

    public void visitBytecodeSequence(BytecodeSequence sequence) {
        visitStatement(sequence);
        ((BytecodeInstruction)sequence.getInstructions().get(0)).visit(mv);
    }

    public LocalVarInferenceTypes getLocalVarInferenceTypes() {
        return ((BytecodeImproverMethodAdapter)mv).getLocalVarInferenceTypes();
    }
}
