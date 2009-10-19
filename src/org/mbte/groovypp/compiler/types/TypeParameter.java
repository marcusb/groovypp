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

class PerClassNodeCache {
    private WeakHashMap<ClassNode, TypeParameter[]> typeParameters = new WeakHashMap<ClassNode, TypeParameter[]>();
    private WeakHashMap<ClassNode, Type> types = new WeakHashMap<ClassNode, Type>();

    private PerClassNodeCache() {}

    public static PerClassNodeCache INSTANCE = new PerClassNodeCache();

    public synchronized TypeParameter[] getTypeParameters(ClassNode node) {
        if (!typeParameters.containsKey(node)) {
            TypeParameter[] result = new TypeParameter[node.getTypeClass().getTypeParameters().length];
            for (int i = 0; i < result.length; i++) {
                result[i] = new TypeParameter(node, i);
            }
            typeParameters.put(node, result);
        }
        return typeParameters.get(node);
    }

    public synchronized Type getType(ClassNode node) {
        Type type = types.get(node);
        if (type == null) {
            type = createType(node);
            types.put(node, type);
        }
        return type;
    }

    private Type createType(ClassNode node) {
        ClassNode redirect = node.redirect();
        GenericsType[] genericsTypes = node.getGenericsTypes();
        TypeVariable[] typeVariables = redirect.getTypeClass().getTypeParameters();
        if (typeVariables.length == 0 || typeVariables.length != genericsTypes.length) return new ClassType(redirect);
        Map<TypeParameter, Type> bindings = new HashMap<TypeParameter, Type>();
        for (int i = 0; i < genericsTypes.length; i++) {
            TypeParameter parameter = new TypeParameter(redirect, i);
            GenericsType genericsType = genericsTypes[i];
            if (genericsType.isWildcard()) {
                ClassNode boundNode = genericsType.getLowerBound();
                if (boundNode == null) {
                    ClassNode[] upperBounds = genericsType.getUpperBounds();
                    if (upperBounds != null) boundNode = upperBounds[0];
                }
                Type boundType = boundNode != null ? createType(boundNode) : null;
                bindings.put(parameter, new WildcardType(boundType, genericsType.getLowerBound() == null));
            } else {
                bindings.put(parameter, createType(genericsType.getType()));
            }
        }
        return new ParameterizedType(redirect, new SubstitutorImpl(bindings));
    }
}