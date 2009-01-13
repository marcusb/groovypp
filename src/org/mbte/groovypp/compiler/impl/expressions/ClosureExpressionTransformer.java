package org.mbte.groovypp.compiler.impl.expressions;

import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.mbte.groovypp.compiler.impl.CompilerTransformer;
import org.mbte.groovypp.compiler.impl.CompiledClosureBytecodeExpr;

public class ClosureExpressionTransformer extends ExprTransformer<ClosureExpression> {
    public Expression transform(ClosureExpression exp, CompilerTransformer compiler) {
        return CompiledClosureBytecodeExpr.createCompiledClosureBytecodeExpr(compiler, exp);
    }
}
