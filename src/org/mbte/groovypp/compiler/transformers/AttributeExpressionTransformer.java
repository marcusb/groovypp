package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.expr.AttributeExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.mbte.groovypp.compiler.bytecode.ResolvedFieldBytecodeExpr;

public class AttributeExpressionTransformer extends ExprTransformer<AttributeExpression> {
    @Override
    public Expression transform(AttributeExpression exp, CompilerTransformer compiler) {
        BytecodeExpr obj = (BytecodeExpr) compiler.transform(exp.getObjectExpression());
        final FieldNode field = compiler.findField(obj.getType(), exp.getPropertyAsString());
        if (field == null) {
            compiler.addError("Can't find field " + exp.getPropertyAsString() + " of class " + obj.getType().getName(), exp);
        }
        return new ResolvedFieldBytecodeExpr(exp, field, obj, null, compiler);
    }
}
