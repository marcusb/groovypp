package org.mbte.groovypp.compiler.impl;

import org.codehaus.groovy.ast.Variable;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.VariableExpression;

class VarInfo  {
    final Variable declaration;
    final ClosureExpression closure;
    private ClassNode type;
    private boolean isShared;

    public VarInfo(ClosureExpression closure, Variable declaration) {
        this.closure = closure;
        this.declaration = declaration;
        this.type = declaration.getType();

        if (declaration instanceof VariableExpression) {
            VariableExpression ve = (VariableExpression) declaration;
//            ve.setAccessedVariable(this);
            isShared = ve.isClosureSharedVariable();
        }
    }

    public ClassNode getType() {
        return type;
    }

    public ClassNode getOriginType() {
        throw new UnsupportedOperationException();
    }

    public String getName() {
        return declaration.getName();
    }

    public Expression getInitialExpression() {
        throw new UnsupportedOperationException();
    }

    public boolean hasInitialExpression() {
        throw new UnsupportedOperationException();
    }

    public boolean isInStaticContext() {
        return declaration.isInStaticContext();
    }

    public boolean isDynamicTyped() {
        return declaration.isDynamicTyped();
    }

    public boolean isClosureSharedVariable() {
        return isShared;
    }

    public void setClosureSharedVariable(boolean inClosure) {
        isShared = true;
    }
}
