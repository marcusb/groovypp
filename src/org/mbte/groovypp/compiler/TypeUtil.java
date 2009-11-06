package org.mbte.groovypp.compiler;

import groovy.lang.*;
import org.codehaus.groovy.ast.ClassHelper;
import static org.codehaus.groovy.ast.ClassHelper.*;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.GenericsType;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;
import org.mbte.groovypp.runtime.HasDefaultImplementation;
import org.mbte.groovypp.runtime.LinkedHashMapEx;

import java.util.*;
import java.util.regex.Matcher;

public class TypeUtil {
    public static final ClassNode Number_TYPE = ClassHelper.make(Number.class);
    public static final String DTT_INTERNAL = BytecodeHelper.getClassInternalName(DefaultTypeTransformation.class.getName());
    public static final ClassNode LINKED_HASH_MAP_TYPE = make(LinkedHashMap.class);
    public static final ClassNode EX_LINKED_HASH_MAP_TYPE = make(LinkedHashMapEx.class);
    public static final ClassNode ARRAY_LIST_TYPE = make(ArrayList.class);
    public static final ClassNode COLLECTION_TYPE = make(Collection.class);
    public static final ClassNode TCLOSURE = make(TypedClosure.class);
    public static final ClassNode RANGE_TYPE = make(Range.class);
    public static final ClassNode OWNER_AWARE = make(OwnerAware.class);
    public static final ClassNode OWNER_AWARE_SETTER = make(OwnerAware.Setter.class);
    public static final ClassNode INT_RANGE_TYPE = make(IntRange.class);
    public static final ClassNode TYPED = make(Typed.class);
    public static final ClassNode TRAIT = make(Trait.class);
    public static final ClassNode HAS_DEFAULT_IMPLEMENTATION = make(HasDefaultImplementation.class);
    public static final ClassNode OBJECT_ARRAY = OBJECT_TYPE.makeArray();
    public static final ClassNode SET_TYPE = make(Set.class);
    public static final ClassNode SORTED_SET_TYPE = make(SortedSet.class);
    public static final ClassNode QUEUE_TYPE = make(Queue.class);
    public static final ClassNode TREE_SET_TYPE = make(TreeSet.class);
    public static final ClassNode LINKED_LIST_TYPE = make(LinkedList.class);
    public static final ClassNode LINKED_HASH_SET_TYPE = make(LinkedHashSet.class);
    public static final ClassNode MATCHER = make(Matcher.class);

    public static class Null {
    }

    public static interface TypedClosure {
    }

    public static final ClassNode NULL_TYPE = new ClassNode(Null.class);

    public static boolean isAssignableFrom(ClassNode classToTransformTo, ClassNode classToTransformFrom) {
        if (classToTransformFrom == null) return true;
        if (classToTransformFrom == TypeUtil.NULL_TYPE) return true;
        if (classToTransformTo.equals(classToTransformFrom)) return true;
        if (classToTransformTo.equals(OBJECT_TYPE)) return true;

        classToTransformTo = TypeUtil.wrapSafely(classToTransformTo);
        classToTransformFrom = TypeUtil.wrapSafely(classToTransformFrom);
        if (classToTransformTo == classToTransformFrom) return true;

        if (TypeUtil.isNumericalType(classToTransformTo)) {
            if (TypeUtil.isNumericalType(classToTransformFrom)
                    || classToTransformFrom.equals(Character_TYPE))
                return true;
        } else if (classToTransformTo.equals(Character_TYPE)) {
            if (TypeUtil.isNumericalType(classToTransformFrom) || classToTransformFrom.equals(STRING_TYPE))
                return true;
        } else if (classToTransformTo.equals(STRING_TYPE)) {
            if (classToTransformFrom.equals(STRING_TYPE) || isDirectlyAssignableFrom(GSTRING_TYPE, classToTransformFrom)) {
                return true;
            }
        } else if (classToTransformTo.isArray() && classToTransformFrom.implementsInterface(TypeUtil.COLLECTION_TYPE)) {
            return isAssignableFrom(classToTransformTo.getComponentType(), classToTransformFrom.getComponentType());
        } else if (classToTransformTo.isArray() && classToTransformFrom.isArray()) {
            return isAssignableFrom(classToTransformTo.getComponentType(), classToTransformFrom.getComponentType());
        }

        return isDirectlyAssignableFrom(classToTransformTo, classToTransformFrom);
    }

