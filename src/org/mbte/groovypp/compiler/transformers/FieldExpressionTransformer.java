package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.FieldExpression;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.mbte.groovypp.compiler.bytecode.ResolvedFieldBytecodeExpr;
import org.objectweb.asm.MethodVisitor;

public class FieldExpressionTransformer extends ExprTransformer<FieldExpression> {

    public Expression transform(final FieldExpression exp, CompilerTransformer compiler) {
        return new ResolvedFieldBytecodeExpr(
                exp,
                exp.getField(),
                new BytecodeExpr(exp, compiler.classNode) {
                    public boolean isThis() {
                        return true;
                    }

                    protected void compile(MethodVisitor mv) {
                        if (!exp.getField().isStatic())
                            mv.visitVarInsn(ALOAD, 0);
                        else
                            mv.visitInsn(ACONST_NULL);
                    }
                },
                null,
                compiler);
    }
}