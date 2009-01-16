package org.mbte.groovypp.compiler.expressions;

import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.CompiledClosureBytecodeExpr;

public class ClosureExpressionTransformer extends ExprTransformer<ClosureExpression> {
    public Expression transform(ClosureExpression exp, CompilerTransformer compiler) {
        return CompiledClosureBytecodeExpr.createCompiledClosureBytecodeExpr(compiler, exp);
    }
}
