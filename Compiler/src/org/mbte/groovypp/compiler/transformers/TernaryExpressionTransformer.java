/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.InnerClassNode;
import org.codehaus.groovy.ast.expr.*;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class TernaryExpressionTransformer extends ExprTransformer<TernaryExpression>{
    public Expression transform(TernaryExpression exp, CompilerTransformer compiler) {
        InnerClassNode newType = new InnerClassNode(compiler.classNode, compiler.getNextClosureName(), ACC_PUBLIC|ACC_SYNTHETIC, ClassHelper.OBJECT_TYPE);
        newType.setInterfaces(new ClassNode[] {TypeUtil.TTERNARY});
        final UntransformedTernaryExpr untransformed = new UntransformedTernaryExpr(exp, newType);
        return untransformed.improve(compiler);
    }

    public static class UntransformedTernaryExpr extends BytecodeExpr {
        public TernaryExpression exp;

        public UntransformedTernaryExpr(TernaryExpression exp, ClassNode type) {
            super(exp, type);
            this.exp = exp;
        }

        protected void compile(MethodVisitor mv) {
            throw new UnsupportedOperationException();
        }

        public BytecodeExpr transform(CompilerTransformer compiler) {
            if (exp instanceof ElvisOperatorExpression) {
                return transfromElvis((ElvisOperatorExpression)exp, compiler);
            }
            else {
                return transfromTernary(exp, compiler);
            }
        }

        private BytecodeExpr transfromTernary(TernaryExpression te, CompilerTransformer compiler) {
            final Label elseLabel = new Label();
            final BytecodeExpr condition = compiler.transformLogical(te.getBooleanExpression().getExpression(), elseLabel, false);

            final BytecodeExpr trueE  = (BytecodeExpr) compiler.transform(te.getTrueExpression());
            final BytecodeExpr falseE = (BytecodeExpr) compiler.transform(te.getFalseExpression());

            final ClassNode type = TypeUtil.commonType(trueE.getType(), falseE.getType());

            final BytecodeExpr finalTrueE = compiler.cast(trueE, type);
            final BytecodeExpr finalFalseE = compiler.cast(falseE, type);

            return new BytecodeExpr(te, type) {
                protected void compile(MethodVisitor mv) {
                    condition.visit(mv);

                    finalTrueE.visit(mv);

                    Label endLabel = new Label();
                    mv.visitJumpInsn(GOTO, endLabel);

                    mv.visitLabel(elseLabel);

                    finalFalseE.visit(mv);

                    mv.visitLabel(endLabel);
                }
            };
        }

        private BytecodeExpr transfromElvis(ElvisOperatorExpression ee, CompilerTransformer compiler) {
            ee = (ElvisOperatorExpression) ee.transformExpression(compiler);
            final ElvisOperatorExpression eee = ee;
            final BytecodeExpr be = (BytecodeExpr) ee.getBooleanExpression().getExpression();
            final BytecodeExpr brunch = compiler.castToBoolean( new BytecodeExpr(be, be.getType()){
                protected void compile(MethodVisitor mv) {
                    be.visit(mv);
                    dup(be.getType(), mv);
                }
            }, ClassHelper.boolean_TYPE);
            return new Elvis(ee, eee, brunch);
        }

        public Expression improve(CompilerTransformer compiler) {
            if (exp instanceof ElvisOperatorExpression)
                return transform(compiler);

            final BytecodeExpr trueE  = (BytecodeExpr) compiler.transform(exp.getTrueExpression());
            final BytecodeExpr falseE = (BytecodeExpr) compiler.transform(exp.getFalseExpression());

            if(trueE instanceof MapExpressionTransformer.UntransformedMapExpr
            || trueE instanceof ListExpressionTransformer.UntransformedListExpr
            || trueE instanceof UntransformedTernaryExpr
            || falseE instanceof MapExpressionTransformer.UntransformedMapExpr
            || falseE instanceof ListExpressionTransformer.UntransformedListExpr
            || falseE instanceof UntransformedTernaryExpr) {
                final TernaryExpression newExp = new TernaryExpression(exp.getBooleanExpression(), trueE, falseE);
                newExp.setSourcePosition(exp);
                exp = newExp;
                return this;
            }
            else {
                return transform(compiler);
            }
        }

        private static class Elvis extends BytecodeExpr {
            private final ElvisOperatorExpression eee;
            private BytecodeExpr branch;

            public Elvis(ElvisOperatorExpression ee, ElvisOperatorExpression eee, BytecodeExpr branch) {
                super(ee, TypeUtil.commonType(ee.getBooleanExpression().getExpression().getType(), ee.getFalseExpression().getType()));
                this.eee = eee;
                this.branch = branch;
            }

            protected void compile(MethodVisitor mv) {
                branch.visit(mv);

                Label elseLabel = new Label();
                mv.visitJumpInsn(Opcodes.IFEQ, elseLabel);

                final BytecodeExpr be = (BytecodeExpr) eee.getBooleanExpression().getExpression();
                box (be.getType(), mv);
                cast(TypeUtil.wrapSafely(be.getType()), TypeUtil.wrapSafely(getType()), mv);
                unbox(getType(), mv);
                Label endLabel = new Label();
                mv.visitJumpInsn(GOTO, endLabel);

                mv.visitLabel(elseLabel);
                final BytecodeExpr falseExp = (BytecodeExpr) eee.getFalseExpression();
                pop(be.getType(), mv);
                falseExp.visit(mv);
                box (falseExp.getType(), mv);
                cast(TypeUtil.wrapSafely(falseExp.getType()), TypeUtil.wrapSafely(getType()), mv);
                unbox(getType(), mv);

                mv.visitLabel(endLabel);
            }
        }
    }
}
