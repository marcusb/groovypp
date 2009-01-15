package org.mbte.groovypp.compiler.impl.expressions;

import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.SpreadExpression;
import org.mbte.groovypp.compiler.impl.*;
import org.mbte.groovypp.compiler.impl.bytecode.BytecodeExpr;

public class SpreadExpressionTransformer extends ExprTransformer<SpreadExpression>{
    public Expression transform(SpreadExpression exp, CompilerTransformer compiler) {
        final BytecodeExpr internal = (BytecodeExpr) compiler.transform(exp.getExpression());

        if (!TypeUtil.isDirectlyAssignableFrom(TypeUtil.COLLECTION_TYPE, internal.getType())) {
          compiler.addError("Spread operator can be applied only to java.util.Collection", exp);
          return null;
        }

        return new BytecodeSpreadExpr(exp, internal);
    }
}