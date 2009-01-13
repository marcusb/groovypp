package org.mbte.groovypp.compiler.impl;

import org.codehaus.groovy.ast.expr.SpreadExpression;

public class BytecodeSpreadExpr extends BytecodeExpr {
    private final BytecodeExpr internal;

    public BytecodeSpreadExpr(SpreadExpression exp, BytecodeExpr internal) {
        super(exp, internal.getType());
        this.internal = internal;
    }

    protected void compile() {
        internal.visit(mv);
    }
}
