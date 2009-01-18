package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.classgen.Verifier;
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

    public void addDefaultParameterMethods(ClassNode node) {
        super.addDefaultParameterMethods(node);
    }

    public void addPropertyMethods(PropertyNode node) {
        classNodeField.setProperty(this, node.getDeclaringClass());
        visitProperty(node);
    }

    @Override
    public void visitMethod(MethodNode node) {
    }
}
