package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.stmt.Statement;
import org.objectweb.asm.Opcodes;

public class ClosureClassNode extends ClassNode {
    private final ClassNode owner;

    public ClosureClassNode(ClassNode owner, String name) {
        super(name, Opcodes.ACC_PUBLIC|Opcodes.ACC_FINAL, ClassHelper.CLOSURE_TYPE, ClassNode.EMPTY_ARRAY, null);
        this.owner = owner;
    }

    public ClassNode getOwner() {
        return owner;
    }
}