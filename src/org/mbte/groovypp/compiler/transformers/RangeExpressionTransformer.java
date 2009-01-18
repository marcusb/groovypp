package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.RangeExpression;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.runtime.ScriptBytecodeAdapter;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;

public class RangeExpressionTransformer extends ExprTransformer<RangeExpression> {

    public Expression transform(final RangeExpression exp, CompilerTransformer compiler) {
        final BytecodeExpr from = (BytecodeExpr) compiler.transform(exp.getFrom());
        final BytecodeExpr to = (BytecodeExpr) compiler.transform(exp.getTo());
        return new BytecodeExpr(exp, ClassHelper.LIST_TYPE) {
            protected void compile() {
                from.visit(mv);
                box(from.getType());
                to.visit(mv);
                box(to.getType());
                mv.visitLdcInsn(exp.isInclusive());
                mv.visitMethodInsn(INVOKESTATIC, BytecodeHelper.getClassInternalName(ScriptBytecodeAdapter.class), "createRange", "(Ljava/lang/Object;Ljava/lang/Object;Z)Ljava/util/List;");
            }
        };
    }
}