    public static boolean isDirectlyAssignableFrom(ClassNode to, ClassNode from) {
        return from == null
                || from == TypeUtil.NULL_TYPE
                || (from.isDerivedFrom(to) && (!from.implementsInterface(TCLOSURE) || to.equals(ClassHelper.CLOSURE_TYPE)))
                || to.isInterface() && implementsInterface(to, from)
                || to.equals(TypeUtil.OBJECT_ARRAY) && from.isArray();
    }

    private static boolean implementsInterface(ClassNode type, ClassNode type1) {
        return type1.implementsInterface(type);
    }

    public static boolean isNumericalType(ClassNode paramType) {
        return paramType == byte_TYPE
                || paramType == short_TYPE
                || paramType == int_TYPE
                || paramType == float_TYPE
                || paramType == long_TYPE
                || paramType == double_TYPE
                || paramType == char_TYPE
                || paramType.equals(Byte_TYPE)
                || paramType.equals(Short_TYPE)
                || paramType.equals(Integer_TYPE)
                || paramType.equals(Float_TYPE)
                || paramType.equals(Long_TYPE)
                || paramType.equals(Double_TYPE)
                || paramType.equals(BigDecimal_TYPE)
                || paramType.equals(BigInteger_TYPE)
                || paramType.equals(Character_TYPE)
                || paramType.equals(Number_TYPE);
    }

    public static ClassNode commonType(ClassNode type1, ClassNode type2) {
        if (type1 == null || type2 == null)
            throw new RuntimeException("Internal Error");

        if (type1.equals(type2))
            return type1;

        if (type1 == NULL_TYPE)
            return type2;

        if (type2 == NULL_TYPE)
            return type1;

        if (type1.equals(ClassHelper.OBJECT_TYPE) || type2.equals(ClassHelper.OBJECT_TYPE))
            return ClassHelper.OBJECT_TYPE;

        type1 = TypeUtil.wrapSafely(type1);
        type2 = TypeUtil.wrapSafely(type2);

        if (isNumericalType(type1) && isNumericalType(type2)) {
            if (type1.equals(ClassHelper.Double_TYPE) || type2.equals(ClassHelper.Double_TYPE))
                return ClassHelper.double_TYPE;
            if (type1.equals(ClassHelper.Float_TYPE) || type2.equals(ClassHelper.Float_TYPE))
                return ClassHelper.float_TYPE;
            if (type1.equals(ClassHelper.Long_TYPE) || type2.equals(ClassHelper.Long_TYPE))
                return ClassHelper.long_TYPE;
            if (type1.equals(ClassHelper.Integer_TYPE) || type2.equals(ClassHelper.Integer_TYPE))
                return ClassHelper.int_TYPE;
            return Number_TYPE;
        }

        final Set<ClassNode> allTypes1 = getAllTypes(type1);
        final Set<ClassNode> allTypes2 = getAllTypes(type2);

        for (ClassNode cn : allTypes1)
            if (allTypes2.contains(cn))
                return cn;

        return ClassHelper.OBJECT_TYPE;
    }

    public static boolean isBigDecimal(ClassNode type) {
        return type.equals(BigDecimal_TYPE);
    }

    public static boolean isBigInteger(ClassNode type) {
        return type.equals(BigInteger_TYPE);
    }

    public static boolean isFloatingPoint(ClassNode type) {
        return type == double_TYPE || type == float_TYPE;
    }

    public static boolean isLong(ClassNode type) {
        return type == long_TYPE;
    }

    public static ClassNode getMathType(ClassNode l, ClassNode r) {
        l = getUnwrapper(l);
        r = getUnwrapper(r);

        if (isFloatingPoint(l) || isFloatingPoint(r)) {
            return double_TYPE;
        }
        if (isBigDecimal(l) || isBigDecimal(r)) {
            return BigDecimal_TYPE;
        }
        if (isBigInteger(l) || isBigInteger(r)) {
            return BigInteger_TYPE;
        }
        if (isLong(l) || isLong(r)) {
            return long_TYPE;
        }
        return int_TYPE;
    }

    static Set<ClassNode> getAllTypes(ClassNode cn) {
        Set<ClassNode> set = new LinkedHashSet<ClassNode>();

        LinkedList<ClassNode> ifaces = new LinkedList<ClassNode>();
        if (!cn.isInterface()) {
            for (ClassNode c = cn; !c.equals(ClassHelper.OBJECT_TYPE); c = c.getSuperClass()) {
                set.add(c);
                ifaces.addAll(Arrays.asList(cn.getInterfaces()));
            }
        } else {
            ifaces.add(cn);
        }

        while (!ifaces.isEmpty()) {
            ClassNode iface = ifaces.removeFirst();
            set.add(iface);
            ifaces.addAll(Arrays.asList(iface.getInterfaces()));
        }

        set.add(ClassHelper.OBJECT_TYPE);
        return set;
    }

