package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.expr.ElvisOperatorExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.TernaryExpression;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.StaticCompiler;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.objectweb.asm.Label;

public class TernaryExpressionTransformer extends ExprTransformer<TernaryExpression>{
    public Expression transform(TernaryExpression exp, CompilerTransformer compiler) {
        if (exp instanceof ElvisOperatorExpression) {
            return transfromElvis((ElvisOperatorExpression)exp, compiler);
        }
        else {
            return transfromTernary(exp, compiler);
        }
    }
    private Expression transfromTernary(TernaryExpression te, CompilerTransformer compiler) {
        te = (TernaryExpression) te.transformExpression(compiler);
        final TernaryExpression fte = te;
        return new Ternary(te, fte);
    }

    private Expression transfromElvis(ElvisOperatorExpression ee, CompilerTransformer compiler) {
        ee = (ElvisOperatorExpression) ee.transformExpression(compiler);
        final ElvisOperatorExpression eee = ee;
        return new Elvis(ee, eee);
    }

    private static class Ternary extends BytecodeExpr {
        private final TernaryExpression fte;

        public Ternary(TernaryExpression te, TernaryExpression fte) {
            super(te, TypeUtil.commonType(te.getTrueExpression().getType(), te.getFalseExpression().getType()));
            this.fte = fte;
        }

        protected void compile() {
            final BytecodeExpr be = (BytecodeExpr) fte.getBooleanExpression().getExpression();
            be.visit(mv);

            Label elseLabel = new Label();
            StaticCompiler.branch(be, IFEQ, elseLabel, mv);

            final BytecodeExpr trueExp = (BytecodeExpr) fte.getTrueExpression();
            trueExp.visit(mv);
            box (trueExp.getType());
            cast(ClassHelper.getWrapper(trueExp.getType()), ClassHelper.getWrapper(getType()));
            unbox(getType());
            Label endLabel = new Label();
            mv.visitJumpInsn(GOTO, endLabel);

            mv.visitLabel(elseLabel);
            final BytecodeExpr falseExp = (BytecodeExpr) fte.getFalseExpression();
            falseExp.visit(mv);
            box (falseExp.getType());
            cast(ClassHelper.getWrapper(falseExp.getType()), ClassHelper.getWrapper(getType()));
            unbox(getType());

            mv.visitLabel(endLabel);
        }
    }

    private static class Elvis extends BytecodeExpr {
        private final ElvisOperatorExpression eee;

        public Elvis(ElvisOperatorExpression ee, ElvisOperatorExpression eee) {
            super(ee, TypeUtil.commonType(ee.getBooleanExpression().getExpression().getType(), ee.getFalseExpression().getType()));
            this.eee = eee;
        }

        protected void compile() {
            final BytecodeExpr be = (BytecodeExpr) eee.getBooleanExpression().getExpression();
            be.visit(mv);
            dup(be.getType());

            Label elseLabel = new Label();
            StaticCompiler.branch(be, IFEQ, elseLabel, mv);

            box (be.getType());
            cast(ClassHelper.getWrapper(be.getType()), ClassHelper.getWrapper(getType()));
            unbox(getType());
            Label endLabel = new Label();
            mv.visitJumpInsn(GOTO, endLabel);

            mv.visitLabel(elseLabel);
            final BytecodeExpr falseExp = (BytecodeExpr) eee.getFalseExpression();
            pop(be.getType());
            falseExp.visit(mv);
            box (falseExp.getType());
            cast(ClassHelper.getWrapper(falseExp.getType()), ClassHelper.getWrapper(getType()));
            unbox(getType());

            mv.visitLabel(endLabel);
        }
    }
}
