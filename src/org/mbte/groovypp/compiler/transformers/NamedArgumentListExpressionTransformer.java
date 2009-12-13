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
        ArgumentListExpression args = new ArgumentListExpression(map);
        args.setSourcePosition(exp);
        return compiler.transform(args);
    }
}
