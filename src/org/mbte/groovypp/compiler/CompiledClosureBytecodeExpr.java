package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.objectweb.asm.MethodVisitor;

public class CompiledClosureBytecodeExpr extends BytecodeExpr {
    private CompilerTransformer compiler;

    ClassNode delegateType;
    private final Parameter[] constrParams;

    public CompiledClosureBytecodeExpr(CompilerTransformer compiler, ClosureExpression ce, ClassNode newType) {
        super(ce, newType);
        this.compiler = compiler;

        ClosureUtil.addFields(ce, newType, compiler);

        constrParams = ClosureUtil.createClosureConstructorParams(newType, compiler);

        compiler.pendingClosures.add(this);
    }

    protected void compile(MethodVisitor mv) {
        ClosureUtil.createClosureConstructor(getType(), constrParams, null, compiler);
        ClosureUtil.instantiateClass(getType(), compiler, constrParams, null, mv);
    }

    public static Expression createCompiledClosureBytecodeExpr(final CompilerTransformer transformer, final ClosureExpression ce) {
        final ClassNode newType = ce.getType();
        return new CompiledClosureBytecodeExpr(transformer, ce, newType);
    }
}
