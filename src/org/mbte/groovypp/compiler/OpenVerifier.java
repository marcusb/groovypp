package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.classgen.Verifier;
import org.codehaus.groovy.classgen.BytecodeSequence;
import org.codehaus.groovy.reflection.CachedField;
import org.codehaus.groovy.reflection.ReflectionCache;

class OpenVerifier extends Verifier {
    static CachedField classNodeField = null;

    static {
        for (CachedField f : ReflectionCache.getCachedClass(Verifier.class).getFields()) {
            if (f.getName().equals("classNode")) {
                classNodeField = f;
                break;
            }
        }
    }

    @Override
    public void visitClass(ClassNode node) {
        super.visitClass(node);

        for (FieldNode fieldNode : node.getFields()) {
            fieldNode.setInitialValueExpression(null);
        }
    }

    public void addPropertyMethods(PropertyNode node) {
        classNodeField.setProperty(this, node.getDeclaringClass());
        visitProperty(node);
    }

    public void visitMethod(MethodNode node) {
    }

    protected void addInitialization(ClassNode node, ConstructorNode constructorNode) {
        if (constructorNode.getCode() instanceof BytecodeSequence)
            return;

        super.addInitialization(node, constructorNode);
    }
}
