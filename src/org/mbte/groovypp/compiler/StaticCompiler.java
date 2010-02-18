package org.mbte.groovypp.compiler;

import groovy.lang.EmptyRange;
import groovy.lang.TypePolicy;
import org.codehaus.groovy.ast.*;
import static org.codehaus.groovy.ast.ClassHelper.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.*;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.classgen.BytecodeInstruction;
import org.codehaus.groovy.classgen.BytecodeSequence;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;
import org.codehaus.groovy.syntax.Types;
import org.codehaus.groovy.syntax.Token;
import org.mbte.groovypp.compiler.bytecode.*;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class StaticCompiler extends CompilerTransformer implements Opcodes {
    private StaticMethodBytecode methodBytecode;

    // exception blocks list
    private List<Runnable> exceptionBlocks = new ArrayList<Runnable>();

    final boolean shouldImproveReturnType;

    ClassNode calculatedReturnType = TypeUtil.NULL_TYPE;
    private Label startLabel = new Label ();

    public StaticCompiler(SourceUnit su, SourceUnitContext context, StaticMethodBytecode methodBytecode, MethodVisitor mv, CompilerStack compileStack, int debug, TypePolicy policy, String baseClosureName) {
        super(su, methodBytecode.methodNode.getDeclaringClass(), methodBytecode.methodNode, mv, compileStack, debug, policy, baseClosureName, context);
        this.methodBytecode = methodBytecode;
        shouldImproveReturnType = methodNode.getName().equals("doCall");

        mv.visitLabel(startLabel);
        if (methodNode instanceof ConstructorNode && !((ConstructorNode) methodNode).firstStatementIsSpecialConstructorCall()) {
            // invokes the super class constructor
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, BytecodeHelper.getClassInternalName(classNode.getSuperClass()), "<init>", "()V");
        }
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

    private int lastLine = -1;

    @Override
    protected void visitStatement(Statement statement) {
        super.visitStatement(statement);

        int line = statement.getLineNumber();
        if (line >= 0 && mv != null && line != lastLine) {
            Label l = new Label();
            mv.visitLabel(l);
            mv.visitLineNumber(line, l);
            lastLine = line;
        }
    }

    @Override
    public void visitAssertStatement(AssertStatement statement) {
        visitStatement(statement);
        Label noError = new Label();

        BytecodeExpr condition = transformLogical(statement.getBooleanExpression().getExpression(), noError, true);
        BytecodeExpr msgExpr = (BytecodeExpr) transform(statement.getMessageExpression());

        condition.visit(mv);
        mv.visitTypeInsn(NEW, "java/lang/AssertionError");
        mv.visitInsn(DUP);
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
            be.unbox(ClassHelper.boolean_TYPE, mv);
        } else {
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
            } else {

                mv.visitMethodInsn(INVOKESTATIC, DTT, "castToBoolean", "(Ljava/lang/Object;)Z");
            }
        }
        mv.visitJumpInsn(op, label);
    }

    @Override
    public void visitBlockStatement(BlockStatement block) {
        compileStack.pushVariableScope(block.getVariableScope());
        for (Statement statement : block.getStatements() ) {
            if (statement instanceof BytecodeSequence)
                visitBytecodeSequence((BytecodeSequence) statement);
            else
                statement.visit(this);
        }
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
        visitStatement(statement);

        super.visitExpressionStatement(statement);

        final BytecodeExpr be = transformSynthetic((BytecodeExpr) statement.getExpression());
        be.visit(mv);
        final ClassNode type = be.getType();
        if (type != ClassHelper.VOID_TYPE && type != ClassHelper.void_WRAPPER_TYPE) {
            be.pop(type, mv);
        }
    }

    private void visitForLoopWithIterator(ForStatement forLoop, BytecodeExpr collectionExpression) {
        compileStack.pushLoop(forLoop.getVariableScope(), forLoop.getStatementLabel());

        Label continueLabel = compileStack.getContinueLabel();
        Label breakLabel = compileStack.getBreakLabel();
        MethodCallExpression iterator = new MethodCallExpression(
                collectionExpression, "iterator", new ArgumentListExpression());
        BytecodeExpr expr = (BytecodeExpr) transform(iterator);
        expr.visit(mv);

        ClassNode etype =  ClassHelper.OBJECT_TYPE;
        ClassNode iteratorType = expr.getType();
        GenericsType[] generics = iteratorType.getGenericsTypes();
        if (generics != null && generics.length == 1) {
            if (!TypeUtil.isSuper(generics[0])) {
                etype = generics[0].getType();
            }
        }
        if (forLoop.getVariable().isDynamicTyped())
            forLoop.getVariable().setType(etype);
        else
            etype = forLoop.getVariable().getType();

        Register variable = compileStack.defineVariable(forLoop.getVariable(), false);

        final int iteratorIdx = compileStack.defineTemporaryVariable(
                "iterator", ClassHelper.make(Iterator.class), true);

        mv.visitLabel(continueLabel);
        mv.visitVarInsn(ALOAD, iteratorIdx);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z");
        mv.visitJumpInsn(IFEQ, breakLabel);

        mv.visitVarInsn(ALOAD, iteratorIdx);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;");
        if (ClassHelper.isPrimitiveType(etype)) {
            BytecodeExpr.unbox(etype, mv);
        } else {
            BytecodeExpr.cast(ClassHelper.OBJECT_TYPE, etype, mv);
        }
        BytecodeExpr.store(etype, variable.getIndex(), mv);

        forLoop.getLoopBlock().visit(this);

        mv.visitJumpInsn(GOTO, continueLabel);
        mv.visitLabel(breakLabel);
        compileStack.pop();
    }

    private void visitForLoopWithClosures(ForStatement forLoop) {

        compileStack.pushLoop(forLoop.getVariableScope(), forLoop.getStatementLabel());

        ClosureListExpression closureExpression = (ClosureListExpression) forLoop.getCollectionExpression();
        compileStack.pushVariableScope(closureExpression.getVariableScope());

        Label continueLabel = compileStack.getContinueLabel();
        Label breakLabel = compileStack.getBreakLabel();
        List<Expression> loopExpr = closureExpression.getExpressions();

        if (!(loopExpr.get(0) instanceof EmptyExpression)) {
            final BytecodeExpr initExpression = (BytecodeExpr) transform(loopExpr.get(0));
            initExpression.visit(mv);
            initExpression.pop(initExpression.getType(), mv);
        }

        Label cond = new Label();
        mv.visitLabel(cond);

        if (!(loopExpr.get(1) instanceof EmptyExpression)) {
            final BytecodeExpr binaryExpression = transformLogical(loopExpr.get(1), breakLabel, false);
            binaryExpression.visit(mv);
        }

        forLoop.getLoopBlock().visit(this);

        mv.visitLabel(continueLabel);

        if (!(loopExpr.get(2) instanceof EmptyExpression)) {
            final BytecodeExpr incrementExpression = (BytecodeExpr) transform(loopExpr.get(2));

            incrementExpression.visit(mv);
            final ClassNode type = incrementExpression.getType();
            if (type != ClassHelper.VOID_TYPE && type != ClassHelper.void_WRAPPER_TYPE) {
                if (type == ClassHelper.long_TYPE || type == ClassHelper.double_TYPE) {
                    mv.visitInsn(POP2);
                } else {
                    mv.visitInsn(POP);
                }
            }
        }

        mv.visitJumpInsn(GOTO, cond);
        mv.visitLabel(breakLabel);

        compileStack.pop();
        compileStack.pop();
    }

    @Override
    public void visitForLoop(ForStatement forLoop) {
        visitStatement(forLoop);
        Parameter loopVar = forLoop.getVariable();
        if (loopVar == ForStatement.FOR_LOOP_DUMMY) {
            visitForLoopWithClosures(forLoop);
        } else {
            BytecodeExpr collectionExpression = (BytecodeExpr) transform(forLoop.getCollectionExpression());
            if (collectionExpression.getType().isArray()) {
                visitForLoopWithArray(forLoop, collectionExpression);
            } else if (forLoop.getCollectionExpression() instanceof RangeExpression &&
                    TypeUtil.equal(TypeUtil.RANGE_OF_INTEGERS_TYPE, collectionExpression.getType())
                    && (forLoop.getVariable().isDynamicTyped() ||
                        forLoop.getVariable().getType().equals(ClassHelper.int_TYPE))) {
                // This is the IntRange (or EmptyRange). Iterate with index.
                visitForLoopWithIntRange(forLoop, collectionExpression);
            } else {
                visitForLoopWithIterator(forLoop, collectionExpression);
            }
        }
    }

    private void visitForLoopWithArray(ForStatement forLoop, BytecodeExpr coll) {
        compileStack.pushLoop(forLoop.getVariableScope(), forLoop.getStatementLabel());
        ClassNode type = coll.getType().getComponentType();
        forLoop.getVariable().setType(type);

        Label breakLabel = compileStack.getBreakLabel();
        Label continueLabel = compileStack.getContinueLabel();

        coll.visit(mv);
        int array = compileStack.defineTemporaryVariable("$array$", ClassHelper.OBJECT_TYPE, true);
        mv.visitInsn(ICONST_0);
        int idx = compileStack.defineTemporaryVariable("$idx$", ClassHelper.int_TYPE, true);

        mv.visitLabel(continueLabel);
        mv.visitVarInsn(ILOAD, idx);
        mv.visitVarInsn(ALOAD, array);
        mv.visitInsn(ARRAYLENGTH);
        mv.visitJumpInsn(IF_ICMPGE, breakLabel);

        mv.visitVarInsn(ALOAD, array);
        mv.visitVarInsn(ILOAD, idx);
        if (ClassHelper.isPrimitiveType(type)) {
                if (type == ClassHelper.long_TYPE)
                    mv.visitInsn(LALOAD);
                else
                if (type == ClassHelper.float_TYPE)
                    mv.visitInsn(FALOAD);
                else
                if (type == ClassHelper.double_TYPE)
                    mv.visitInsn(DALOAD);
                else
                    mv.visitInsn(IALOAD);
            }
            else
                mv.visitInsn(AALOAD);
        compileStack.defineVariable(forLoop.getVariable(), true);
        forLoop.getLoopBlock().visit(this);

        mv.visitVarInsn(ILOAD, idx);
        mv.visitInsn(ICONST_1);
        mv.visitInsn(IADD);
        mv.visitVarInsn(ISTORE, idx);

        mv.visitJumpInsn(GOTO, continueLabel);

        mv.visitLabel(breakLabel);
        compileStack.pop();
    }

    private void visitForLoopWithIntRange(ForStatement forLoop, BytecodeExpr coll) {
        compileStack.pushLoop(forLoop.getVariableScope(), forLoop.getStatementLabel());
        forLoop.getVariable().setType(ClassHelper.int_TYPE);

        Label breakLabel = compileStack.getBreakLabel();
        Label continueLabel = compileStack.getContinueLabel();

        coll.visit(mv);
        mv.visitInsn(DUP);
        int collIdx = compileStack.defineTemporaryVariable("$coll$", ClassHelper.OBJECT_TYPE, true);
        mv.visitTypeInsn(INSTANCEOF, BytecodeHelper.getClassInternalName(EmptyRange.class));
        mv.visitJumpInsn(IFNE, breakLabel);

        mv.visitVarInsn(ALOAD, collIdx);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "groovy/lang/Range", "getFrom", "()Ljava/lang/Comparable;");
        BytecodeExpr.unbox(ClassHelper.int_TYPE, mv);
        mv.visitVarInsn(ALOAD, collIdx);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "groovy/lang/Range", "getTo", "()Ljava/lang/Comparable;");
        BytecodeExpr.unbox(ClassHelper.int_TYPE, mv);

        mv.visitVarInsn(ALOAD, collIdx);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "groovy/lang/Range", "isReverse", "()Z");

        mv.visitInsn(DUP);
        int isReverse = compileStack.defineTemporaryVariable("$isReverse$", ClassHelper.boolean_TYPE, true);
        Label lElse1 = new Label();
        mv.visitJumpInsn(IFEQ, lElse1);
        mv.visitInsn(SWAP);
        mv.visitLabel(lElse1);
        int otherEnd = compileStack.defineTemporaryVariable("$otherEnd$", ClassHelper.int_TYPE, true);
        int thisEnd = compileStack.defineTemporaryVariable("$thisEnd$", ClassHelper.int_TYPE, true);
        Register it = compileStack.defineVariable(forLoop.getVariable(), false);

        mv.visitLabel(continueLabel);

        mv.visitVarInsn(ILOAD, otherEnd);
        mv.visitVarInsn(ILOAD, thisEnd);

        Label lElse2 = new Label(), lDone2 = new Label();
        mv.visitVarInsn(ILOAD, isReverse);
        mv.visitJumpInsn(IFNE, lElse2);
        mv.visitJumpInsn(IF_ICMPLT, breakLabel);
        mv.visitJumpInsn(GOTO, lDone2);
        mv.visitLabel(lElse2);
        mv.visitJumpInsn(IF_ICMPGT, breakLabel);
        mv.visitLabel(lDone2);

        mv.visitVarInsn(ILOAD, thisEnd);
        mv.visitInsn(DUP);
        mv.visitVarInsn(ISTORE, it.getIndex());

        mv.visitInsn(ICONST_1);
        mv.visitVarInsn(ILOAD, isReverse);

        Label lElse3 = new Label(), lDone3 = new Label();
        mv.visitJumpInsn(IFNE, lElse3);
        mv.visitInsn(IADD);
        mv.visitJumpInsn(GOTO, lDone3);
        mv.visitLabel(lElse3);
        mv.visitInsn(ISUB);
        mv.visitLabel(lDone3);

        mv.visitVarInsn(ISTORE, thisEnd);

        forLoop.getLoopBlock().visit(this);

        mv.visitJumpInsn(GOTO, continueLabel);

        mv.visitLabel(breakLabel);
        compileStack.pop();
    }

    @Override
    public void visitIfElse(IfStatement ifElse) {
        visitStatement(ifElse);

        final BooleanExpression ifExpr = ifElse.getBooleanExpression();

        Label elseLabel = new Label();

        final BytecodeExpr condition = transformLogical(ifExpr, elseLabel, false);
        condition.visit(mv);

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
        visitStatement(statement);

        Expression returnExpression = statement.getExpression();
        if (!shouldImproveReturnType && !methodNode.getReturnType().equals(ClassHelper.VOID_TYPE)) {
            CastExpression castExpression = new CastExpression(methodNode.getReturnType(), returnExpression);
            castExpression.setSourcePosition(returnExpression);
            returnExpression = castExpression;
        }

        BytecodeExpr bytecodeExpr = (BytecodeExpr) transformToGround(returnExpression);

        if (bytecodeExpr instanceof ResolvedMethodBytecodeExpr) {
            ResolvedMethodBytecodeExpr resolvedMethodBytecodeExpr = (ResolvedMethodBytecodeExpr) bytecodeExpr;
            if (resolvedMethodBytecodeExpr.getMethodNode() == methodNode) {
                if (methodNode.isStatic()
                || resolvedMethodBytecodeExpr.getObject().isThis()
                || methodNode.isPrivate()
                || (methodNode.getModifiers() & ACC_FINAL) != 0) {
                    tailRecursive(resolvedMethodBytecodeExpr);
                    return;
                }
            }
        }

        bytecodeExpr.visit(mv);
        ClassNode exprType = bytecodeExpr.getType();
        ClassNode returnType = methodNode.getReturnType();
        if (returnType.equals(ClassHelper.VOID_TYPE)) {
            compileStack.applyFinallyBlocks();
        } else {
            if (shouldImproveReturnType) {
                if (bytecodeExpr.getType().equals(ClassHelper.VOID_TYPE)) {
                    mv.visitInsn(ACONST_NULL);
                } else {
                    BytecodeExpr.box(exprType, mv);
                    exprType = TypeUtil.wrapSafely(exprType);
                    calculatedReturnType = TypeUtil.commonType(calculatedReturnType, exprType);
                    BytecodeExpr.cast(exprType, calculatedReturnType, mv);
                }
            }
            else {
                if (bytecodeExpr.getType().equals(ClassHelper.VOID_TYPE)) {
                    mv.visitInsn(ACONST_NULL);
                } else {
                    BytecodeExpr.box(exprType, mv);
                    BytecodeExpr.cast(TypeUtil.wrapSafely(exprType), TypeUtil.wrapSafely(returnType), mv);
                }
            }

            if (compileStack.hasFinallyBlocks()) {
                int returnValueIdx = compileStack.defineTemporaryVariable("returnValue", ClassHelper.OBJECT_TYPE, true);
                compileStack.applyFinallyBlocks();
                mv.visitVarInsn(ALOAD, returnValueIdx);
            }
            BytecodeExpr.unbox(returnType, mv);
        }

        bytecodeExpr.doReturn(returnType, mv);
    }

    private void tailRecursive(ResolvedMethodBytecodeExpr resolvedMethodBytecodeExpr) {
        Parameter[] parameters = methodNode.getParameters();

        int varIndex = methodNode.isStatic() ? 0 : 1;
        if (varIndex != 0) {
            resolvedMethodBytecodeExpr.getObject().visit(mv);
        }
        for (int i = 0; i != parameters.length; ++i) {
            BytecodeExpr be = (BytecodeExpr) resolvedMethodBytecodeExpr.getBargs().getExpressions().get(i);
            be.visit(mv);
            final ClassNode paramType = parameters[i].getType();
            final ClassNode type = be.getType();
            BytecodeExpr.box(type, mv);
            BytecodeExpr.cast(TypeUtil.wrapSafely(type), TypeUtil.wrapSafely(paramType), mv);
            BytecodeExpr.unbox(paramType, mv);

            varIndex += (paramType == ClassHelper.long_TYPE || paramType == ClassHelper.double_TYPE) ? 2 : 1;
        }

        for (int i = parameters.length-1; i >= 0; --i) {
            final ClassNode paramType = parameters[i].getType();
            varIndex -= (paramType == ClassHelper.long_TYPE || paramType == ClassHelper.double_TYPE) ? 2 : 1;

            if (paramType == double_TYPE) {
                mv.visitVarInsn(Opcodes.DSTORE, varIndex);
            } else if (paramType == float_TYPE) {
                mv.visitVarInsn(Opcodes.FSTORE, varIndex);
            } else if (paramType == long_TYPE) {
                mv.visitVarInsn(Opcodes.LSTORE, varIndex);
            } else if (
                   paramType == boolean_TYPE
                || paramType == char_TYPE
                || paramType == byte_TYPE
                || paramType == int_TYPE
                || paramType == short_TYPE) {
                mv.visitVarInsn(Opcodes.ISTORE, varIndex);
            } else {
                mv.visitVarInsn(Opcodes.ASTORE, varIndex);
            }
        }

        if (!methodNode.isStatic()) {
            mv.visitVarInsn(ASTORE, 0);
        }
        mv.visitJumpInsn(GOTO, startLabel);
        return;
    }

    @Override
    public void visitWhileLoop(WhileStatement loop) {
        visitStatement(loop);
        compileStack.pushLoop(loop.getStatementLabel());
        Label continueLabel = compileStack.getContinueLabel();
        Label breakLabel = compileStack.getBreakLabel();

        final BytecodeExpr be = transformLogical(loop.getBooleanExpression().getExpression(), breakLabel, false);

        mv.visitLabel(continueLabel);
        be.visit(mv);

        loop.getLoopBlock().visit(this);

        mv.visitJumpInsn(GOTO, continueLabel);
        mv.visitLabel(breakLabel);

        compileStack.pop();
    }

    public void visitSwitch(SwitchStatement statement) {
        visitStatement(statement);

        BytecodeExpr cond = (BytecodeExpr) transform(statement.getExpression());
        cond.visit(mv);

        // switch does not have a continue label. use its parent's for continue
        Label breakLabel = compileStack.pushSwitch();

        int switchVariableIndex = compileStack.defineTemporaryVariable("switch", cond.getType(), true);

        List caseStatements = statement.getCaseStatements();
        int caseCount = caseStatements.size();
        Label[] codeLabels = new Label[caseCount];
        Label[] condLabels = new Label[caseCount + 1];
        int i;
        for (i = 0; i < caseCount; i++) {
            codeLabels[i] = new Label();
            condLabels[i] = new Label();
        }

        Label defaultLabel = new Label();

        i = 0;
        for (Iterator iter = caseStatements.iterator(); iter.hasNext(); i++) {
            CaseStatement caseStatement = (CaseStatement) iter.next();

            mv.visitLabel(condLabels[i]);

            visitStatement(caseStatement);

            BytecodeExpr.load(cond.getType(), switchVariableIndex, mv);
            BytecodeExpr option = (BytecodeExpr) transformToGround(caseStatement.getExpression());

            if (!ClassHelper.isPrimitiveType(option.getType()) || !ClassHelper.isPrimitiveType(cond.getType())) {
                BytecodeExpr.box(cond.getType(), mv);

                option.visit(mv);
                BytecodeExpr.box(option.getType(), mv);

                Label next = i == caseCount - 1 ? defaultLabel : condLabels[i + 1];

                Label notNull = new Label();
                mv.visitInsn(DUP);
                mv.visitJumpInsn(IFNONNULL, notNull);
                mv.visitJumpInsn(IF_ACMPEQ, codeLabels[i]);
                mv.visitJumpInsn(GOTO, next);

                mv.visitLabel(notNull);

                final BytecodeExpr caseValue = new BytecodeExpr(option, TypeUtil.wrapSafely(option.getType())) {
                    protected void compile(MethodVisitor mv) {
                    }
                };

                final BytecodeExpr switchValue = new BytecodeExpr(cond, TypeUtil.wrapSafely(cond.getType())) {
                    protected void compile(MethodVisitor mv) {
                        mv.visitInsn(SWAP);
                    }
                };
                MethodCallExpression exp = new MethodCallExpression(caseValue, "isCase", new ArgumentListExpression(switchValue));
                exp.setSourcePosition(caseValue);
                transformLogical(exp, codeLabels[i], true).visit(mv);
            }
            else {
                option.visit(mv);
                final BytecodeExpr caseValue = new BytecodeExpr(option, option.getType()) {
                    protected void compile(MethodVisitor mv) {
                    }
                };

                final BytecodeExpr switchValue = new BytecodeExpr(cond, cond.getType()) {
                    protected void compile(MethodVisitor mv) {
                    }
                };
                BinaryExpression eq = new BinaryExpression(caseValue, Token.newSymbol(Types.COMPARE_EQUAL, -1, -1), switchValue);
                eq.setSourcePosition(caseValue);
                transformLogical(eq, codeLabels[i], true).visit(mv);
            }
        }

        mv.visitJumpInsn(GOTO, defaultLabel);

        i = 0;
        for (Iterator iter = caseStatements.iterator(); iter.hasNext(); i++) {
            CaseStatement caseStatement = (CaseStatement) iter.next();
            visitStatement(caseStatement);
            mv.visitLabel(codeLabels[i]);
            caseStatement.getCode().visit(this);
        }

        mv.visitLabel(defaultLabel);
        statement.getDefaultStatement().visit(this);

        mv.visitLabel(breakLabel);

        compileStack.pop();
    }

    @Override
    public void visitCaseStatement(CaseStatement statement) {
    }

    @Override
    public void visitDoWhileLoop(DoWhileStatement loop) {
        visitStatement(loop);

        super.visitDoWhileLoop(loop);
        throw new UnsupportedOperationException();
    }

    @Override
    public void visitSynchronizedStatement(SynchronizedStatement sync) {
        visitStatement(sync);

        sync.setExpression(transform(sync.getExpression()));

        ((BytecodeExpr) sync.getExpression()).visit(mv);
        final int index = compileStack.defineTemporaryVariable("synchronized", ClassHelper.OBJECT_TYPE, true);

        final Label synchronizedStart = new Label();
        final Label synchronizedEnd = new Label();
        final Label catchAll = new Label();

        mv.visitVarInsn(ALOAD, index);
        mv.visitInsn(MONITORENTER);
        mv.visitLabel(synchronizedStart);

        Runnable finallyPart = new Runnable() {
            public void run() {
                mv.visitVarInsn(ALOAD, index);
                mv.visitInsn(MONITOREXIT);
            }
        };
        compileStack.pushFinallyBlock(finallyPart);

        sync.getCode().visit(this);

        finallyPart.run();
        mv.visitJumpInsn(GOTO, synchronizedEnd);
        ((StackAwareMethodAdapter) mv).startExceptionBlock(); // exception variable
        mv.visitLabel(catchAll);
        finallyPart.run();
        mv.visitInsn(ATHROW);
        mv.visitLabel(synchronizedEnd);

        compileStack.popFinallyBlock();
        exceptionBlocks.add(new Runnable() {
            public void run() {
                mv.visitTryCatchBlock(synchronizedStart, catchAll, catchAll, null);
            }
        });
    }

    @Override
    public void visitThrowStatement(ThrowStatement ts) {
        visitStatement(ts);

        super.visitThrowStatement(ts);
        ((BytecodeExpr) ts.getExpression()).visit(mv);
        mv.visitInsn(ATHROW);
    }

    public void visitContinueStatement(ContinueStatement statement) {
        visitStatement(statement);

        String name = statement.getLabel();
        Label continueLabel = compileStack.getContinueLabel();
        if (name != null) continueLabel = compileStack.getNamedContinueLabel(name);
        compileStack.applyFinallyBlocks(continueLabel, false);
        mv.visitJumpInsn(GOTO, continueLabel);
    }

    public void visitTryCatchFinally(TryCatchStatement statement) {
        visitStatement(statement);

        Statement tryStatement = statement.getTryStatement();
        final Statement finallyStatement = statement.getFinallyStatement();

        int anyExceptionIndex = compileStack.defineTemporaryVariable("exception", DYNAMIC_TYPE, false);
        if (!finallyStatement.isEmpty()) {
            compileStack.pushFinallyBlock(
                    new Runnable() {
                        public void run() {
                            compileStack.pushFinallyBlockVisit(this);
                            finallyStatement.visit(StaticCompiler.this);
                            compileStack.popFinallyBlockVisit(this);
                        }
                    }
            );
        }

        // start try block, label needed for exception table
        final Label tryStart = new Label();
        mv.visitLabel(tryStart);
        tryStatement.visit(this);

        // goto finally part
        final Label finallyStart = new Label();
        mv.visitJumpInsn(GOTO, finallyStart);

        // marker needed for Exception table
        final Label greEnd = new Label();
        mv.visitLabel(greEnd);

        final Label tryEnd = new Label();
        mv.visitLabel(tryEnd);

        for (CatchStatement catchStatement : statement.getCatchStatements()) {
            ClassNode exceptionType = catchStatement.getExceptionType();
            // start catch block, label needed for exception table
            final Label catchStart = new Label();
            mv.visitLabel(catchStart);

            ((StackAwareMethodAdapter) mv).startExceptionBlock();

            // create exception variable and store the exception
            compileStack.pushState();
            compileStack.defineVariable(catchStatement.getVariable(), true);
            // handle catch body
            catchStatement.visit(this);
            compileStack.pop();
            // goto finally start
            mv.visitJumpInsn(GOTO, finallyStart);
            // add exception to table
            final String exceptionTypeInternalName = BytecodeHelper.getClassInternalName(exceptionType);
            exceptionBlocks.add(new Runnable() {
                public void run() {
                    mv.visitTryCatchBlock(tryStart, tryEnd, catchStart, exceptionTypeInternalName);
                }
            });
        }

        // marker needed for the exception table
        final Label endOfAllCatches = new Label();
        mv.visitLabel(endOfAllCatches);

        // remove the finally, don't let it visit itself
        if (!finallyStatement.isEmpty()) compileStack.popFinallyBlock();

        // start finally
        mv.visitLabel(finallyStart);
        finallyStatement.visit(this);
        // goto end of finally
        Label afterFinally = new Label();
        mv.visitJumpInsn(GOTO, afterFinally);

        // start a block catching any Exception
        final Label catchAny = new Label();
        mv.visitLabel(catchAny);
        ((StackAwareMethodAdapter) mv).startExceptionBlock();
        //store exception
        mv.visitVarInsn(ASTORE, anyExceptionIndex);
        finallyStatement.visit(this);
        // load the exception and rethrow it
        mv.visitVarInsn(ALOAD, anyExceptionIndex);
        mv.visitInsn(ATHROW);

        // end of all catches and finally parts
        mv.visitLabel(afterFinally);
        mv.visitInsn(NOP);

        // add catch any block to exception table
        exceptionBlocks.add(new Runnable() {
            public void run() {
                mv.visitTryCatchBlock(tryStart, endOfAllCatches, catchAny, null);
            }
        });
    }

    public void execute() {
        addReturnIfNeeded();
        compileStack.init(methodNode.getVariableScope(), methodNode.getParameters(), mv, methodNode.getDeclaringClass());
        getCode().visit(this);
        compileStack.clear();
        for (Runnable runnable : exceptionBlocks) {
            runnable.run();
        }
    }

    public void visitBytecodeSequence(BytecodeSequence sequence) {
        visitStatement(sequence);

        ((BytecodeInstruction) sequence.getInstructions().get(0)).visit(mv);
    }

    public LocalVarInferenceTypes getLocalVarInferenceTypes() {
        return ((UneededBoxingRemoverMethodAdapter) mv).getLocalVarInferenceTypes();
    }
}
