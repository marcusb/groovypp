package org.mbte.groovypp.compiler.impl.expressions;

import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.mbte.groovypp.compiler.impl.CompilerTransformer;
import org.mbte.groovypp.compiler.impl.bytecode.BytecodeExpr;

public class ConstructorCallExpressionTransformer extends ExprTransformer<ConstructorCallExpression>{

    public Expression transform(ConstructorCallExpression exp, CompilerTransformer compiler) {
        return new BytecodeExpr(exp, exp.getType()) {
            protected void compile() {
                final String classInternalName = BytecodeHelper.getClassInternalName(getType());
                mv.visitTypeInsn(NEW, classInternalName);
                mv.visitInsn(DUP);
                mv.visitMethodInsn(INVOKESPECIAL, classInternalName, "<init>", "()V");
            }
        };
    }
}
