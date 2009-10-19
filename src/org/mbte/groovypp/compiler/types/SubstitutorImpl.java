package org.mbte.groovypp.compiler.types;

import org.codehaus.groovy.ast.ClassNode;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ven
 */
public class SubstitutorImpl implements Substitutor {
    SubstitutorImpl(Map<TypeParameter, Type> bindings) {
        this.bindings = bindings;
    }

    private Map<TypeParameter, Type> bindings;

    public Type substitute(Type t) {
        return t.accept(visitor);
    }

    private class MyTypeVisitor implements TypeVisitor<Type> {
        public Type visitParameterizedType(ParameterizedType t) {
            ClassNode classNode = t.getClassNode();
            TypeParameter[] typeParameters = PerClassNodeCache.INSTANCE.getTypeParameters(classNode);
            Map<TypeParameter, Type> newBindings = new HashMap<TypeParameter, Type>();
            for (TypeParameter parameter : typeParameters) {
                TypeParameterType parameterType = new TypeParameterType(parameter);
                newBindings.put(parameter, t.getSubstitutor().substitute(parameterType).accept(this));
            }

            return new ParameterizedType(classNode, new SubstitutorImpl(newBindings));
        }

        public Type visitClassType(ClassType t) {
            return t;
        }

        public Type visitWildcardType(WildcardType t) {
            Type bound = t.getBound();
            if (bound == null) return t;
            return new WildcardType(bound.accept(this), t.isExtends());
        }

        public Type visitTypeParameterType(TypeParameterType t) {
            TypeParameter typeParameter = t.getTypeParameter();
            if (bindings.containsKey(typeParameter)) return bindings.get(typeParameter);
            return t;
        }

        public Type visitArrayType(ArrayType t) {
            Type oldComp = t.getComponentType();
            Type newComp = oldComp.accept(this);
            return oldComp == newComp ? t : newComp.makeArray();
        }
    }

    private MyTypeVisitor visitor = new MyTypeVisitor();
}
