package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.syntax.Types;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.objectweb.asm.MethodVisitor;

public class PostfixExpressionTransformer extends ExprTransformer<PostfixExpression> {
    public Expression transform(final PostfixExpression exp, CompilerTransformer compiler) {
        final Expression operand = exp.getExpression();

        final BytecodeExpr oper = (BytecodeExpr) compiler.transform(operand);
        return oper.createPostfixOp(exp, exp.getOperation().getType(), compiler);
    }
}
