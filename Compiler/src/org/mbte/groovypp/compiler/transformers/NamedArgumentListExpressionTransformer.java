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

import org.codehaus.groovy.ast.expr.NamedArgumentListExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.MapExpression;
import org.mbte.groovypp.compiler.CompilerTransformer;

public class NamedArgumentListExpressionTransformer extends ExprTransformer<NamedArgumentListExpression> {
    public Expression transform(NamedArgumentListExpression exp, CompilerTransformer compiler) {
        MapExpression map = new MapExpression(exp.getMapEntryExpressions());
        map.setSourcePosition(exp);
        return compiler.transform(map);
    }
}
