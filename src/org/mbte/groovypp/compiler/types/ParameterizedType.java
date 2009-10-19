package org.mbte.groovypp.compiler.types;

import org.codehaus.groovy.ast.ClassNode;

/**
 * @author ven
 */
class ParameterizedType extends Type {
    private ClassNode classNode;
    private Substitutor substitutor;

    ParameterizedType(ClassNode classNode, Substitutor substitutor) {
        this.classNode = classNode;
        this.substitutor = substitutor;
    }

    public ClassNode getClassNode() {
        return classNode;
    }

    public Substitutor getSubstitutor() {
        return substitutor;
    }

    @Override
    public <T> T accept(TypeVisitor<T> visitor) {
        return visitor.visitParameterizedType(this);
    }
}

//Represents either parameterless or raw class type
class ClassType extends Type {
    ClassType(ClassNode classNode) {
        this.classNode = classNode;
    }

    private ClassNode classNode;

    public ClassNode getClassNode() {
        return classNode;
    }

    @Override
    public <T> T accept(TypeVisitor<T> visitor) {
        return visitor.visitClassType(this);
    }
}
class TypeParameterType extends Type {
    TypeParameterType(TypeParameter typeParameter) {
        this.typeParameter = typeParameter;
    }

    private TypeParameter typeParameter;

    public TypeParameter getTypeParameter() {
        return typeParameter;
    }

    @Override
    public <T> T accept(TypeVisitor<T> visitor) {
        return visitor.visitTypeParameterType(this);
    }
}

class WildcardType extends Type {
    private Type bound;
    private boolean isExtends;

    WildcardType(Type bound, boolean anExtends) {
        this.bound = bound;
        isExtends = anExtends;
    }

    public Type getBound() {
        return bound;
    }

    public boolean isExtends() {
        return isExtends;
    }

    @Override
    public <T> T accept(TypeVisitor<T> visitor) {
        return visitor.visitWildcardType(this);
    }
}

class ArrayType extends Type {
    private Type componentType;

    public Type getComponentType() {
        return componentType;
    }

    @Override
    public <T> T accept(TypeVisitor<T> visitor) {
        return visitor.visitArrayType(this);
    }

    ArrayType(Type componentType) {
        this.componentType = componentType;
    }
}