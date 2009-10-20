package org.mbte.groovypp.compiler.types;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.GenericsType;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author ven
 */
class PerClassNodeCache {
    private WeakHashMap<ClassNode, TypeParameter[]> typeParameters = new WeakHashMap<ClassNode, TypeParameter[]>();
    private WeakHashMap<ClassNode, Type> types = new WeakHashMap<ClassNode, Type>();

    private PerClassNodeCache() {}

    public static PerClassNodeCache INSTANCE = new PerClassNodeCache();

    public synchronized TypeParameter[] getTypeParameters(ClassNode node) {
        if (!typeParameters.containsKey(node)) {
            GenericsType[] genericsTypes = node.getGenericsTypes();
            if (genericsTypes == null || genericsTypes.length == 0) return TypeParameter.EMPTY_ARRAY;
            TypeParameter[] result = new TypeParameter[genericsTypes.length];
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
        GenericsType[] typeArgs = node.getGenericsTypes();
        GenericsType[] typeVars = redirect.getGenericsTypes();
        if (typeVars.length == 0 || typeVars.length != typeArgs.length) return new ClassType(redirect);
        Map<TypeParameter, Type> bindings = new HashMap<TypeParameter, Type>();
        for (int i = 0; i < typeArgs.length; i++) {
            TypeParameter parameter = new TypeParameter(redirect, i);
            GenericsType genericsType = typeArgs[i];
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
