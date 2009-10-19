package org.mbte.groovypp.compiler.types;

/**
 * @author ven
 */
public abstract class Type {
    public boolean isAssignableFrom(Type other) {
        return TypesUtil.isAssignable(this, other);
    }

    public boolean isConvertibleFrom(Type other) {
        return TypesUtil.isConvertible(other, this);
    }

    public ArrayType makeArray() { return new ArrayType(this); }

    public abstract <T> T accept(TypeVisitor<T> visitor);
}
