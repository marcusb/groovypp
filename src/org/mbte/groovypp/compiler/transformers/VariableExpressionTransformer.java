package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;

public class VariableExpressionTransformer extends ExprTransformer<VariableExpression> {
    public Expression transform(final VariableExpression exp, final CompilerTransformer compiler) {
        if (exp.isThisExpression()) {
          if (!compiler.methodNode.isStatic())
             return new This(exp, compiler);
          else {
             if (compiler.methodNode.getName().equals("$doCall")) {
                 return compiler.transform(new VariableExpression("$self"));
             }
             else {
                 compiler.addError("Can't use 'this' in static method", exp);
                 return null;
             }
          }
        }

        final org.codehaus.groovy.classgen.Variable var = compiler.compileStack.getVariable(exp.getName(), false);

        if (var == null) {
            if (exp.isClosureSharedVariable()) {
                // we are in closure
                final VariableExpression ve = new VariableExpression("$self");
                final PropertyExpression pe = new PropertyExpression(ve, exp.getName());
                pe.setType(exp.getType());
                pe.setSourcePosition(exp);
                return compiler.transform(pe);
            }
            else if (exp.getAccessedVariable() != null) {
                // it is property
                final PropertyExpression pe = new PropertyExpression(VariableExpression.THIS_EXPRESSION, exp.getName());
                pe.setImplicitThis(true);
                pe.setSourcePosition(exp);
                return compiler.transform(pe);
            }
        } else {
            ClassNode vtype = compiler.getLocalVarInferenceTypes().get(exp);
            return new Var(exp, vtype, var);
        }

        compiler.addError("Can't find variable " + exp.getName(), exp);
            return null;
    }

    private static class This extends BytecodeExpr {
        public This(VariableExpression exp, CompilerTransformer compiler) {
            super(exp, compiler.classNode);
        }

        public void compile() {
            mv.visitVarInsn(ALOAD, 0);
        }
    }

    private static class Var extends BytecodeExpr {
        private final org.codehaus.groovy.classgen.Variable var;

        public Var(VariableExpression exp, ClassNode vtype, org.codehaus.groovy.classgen.Variable var) {
            super(exp, vtype != null ? vtype : var.getType());
            this.var = var;
        }

        protected void compile() {
            loadVar(var);
//                    if (!vtype.equals(var.getType()))
//                       mv.visitTypeInsn(CHECKCAST, BytecodeHelper.getClassInternalName(vtype));
            unbox(getType());
        }
    }
}