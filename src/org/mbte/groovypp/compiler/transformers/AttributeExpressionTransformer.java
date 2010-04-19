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

import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.expr.AttributeExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.PresentationUtil;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.mbte.groovypp.compiler.bytecode.ResolvedFieldBytecodeExpr;

public class AttributeExpressionTransformer extends ExprTransformer<AttributeExpression> {
    @Override
    public Expression transform(AttributeExpression exp, CompilerTransformer compiler) {
        Expression objectExpr = exp.getObjectExpression();

        BytecodeExpr obj;
        final FieldNode field;
        if (objectExpr instanceof ClassExpression) {
            obj = null;
            field = compiler.findField(objectExpr.getType(), exp.getPropertyAsString());
            if (field == null) {
              compiler.addError("Cannot find field " + exp.getPropertyAsString() + " of class " + PresentationUtil.getText(objectExpr.getType()), exp);
            }
        } else {
            obj = (BytecodeExpr) compiler.transform(objectExpr);
            field = compiler.findField(obj.getType(), exp.getPropertyAsString());
            if (field == null) {
              compiler.addError("Cannot find field " + exp.getPropertyAsString() + " of class " + PresentationUtil.getText(obj.getType()), exp);
            }
        }

        return new ResolvedFieldBytecodeExpr(exp, field, obj, null, compiler);
    }
}
