package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.PresentationUtil;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.bytecode.*;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import static org.codehaus.groovy.ast.ClassHelper.int_TYPE;

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
            case Types.COMPARE_NOT_IDENTICAL: // ===
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

            case Types.FIND_REGEX:
                return evaluateFindRegexp(exp, compiler);

            case Types.MATCH_REGEX:
				return evaluateMatchRegexp(exp, compiler);

/*
            case Types.KEYWORD_IN:
	            compiler.addError("Operation: " + exp.getOperation() + " not supported", exp);
	            return null;
*/

            default: {
                compiler.addError("Operation: " + exp.getOperation() + " not supported", exp);
                return null;
            }
        }
    }

    private Expression evaluateCompareTo(BinaryExpression be, CompilerTransformer compiler) {
        final Operands operands = new Operands(be, compiler);
        return new BytecodeExpr(be, ClassHelper.Integer_TYPE) {
            protected void compile(MethodVisitor mv) {
                operands.getLeft().visit(mv);
                if (ClassHelper.isPrimitiveType(operands.getLeft().getType()))
                    box(operands.getLeft().getType(), mv);
                operands.getRight().visit(mv);
                if (ClassHelper.isPrimitiveType(operands.getRight().getType()))
                    box(operands.getRight().getType(), mv);
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
                return evaluateCompare(exp, compiler, label, onTrue, op);

            case Types.COMPARE_NOT_IDENTICAL: // !==
                return evaluateCompare(exp, compiler, label, onTrue, op);

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

    private BytecodeExpr unboxReference(BinaryExpression parent, BytecodeExpr left, CompilerTransformer compiler) {
        MethodNode unboxing = TypeUtil.getReferenceUnboxingMethod(left.getType());
        if (unboxing != null) {
            left = ResolvedMethodBytecodeExpr.create(parent, unboxing, left, new ArgumentListExpression(), compiler);
        }
        return left;
    }

    private Expression evaluateMathOperation(final BinaryExpression be, String method, final CompilerTransformer compiler) {
        final Operands operands = new Operands(be, compiler);
        final BytecodeExpr l = operands.getLeft();
        final BytecodeExpr r = operands.getRight();

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
        ConstantExpression methodExpression = new ConstantExpression(method);
        methodExpression.setLineNumber(be.getOperation().getStartLine());
        methodExpression.setColumnNumber(be.getOperation().getStartColumn());
        final MethodCallExpression mce = new MethodCallExpression(l, methodExpression, new ArgumentListExpression(r));
        mce.setSourcePosition(be);
        return compiler.transform(mce);
    }

    private Expression evaluateAssign(BinaryExpression be, CompilerTransformer compiler) {
        BytecodeExpr left = (BytecodeExpr) compiler.transform(be.getLeftExpression());

        if (!(left instanceof ResolvedLeftExpr)) {
            compiler.addError("Assignment operator is applicable only to variable or property or array element", be);
            return null;
        }

        BytecodeExpr right = (BytecodeExpr) compiler.transform(be.getRightExpression());
        MethodNode boxing = TypeUtil.getReferenceBoxingMethod(left.getType(), right.getType());
        if (boxing != null) {
            return ResolvedMethodBytecodeExpr.create(be, boxing, left, new ArgumentListExpression(right), compiler);
        }
        return ((ResolvedLeftExpr) left).createAssign(be, right, compiler);
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
        BytecodeExpr object = (BytecodeExpr) compiler.transform(bin.getLeftExpression());
        final BytecodeExpr indexExp = (BytecodeExpr) compiler.transform(bin.getRightExpression());
        if (object.getType().isArray() && TypeUtil.isAssignableFrom(int_TYPE, indexExp.getType()))
            return new ResolvedArrayBytecodeExpr(bin, object, indexExp, compiler);
        else {
            MethodNode getter = compiler.findMethod(object.getType(), "getAt", new ClassNode[]{indexExp.getType()}, false);
            if (getter == null) {
                MethodNode unboxing = TypeUtil.getReferenceUnboxingMethod(object.getType());
                if (unboxing != null) {
                    ClassNode t = TypeUtil.getSubstitutedType(unboxing.getReturnType(), unboxing.getDeclaringClass(), object.getType());
                    getter = compiler.findMethod(t, "getAt", new ClassNode[]{indexExp.getType()}, false);
                    if (getter != null) {
                        object = ResolvedMethodBytecodeExpr.create(bin, unboxing, object,
                                new ArgumentListExpression(), compiler);
                        return new ResolvedArrayLikeBytecodeExpr(bin, object, indexExp, getter, compiler);
                    }
                }
            } else {
                return new ResolvedArrayLikeBytecodeExpr(bin, object, indexExp, getter, compiler);
            }

            if (indexExp instanceof ListExpressionTransformer.UntransformedListExpr) {
                MethodCallExpression mce = new MethodCallExpression(object, "getAt", new ArgumentListExpression(((ListExpressionTransformer.UntransformedListExpr) indexExp).exp.getExpressions()));
                mce.setSourcePosition(bin);
                return compiler.transform(mce);
            }

            compiler.addError("Can't find method 'getAt' for type: " + PresentationUtil.getText(object.getType()), bin);
            return null;
        }
    }

    private BytecodeExpr evaluateLogicalOr(final BinaryExpression exp, CompilerTransformer compiler, Label label, boolean onTrue) {
        if (onTrue) {
            final BytecodeExpr l = unboxReference(exp, compiler.transformLogical(exp.getLeftExpression(), label, true), compiler);
            final BytecodeExpr r = unboxReference(exp, compiler.transformLogical(exp.getRightExpression(), label, true), compiler);
            return new BytecodeExpr(exp, ClassHelper.VOID_TYPE) {
                protected void compile(MethodVisitor mv) {
                    l.visit(mv);
                    r.visit(mv);
                }
            };
        } else {
            final Label _true = new Label();
            final BytecodeExpr l = unboxReference(exp, compiler.transformLogical(exp.getLeftExpression(), _true, true), compiler);
            final BytecodeExpr r = unboxReference(exp, compiler.transformLogical(exp.getRightExpression(), label, false), compiler);
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
            final BytecodeExpr l = unboxReference(exp, compiler.transformLogical(exp.getLeftExpression(), _false, false), compiler);
            final BytecodeExpr r = unboxReference(exp, compiler.transformLogical(exp.getRightExpression(), label, true), compiler);
            return new BytecodeExpr(exp, ClassHelper.VOID_TYPE) {
                protected void compile(MethodVisitor mv) {
                    l.visit(mv);
                    r.visit(mv);
                    mv.visitLabel(_false);
                }
            };
        } else {
            final BytecodeExpr l = unboxReference(exp, compiler.transformLogical(exp.getLeftExpression(), label, false), compiler);
            final BytecodeExpr r = unboxReference(exp, compiler.transformLogical(exp.getRightExpression(), label, false), compiler);
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
        final Operands operands = new Operands(be, compiler);
        BytecodeExpr l = operands.getLeft();
        if (l instanceof ListExpressionTransformer.UntransformedListExpr)
            l = new ListExpressionTransformer.TransformedListExpr(((ListExpressionTransformer.UntransformedListExpr)l).exp, TypeUtil.ARRAY_LIST_TYPE, compiler);

        BytecodeExpr r = operands.getRight();
        if (r instanceof ListExpressionTransformer.UntransformedListExpr)
            r = new ListExpressionTransformer.TransformedListExpr(((ListExpressionTransformer.UntransformedListExpr)r).exp, TypeUtil.ARRAY_LIST_TYPE, compiler);

        if (TypeUtil.isNumericalType(l.getType()) && TypeUtil.isNumericalType(r.getType())) {
            final ClassNode mathType = TypeUtil.getMathType(l.getType(), r.getType());
            final BytecodeExpr l1 = l;
            final BytecodeExpr r1 = r;
            return new BytecodeExpr(be, ClassHelper.boolean_TYPE) {
                public void compile(MethodVisitor mv) {
                    l1.visit(mv);
                    box(l1.getType(), mv);
                    cast(TypeUtil.wrapSafely(l1.getType()), TypeUtil.wrapSafely(mathType), mv);
                    if (ClassHelper.isPrimitiveType(mathType))
                        unbox(mathType, mv);

                    r1.visit(mv);
                    box(r1.getType(), mv);
                    cast(TypeUtil.wrapSafely(r1.getType()), TypeUtil.wrapSafely(mathType), mv);
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
        } else {
            final BytecodeExpr l2 = l;
            final BytecodeExpr r2 = r;
            return new BytecodeExpr(be, ClassHelper.boolean_TYPE) {
                public void compile(MethodVisitor mv) {
                    l2.visit(mv);
                    box(l2.getType(), mv);

                    r2.visit(mv);
                    box(r2.getType(), mv);

                    switch (be.getOperation().getType()) {
                        case  Types.COMPARE_EQUAL:
                            if (l2.getType().equals(TypeUtil.NULL_TYPE) || r2.getType().equals(TypeUtil.NULL_TYPE)) {
                                mv.visitJumpInsn(onTrue ? IF_ACMPEQ : IF_ACMPNE, label);
                            }
                            else {
                                mv.visitMethodInsn(INVOKESTATIC, TypeUtil.DTT_INTERNAL, "compareEqual", "(Ljava/lang/Object;Ljava/lang/Object;)Z");
                                mv.visitJumpInsn(onTrue ? IFNE : IFEQ, label);
                            }
                            break;

                        case  Types.COMPARE_NOT_EQUAL:
                            if (l2.getType().equals(TypeUtil.NULL_TYPE) || r2.getType().equals(TypeUtil.NULL_TYPE)) {
                                mv.visitJumpInsn(onTrue ? IF_ACMPNE : IF_ACMPEQ, label);
                            }
                            else {
                                mv.visitMethodInsn(INVOKESTATIC, TypeUtil.DTT_INTERNAL, "compareEqual", "(Ljava/lang/Object;Ljava/lang/Object;)Z");
                                mv.visitJumpInsn(onTrue ? IFEQ : IFNE, label);
                            }
                            break;

                        case  Types.COMPARE_IDENTICAL:
                            mv.visitJumpInsn(onTrue ? IF_ACMPEQ : IF_ACMPNE, label);
                            break;

                        case  Types.COMPARE_NOT_IDENTICAL:
                            mv.visitJumpInsn(onTrue ? IF_ACMPNE : IF_ACMPEQ, label);
                            break;

                        case Types.COMPARE_LESS_THAN:
                            mv.visitMethodInsn(INVOKESTATIC, TypeUtil.DTT_INTERNAL, "compareTo", "(Ljava/lang/Object;Ljava/lang/Object;)I");
                            mv.visitJumpInsn(onTrue ? IFLT : IFGE, label);
                            break;

                        case Types.COMPARE_LESS_THAN_EQUAL:
                            mv.visitMethodInsn(INVOKESTATIC, TypeUtil.DTT_INTERNAL, "compareTo", "(Ljava/lang/Object;Ljava/lang/Object;)I");
                            mv.visitJumpInsn(onTrue ? IFLE : IFGT, label);
                            break;

                        case Types.COMPARE_GREATER_THAN:
                            mv.visitMethodInsn(INVOKESTATIC, TypeUtil.DTT_INTERNAL, "compareTo", "(Ljava/lang/Object;Ljava/lang/Object;)I");
                            mv.visitJumpInsn(onTrue ? IFGT : IFLE, label);
                            break;

                        case Types.COMPARE_GREATER_THAN_EQUAL:
                            mv.visitMethodInsn(INVOKESTATIC, TypeUtil.DTT_INTERNAL, "compareTo", "(Ljava/lang/Object;Ljava/lang/Object;)I");
                            mv.visitJumpInsn(onTrue ? IFGE : IFLT, label);
                            break;

                        default:
                            throw new UnsupportedOperationException();
                    }
                }
            };
        }
    }

    private static BytecodeExpr evaluateFindRegexp(final BinaryExpression exp, final CompilerTransformer compiler) {
        final BytecodeExpr left = (BytecodeExpr) compiler.transform(exp.getLeftExpression());
        final BytecodeExpr right = (BytecodeExpr) compiler.transform(exp.getRightExpression());

        return new BytecodeExpr(exp, TypeUtil.MATCHER) {
            protected void compile(MethodVisitor mv) {
                left.visit(mv);
                if (ClassHelper.isPrimitiveType(left.getType()))
                    box(left.getType(), mv);
                right.visit(mv);
                if (ClassHelper.isPrimitiveType(right.getType()))
                    box(right.getType(), mv);

                mv.visitMethodInsn(
                        INVOKESTATIC, "org/codehaus/groovy/runtime/InvokerHelper",
                        "findRegex", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/regex/Matcher;");
            }
        };
    }

    private static BytecodeExpr evaluateMatchRegexp(final BinaryExpression exp, final CompilerTransformer compiler) {
        final BytecodeExpr left = (BytecodeExpr) compiler.transform(exp.getLeftExpression());
        final BytecodeExpr right = (BytecodeExpr) compiler.transform(exp.getRightExpression());

        return new BytecodeExpr(exp, ClassHelper.boolean_TYPE) {
            protected void compile(MethodVisitor mv) {
                left.visit(mv);
                if (ClassHelper.isPrimitiveType(left.getType()))
                    box(left.getType(), mv);
                right.visit(mv);
                if (ClassHelper.isPrimitiveType(right.getType()))
                    box(right.getType(), mv);

                mv.visitMethodInsn(
                        INVOKESTATIC, "org/codehaus/groovy/runtime/InvokerHelper",
                        "matchRegex", "(Ljava/lang/Object;Ljava/lang/Object;)Z");

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

    private class Operands {
        private BytecodeExpr left;
        private BytecodeExpr right;

        public Operands(BinaryExpression be, CompilerTransformer compiler) {
            left = (BytecodeExpr) compiler.transform(be.getLeftExpression());
            right = (BytecodeExpr) compiler.transform(be.getRightExpression());
            if (!TypeUtil.areTypesDirectlyConvertible(left.getType(), right.getType())) {
                left = unboxReference(be, left, compiler);
                right = unboxReference(be, right, compiler);
            }
        }

        public BytecodeExpr getLeft() {
            return left;
        }

        public BytecodeExpr getRight() {
            return right;
        }
    }
}
