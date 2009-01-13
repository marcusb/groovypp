package org.mbte.groovypp.compiler.impl.expressions;

import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.mbte.groovypp.compiler.impl.BytecodeExpr;
import org.mbte.groovypp.compiler.impl.CompilerTransformer;

public class VariableExpressionTransformer extends ExprTransformer<VariableExpression> {
    public Expression transform(final VariableExpression exp, final CompilerTransformer compiler) {
            if (exp.isThisExpression())
              return new This(exp, compiler);

            final org.codehaus.groovy.classgen.Variable var = compiler.compileStack.getVariable(exp.getName(), false);
            if (var != null) {
                ClassNode vtype = compiler.getLocalVarInferenceTypes().get(exp);
                return new Var(exp, vtype, var);
            }
            else
                if (exp.isClosureSharedVariable()) {
                    VariableExpression v = exp;
                    while (v != null && v.getType() == ClassHelper.OBJECT_TYPE && v != v.getAccessedVariable())
                        v = (VariableExpression) v.getAccessedVariable();

                    final ClassNode vtype = v == null ? ClassHelper.OBJECT_TYPE : v.getType();
                    return new ClosureShared(exp, vtype, compiler);
                }
                else if (exp.getAccessedVariable() != null) {
                    VariableExpression v = exp;
                    while (v != null && v.getType() == ClassHelper.OBJECT_TYPE && v != v.getAccessedVariable())
                        v = (VariableExpression) v.getAccessedVariable();

                    final ClassNode vtype = v == null ? ClassHelper.OBJECT_TYPE : v.getType();
                    return new ClosureShared2(exp, vtype, compiler);
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

    private static class ClosureShared extends BytecodeExpr {
        private final VariableExpression exp;
        private final ClassNode vtype;
        private final CompilerTransformer compiler;

        public ClosureShared(VariableExpression exp, ClassNode vtype, CompilerTransformer compiler) {
            super(exp, vtype);
            this.exp = exp;
            this.vtype = vtype;
            this.compiler = compiler;
        }

        public void compile() {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(
                    GETFIELD,
                    BytecodeHelper.getClassInternalName(compiler.methodNode.getParameters()[0].getType()),
                    exp.getName(),
                    "Lgroovy/lang/Reference;");
            mv.visitMethodInsn(INVOKEVIRTUAL, "groovy/lang/Reference", "get", "()Ljava/lang/Object;");
            mv.visitTypeInsn(CHECKCAST, BytecodeHelper.getClassInternalName(ClassHelper.getWrapper(vtype)));
            unbox(vtype);
        }
    }

    private static class ClosureShared2 extends BytecodeExpr {
        private final VariableExpression exp;
        private final ClassNode vtype;
        private final CompilerTransformer compiler;

        public ClosureShared2(VariableExpression exp, ClassNode vtype, CompilerTransformer compiler) {
            super(exp, vtype);
            this.exp = exp;
            this.vtype = vtype;
            this.compiler = compiler;
        }

        public void compile() {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(
                    GETFIELD,
                    BytecodeHelper.getClassInternalName(compiler.classNode),
                    exp.getName(),
                    BytecodeHelper.getTypeDescription(vtype));
            unbox(vtype);
        }
    }
}