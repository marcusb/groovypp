package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.Parameter;
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

    public static class Dependent extends ClosureMethodNode {
        private ClosureMethodNode master;

        public Dependent(ClosureMethodNode master, String name, int modifiers, ClassNode returnType, Parameter[] parameters, Statement code) {
            super(name, modifiers, returnType, parameters, code);
            this.master = master;
        }

        public ClosureMethodNode getMaster() {
            return master;
        }

        public String getTypeDescriptor() {
            StringBuilder buf = new StringBuilder(master.getName().length()+20);
            buf.append(master.getReturnType().getName());
            buf.append(' ');
            buf.append(master.getName());
            buf.append("()");
            return buf.toString();
        }

        @Override
        public ClassNode getReturnType() {
            return master.getReturnType();
        }
    }
}
