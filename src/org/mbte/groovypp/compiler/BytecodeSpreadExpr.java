package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.expr.SpreadExpression;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;

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
