package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;

public class DeclarationExpressionTransformer extends ExprTransformer<DeclarationExpression> {
    public Expression transform(DeclarationExpression exp, final CompilerTransformer compiler) {
        if (!(exp.getLeftExpression() instanceof VariableExpression)) {
            compiler.addError("Variable name expected", exp);
        }
        final VariableExpression ve = (VariableExpression) exp.getLeftExpression();
        if (ve.getOriginType() != ve.getType())
            ve.setType(ve.getOriginType());
        final BytecodeExpr right0 = (BytecodeExpr) compiler.transform(exp.getRightExpression());
        final BytecodeExpr right;
        if (right0.getType() != TypeUtil.NULL_TYPE || !ClassHelper.isPrimitiveType(ve.getType())) {
            right = compiler.cast(right0, ve.getType());
        } else {
            final ConstantExpression cnst = new ConstantExpression(0);
            cnst.setSourcePosition(exp);
            right = (BytecodeExpr) compiler.transform(cnst);
        }
        if (!ve.isDynamicTyped()) {
            return new Static(exp, ve, right, compiler);
        } else {
            // let's try local type inference
            compiler.getLocalVarInferenceTypes().add(ve, ClassHelper.getWrapper(right.getType()));
            return new Dynamic(exp, right, compiler, ve);
        }
    }

    private static class Static extends BytecodeExpr {
        private final VariableExpression ve;
        private final BytecodeExpr right;
        private final CompilerTransformer compiler;

        public Static(DeclarationExpression exp, VariableExpression ve, BytecodeExpr right, CompilerTransformer compiler) {
            super(exp, ve.getType());
            this.ve = ve;
            this.right = right;
            this.compiler = compiler;
        }

        protected void compile() {
            right.visit(mv);
            box(right.getType());
            unbox(ve.getType());
            dup(ve.getType());
            compiler.compileStack.defineVariable(ve, true);
        }
    }

    private static class Dynamic extends BytecodeExpr {
        private final BytecodeExpr right;
        private final CompilerTransformer compiler;
        private final VariableExpression ve;

        public Dynamic(DeclarationExpression exp, BytecodeExpr right, CompilerTransformer compiler, VariableExpression ve) {
            super(exp, ClassHelper.getWrapper(right.getType()));
            this.right = right;
            this.compiler = compiler;
            this.ve = ve;
        }

        protected void compile() {
            right.visit(mv);
            box(right.getType());
            dup(ClassHelper.getWrapper(right.getType()));
            compiler.compileStack.defineVariable(ve, true);
        }
    }
}