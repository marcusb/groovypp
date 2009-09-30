package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.expr.ArrayExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.CastExpression;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;

import java.util.List;
import java.util.Iterator;

public class ArrayExpressionTransformer extends ExprTransformer<ArrayExpression> {

    public Expression transform(final ArrayExpression exp, final CompilerTransformer compiler) {
        final ClassNode elementType = exp.getElementType();
        final List sizeExpression = exp.getSizeExpression();

        return new BytecodeExpr(exp, exp.getType()) {
            protected void compile() {
                int size = 0;
                int dimensions = 0;
                if (sizeExpression != null) {
                    for (Iterator iter = sizeExpression.iterator(); iter.hasNext();) {
                        Expression element = (Expression) iter.next();
                        if (element == ConstantExpression.EMPTY_EXPRESSION) break;
                        dimensions++;

                        final BytecodeExpr expression = (BytecodeExpr) compiler.transform(element);
                        expression.visit(mv);
                        box(expression.getType());
                        unbox(ClassHelper.int_TYPE);
                    }
                } else {
                    size = exp.getExpressions().size();
                    pushConstant(size);
                }

                int storeIns = AASTORE;
                String arrayTypeName = BytecodeHelper.getTypeDescription(exp.getType());
                if (sizeExpression != null) {
                    mv.visitMultiANewArrayInsn(arrayTypeName, dimensions);
                } else if (ClassHelper.isPrimitiveType(elementType)) {
                    int primType = 0;
                    if (elementType == ClassHelper.boolean_TYPE) {
                        primType = T_BOOLEAN;
                        storeIns = BASTORE;
                    } else if (elementType == ClassHelper.char_TYPE) {
                        primType = T_CHAR;
                        storeIns = CASTORE;
                    } else if (elementType == ClassHelper.float_TYPE) {
                        primType = T_FLOAT;
                        storeIns = FASTORE;
                    } else if (elementType == ClassHelper.double_TYPE) {
                        primType = T_DOUBLE;
                        storeIns = DASTORE;
                    } else if (elementType == ClassHelper.byte_TYPE) {
                        primType = T_BYTE;
                        storeIns = BASTORE;
                    } else if (elementType == ClassHelper.short_TYPE) {
                        primType = T_SHORT;
                        storeIns = SASTORE;
                    } else if (elementType == ClassHelper.int_TYPE) {
                        primType = T_INT;
                        storeIns = IASTORE;
                    } else if (elementType == ClassHelper.long_TYPE) {
                        primType = T_LONG;
                        storeIns = LASTORE;
                    }
                    mv.visitIntInsn(NEWARRAY, primType);
                } else {
                    mv.visitTypeInsn(ANEWARRAY, arrayTypeName);
                }

                for (int i = 0; i < size; i++) {
                    mv.visitInsn(DUP);
                    pushConstant(i);
                    BytecodeExpr elementExpression = (BytecodeExpr) compiler.transform(exp.getExpression(i));
                    elementExpression.visit(mv);
                    box(elementExpression.getType());
                    unbox(elementType);
                    mv.visitInsn(storeIns);
                }
            }
        };
    }
}
