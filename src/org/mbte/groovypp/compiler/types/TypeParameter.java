package org.mbte.groovypp.compiler.types;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.GenericsType;

import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author ven
 */
public class TypeParameter {
    public ClassNode classNode;
    public int index;

    public static final TypeParameter[] EMPTY_ARRAY = new TypeParameter[0];

    public TypeParameter(ClassNode classNode, int index) {
        this.classNode = classNode;
        this.index = index;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TypeParameter)) return false;

        TypeParameter that = (TypeParameter) o;

        if (index != that.index) return false;
        if (!classNode.equals(that.classNode)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = classNode.hashCode();
        result = 31 * result + index;
        return result;
    }
}

