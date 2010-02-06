package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.InnerClassNode;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.codehaus.groovy.ast.expr.MapExpression;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.objectweb.asm.MethodVisitor;

import java.util.List;

public class MapExpressionTransformer extends ExprTransformer<MapExpression> {
    public Expression transform(final MapExpression exp, final CompilerTransformer compiler) {
        InnerClassNode newType = new InnerClassNode(compiler.classNode, compiler.getNextClosureName(), ACC_PUBLIC|ACC_SYNTHETIC, ClassHelper.OBJECT_TYPE);
        newType.setModule(compiler.classNode.getModule());
        newType.setInterfaces(new ClassNode[] {TypeUtil.TMAP});
        return new UntransformedMapExpr(exp, newType);
    }

    public static class UntransformedMapExpr extends BytecodeExpr {
        public final MapExpression exp;

        public UntransformedMapExpr(MapExpression exp, ClassNode type) {
            super(exp, type);
            this.exp = exp;
        }

        protected void compile(MethodVisitor mv) {
            throw new UnsupportedOperationException();
        }

        public BytecodeExpr transform(CompilerTransformer compiler) {
            return new TransformedMapExpr(exp, compiler);
        }
    }

    public static class TransformedMapExpr extends BytecodeExpr {
        private final MapExpression exp;

        public TransformedMapExpr(MapExpression exp, CompilerTransformer compiler) {
            super(exp, TypeUtil.EX_LINKED_HASH_MAP_TYPE);
            this.exp = exp;

            final List<MapEntryExpression> list = exp.getMapEntryExpressions();
            if (list.size() > 0) {
                ClassNode keyArg = TypeUtil.NULL_TYPE;
                ClassNode valueArg = TypeUtil.NULL_TYPE;
                for (int i = 0; i != list.size(); ++i) {
                    final MapEntryExpression me = list.get(i);
                    final Expression key = transformInnerListsAndMaps(compiler.transform(me.getKeyExpression()), compiler);
                    final Expression value = transformInnerListsAndMaps(compiler.transform(me.getValueExpression()), compiler);
                    MapEntryExpression nme = new MapEntryExpression(key, value);
                    keyArg = TypeUtil.commonType(keyArg, nme.getKeyExpression().getType());
                    valueArg = TypeUtil.commonType(valueArg, nme.getValueExpression().getType());
                    nme.setSourcePosition(me);
                    list.set(i, nme);
                }
                if (keyArg == TypeUtil.NULL_TYPE) keyArg = ClassHelper.OBJECT_TYPE;
                if (valueArg == TypeUtil.NULL_TYPE) valueArg = ClassHelper.OBJECT_TYPE;
                setType(TypeUtil.withGenericTypes(getType(), keyArg, valueArg));
            }
        }

        private Expression transformInnerListsAndMaps(Expression expr, CompilerTransformer compiler) {
            if (expr instanceof ListExpressionTransformer.UntransformedListExpr)
                expr = ((ListExpressionTransformer.UntransformedListExpr) expr).transform(TypeUtil.ARRAY_LIST_TYPE, compiler);
            if (expr instanceof UntransformedMapExpr)
                expr = ((UntransformedMapExpr) expr).transform(compiler);
            return expr;
        }

        protected void compile(MethodVisitor mv) {
            final List<MapEntryExpression> list = exp.getMapEntryExpressions();
            mv.visitTypeInsn(NEW, "org/mbte/groovypp/runtime/LinkedHashMapEx");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL,"org/mbte/groovypp/runtime/LinkedHashMapEx","<init>","()V");
            for (int i = 0; i != list.size(); ++i) {
                mv.visitInsn(DUP);
                final MapEntryExpression me = list.get(i);
                final BytecodeExpr ke = (BytecodeExpr) me.getKeyExpression();
                ke.visit(mv);
                box(ke.getType(), mv);
                final BytecodeExpr ve = (BytecodeExpr) me.getValueExpression();
                ve.visit(mv);
                box(ve.getType(), mv);
                mv.visitMethodInsn(INVOKEVIRTUAL,"org/mbte/groovypp/runtime/LinkedHashMapEx","put","(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
                mv.visitInsn(POP);
            }
        }
    }
}
