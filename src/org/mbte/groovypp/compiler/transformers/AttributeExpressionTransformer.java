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
