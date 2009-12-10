package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.Register;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.mbte.groovypp.compiler.bytecode.ResolvedVarBytecodeExpr;
import org.objectweb.asm.MethodVisitor;

public class VariableExpressionTransformer extends ExprTransformer<VariableExpression> {
    public Expression transform(final VariableExpression exp, final CompilerTransformer compiler) {
        if (exp.isThisExpression()) {
            if (!compiler.methodNode.isStatic())
                return new This(exp, compiler);
            else {
                if (compiler.methodNode.getName().equals("$doCall")) {
                    return new Self(exp, compiler);
                } else {
//                 compiler.addError("Can't use 'this' in static method", exp);
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
//                 compiler.addError("Can't use 'this' in static method", exp);
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
            return new ResolvedVarBytecodeExpr(vtype, exp, compiler);
        }

        compiler.addError("Can't find variable " + exp.getName(), exp);
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
            super(exp, compiler.classNode);
        }

        @Override
        public boolean isThis() {
            return true;
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