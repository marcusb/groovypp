package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.syntax.Types;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.mbte.groovypp.compiler.bytecode.ResolvedLeftExpr;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.StaticCompiler;
import org.mbte.groovypp.compiler.transformers.ExprTransformer;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class BinaryExpressionTransformer extends ExprTransformer<BinaryExpression> {
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
                return new Logical (exp, compiler);

            case Types.EQUAL:
                return evaluateAssign(exp, compiler);

            case Types.LEFT_SQUARE_BRACKET:
                return evaluateArraySubscript(exp, compiler);

            case Types.MULTIPLY:
                return evaluateMathOperation(exp, "multiply", compiler);

            case Types.DIVIDE:
                return evaluateMathOperation(exp, "divide", compiler);

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

/*
            case Types.COMPARE_TO:
                evaluateCompareTo(expression);
                break;

            case Types.BITWISE_AND_EQUAL:
                evaluateBinaryExpressionWithAssignment("and", expression);
                break;

            case Types.BITWISE_OR_EQUAL:
                evaluateBinaryExpressionWithAssignment("or", expression);
                break;

            case Types.BITWISE_XOR_EQUAL:
                evaluateBinaryExpressionWithAssignment("xor", expression);
                break;

            case Types.PLUS_EQUAL:
                evaluateBinaryExpressionWithAssignment("plus", expression);
                break;

            case Types.MINUS_EQUAL:
                evaluateBinaryExpressionWithAssignment("minus", expression);
                break;

            case Types.MULTIPLY_EQUAL:
                evaluateBinaryExpressionWithAssignment("multiply", expression);
                break;

            case Types.DIVIDE_EQUAL:
                //SPG don't use divide since BigInteger implements directly
                //and we want to dispatch through DefaultGroovyMethods to get a BigDecimal result
                evaluateBinaryExpressionWithAssignment("div", expression);
                break;

            case Types.INTDIV_EQUAL:
                evaluateBinaryExpressionWithAssignment("intdiv", expression);
                break;

            case Types.MOD_EQUAL:
                evaluateBinaryExpressionWithAssignment("mod", expression);
                break;

            case Types.POWER_EQUAL:
                evaluateBinaryExpressionWithAssignment("power", expression);
                break;

            case Types.LEFT_SHIFT_EQUAL:
                evaluateBinaryExpressionWithAssignment("leftShift", expression);
                break;

            case Types.RIGHT_SHIFT_EQUAL:
                evaluateBinaryExpressionWithAssignment("rightShift", expression);
                break;

            case Types.RIGHT_SHIFT_UNSIGNED_EQUAL:
                evaluateBinaryExpressionWithAssignment("rightShiftUnsigned", expression);
                break;

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

    public BytecodeExpr transformLogical(BinaryExpression exp, CompilerTransformer compiler, Label label, boolean onTrue) {
        switch (exp.getOperation().getType()) {
            case Types.COMPARE_EQUAL:
                return evaluateEqual(exp, compiler, label, onTrue);

            case Types.COMPARE_NOT_EQUAL:
                return evaluateEqual(exp, compiler, label, !onTrue);

            case Types.LOGICAL_AND:
                return evaluateLogicalAnd(exp, compiler, label, onTrue);

            case Types.LOGICAL_OR:
                return evaluateLogicalOr(exp, compiler, label, onTrue);

            case Types.KEYWORD_INSTANCEOF:
                return evaluateInstanceof(exp, compiler, label, onTrue);
/*
            case Types.COMPARE_IDENTICAL: // ===
                evaluateBinaryExpression(compareIdenticalMethod, expression);
                break;

            case Types.COMPARE_TO:
                evaluateCompareTo(expression);
                break;

            case Types.COMPARE_GREATER_THAN:
                evaluateBinaryExpression(compareGreaterThanMethod, expression);
                break;

            case Types.COMPARE_GREATER_THAN_EQUAL:
                evaluateBinaryExpression(compareGreaterThanEqualMethod, expression);
                break;

            case Types.COMPARE_LESS_THAN:
                evaluateBinaryExpression(compareLessThanMethod, expression);
                break;

            case Types.COMPARE_LESS_THAN_EQUAL:
                evaluateBinaryExpression(compareLessThanEqualMethod, expression);
                break;

            case Types.BITWISE_AND_EQUAL:
                evaluateBinaryExpressionWithAssignment("and", expression);
                break;

            case Types.BITWISE_OR_EQUAL:
                evaluateBinaryExpressionWithAssignment("or", expression);
                break;

            case Types.BITWISE_XOR_EQUAL:
                evaluateBinaryExpressionWithAssignment("xor", expression);
                break;

            case Types.PLUS_EQUAL:
                evaluateBinaryExpressionWithAssignment("plus", expression);
                break;

            case Types.MINUS_EQUAL:
                evaluateBinaryExpressionWithAssignment("minus", expression);
                break;

            case Types.MULTIPLY_EQUAL:
                evaluateBinaryExpressionWithAssignment("multiply", expression);
                break;

            case Types.DIVIDE_EQUAL:
                //SPG don't use divide since BigInteger implements directly
                //and we want to dispatch through DefaultGroovyMethods to get a BigDecimal result
                evaluateBinaryExpressionWithAssignment("div", expression);
                break;

            case Types.INTDIV_EQUAL:
                evaluateBinaryExpressionWithAssignment("intdiv", expression);
                break;

            case Types.MOD_EQUAL:
                evaluateBinaryExpressionWithAssignment("mod", expression);
                break;

            case Types.POWER_EQUAL:
                evaluateBinaryExpressionWithAssignment("power", expression);
                break;

            case Types.LEFT_SHIFT_EQUAL:
                evaluateBinaryExpressionWithAssignment("leftShift", expression);
                break;

            case Types.RIGHT_SHIFT_EQUAL:
                evaluateBinaryExpressionWithAssignment("rightShift", expression);
                break;

            case Types.RIGHT_SHIFT_UNSIGNED_EQUAL:
                evaluateBinaryExpressionWithAssignment("rightShiftUnsigned", expression);
                break;

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
                return super.transformLogical(exp, compiler, label, onTrue);
            }
        }
    }


    private BytecodeExpr evaluateInstanceof(BinaryExpression be, CompilerTransformer compiler, final Label label, final boolean onTrue) {
        final BytecodeExpr l = (BytecodeExpr) compiler.transform(be.getLeftExpression());
        final ClassNode type = be.getRightExpression().getType();
        return new BytecodeExpr(be, ClassHelper.boolean_TYPE) {
            protected void compile() {
                l.visit(mv);
                box(l.getType());
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

            final ClassNode mathType = TypeUtil.getMathType(l.getType(), r.getType());

            if (mathType == ClassHelper.BigDecimal_TYPE || mathType == ClassHelper.BigInteger_TYPE)
                return callMethod(be, method, compiler, l, r);

            if (mathType != ClassHelper.Integer_TYPE && mathType != ClassHelper.Long_TYPE) {
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
                protected void compile() {
                    l.visit(mv);
                    box(l.getType());
                    cast(ClassHelper.getWrapper(l.getType()), ClassHelper.getWrapper(mathType));
                    if (ClassHelper.isPrimitiveType(mathType))
                       unbox(mathType);

                    r.visit(mv);
                    box(r.getType());
                    cast(ClassHelper.getWrapper(r.getType()), ClassHelper.getWrapper(mathType));
                    if (ClassHelper.isPrimitiveType(mathType))
                       unbox(mathType);

                    compiler.mathOp(mathType, be.getOperation(), be);
                }
            };
        }
        else {
            return callMethod(be, method, compiler, l, r);
        }
    }

    private Expression callMethod(BinaryExpression be, String method, CompilerTransformer compiler, BytecodeExpr l, BytecodeExpr r) {
        final MethodCallExpression mce = new MethodCallExpression(l, method, new ArgumentListExpression(r));
        mce.setSourcePosition(be);
        return compiler.transform(mce);
    }

    private Expression evaluateAssign(BinaryExpression be, CompilerTransformer compiler) {
        final Expression left = compiler.transform(be.getLeftExpression());

        if (!(left instanceof ResolvedLeftExpr)) {
            compiler.addError("Assignment operator is applicable only to variable or property or array element", be);
            return null;
        }

        return ((ResolvedLeftExpr)left).createAssign(be, (BytecodeExpr) compiler.transform(be.getRightExpression()), compiler);
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
                protected void compile() {
                    l.visit(mv);
                    r.visit(mv);
                }
            };
        }
        else {
            final Label _true = new Label();
            final BytecodeExpr l = compiler.transformLogical(exp.getLeftExpression(), _true, true);
            final BytecodeExpr r = compiler.transformLogical(exp.getRightExpression(), label, false);
            return new BytecodeExpr(exp, ClassHelper.VOID_TYPE) {
                protected void compile() {
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
                protected void compile() {
                    l.visit(mv);
                    r.visit(mv);
                    mv.visitLabel(_false);
                }
            };
        }
        else {
            final BytecodeExpr l = compiler.transformLogical(exp.getLeftExpression(), label, false);
            final BytecodeExpr r = compiler.transformLogical(exp.getRightExpression(), label, false);
            return new BytecodeExpr(exp, ClassHelper.VOID_TYPE) {
                protected void compile() {
                    l.visit(mv);
                    r.visit(mv);
                }
            };
        }
    }

    private BytecodeExpr evaluateEqual(final BinaryExpression be, final CompilerTransformer compiler, final Label label, final boolean onTrue) {
        final BytecodeExpr l = (BytecodeExpr) compiler.transform(be.getLeftExpression());
        be.setLeftExpression(l);
        final BytecodeExpr r = (BytecodeExpr) compiler.transform(be.getRightExpression());
        be.setRightExpression(r);
        if (TypeUtil.isNumericalType(l.getType()) && TypeUtil.isNumericalType(r.getType())) {
            final ClassNode mathType = TypeUtil.getMathType(l.getType(), r.getType());
            return new BytecodeExpr(be, ClassHelper.boolean_TYPE) {
                public void compile() {
                    l.visit(mv);
                    box(l.getType());
                    cast(ClassHelper.getWrapper(l.getType()), ClassHelper.getWrapper(mathType));
                    if (ClassHelper.isPrimitiveType(mathType))
                       unbox(mathType);

                    r.visit(mv);
                    box(r.getType());
                    cast(ClassHelper.getWrapper(r.getType()), ClassHelper.getWrapper(mathType));
                    if (ClassHelper.isPrimitiveType(mathType))
                       unbox(mathType);

                    if (mathType == ClassHelper.int_TYPE)
                        mv.visitJumpInsn(onTrue ? IF_ICMPEQ : IF_ICMPNE, label);
                    else
                    if (mathType == ClassHelper.double_TYPE) {
                        mv.visitInsn(DCMPG);
                        mv.visitJumpInsn(onTrue ? IFEQ : IFNE, label);
                    }
                    else
                    if (mathType == ClassHelper.float_TYPE) {
                        mv.visitInsn(FCMPG);
                        mv.visitJumpInsn(onTrue ? IFEQ : IFNE, label);
                    }
                    else
                    if (mathType == ClassHelper.long_TYPE) {
                        mv.visitInsn(LCMP);
                        mv.visitJumpInsn(onTrue ? IFEQ : IFNE, label);
                    }
                    else
                        throw new RuntimeException("Internal Error");
                }
            };
        }
        else
            return new BytecodeExpr(be, ClassHelper.boolean_TYPE) {
                public void compile() {
                    final BytecodeExpr l = (BytecodeExpr) be.getLeftExpression();
                    l.visit(mv);
                    box(l.getType());

                    final BytecodeExpr r = (BytecodeExpr) be.getRightExpression();
                    r.visit(mv);
                    box(r.getType());

                    mv.visitMethodInsn(INVOKESTATIC, TypeUtil.DTT_INTERNAL, "compareEqual", "(Ljava/lang/Object;Ljava/lang/Object;)Z");
                    mv.visitJumpInsn(onTrue ? IFNE : IFEQ, label);
                }
            };
    }

    private static class Logical extends BytecodeExpr {
        private final Label _false = new Label (), _end = new Label();
        private final BytecodeExpr be;

        public Logical(Expression parent, CompilerTransformer compiler) {
            super(parent, ClassHelper.boolean_TYPE);
            be = compiler.transformLogical(parent, _false, false);
        }

        protected void compile() {
            be.visit(mv);
            mv.visitInsn(ICONST_1);
            mv.visitJumpInsn(GOTO, _end);
            mv.visitLabel(_false);
            mv.visitInsn(ICONST_0);
            mv.visitLabel(_end);
        }
    }
}
