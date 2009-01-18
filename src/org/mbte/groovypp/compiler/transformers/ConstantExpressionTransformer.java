package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.classgen.ClassGeneratorException;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;

import java.math.BigDecimal;
import java.math.BigInteger;

public class ConstantExpressionTransformer extends ExprTransformer<ConstantExpression> {
    public BytecodeExpr transform(final ConstantExpression exp, CompilerTransformer compiler) {
        return new MyBytecodeExpr(exp);
    }

    private ClassNode getConstantType(Object value) {
        if (value == null) {
            return TypeUtil.NULL_TYPE;
        } else if (value instanceof String) {
            return ClassHelper.STRING_TYPE;
        } else if (value instanceof Character) {
            return ClassHelper.char_TYPE;
        } else if (value instanceof Number) {
            Number n = (Number) value;
            if (n instanceof Integer) {
                return ClassHelper.int_TYPE;
            } else if (n instanceof Double) {
                return ClassHelper.double_TYPE;
            } else if (n instanceof Float) {
                return ClassHelper.float_TYPE;
            } else if (n instanceof Long) {
                return ClassHelper.long_TYPE;
            } else if (n instanceof BigDecimal) {
                return ClassHelper.BigDecimal_TYPE;
            } else if (n instanceof BigInteger) {
                return ClassHelper.BigInteger_TYPE;
            } else if (n instanceof Short) {
                return ClassHelper.short_TYPE;
            } else if (n instanceof Byte) {
                return ClassHelper.byte_TYPE;
            } else {
                throw new ClassGeneratorException(
                        "Cannot generate bytecode for constant: " + value
                                + " of type: " + value.getClass().getName()
                                + ".  Numeric constant type not supported.");
            }
        } else if (value instanceof Boolean) {
            return ClassHelper.boolean_TYPE;
        } else if (value instanceof Class) {
            Class vc = (Class) value;
            if (vc.getName().equals("java.lang.Void")) {
                // load nothing here for void
            } else {
                throw new ClassGeneratorException(
                        "Cannot generate bytecode for constant: " + value + " of type: " + value.getClass().getName());
            }
        }

        throw new ClassGeneratorException(
                    "Cannot generate bytecode for constant: " + value + " of type: " + value.getClass().getName());
    }

    private class MyBytecodeExpr extends BytecodeExpr {
        private final ConstantExpression exp;

        public MyBytecodeExpr(ConstantExpression exp) {
            super(exp, ConstantExpressionTransformer.this.getConstantType(exp.getValue()));
            this.exp = exp;
        }

        public void compile() {
            final Object val = exp.getValue();
            if (val == null) {
                mv.visitInsn(ACONST_NULL);
            }
            else {
                mv.visitLdcInsn(val);
            }
        }
    }
}