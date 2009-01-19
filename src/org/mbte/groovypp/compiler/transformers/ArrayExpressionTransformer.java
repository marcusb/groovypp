package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.expr.ArrayExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.mbte.groovypp.compiler.CompilerTransformer;

import java.util.List;
import java.util.Iterator;

public class ArrayExpressionTransformer extends ExprTransformer<ArrayExpression> {

    public Expression transform(ArrayExpression exp, CompilerTransformer compiler) {
//        ClassNode elementType = exp.getElementType();
//        String arrayTypeName = BytecodeHelper.getClassInternalName(elementType);
//        List sizeExpression = exp.getSizeExpression();
//
//        int size = 0;
//        int dimensions = 0;
//        if (sizeExpression != null) {
//            for (Iterator iter = sizeExpression.iterator(); iter.hasNext();) {
//                Expression element = (Expression) iter.next();
//                if (element == ConstantExpression.EMTPY_EXPRESSION) break;
//                dimensions++;
//                // let's convert to an int
//                visitAndAutoboxBoolean(element);
//                helper.unbox(int.class);
//            }
//        } else {
//            size = exp.getExpressions().size();
//            helper.pushConstant(size);
//        }
//
//        int storeIns = AASTORE;
//        if (sizeExpression != null) {
//            arrayTypeName = BytecodeHelper.getTypeDescription(exp.getType());
//            mv.visitMultiANewArrayInsn(arrayTypeName, dimensions);
//        } else if (ClassHelper.isPrimitiveType(elementType)) {
//            int primType = 0;
//            if (elementType == ClassHelper.boolean_TYPE) {
//                primType = T_BOOLEAN;
//                storeIns = BASTORE;
//            } else if (elementType == ClassHelper.char_TYPE) {
//                primType = T_CHAR;
//                storeIns = CASTORE;
//            } else if (elementType == ClassHelper.float_TYPE) {
//                primType = T_FLOAT;
//                storeIns = FASTORE;
//            } else if (elementType == ClassHelper.double_TYPE) {
//                primType = T_DOUBLE;
//                storeIns = DASTORE;
//            } else if (elementType == ClassHelper.byte_TYPE) {
//                primType = T_BYTE;
//                storeIns = BASTORE;
//            } else if (elementType == ClassHelper.short_TYPE) {
//                primType = T_SHORT;
//                storeIns = SASTORE;
//            } else if (elementType == ClassHelper.int_TYPE) {
//                primType = T_INT;
//                storeIns = IASTORE;
//            } else if (elementType == ClassHelper.long_TYPE) {
//                primType = T_LONG;
//                storeIns = LASTORE;
//            }
//            mv.visitIntInsn(NEWARRAY, primType);
//        } else {
//            mv.visitTypeInsn(ANEWARRAY, arrayTypeName);
//        }
//
//        for (int i = 0; i < size; i++) {
//            mv.visitInsn(DUP);
//            helper.pushConstant(i);
//            Expression elementExpression = exp.getExpression(i);
//            if (elementExpression == null) {
//                ConstantExpression.NULL.visit(this);
//            } else {
//                if (!elementType.equals(elementExpression.getType())) {
//                    visitCastExpression(new CastExpression(elementType, elementExpression, true));
//                } else {
//                    visitAndAutoboxBoolean(elementExpression);
//                }
//            }
//            mv.visitInsn(storeIns);
//        }
//
//        if (sizeExpression == null && ClassHelper.isPrimitiveType(elementType)) {
//            int par = compileStack.defineTemporaryVariable("par", true);
//            mv.visitVarInsn(ALOAD, par);
//        }

        return null;
    }
}
