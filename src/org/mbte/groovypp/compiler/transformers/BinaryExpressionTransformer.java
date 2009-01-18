package org.mbte.groovypp.compiler.expressions;

import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.syntax.Types;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.StaticCompiler;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class BinaryExpressionTransformer extends ExprTransformer<BinaryExpression>{
    public Expression transform(BinaryExpression exp, CompilerTransformer compiler) {
        switch (exp.getOperation().getType()) {
            case Types.COMPARE_EQUAL:
                return evaluateEqual(exp, compiler);

            case Types.LOGICAL_AND:
                return evaluateLogicalAnd(exp, compiler);

            case Types.LOGICAL_OR:
                return evaluateLogicalOr(exp, compiler);

            case Types.LEFT_SQUARE_BRACKET:
                return evaluateArraySubscript(exp, compiler);

            case Types.EQUAL:
                return evaluateAssign(exp, compiler);

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

            case Types.KEYWORD_INSTANCEOF:
                return evaluateInstanceof(exp, compiler);

/*
            case Types.COMPARE_IDENTICAL: // ===
                evaluateBinaryExpression(compareIdenticalMethod, expression);
                break;

            case Types.COMPARE_NOT_EQUAL:
                evaluateBinaryExpression(compareNotEqualMethod, expression);
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
                compiler.addError("Operation: " + exp.getOperation() + " not supported", exp);
                return null;
            }
        }
    }
    private Expression evaluateInstanceof(BinaryExpression be, CompilerTransformer compiler) {
        final BytecodeExpr l = (BytecodeExpr) compiler.transform(be.getLeftExpression());
        final ClassNode type = be.getRightExpression().getType();
        return new BytecodeExpr(be, ClassHelper.boolean_TYPE) {
            protected void compile() {
                l.visit(mv);
                box(l.getType());
                mv.visitTypeInsn(INSTANCEOF, BytecodeHelper.getClassInternalName(type));
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
        final Expression left = be.getLeftExpression();
        if (left instanceof VariableExpression) {
            return evaluateAssignVariable(be, (VariableExpression)left, compiler.transform(be.getRightExpression()), compiler);
        }

//        if (left instanceof BinaryExpression && ((BinaryExpression)left).getOperation().getType() == Types.LEFT_SQUARE_BRACKET) {
//            return transformArrayPostfixExpression(exp);
//        }
//
//        if (left instanceof PropertyExpression) {
//            return transformPostfixPropertyExpression(exp, (PropertyExpression)left);
//        }

        compiler.addError("Assignment operator is applicable only to variable or property or array element", be);
        return null;
    }

    private Expression evaluateAssignVariable(BinaryExpression be, final VariableExpression ve, final Expression right, CompilerTransformer compiler) {
        if (ve.isThisExpression() || ve.isSuperExpression()) {
            compiler.addError("Can't assign value to 'this' or 'super'", be );
        }

        final org.codehaus.groovy.classgen.Variable var = compiler.compileStack.getVariable(ve.getName(), true);

        final ClassNode vtype;
        if (ve.getAccessedVariable().isDynamicTyped()) {
            vtype = ClassHelper.getWrapper(right.getType());
            compiler.getLocalVarInferenceTypes().add(ve, vtype);
        }
        else {
            vtype = right.getType();
        }

        return new BytecodeExpr(ve, vtype) {
            public void compile() {
                ((BytecodeExpr)right).visit(mv);
                box (right.getType());
                cast(ClassHelper.getWrapper(right.getType()), ClassHelper.getWrapper(ve.getType()));
                unbox(vtype);
                dup(vtype);
                storeVar(var);
            }
          };
    }

    private Expression evaluateArraySubscript(final BinaryExpression bin, CompilerTransformer compiler) {
        final BytecodeExpr arrExp = (BytecodeExpr) compiler.transform(bin.getLeftExpression());
        final BytecodeExpr indexExp = (BytecodeExpr) compiler.transform(bin.getRightExpression());

        if (arrExp.getType().isArray()) {
            final ClassNode type = arrExp.getType().getComponentType();

            if (!TypeUtil.isNumericalType(indexExp.getType())) {
                compiler.addError("Array subscript index should be integer", bin);
                return null;
            }

            return new BytecodeExpr(bin, arrExp.getType().getComponentType()) {
                public void compile() {
                    arrExp.visit(mv);
                    indexExp.visit(mv);
                    toInt(indexExp.getType());
                    loadArray(type);
                }
            };
        } else {
            return compiler.transform(new MethodCallExpression(arrExp, "getAt", new ArgumentListExpression(indexExp)));
        }
    }

    private Expression evaluateLogicalOr(final BinaryExpression be, CompilerTransformer compiler) {
        be.setLeftExpression(compiler.transform(be.getLeftExpression()));
        be.setRightExpression(compiler.transform(be.getRightExpression()));
        return new BytecodeExpr(be, ClassHelper.Boolean_TYPE) {
            public void compile() {
                mv.visitInsn(ICONST_1);
                final BytecodeExpr l = (BytecodeExpr) be.getLeftExpression();
                l.visit(mv);
                Label ok = new Label();
                StaticCompiler.branch(l, IFNE, ok, mv);

                final BytecodeExpr r = (BytecodeExpr) be.getRightExpression();
                r.visit(mv);
                StaticCompiler.branch(r, IFNE, ok, mv);

                mv.visitInsn(POP);
                mv.visitInsn(ICONST_0);

                mv.visitLabel(ok);
                box(ClassHelper.boolean_TYPE);
            }
        };
    }

    private Expression evaluateLogicalAnd(final BinaryExpression be, CompilerTransformer compiler) {
        be.setLeftExpression(compiler.transform(be.getLeftExpression()));
        be.setRightExpression(compiler.transform(be.getRightExpression()));
        return new BytecodeExpr(be, ClassHelper.Boolean_TYPE) {
            public void compile() {
                mv.visitInsn(ICONST_0);
                final BytecodeExpr l = (BytecodeExpr) be.getLeftExpression();
                l.visit(mv);
                Label notOk = new Label();
                StaticCompiler.branch(l, IFEQ, notOk, mv);

                final BytecodeExpr r = (BytecodeExpr) be.getRightExpression();
                r.visit(mv);
                StaticCompiler.branch(r, IFEQ, notOk, mv);

                mv.visitInsn(POP);
                mv.visitInsn(ICONST_1);

                mv.visitLabel(notOk);
                box(ClassHelper.boolean_TYPE);
            }
        };
    }

    private BytecodeExpr evaluateEqual(final BinaryExpression be, CompilerTransformer compiler) {
        be.setLeftExpression(compiler.transform(be.getLeftExpression()));
        be.setRightExpression(compiler.transform(be.getRightExpression()));
        return new BytecodeExpr(be, ClassHelper.Boolean_TYPE) {
            public void compile() {
                final BytecodeExpr l = (BytecodeExpr) be.getLeftExpression();
                l.visit(mv);
                box(l.getType());

                final BytecodeExpr r = (BytecodeExpr) be.getRightExpression();
                r.visit(mv);
                box(r.getType());

                mv.visitMethodInsn(INVOKESTATIC, TypeUtil.DTT_INTERNAL, "compareEqual", "(Ljava/lang/Object;Ljava/lang/Object;)Z");
                mv.visitMethodInsn(INVOKESTATIC, TypeUtil.DTT_INTERNAL, "box", "(Z)Ljava/lang/Object;");
            }
        };
    }


    public void pushConstant(MethodVisitor mv, int value) {
        switch (value) {
            case 0:
                mv.visitInsn(Opcodes.ICONST_0);
                break;
            case 1:
                mv.visitInsn(Opcodes.ICONST_1);
                break;
            case 2:
                mv.visitInsn(Opcodes.ICONST_2);
                break;
            case 3:
                mv.visitInsn(Opcodes.ICONST_3);
                break;
            case 4:
                mv.visitInsn(Opcodes.ICONST_4);
                break;
            case 5:
                mv.visitInsn(Opcodes.ICONST_5);
                break;
            default:
                if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
                    mv.visitIntInsn(Opcodes.BIPUSH, value);
                } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
                    mv.visitIntInsn(Opcodes.SIPUSH, value);
                } else {
                    mv.visitLdcInsn(Integer.valueOf(value));
                }
        }
    }
}
