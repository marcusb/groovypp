package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.expr.*;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.objectweb.asm.Opcodes;

import java.util.IdentityHashMap;

public abstract class ExprTransformer<T extends Expression> implements Opcodes {

    private static IdentityHashMap<Class,ExprTransformer> transformers = new IdentityHashMap<Class,ExprTransformer> ();

    static {
        transformers.put(CastExpression.class, new CastExpressionTransformer());
        transformers.put(ClassExpression.class, new ClassExpressionTransformer());
        transformers.put(ConstantExpression.class, new ConstantExpressionTransformer());
        transformers.put(ListExpression.class, new ListExpressionTransformer());
        transformers.put(MapExpression.class, new MapExpressionTransformer());
        transformers.put(SpreadExpression.class, new SpreadExpressionTransformer());
        transformers.put(VariableExpression.class, new VariableExpressionTransformer());
        transformers.put(DeclarationExpression.class, new DeclarationExpressionTransformer());
        transformers.put(ClassExpression.class, new ClassExpressionTransformer());
        transformers.put(ClosureExpression.class, new ClosureExpressionTransformer());
        transformers.put(MethodCallExpression.class, new MethodCallExpressionTransformer());
        transformers.put(PostfixExpression.class, new PostfixExpressionTransformer());
        transformers.put(PrefixExpression.class, new PrefixExpressionTransformer());
        transformers.put(PropertyExpression.class, new PropertyExpressionTransformer());
        transformers.put(BinaryExpression.class, new BinaryExpressionTransformer());
        transformers.put(GStringExpression.class, new GStringExpressionTransformer());
        transformers.put(ConstructorCallExpression.class, new ConstructorCallExpressionTransformer());
        transformers.put(RangeExpression.class, new RangeExpressionTransformer());

        final BooleanExpressionTransformer bool = new BooleanExpressionTransformer();
        transformers.put(BooleanExpression.class, bool);
        transformers.put(NotExpression.class, bool);

        final TernaryExpressionTransformer ternary = new TernaryExpressionTransformer();
        transformers.put(TernaryExpression.class, ternary);
        transformers.put(ElvisOperatorExpression.class, ternary);
    }

    public static Expression transformExpression (Expression exp, CompilerTransformer compiler) {
        ExprTransformer t = transformers.get(exp.getClass());
        if (t == null)
            return compiler.transformImpl(exp);

        return t.transform(exp, compiler);
    }

    public abstract Expression transform (T exp, CompilerTransformer compiler);
}
