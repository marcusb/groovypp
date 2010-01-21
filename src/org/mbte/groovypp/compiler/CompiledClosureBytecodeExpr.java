package org.mbte.groovypp.compiler;

import groovy.lang.TypePolicy;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.objectweb.asm.MethodVisitor;

public class CompiledClosureBytecodeExpr extends BytecodeExpr {
    private CompilerTransformer compiler;

    public CompiledClosureBytecodeExpr(CompilerTransformer compiler, ClosureExpression ce, ClassNode newType) {
        super(ce, newType);
        this.compiler = compiler;

        ClosureUtil.addFields(ce, newType, compiler);

        compiler.pendingClosures.add(this);
    }

    protected void compile(MethodVisitor mv) {
        ClassNode type = getType();
        if (compiler.policy == TypePolicy.STATIC && !compiler.context.isOuterClassInstanceUsed(type) &&
                type.getDeclaredField("this$0") != null /* todo: remove this check */) {
            type.removeField("this$0");
        }
        Parameter[] constrParams = ClosureUtil.createClosureConstructorParams(type, compiler);
        ClosureUtil.createClosureConstructor(type, constrParams, null, compiler);
        ClosureUtil.instantiateClass(type, compiler, constrParams, null, mv);
    }

    public static Expression createCompiledClosureBytecodeExpr(final CompilerTransformer transformer, final ClosureExpression ce) {
        final ClassNode newType = ce.getType();
        return new CompiledClosureBytecodeExpr(transformer, ce, newType);
    }
}
