package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.objectweb.asm.Opcodes;

public class ClosureClassNode extends ClassNode {
    private final ClassNode owner;
    private ClosureMethodNode doCallMethod;
    private ClosureExpression closureExpression;

    public ClosureClassNode(ClassNode owner, String name) {
        super(name, Opcodes.ACC_PUBLIC|Opcodes.ACC_FINAL, ClassHelper.CLOSURE_TYPE, ClassNode.EMPTY_ARRAY, null);
        this.owner = owner;
    }

    public ClassNode getOwner() {
        return owner;
    }

    public void setDoCallMethod(ClosureMethodNode doCallMethod) {
        this.doCallMethod = doCallMethod;
    }

    public ClosureMethodNode getDoCallMethod() {
        return doCallMethod;
    }

    public void setClosureExpression(ClosureExpression code) {
        this.closureExpression = code;
    }

    public ClosureExpression getClosureExpression() {
        return closureExpression;
    }
}