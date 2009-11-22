package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.SpreadExpression;
import org.mbte.groovypp.compiler.BytecodeSpreadExpr;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;

public class SpreadExpressionTransformer extends ExprTransformer<SpreadExpression>{
    public Expression transform(SpreadExpression exp, CompilerTransformer compiler) {
        BytecodeExpr internal = (BytecodeExpr) compiler.transform(exp.getExpression());

        if (internal instanceof ListExpressionTransformer.UntransformedListExpr)
            internal = new ListExpressionTransformer.TransformedListExpr(((ListExpressionTransformer.UntransformedListExpr)internal).exp, TypeUtil.ARRAY_LIST_TYPE, compiler);

        if (!TypeUtil.isDirectlyAssignableFrom(TypeUtil.COLLECTION_TYPE, internal.getType())) {
          compiler.addError("Spread operator can be applied only to java.util.Collection", exp);
          return null;
        }

        return new BytecodeSpreadExpr(exp, internal);
    }
}