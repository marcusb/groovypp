package org.mbte.groovypp.compiler.types;

/**
 * @author ven
 */
public interface TypeVisitor<T> {
  T visitParameterizedType(ParameterizedType t);
  T visitClassType(ClassType t);
  T visitTypeParameterType(TypeParameterType t);
  T visitWildcardType(WildcardType t);
  T visitArrayType(ArrayType t);
}
