package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.mbte.groovypp.compiler.bytecode.ResolvedLeftExpr;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class BinaryExpressionTransformer extends ExprTransformer<BinaryExpression> {
    private static final Token INTDIV = Token.newSymbol(Types.INTDIV, -1, -1);
    private static final Token DIVIDE = Token.newSymbol(Types.DIVIDE, -1, -1);
    private static final Token RIGHT_SHIFT_UNSIGNED = Token.newSymbol(Types.RIGHT_SHIFT_UNSIGNED, -1, -1);
    private static final Token RIGHT_SHIFT = Token.newSymbol(Types.RIGHT_SHIFT, -1, -1);
    private static final Token LEFT_SHIFT = Token.newSymbol(Types.LEFT_SHIFT, -1, -1);
    private static final Token POWER = Token.newSymbol(Types.POWER, -1, -1);
    private static final Token MOD = Token.newSymbol(Types.MOD, -1, -1);
    private static final Token MULTIPLY = Token.newSymbol(Types.MULTIPLY, -1, -1);
    private static final Token BITWISE_XOR = Token.newSymbol(Types.BITWISE_XOR, -1, -1);
    private static final Token BITWISE_OR = Token.newSymbol(Types.BITWISE_OR, -1, -1);
    private static final Token BITWISE_AND = Token.newSymbol(Types.BITWISE_AND, -1, -1);
    private static final Token MINUS = Token.newSymbol(Types.MINUS, -1, -1);
    private static final Token PLUS = Token.newSymbol(Types.PLUS, -1, -1);

    public Expression transform(BinaryExpression exp, CompilerTransformer compiler) {
        switch (exp.getOperation().getType()) {
            case Types.COMPARE_EQUAL:
            case Types.COMPARE_NOT_EQUAL:
            case Types.LOGICAL_AND:
            case Types.LOGICAL_OR:
            case Types.KEYWORD_INSTANCEOF:
            case Types.COMPARE_IDENTICAL: // ===
            case Types.COMPARE_GREATER_THAN:
            case Types.COMPARE_GREATER_THAN_EQUAL:
            case Types.COMPARE_LESS_THAN:
            case Types.COMPARE_LESS_THAN_EQUAL:
                return new Logical(exp, compiler);

            case Types.EQUAL:
                return evaluateAssign(exp, compiler);

            case Types.LEFT_SQUARE_BRACKET:
                return evaluateArraySubscript(exp, compiler);

            case Types.MULTIPLY:
                return evaluateMathOperation(exp, "multiply", compiler);

            case Types.DIVIDE:
                return evaluateMathOperation(exp, "div", compiler);

            case Types.MINUS:
                return evaluateMathOperation(exp, "minus", compiler);

            case Types.PLUS:
                return evaluateMathOperation(exp, "plus", compiler);

            case Types.BITWISE_XOR:
                return evaluateMathOperation(exp, "xor", compiler);

            case Types.BITWISE_AND:
                return evaluateMathOperation(exp, "and", compiler);

            case Types.INTDIV:
                return evaluateMathOperation(exp, "intdiv", compiler);

            case Types.LEFT_SHIFT:
                return evaluateMathOperation(exp, "leftShift", compiler);

            case Types.RIGHT_SHIFT:
                return evaluateMathOperation(exp, "rightShift", compiler);

            case Types.RIGHT_SHIFT_UNSIGNED:
                return evaluateMathOperation(exp, "rightShiftUnsigned", compiler);

            case Types.MOD:
                return evaluateMathOperation(exp, "mod", compiler);

            case Types.BITWISE_OR:
                return evaluateMathOperation(exp, "or", compiler);

            case Types.POWER:
                return evaluateMathOperation(exp, "power", compiler);

            case Types.COMPARE_TO:
                return evaluateCompareTo(exp, compiler);

            case Types.PLUS_EQUAL:
                return evaluateMathOperationAssign(exp, PLUS, compiler);

            case Types.MINUS_EQUAL:
                return evaluateMathOperationAssign(exp, MINUS, compiler);

            case Types.BITWISE_AND_EQUAL:
                return evaluateMathOperationAssign(exp, BITWISE_AND, compiler);

            case Types.BITWISE_OR_EQUAL:
                return evaluateMathOperationAssign(exp, BITWISE_OR, compiler);

            case Types.BITWISE_XOR_EQUAL:
                return evaluateMathOperationAssign(exp, BITWISE_XOR, compiler);

            case Types.MULTIPLY_EQUAL:
                return evaluateMathOperationAssign(exp, MULTIPLY, compiler);

            case Types.MOD_EQUAL:
                return evaluateMathOperationAssign(exp, MOD, compiler);

            case Types.POWER_EQUAL:
                return evaluateMathOperationAssign(exp, POWER, compiler);

            case Types.LEFT_SHIFT_EQUAL:
                return evaluateMathOperationAssign(exp, LEFT_SHIFT, compiler);

            case Types.RIGHT_SHIFT_EQUAL:
                return evaluateMathOperationAssign(exp, RIGHT_SHIFT, compiler);

            case Types.RIGHT_SHIFT_UNSIGNED_EQUAL:
                return evaluateMathOperationAssign(exp, RIGHT_SHIFT_UNSIGNED, compiler);

            case Types.DIVIDE_EQUAL:
                return evaluateMathOperationAssign(exp, DIVIDE, compiler);

            case Types.INTDIV_EQUAL:
                return evaluateMathOperationAssign(exp, INTDIV, compiler);

/*
            case Types.FIND_REGEX:
                evaluateBinaryExpression(findRegexMethod, expression);
                break;

            case Types.MATCH_REGEX:
                evaluateBinaryExpression(matchRegexMethod, expression);
                break;

            case Types.KEYWORD_IN:
                evaluateBinaryExpression(isCaseMethod, expression);
                break;
*/
            default: {
                compiler.addError("Operation: " + exp.getOperation() + " not supported", exp);
                return null;
            }
        }
    }

    private Expression evaluateCompareTo(BinaryExpression exp, CompilerTransformer compiler) {
        final BytecodeExpr left = (BytecodeExpr) compiler.transform(exp.getLeftExpression());
        final BytecodeExpr right = (BytecodeExpr) compiler.transform(exp.getRightExpression());
        return new BytecodeExpr(exp, ClassHelper.Integer_TYPE) {
            protected void compile(MethodVisitor mv) {
                left.visit(mv);
                if (ClassHelper.isPrimitiveType(left.getType()))
                    box(left.getType(), mv);
                right.visit(mv);
                if (ClassHelper.isPrimitiveType(right.getType()))
                    box(right.getType(), mv);
                mv.visitMethodInsn(INVOKESTATIC, "org/codehaus/groovy/runtime/ScriptBytecodeAdapter", "compareTo", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Integer;");
            }
        };
    }

    public BytecodeExpr transformLogical(BinaryExpression exp, CompilerTransformer compiler, Label label, boolean onTrue) {
        final int op = exp.getOperation().getType();
        switch (op) {
            case Types.LOGICAL_AND:
                return evaluateLogicalAnd(exp, compiler, label, onTrue);

            case Types.LOGICAL_OR:
                return evaluateLogicalOr(exp, compiler, label, onTrue);

            case Types.KEYWORD_INSTANCEOF:
                return evaluateInstanceof(exp, compiler, label, onTrue);


            case Types.COMPARE_NOT_EQUAL:
                return evaluateCompare(exp, compiler, label, onTrue, op);

            case Types.COMPARE_EQUAL:
                return evaluateCompare(exp, compiler, label, onTrue, op);

            case Types.COMPARE_TO:
                throw new UnsupportedOperationException();

            case Types.COMPARE_IDENTICAL: // ===
                throw new UnsupportedOperationException();

            case Types.COMPARE_GREATER_THAN:
                return evaluateCompare(exp, compiler, label, onTrue, op);

            case Types.COMPARE_GREATER_THAN_EQUAL:
                return evaluateCompare(exp, compiler, label, onTrue, op);

            case Types.COMPARE_LESS_THAN:
                return evaluateCompare(exp, compiler, label, onTrue, op);

            case Types.COMPARE_LESS_THAN_EQUAL:
                return evaluateCompare(exp, compiler, label, onTrue, op);

            default: {
                return super.transformLogical(exp, compiler, label, onTrue);
            }
        }
    }

    private BytecodeExpr evaluateInstanceof(BinaryExpression be, CompilerTransformer compiler, final Label label, final boolean onTrue) {
        final BytecodeExpr l = (BytecodeExpr) compiler.transform(be.getLeftExpression());
        final ClassNode type = be.getRightExpression().getType();
        return new BytecodeExpr(be, ClassHelper.boolean_TYPE) {
            protected void compile(MethodVisitor mv) {
                l.visit(mv);
                box(l.getType(), mv);
                mv.visitTypeInsn(INSTANCEOF, BytecodeHelper.getClassInternalName(type));
                mv.visitJumpInsn(onTrue ? IFNE : IFEQ, label);
            }
        };
    }

    private Expression evaluateMathOperation(final BinaryExpression be, String method, final CompilerTransformer compiler) {
        final BytecodeExpr l = (BytecodeExpr) compiler.transform(be.getLeftExpression());
        final BytecodeExpr r = (BytecodeExpr) compiler.transform(be.getRightExpression());

        if (TypeUtil.isNumericalType(l.getType()) && TypeUtil.isNumericalType(r.getType())) {
            if (be.getOperation().getType() == Types.POWER)
                return callMethod(be, method, compiler, l, r);

            ClassNode mathType0 = TypeUtil.getMathType(l.getType(), r.getType());

            if (be.getOperation().getType() == Types.DIVIDE
                    && (
                    mathType0.equals(ClassHelper.int_TYPE) ||
                            mathType0.equals(ClassHelper.long_TYPE) ||
                            mathType0.equals(ClassHelper.BigInteger_TYPE)))
                mathType0 = ClassHelper.BigDecimal_TYPE;

            final ClassNode mathType = mathType0;

            if (mathType == ClassHelper.BigDecimal_TYPE || mathType == ClassHelper.BigInteger_TYPE)
                return callMethod(be, method, compiler, l, r);

            if (mathType != ClassHelper.int_TYPE && mathType != ClassHelper.long_TYPE) {
                switch (be.getOperation().getType()) {
                    case Types.BITWISE_XOR:
                    case Types.BITWISE_AND:
                    case Types.INTDIV:
                    case Types.LEFT_SHIFT:
                    case Types.RIGHT_SHIFT:
                    case Types.RIGHT_SHIFT_UNSIGNED:
                    case Types.BITWISE_OR:
                        return callMethod(be, method, compiler, l, r);
                }
            }

            return new BytecodeExpr(be, mathType) {
                protected void compile(MethodVisitor mv) {
                    l.visit(mv);
                    box(l.getType(), mv);
                    cast(TypeUtil.wrapSafely(l.getType()), TypeUtil.wrapSafely(mathType), mv);
                    if (ClassHelper.isPrimitiveType(mathType))
                        unbox(mathType, mv);

                    r.visit(mv);
                    box(r.getType(), mv);
                    cast(TypeUtil.wrapSafely(r.getType()), TypeUtil.wrapSafely(mathType), mv);
                    if (ClassHelper.isPrimitiveType(mathType))
                        unbox(mathType, mv);

                    compiler.mathOp(mathType, be.getOperation(), be);
                }
            };
        } else {
            return callMethod(be, method, compiler, l, r);
        }
    }

    private Expression callMethod(BinaryExpression be, String method, CompilerTransformer compiler, BytecodeExpr l, BytecodeExpr r) {
        final MethodCallExpression mce = new MethodCallExpression(l, method, new ArgumentListExpression(r));
        mce.setSourcePosition(be);
        return compiler.transform(mce);
    }

    private Expression evaluateAssign(BinaryExpression be, CompilerTransformer compiler) {
        Expression left = compiler.transform(be.getLeftExpression());

        if (!(left instanceof ResolvedLeftExpr)) {
            compiler.addError("Assignment operator is applicable only to variable or property or array element", be);
            return null;
        }

        return ((ResolvedLeftExpr) left).createAssign(be, (BytecodeExpr) compiler.transform(be.getRightExpression()), compiler);
    }

    private Expression evaluateMathOperationAssign(BinaryExpression be, Token method, CompilerTransformer compiler) {
        Expression left = compiler.transform(be.getLeftExpression());

        if (!(left instanceof ResolvedLeftExpr)) {
            compiler.addError("Assignment operator is applicable only to variable or property or array element", be);
            return null;
        }

        return ((ResolvedLeftExpr) left).createBinopAssign(be, method, (BytecodeExpr) compiler.transform(be.getRightExpression()), compiler);
    }

    private Expression evaluateArraySubscript(final BinaryExpression bin, CompilerTransformer compiler) {
        final BytecodeExpr arrExp = (BytecodeExpr) compiler.transform(bin.getLeftExpression());
        final BytecodeExpr indexExp = (BytecodeExpr) compiler.transform(bin.getRightExpression());

        return arrExp.createIndexed(bin, indexExp, compiler);
    }

    private BytecodeExpr evaluateLogicalOr(final BinaryExpression exp, CompilerTransformer compiler, Label label, boolean onTrue) {
        if (onTrue) {
            final BytecodeExpr l = compiler.transformLogical(exp.getLeftExpression(), label, true);
            final BytecodeExpr r = compiler.transformLogical(exp.getRightExpression(), label, true);
            return new BytecodeExpr(exp, ClassHelper.VOID_TYPE) {
                protected void compile(MethodVisitor mv) {
                    l.visit(mv);
                    r.visit(mv);
                }
            };
        } else {
            final Label _true = new Label();
            final BytecodeExpr l = compiler.transformLogical(exp.getLeftExpression(), _true, true);
            final BytecodeExpr r = compiler.transformLogical(exp.getRightExpression(), label, false);
            return new BytecodeExpr(exp, ClassHelper.VOID_TYPE) {
                protected void compile(MethodVisitor mv) {
                    l.visit(mv);
                    r.visit(mv);
                    mv.visitLabel(_true);
                }
            };
        }
    }

    private BytecodeExpr evaluateLogicalAnd(final BinaryExpression exp, CompilerTransformer compiler, Label label, boolean onTrue) {
        if (onTrue) {
            final Label _false = new Label();
            final BytecodeExpr l = compiler.transformLogical(exp.getLeftExpression(), _false, false);
            final BytecodeExpr r = compiler.transformLogical(exp.getRightExpression(), label, true);
            return new BytecodeExpr(exp, ClassHelper.VOID_TYPE) {
                protected void compile(MethodVisitor mv) {
                    l.visit(mv);
                    r.visit(mv);
                    mv.visitLabel(_false);
                }
            };
        } else {
            final BytecodeExpr l = compiler.transformLogical(exp.getLeftExpression(), label, false);
            final BytecodeExpr r = compiler.transformLogical(exp.getRightExpression(), label, false);
            return new BytecodeExpr(exp, ClassHelper.VOID_TYPE) {
                protected void compile(MethodVisitor mv) {
                    l.visit(mv);
                    r.visit(mv);
                }
            };
        }
    }

    private void intCmp(int op, boolean onTrue, MethodVisitor mv, Label label) {
        switch (op) {
            case Types.COMPARE_NOT_EQUAL:
                mv.visitJumpInsn(onTrue ? IFNE : IFEQ, label);
                break;

            case Types.COMPARE_EQUAL:
                mv.visitJumpInsn(onTrue ? IFEQ : IFNE, label);
                break;

            case Types.COMPARE_LESS_THAN:
                mv.visitJumpInsn(onTrue ? IFLT : IFGE, label);
                break;

            case Types.COMPARE_LESS_THAN_EQUAL:
                mv.visitJumpInsn(onTrue ? IFLE : IFGT, label);
                break;

            case Types.COMPARE_GREATER_THAN:
                mv.visitJumpInsn(onTrue ? IFGT : IFLE, label);
                break;

            case Types.COMPARE_GREATER_THAN_EQUAL:
                mv.visitJumpInsn(onTrue ? IFGE : IFLT, label);
                break;

            default:
                throw new IllegalStateException();
        }
    }

    private BytecodeExpr evaluateCompare(final BinaryExpression be, final CompilerTransformer compiler, final Label label, final boolean onTrue, final int op) {
        final BytecodeExpr l = (BytecodeExpr) compiler.transform(be.getLeftExpression());
        be.setLeftExpression(l);
        final BytecodeExpr r = (BytecodeExpr) compiler.transform(be.getRightExpression());
        be.setRightExpression(r);
        if (TypeUtil.isNumericalType(l.getType()) && TypeUtil.isNumericalType(r.getType())) {
            final ClassNode mathType = TypeUtil.getMathType(l.getType(), r.getType());
            return new BytecodeExpr(be, ClassHelper.boolean_TYPE) {
                public void compile(MethodVisitor mv) {
                    l.visit(mv);
                    box(l.getType(), mv);
                    cast(TypeUtil.wrapSafely(l.getType()), TypeUtil.wrapSafely(mathType), mv);
                    if (ClassHelper.isPrimitiveType(mathType))
                        unbox(mathType, mv);

                    r.visit(mv);
                    box(r.getType(), mv);
                    cast(TypeUtil.wrapSafely(r.getType()), TypeUtil.wrapSafely(mathType), mv);
                    if (ClassHelper.isPrimitiveType(mathType))
                        unbox(mathType, mv);

                    if (mathType == ClassHelper.int_TYPE) {
                        switch (op) {
                            case Types.COMPARE_EQUAL:
                                mv.visitJumpInsn(onTrue ? IF_ICMPEQ : IF_ICMPNE, label);
                                break;

                            case Types.COMPARE_NOT_EQUAL:
                                mv.visitJumpInsn(onTrue ? IF_ICMPNE : IF_ICMPEQ, label);
                                break;

                            case Types.COMPARE_LESS_THAN:
                                mv.visitJumpInsn(onTrue ? IF_ICMPLT : IF_ICMPGE, label);
                                break;

                            case Types.COMPARE_LESS_THAN_EQUAL:
                                mv.visitJumpInsn(onTrue ? IF_ICMPLE : IF_ICMPGT, label);
                                break;

                            case Types.COMPARE_GREATER_THAN:
                                mv.visitJumpInsn(onTrue ? IF_ICMPGT : IF_ICMPLE, label);
                                break;

                            case Types.COMPARE_GREATER_THAN_EQUAL:
                                mv.visitJumpInsn(onTrue ? IF_ICMPGE : IF_ICMPLT, label);
                                break;

                            default:
                                throw new IllegalStateException();
                        }
                    } else if (mathType == ClassHelper.double_TYPE) {
                        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "compare", "(DD)I");
                        intCmp(op, onTrue, mv, label);
                    } else if (mathType == ClassHelper.long_TYPE) {
                        mv.visitInsn(LCMP);
                        intCmp(op, onTrue, mv, label);
                    } else if (mathType == ClassHelper.BigInteger_TYPE) {
                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/math/BigInteger", "compareTo", "(Ljava/math/BigInteger;)I");
                        intCmp(op, onTrue, mv, label);
                    } else if (mathType == ClassHelper.BigDecimal_TYPE) {
                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/math/BigDecimal", "compareTo", "(Ljava/math/BigDecimal;)I");
                        intCmp(op, onTrue, mv, label);
                    } else
                        throw new RuntimeException("Internal Error");
                }
            };
        } else
            return new BytecodeExpr(be, ClassHelper.boolean_TYPE) {
                public void compile(MethodVisitor mv) {
                    final BytecodeExpr l = (BytecodeExpr) be.getLeftExpression();
                    l.visit(mv);
                    box(l.getType(), mv);

                    final BytecodeExpr r = (BytecodeExpr) be.getRightExpression();
                    r.visit(mv);
                    box(r.getType(), mv);

                    mv.visitMethodInsn(INVOKESTATIC, TypeUtil.DTT_INTERNAL, "compareEqual", "(Ljava/lang/Object;Ljava/lang/Object;)Z");
                    mv.visitJumpInsn(onTrue && be.getOperation().getType() == Types.COMPARE_EQUAL ? IFNE : IFEQ, label);
                }
            };
    }

    private static class Logical extends BytecodeExpr {
        private final Label _false = new Label(), _end = new Label();
        private final BytecodeExpr be;

        public Logical(Expression parent, CompilerTransformer compiler) {
            super(parent, ClassHelper.boolean_TYPE);
            be = compiler.transformLogical(parent, _false, false);
        }

        protected void compile(MethodVisitor mv) {
            be.visit(mv);
            mv.visitInsn(ICONST_1);
            mv.visitJumpInsn(GOTO, _end);
            mv.visitLabel(_false);
            mv.visitInsn(ICONST_0);
            mv.visitLabel(_end);
        }
    }
}