    // Below is the implementation of poor-man's generics.
    public static ClassNode getSubstitutedType(ClassNode toSubstitute,
                                               final ClassNode declaringClass,
                                               final ClassNode accessType) {
        ClassNode accessClass = accessType.redirect();

        ClassNode mapped = mapTypeFromSuper(toSubstitute, declaringClass.redirect(), accessClass);
        if (mapped == null) return toSubstitute;
        toSubstitute = mapped;
        final GenericsType[] typeArgs = accessType.getGenericsTypes();
        return getSubstitutedTypeToplevel(toSubstitute, accessClass, typeArgs);
    }

    public static ClassNode getSubstitutedType(ClassNode toSubstitute,
                                               final MethodNode method,
                                               final ClassNode[] methodTypeArgs) {
        if (methodTypeArgs == null || methodTypeArgs.length == 0) return toSubstitute;
        GenericsType[] genericsTypes = new GenericsType[methodTypeArgs.length];
        for (int i = 0; i < genericsTypes.length; i++) {
            genericsTypes[i] = methodTypeArgs[i] == null ? null : new GenericsType(methodTypeArgs[i]);
        }
        return getSubstitutedTypeToplevelInner(toSubstitute, genericsTypes, getTypeParameterNames(method));
    }


    private static ClassNode getSubstitutedTypeToplevel(ClassNode toSubstitute, ClassNode accessClass, GenericsType[] typeArgs) {
        if (typeArgs == null || typeArgs.length == 0) return toSubstitute;  // all done.
        String[] typeVariables = getTypeParameterNames(accessClass);
        return getSubstitutedTypeToplevelInner(toSubstitute, typeArgs, typeVariables);
    }

    private static ClassNode getSubstitutedTypeToplevelInner(ClassNode toSubstitute, GenericsType[] typeArgs, String[] typeVariables) {
        int arrayCount = 0;
        while (toSubstitute.isArray()) {
            toSubstitute = toSubstitute.getComponentType();
            arrayCount++;
        }
        if (isTypeParameterPlaceholder(toSubstitute)) {
            String name = toSubstitute.getUnresolvedName();
            // This is an erased type parameter
            ClassNode binding = getBindingNormalized(name, typeVariables, typeArgs);
            return createArrayType(arrayCount, binding != null ? binding : toSubstitute);
        }
        if (typeVariables.length != typeArgs.length) return toSubstitute;
        return createArrayType(arrayCount, getSubstitutedTypeInner(toSubstitute, typeVariables, typeArgs));
    }

    private static boolean isTypeParameterPlaceholder(ClassNode type) {
        return type.isGenericsPlaceHolder();
    }

    private static ClassNode createArrayType(int arrayCount, ClassNode type) {
        while (arrayCount-- > 0) type = type.makeArray();
        return type;
    }

    private static String[] getTypeParameterNames(ClassNode clazz) {
        GenericsType[] generics = clazz.redirect().getGenericsTypes();
        if (generics == null || generics.length == 0) return new String[0];
        String[] result = new String[generics.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = generics[i].getName();
        }
        return result;
    }

    private static String[] getTypeParameterNames(MethodNode methodNode) {
        GenericsType[] generics = methodNode.getGenericsTypes();
        if (generics == null || generics.length == 0) return new String[0];
        String[] result = new String[generics.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = generics[i].getName();
        }
        return result;
    }

    private static ClassNode getSubstitutedTypeInner(ClassNode toSubstitute, String[] typeVariables, GenericsType[] typeArgs) {
        GenericsType[] toSubstituteTypeArgs = toSubstitute.getGenericsTypes();
        if (toSubstituteTypeArgs == null || toSubstituteTypeArgs.length == 0) return toSubstitute;
        GenericsType[] substitutedArgs = new GenericsType[toSubstituteTypeArgs.length];
        for (int i = 0; i < toSubstituteTypeArgs.length; i++) {
            GenericsType typeArg = toSubstituteTypeArgs[i];
            if (isTypeParameterPlaceholder(typeArg.getType())) {
                GenericsType binding = getBinding(typeArg.getType().getUnresolvedName(), typeVariables, typeArgs);
                substitutedArgs[i] = binding != null ? binding : typeArg;
            } else {
                ClassNode type = getSubstitutedTypeInner(typeArg.getType(), typeVariables, typeArgs);
                ClassNode oldLower = typeArg.getLowerBound();
                ClassNode lowerBound = oldLower != null ? getSubstitutedTypeInner(oldLower, typeVariables, typeArgs) : oldLower;
                ClassNode[] oldUpper = typeArg.getUpperBounds();
                ClassNode[] upperBounds = null;
                if (oldUpper != null) {
                    upperBounds = new ClassNode[oldUpper.length];
                    for (int j = 0; j < upperBounds.length; j++) {
                        upperBounds[j] = getSubstitutedTypeInner(oldUpper[i], typeVariables, typeArgs);

                    }
                }
                substitutedArgs[i] = new GenericsType(type, upperBounds, lowerBound);
                substitutedArgs[i].setWildcard(typeArg.isWildcard());
                substitutedArgs[i].setResolved(typeArg.isResolved());
            }
        }
        ClassNode result = withGenericTypes(toSubstitute, substitutedArgs);
        return result;
    }

