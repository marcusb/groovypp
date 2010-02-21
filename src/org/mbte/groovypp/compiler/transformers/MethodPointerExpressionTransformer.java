package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.mbte.groovypp.compiler.ClassNodeCache;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;

import java.util.ArrayList;
import java.util.List;

public class MethodPointerExpressionTransformer extends ExprTransformer<MethodPointerExpression> {
    public Expression transform(MethodPointerExpression exp, CompilerTransformer compiler) {

        String methodName;
        if (!(exp.getMethodName() instanceof ConstantExpression) || !(((ConstantExpression) exp.getMethodName()).getValue() instanceof String)) {
            compiler.addError("Non-static method name", exp);
            return null;
        } else {
            methodName = (String) ((ConstantExpression) exp.getMethodName()).getValue();
        }

        final ClassNode type;
        if (exp.getExpression() instanceof ClassExpression) {
            type = TypeUtil.wrapSafely(exp.getExpression().getType());
        } else {
            type = compiler.transform(exp.getExpression()).getType();
        }

        final Object methods = ClassNodeCache.getMethods(type, methodName);
        // todo: dynamic dispatch
        if (methods == null) {
            compiler.addError("Cannot find method '" + methodName + "'", exp);
        } else if (!(methods instanceof MethodNode)) {
            compiler.addError("Multiple methods '" + methodName + "' referenced. Cannot take the pointer", exp);
        }
        final MethodNode method = (MethodNode) methods;
        final Parameter[] methodParameters = method.getParameters();
        final Parameter[] closureParameters = new Parameter[methodParameters.length];
        final GenericsType[] generics = method.getGenericsTypes();
        final ClassNode[] erasureBindings = generics == null ? null : new ClassNode[generics.length];  // All nulls.
        for (int i = 0; i < closureParameters.length; i++) {
            ClassNode t = methodParameters[i].getType();
            if (erasureBindings != null) t = TypeUtil.getSubstitutedType(t, method, erasureBindings);
            t = TypeUtil.getSubstitutedType(t, method.getDeclaringClass(), type);
            closureParameters[i] = new Parameter(t, methodParameters[i].getName());
        }

        List<Expression> args = new ArrayList<Expression>();
        for (int i = 0; i < closureParameters.length; i++) {
            args.add(new VariableExpression(closureParameters[i].getName()));
        }

        final ExpressionStatement statement = new ExpressionStatement(
                new MethodCallExpression(exp.getExpression(), methodName, new ArgumentListExpression(args)));
        final ClosureExpression closure =
                new ClosureExpression(closureParameters.length == 0 ? null : closureParameters, statement);
        closure.setVariableScope(new VariableScope(compiler.compileStack.getScope()));
        closure.setSourcePosition(exp);
        return compiler.transform(closure);
    }
}
