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
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.GStringExpression;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.objectweb.asm.MethodVisitor;

import java.util.List;

public class GStringExpressionTransformer extends ExprTransformer<GStringExpression> {
    public Expression transform(final GStringExpression exp, CompilerTransformer compiler) {
        final List list = exp.getValues();
        for (int i = 0; i != list.size(); i++)
           list.set(i, compiler.transform((Expression) list.get(i)));
        return new BytecodeExpr (exp, ClassHelper.GSTRING_TYPE) {
            protected void compile(MethodVisitor mv) {
                mv.visitTypeInsn(NEW, "org/codehaus/groovy/runtime/GStringImpl");
                mv.visitInsn(DUP);

                int size = exp.getValues().size();
                mv.visitLdcInsn(size);
                mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");

                for (int i = 0; i < size; i++) {
                    mv.visitInsn(DUP);
                    mv.visitLdcInsn(i);
                    final BytecodeExpr el = (BytecodeExpr) exp.getValue(i);
                    el.visit(mv);
                    box(el.getType(), mv);
                    mv.visitInsn(AASTORE);
                }

                List strings = exp.getStrings();
                size = strings.size();
                mv.visitLdcInsn(size);
                mv.visitTypeInsn(ANEWARRAY, "java/lang/String");

                for (int i = 0; i < size; i++) {
                    mv.visitInsn(DUP);
                    mv.visitLdcInsn(i);
                    mv.visitLdcInsn(((ConstantExpression) strings.get(i)).getValue());
                    mv.visitInsn(AASTORE);
                }

                mv.visitMethodInsn(INVOKESPECIAL, "org/codehaus/groovy/runtime/GStringImpl", "<init>", "([Ljava/lang/Object;[Ljava/lang/String;)V");
            }
        };
    }
}