    public static ClassNode mapTypeFromSuper(ClassNode type, ClassNode aSuper, ClassNode bDerived) {
        return mapTypeFromSuper(type, aSuper, bDerived, true);
    }

    private static ClassNode mapTypeFromSuper(ClassNode type, ClassNode aSuper, ClassNode bDerived, boolean substituteOwn) {
        if (bDerived.redirect().equals(aSuper)) {
            return substituteOwn ? getSubstitutedTypeToplevel(type, bDerived.redirect(),
                        bDerived.getGenericsTypes()) : type;
        }
        ClassNode derivedSuperClass = bDerived.getUnresolvedSuperClass(true);
        if (derivedSuperClass != null) {
            ClassNode rec = mapTypeFromSuper(type, aSuper, derivedSuperClass, false);
            if (rec != null) {
                rec = getSubstitutedTypeToplevel(rec, derivedSuperClass.redirect(),
                        derivedSuperClass.getGenericsTypes());
                return substituteOwn ? getSubstitutedTypeToplevel(rec, bDerived.redirect(),
                        bDerived.getGenericsTypes()) : rec;
            }
        }
        ClassNode[] interfaces = bDerived.getUnresolvedInterfaces(true);
        if (interfaces != null) {
            for (ClassNode derivedInterface : interfaces) {
                ClassNode rec = mapTypeFromSuper(type, aSuper, derivedInterface, false);
                if (rec != null) {
                    rec = getSubstitutedTypeToplevel(rec, derivedInterface.redirect(),
                            derivedInterface.getGenericsTypes());
                    return substituteOwn ? getSubstitutedTypeToplevel(rec, bDerived.redirect(),
                            bDerived.getGenericsTypes()) : rec;
                }
            }
        }
        return null;
    }

    private static ClassNode getBindingNormalized(String name, String[] typeParameters, GenericsType[] typeArgs) {
        GenericsType genericType = getBinding(name, typeParameters, typeArgs);
        if (genericType == null) return null;
        if (genericType.isWildcard()) return genericType.getUpperBounds()[0];
        return genericType.getType();
    }

    private static GenericsType getBinding(String name, String[] typeParameters, GenericsType[] typeArgs) {
        for (int i = 0; i < typeParameters.length; i++) {
            if (typeParameters[i].equals(name)) return typeArgs[i];
        }
        return null;
    }

    public static boolean isExtends(GenericsType type) {
        return type.isWildcard() && type.getUpperBounds() != null;
    }

    public static boolean isSuper(GenericsType type) {
        return type.isWildcard() && type.getLowerBound() != null;
    }

    public static boolean equal(ClassNode type1, ClassNode type2) {
        if (!type1.redirect().equals(type2.redirect())) return false;
        GenericsType[] args1 = type1.getGenericsTypes();
        GenericsType[] args2 = type2.getGenericsTypes();
        if (args1 == null || args2 == null) {
            return args1 == null && args2 == null;
        }
        for (int i = 0; i < args2.length; i++) {
            if (args1[i].isWildcard()) {
                if (!args2[i].isWildcard()) return false;
                if (isExtends(args1[i]) && !isExtends(args2[i])) return false;
                if (isSuper(args1[i]) && !isSuper(args2[i])) return false;
            } else if (args2[i].isWildcard()) return false;
            if (!equal(args1[i].getType(), args2[i].getType())) return false;
        }
        return true;
    }

    public static ClassNode withGenericTypes(ClassNode baseType, GenericsType[] genericTypes) {
        ClassNode newBase = makeWithoutCaching(baseType.getName());
        newBase.setRedirect(baseType);
        newBase.setGenericsTypes(genericTypes);
        return newBase;
    }

    public static ClassNode wrapSafely(ClassNode type) {
        if (ClassHelper.isPrimitiveType(type)) return ClassHelper.getWrapper(type);
        else return type;
    }
}
