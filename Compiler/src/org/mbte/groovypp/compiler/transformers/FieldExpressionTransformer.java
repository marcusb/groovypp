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

import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.FieldExpression;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.mbte.groovypp.compiler.bytecode.ResolvedFieldBytecodeExpr;
import org.objectweb.asm.MethodVisitor;

public class FieldExpressionTransformer extends ExprTransformer<FieldExpression> {

    public Expression transform(final FieldExpression exp, CompilerTransformer compiler) {
        return new ResolvedFieldBytecodeExpr(
                exp,
                exp.getField(),
                new BytecodeExpr(exp, compiler.classNode) {
                    public boolean isThis() {
                        return true;
                    }

                    protected void compile(MethodVisitor mv) {
                        if (!exp.getField().isStatic())
                            mv.visitVarInsn(ALOAD, 0);
                        else
                            mv.visitInsn(ACONST_NULL);
                    }
                },
                null,
                compiler);
    }
}