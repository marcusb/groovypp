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
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.InnerClassNode;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.Register;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.mbte.groovypp.compiler.bytecode.ResolvedVarBytecodeExpr;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class VariableExpressionTransformer extends ExprTransformer<VariableExpression> {
    public Expression transform(final VariableExpression exp, final CompilerTransformer compiler) {
        if (exp.isThisExpression()) {
            if (!compiler.methodNode.isStatic())
                return new This(exp, compiler);
            else {
                if (compiler.methodNode.getName().equals("$doCall")) {
                    return new Self(exp, compiler);
                } else {
//                 compiler.addError("Cannot use 'this' in static method", exp);
                    return ClassExpressionTransformer.newExpr(exp, compiler.classNode);
                }
            }
        }

        if (exp.isSuperExpression()) {
            if (!compiler.methodNode.isStatic())
                return new Super(exp, compiler);
            else {
                if (compiler.methodNode.getName().equals("$doCall")) {
                    return new Self(exp, compiler);
                } else {
//                 compiler.addError("Cannot use 'this' in static method", exp);
                    return ClassExpressionTransformer.newExpr(exp, compiler.classNode);
                }
            }
        }

        final Register var = compiler.compileStack.getRegister(exp.getName(), false);

        if (var == null) {
            if (exp.isClosureSharedVariable()) {
                // we are in closure
                final VariableExpression ve = VariableExpression.THIS_EXPRESSION;
                final PropertyExpression pe = new PropertyExpression(ve, exp.getName());
                pe.setType(exp.getType());
                pe.setSourcePosition(exp);
                return compiler.transform(pe);
            } else if (exp.getAccessedVariable() != null) {
                String name = exp.getName();
                if (exp.getAccessedVariable() instanceof VariableExpression && DeclarationExpressionTransformer.hasFieldAnnotation((VariableExpression)exp.getAccessedVariable())) {
                    // @Field
                    FieldNode fieldNode = (FieldNode) ((VariableExpression)exp.getAccessedVariable()).getAccessedVariable();
                    name = fieldNode.getName();
                }
                PropertyExpression pe = new PropertyExpression(VariableExpression.THIS_EXPRESSION, name);
                pe.setImplicitThis(true);
                pe.setSourcePosition(exp);
                return compiler.transform(pe);
            }
        } else {
            ClassNode vtype = compiler.getLocalVarInferenceTypes().get(exp);
            if (vtype == null)
                vtype = var.getType();
            if (var.getIndex() == 0 && var.getName().equals("$self"))
                return new Self(exp, compiler);
            else
                return new ResolvedVarBytecodeExpr(vtype, exp, compiler);
        }

        compiler.addError("Cannot find variable " + exp.getName(), exp);
        return null;
    }

    private static class ThisBase extends BytecodeExpr {
        public ThisBase(VariableExpression exp, ClassNode type) {
            super(exp, type);
        }

        public void compile(MethodVisitor mv) {
            mv.visitVarInsn(ALOAD, 0);
        }
    }

    public static class This extends ThisBase {
        public This(VariableExpression exp, CompilerTransformer compiler) {
            super(exp, getThisType(compiler));
        }

        private static ClassNode getThisType(CompilerTransformer compiler) {
            final ClassNode classNode = compiler.classNode;
            if (hasChoiceOfThis(classNode)) {
                InnerClassNode newType = new InnerClassNode(classNode, compiler.getNextClosureName(),
                        ACC_PUBLIC|ACC_SYNTHETIC, ClassHelper.OBJECT_TYPE);
                newType.setInterfaces(new ClassNode[] {TypeUtil.TTHIS});
                return newType;
            } else {
                return classNode;
            }
        }

        private static boolean hasChoiceOfThis(ClassNode classNode) {
            return classNode instanceof InnerClassNode && (classNode.getModifiers() & Opcodes.ACC_STATIC) == 0;
        }

        @Override
        public boolean isThis() {
            return true;
        }
    }

    public static class ThisSpecial extends This {
        public ThisSpecial(VariableExpression exp, CompilerTransformer compiler) {
            super(exp, compiler);
        }
    }

    public static class Super extends ThisBase {
        public Super(VariableExpression exp, CompilerTransformer compiler) {
            super(exp, compiler.classNode.getSuperClass());
        }
    }

    public static class Self extends ThisBase {
        public Self(VariableExpression exp, CompilerTransformer compiler) {
            super(exp, compiler.methodNode.getParameters()[0].getType());
        }

        @Override
        public boolean isThis() {
            return true;
        }
    }
}
