package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.mbte.groovypp.compiler.CompiledClosureBytecodeExpr;
import org.mbte.groovypp.compiler.CompilerTransformer;

public class ClosureExpressionTransformer extends ExprTransformer<ClosureExpression> {
    public Expression transform(ClosureExpression exp, CompilerTransformer compiler) {
        return CompiledClosureBytecodeExpr.createCompiledClosureBytecodeExpr(compiler, exp);
    }
}
