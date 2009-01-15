package org.mbte.groovypp.compiler.impl.expressions;

import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MapExpression;
import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.mbte.groovypp.compiler.impl.CompilerTransformer;
import org.mbte.groovypp.compiler.impl.bytecode.BytecodeExpr;
import org.mbte.groovypp.compiler.impl.TypeUtil;

import java.util.List;

public class MapExpressionTransformer extends ExprTransformer<MapExpression> {
    public Expression transform(final MapExpression exp, CompilerTransformer compiler) {
        final List list = exp.getMapEntryExpressions();
        for (int i = 0; i != list.size(); ++i) {
            final MapEntryExpression me = (MapEntryExpression) list.get(i);
            MapEntryExpression nme = new MapEntryExpression(compiler.transform(me.getKeyExpression()), compiler.transform(me.getValueExpression()));
            nme.setSourcePosition(me);
            list.set(i, nme);
        }

        return new MyBytecodeExpr(exp);
}

    private static class MyBytecodeExpr extends BytecodeExpr {
        private final MapExpression exp;

        public MyBytecodeExpr(MapExpression exp) {
            super(exp, TypeUtil.LINKED_HASH_MAP_TYPE);
            this.exp = exp;
        }

        protected void compile() {
            final List list = exp.getMapEntryExpressions();
            mv.visitTypeInsn(NEW, "java/util/LinkedHashMap");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL,"java/util/LinkedHashMap","<init>","()V");
            for (int i = 0; i != list.size(); ++i) {
                mv.visitInsn(DUP);
                final MapEntryExpression me = (MapEntryExpression) list.get(i);
                final BytecodeExpr ke = (BytecodeExpr) me.getKeyExpression();
                ke.visit(mv);
                box(ke.getType());
                final BytecodeExpr ve = (BytecodeExpr) me.getValueExpression();
                ve.visit(mv);
                box(ve.getType());
                mv.visitMethodInsn(INVOKEVIRTUAL,"java/util/LinkedHashMap","put","(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
                mv.visitInsn(POP);
            }
        }
    }
}