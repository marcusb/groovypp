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
import org.mbte.groovypp.compiler.bytecode.ResolvedMethodBytecodeExpr;
import org.objectweb.asm.MethodVisitor;

public class PrefixExpressionTransformer extends ExprTransformer<PrefixExpression> {
    public Expression transform(final PrefixExpression exp, final CompilerTransformer compiler) {
        final Expression operand = exp.getExpression();

        final BytecodeExpr oper = (BytecodeExpr) compiler.transform(operand);

        // It's safe to search for unboxing first as long as we have a limited set of classes to apply transparent
        //  dereferencing.
        final MethodNode unboxing = TypeUtil.getReferenceUnboxingMethod(oper.getType());
        if (unboxing != null) {
            final ClassNode t = TypeUtil.getSubstitutedType(unboxing.getReturnType(), unboxing.getDeclaringClass(),
                    oper.getType());
            final MethodNode boxing = TypeUtil.getReferenceBoxingMethod(oper.getType(), t);
            if (boxing != null) {
                return new BytecodeExpr(exp, t) {
                    protected void compile(MethodVisitor mv) {
                        oper.visit(mv);

                        final VariableExpression ref = new VariableExpression(compiler.context.getNextTempVarName());
                        compiler.compileStack.defineVariable(ref, true);
                        final BytecodeExpr refTransformed = (BytecodeExpr) compiler.transform(ref);

                        final ResolvedMethodBytecodeExpr unboxingCall = ResolvedMethodBytecodeExpr.create(exp, unboxing, refTransformed, new ArgumentListExpression(),
                                compiler);
                        compiler.cast(unboxingCall, t).visit(mv);

                        final VariableExpression v1 = new VariableExpression(compiler.context.getNextTempVarName(), t);
                        compiler.compileStack.defineVariable(v1, true);

                        final BytecodeExpr prefix = ((BytecodeExpr) compiler.transform(v1)).createPrefixOp(exp,
                                exp.getOperation().getType(), compiler);

                        ResolvedMethodBytecodeExpr.create(exp, boxing, refTransformed,
                                new ArgumentListExpression(prefix), compiler).visit(mv);

                        ((BytecodeExpr) compiler.transform(v1)).visit(mv);
                    }
                };

            }
        }

        return oper.createPrefixOp(exp, exp.getOperation().getType(), compiler);
    }
}
