package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.expr.*;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.objectweb.asm.MethodVisitor;

public class DeclarationExpressionTransformer extends ExprTransformer<DeclarationExpression> {
    public Expression transform(DeclarationExpression exp, final CompilerTransformer compiler) {
        if (!(exp.getLeftExpression() instanceof VariableExpression)) {
            compiler.addError("Variable name expected", exp);
        }

        final VariableExpression ve = (VariableExpression) exp.getLeftExpression();
        if (ve.getOriginType() != ve.getType())
            ve.setType(ve.getOriginType());

        if (!ve.isDynamicTyped()) {
            CastExpression cast = new CastExpression(ve.getType(), exp.getRightExpression());
            cast.setSourcePosition(exp.getRightExpression());
            exp.setRightExpression(cast);
        }

        BytecodeExpr right = (BytecodeExpr) compiler.transform(exp.getRightExpression());
        if (right.getType() == TypeUtil.NULL_TYPE && ClassHelper.isPrimitiveType(ve.getType())) {
            final ConstantExpression cnst = new ConstantExpression(0);
            cnst.setSourcePosition(exp);
            right = (BytecodeExpr) compiler.transform(cnst);
        }

        if (!ve.isDynamicTyped()) {
            right = compiler.cast(right, ve.getType());
            return new Static(exp, ve, right, compiler);
        } else {
            // let's try local type inference
            compiler.getLocalVarInferenceTypes().add(ve, TypeUtil.wrapSafely(right.getType()));
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

        protected void compile(MethodVisitor mv) {
            right.visit(mv);
            box(right.getType(), mv);
            unbox(ve.getType(), mv);
            dup(ve.getType(), mv);
            compiler.compileStack.defineVariable(ve, true);
        }
    }

    private static class Dynamic extends BytecodeExpr {
        private final BytecodeExpr right;
        private final CompilerTransformer compiler;
        private final VariableExpression ve;

        public Dynamic(DeclarationExpression exp, BytecodeExpr right, CompilerTransformer compiler, VariableExpression ve) {
            super(exp, TypeUtil.wrapSafely(right.getType()));
            this.right = right;
            this.compiler = compiler;
            this.ve = ve;
        }

        protected void compile(MethodVisitor mv) {
            right.visit(mv);
            box(right.getType(), mv);
            dup(TypeUtil.wrapSafely(right.getType()), mv);
            compiler.compileStack.defineVariable(ve, true);
        }
    }
}