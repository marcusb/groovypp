package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.stmt.Statement;

public class ClosureMethodNode extends MethodNode {
    private ClosureMethodNode owner;

    public ClosureMethodNode(String name, int modifiers, ClassNode returnType, Parameter[] parameters, Statement code) {
        super(name, modifiers, returnType, parameters, ClassNode.EMPTY_ARRAY, code);
    }

    public ClosureMethodNode getOwner() {
        return owner;
    }

    public void setOwner(ClosureMethodNode owner) {
        this.owner = owner;
    }
}
