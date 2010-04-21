/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.ArrayExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.objectweb.asm.MethodVisitor;

import java.util.Iterator;
import java.util.List;

public class ArrayExpressionTransformer extends ExprTransformer<ArrayExpression> {

    public Expression transform(final ArrayExpression exp, final CompilerTransformer compiler) {
        final ClassNode elementType = exp.getElementType();
        final List sizeExpression = exp.getSizeExpression();

        return new BytecodeExpr(exp, exp.getType()) {
            protected void compile(MethodVisitor mv) {
                int size = 0;
                int dimensions = 0;
                if (sizeExpression != null) {
                    for (Iterator iter = sizeExpression.iterator(); iter.hasNext();) {
                        Expression element = (Expression) iter.next();
                        if (element == ConstantExpression.EMPTY_EXPRESSION) break;
                        dimensions++;

                        final BytecodeExpr expression = (BytecodeExpr) compiler.transform(element);
                        expression.visit(mv);
                        box(expression.getType(), mv);
                        unbox(ClassHelper.int_TYPE, mv);
                    }
                } else {
                    size = exp.getExpressions().size();
                    pushConstant(size, mv);
                }

                int storeIns = AASTORE;
                String arrayTypeName = BytecodeHelper.getTypeDescription(exp.getType().getComponentType());
                if (sizeExpression != null) {
                    mv.visitMultiANewArrayInsn(BytecodeHelper.getTypeDescription(exp.getType()), dimensions);
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
                    mv.visitTypeInsn(ANEWARRAY, BytecodeHelper.getClassInternalName(exp.getType().getComponentType()));
                }

                for (int i = 0; i < size; i++) {
                    mv.visitInsn(DUP);
                    pushConstant(i, mv);
                    BytecodeExpr elementExpression = (BytecodeExpr) compiler.transform(exp.getExpression(i));
                    elementExpression = compiler.cast(elementExpression, elementType);
                    elementExpression.visit(mv);
                    box(elementExpression.getType(), mv);
                    unbox(elementType, mv);
                    mv.visitInsn(storeIns);
                }
            }
        };
    }
}
