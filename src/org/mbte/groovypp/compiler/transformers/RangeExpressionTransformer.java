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
import org.codehaus.groovy.ast.GenericsType;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.RangeExpression;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.runtime.ScriptBytecodeAdapter;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.objectweb.asm.MethodVisitor;

public class RangeExpressionTransformer extends ExprTransformer<RangeExpression> {

    public Expression transform(final RangeExpression exp, CompilerTransformer compiler) {
        final BytecodeExpr from = (BytecodeExpr) compiler.transform(exp.getFrom());
        final BytecodeExpr to = (BytecodeExpr) compiler.transform(exp.getTo());
        final boolean intRange =
                   (from.getType() == ClassHelper.int_TYPE || from.getType().equals(ClassHelper.Integer_TYPE))
                && (to.getType() == ClassHelper.int_TYPE || to.getType().equals(ClassHelper.Integer_TYPE));
        ClassNode type = intRange ? TypeUtil.RANGE_OF_INTEGERS_TYPE : ClassHelper.RANGE_TYPE;
        return new BytecodeExpr(exp, type) {
            protected void compile(MethodVisitor mv) {
                from.visit(mv);
                box(from.getType(), mv);
                to.visit(mv);
                box(to.getType(), mv);
                mv.visitLdcInsn(exp.isInclusive());
                mv.visitMethodInsn(INVOKESTATIC, BytecodeHelper.getClassInternalName(ScriptBytecodeAdapter.class), "createRange", "(Ljava/lang/Object;Ljava/lang/Object;Z)Ljava/util/List;");
            }
        };
    }
}
