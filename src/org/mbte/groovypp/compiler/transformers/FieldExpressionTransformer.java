package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.expr.ArrayExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.FieldExpression;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.bytecode.ResolvedFieldBytecodeExpr;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;

import java.util.List;
import java.util.Iterator;

public class FieldExpressionTransformer extends ExprTransformer<FieldExpression> {

    public Expression transform(FieldExpression exp, CompilerTransformer compiler) {
        return new ResolvedFieldBytecodeExpr (
                exp,
                exp.getField(),
                new BytecodeExpr(exp, compiler.classNode) {
                    protected void compile() {
                        mv.visitVarInsn(ALOAD, 0);
                    }
                },
                null,
                true
        );
    }
}