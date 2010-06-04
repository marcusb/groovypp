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
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.mbte.groovypp.runtime.Format;
import org.objectweb.asm.MethodVisitor;

import java.util.List;

import static org.codehaus.groovy.ast.ClassHelper.make;

public class GStringExpressionTransformer extends ExprTransformer<GStringExpression> {
    public static final ClassNode FORMAT = make(Format.class);

    public Expression transform(final GStringExpression exp, CompilerTransformer compiler) {
        final List<Expression> values = exp.getValues();
        final List<ConstantExpression> strings = exp.getStrings();

        int n = strings.size();

        Expression acc = new ConstructorCallExpression(TypeUtil.STRING_BUILDER, new ArgumentListExpression());
        acc.setSourcePosition(exp);
        for (int i = 0; i != n; ++i) {

            acc = new MethodCallExpression(acc, "append", new ArgumentListExpression(strings.get(i)));
            acc.setSourcePosition(exp);

            if (i < values.size()) {
                acc = new StaticMethodCallExpression(FORMAT, "toString", new ArgumentListExpression(values.get(i), acc));
                acc.setSourcePosition(exp);
            }
        }
        acc = new MethodCallExpression(acc, "toString", new ArgumentListExpression());
        acc.setSourcePosition(exp);
        return compiler.transform(acc);
    }
}
