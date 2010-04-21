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
import org.codehaus.groovy.ast.expr.UnaryPlusExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;

public class UnaryPlusExpressionTransformer extends ExprTransformer<UnaryPlusExpression> {
    public Expression transform(UnaryPlusExpression exp, CompilerTransformer compiler) {
        final BytecodeExpr inner = (BytecodeExpr) compiler.transform(exp.getExpression());
        final ClassNode type = ClassHelper.getUnwrapper(inner.getType());
        if (   type == ClassHelper.byte_TYPE
            || type == ClassHelper.short_TYPE
            || type == ClassHelper.int_TYPE
            || type == ClassHelper.float_TYPE
            || type == ClassHelper.long_TYPE
            || type == ClassHelper.double_TYPE
            || type.equals(ClassHelper.Byte_TYPE)
            || type.equals(ClassHelper.Short_TYPE)
            || type.equals(ClassHelper.Integer_TYPE)
            || type.equals(ClassHelper.Float_TYPE)
            || type.equals(ClassHelper.Long_TYPE)
            || type.equals(ClassHelper.Double_TYPE)) {
            return inner;
        }
        else {
            return compiler.transform(new MethodCallExpression(inner, "positive", new ArgumentListExpression()));
        }
    }
}
