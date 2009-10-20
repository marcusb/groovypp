package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.expr.SpreadExpression;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.objectweb.asm.MethodVisitor;

public class BytecodeSpreadExpr extends BytecodeExpr {
    private final BytecodeExpr internal;

    public BytecodeSpreadExpr(SpreadExpression exp, BytecodeExpr internal) {
        super(exp, internal.getType());
        this.internal = internal;
    }

    protected void compile(MethodVisitor mv) {
        internal.visit(mv);
    }
}
