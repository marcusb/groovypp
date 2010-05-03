/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbte.groovypp.compiler;

import groovy.lang.Delegating;
import groovy.lang.Trait;
import groovy.lang.Typed;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;
import org.mbte.groovypp.runtime.HasDefaultImplementation;
import org.mbte.groovypp.runtime.LinkedHashMapEx;
import org.objectweb.asm.Opcodes;

import java.util.*;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.regex.Matcher;

import static org.codehaus.groovy.ast.ClassHelper.*;

public class TypeUtil {
    public static final ClassNode Number_TYPE = ClassHelper.make(Number.class);
    public static final String DTT_INTERNAL = BytecodeHelper.getClassInternalName(DefaultTypeTransformation.class.getName());
    public static final ClassNode LINKED_HASH_MAP_TYPE = make(LinkedHashMap.class);
    public static final ClassNode EX_LINKED_HASH_MAP_TYPE = make(LinkedHashMapEx.class);
    public static final ClassNode ARRAY_LIST_TYPE = make(ArrayList.class);
    public static final ClassNode COLLECTION_TYPE = make(Collection.class);
    public static final ClassNode RANGE_OF_INTEGERS_TYPE = TypeUtil.withGenericTypes(ClassHelper.RANGE_TYPE, ClassHelper.Integer_TYPE);
    public static final ClassNode TYPED = make(Typed.class);
    public static final ClassNode TRAIT = make(Trait.class);
    public static final ClassNode HAS_DEFAULT_IMPLEMENTATION = make(HasDefaultImplementation.class);
    public static final ClassNode OBJECT_ARRAY = OBJECT_TYPE.makeArray();
    public static final ClassNode SET_TYPE = make(Set.class);
    public static final ClassNode SORTED_SET_TYPE = make(SortedSet.class);
    public static final ClassNode SORTED_MAP_TYPE = make(SortedMap.class);
    public static final ClassNode TREE_MAP_TYPE = make(TreeMap.class);
    public static final ClassNode QUEUE_TYPE = make(Queue.class);
    public static final ClassNode TREE_SET_TYPE = make(TreeSet.class);
    public static final ClassNode LINKED_LIST_TYPE = make(LinkedList.class);
    public static final ClassNode LINKED_HASH_SET_TYPE = make(LinkedHashSet.class);
    public static final ClassNode MATCHER = make(Matcher.class);
    public static final ClassNode TMAP = make(TypedMap.class);
    public static final ClassNode TLIST = make(TypedList.class);
    public static final ClassNode TTHIS = make(TypedThis.class);
    public static final ClassNode TCLOSURE = make(TypedClosure.class);
    public static final ClassNode TCLOSURE_NULL = make(TypedClosure.Null.class);
    public static final ClassNode ITERABLE = make(Iterable.class);
    public static final ClassNode ITERATOR = make(Iterator.class);
    public static final ClassNode ATOMIC_REFERENCE_FIELD_UPDATER = make(AtomicReferenceFieldUpdater.class);
    public static final ClassNode ATOMIC_INTEGER_FIELD_UPDATER = make(AtomicIntegerFieldUpdater.class);
    public static final ClassNode ATOMIC_LONG_FIELD_UPDATER = make(AtomicLongFieldUpdater.class);
    public static final ClassNode DELEGATING = make(Delegating.class);
    public static final ClassNode THROWABLE = make(Throwable.class);
    public static final ClassNode COMPARABLE = make(Comparable.class);
    public static final ClassNode STRING_BUILDER = make(StringBuilder.class);

    public TypeUtil() {
        RAW_CLASS = new ClassNode(RawMarker.class);
    }

    public static boolean hasGenericsTypes(MethodNode methodNode) {
        if (methodNode.getGenericsTypes() != null && methodNode.getGenericsTypes().length > 0) return true;
        if (methodNode.isStatic()) return false;
        ClassNode clazz = methodNode.getDeclaringClass();
        do {
            if (clazz.getGenericsTypes() != null && clazz.getGenericsTypes().length > 0) return true;
            if (clazz.isStaticClass()) break;
            clazz = clazz.getDeclaringClass();
        } while (clazz != null);
        return false;
    }

    public static class Null {
    }

    public static interface TypedClosure {
        public static interface Null<T> {}
    }

    public static interface TypedMap {
    }

    public static interface TypedList {
    }

    public static interface TypedThis {
    }

    public static final ClassNode NULL_TYPE = new ClassNode(Null.class);

    public static boolean isAssignableFrom(ClassNode classToTransformTo, ClassNode classToTransformFrom) {
        if (classToTransformFrom == null) return true;
        if (classToTransformFrom == TypeUtil.NULL_TYPE) return true;

        if (classToTransformTo.equals(classToTransformFrom)||
            classToTransformTo.equals(OBJECT_TYPE) ||
            classToTransformTo.equals(boolean_TYPE) ||
            classToTransformTo.equals(Boolean_TYPE))
            if (!classToTransformFrom.implementsInterface(TypeUtil.TMAP) &&
                !classToTransformFrom.implementsInterface(TypeUtil.TLIST)) return true;

        classToTransformTo = TypeUtil.wrapSafely(classToTransformTo);
        classToTransformFrom = TypeUtil.wrapSafely(classToTransformFrom);
        if (classToTransformTo == classToTransformFrom) return true;

        if (TypeUtil.isNumericalType(classToTransformTo)) {
            if (TypeUtil.isNumericalType(classToTransformFrom)
                    || classToTransformFrom.equals(STRING_TYPE) || classToTransformFrom.equals(Character_TYPE))
                return true;
        } else if (classToTransformTo.equals(Character_TYPE)) {
            if (TypeUtil.isNumericalType(classToTransformFrom) || classToTransformFrom.equals(STRING_TYPE))
                return true;
        } else if (classToTransformTo.equals(STRING_TYPE)) {
            if (TypeUtil.isNumericalType(classToTransformFrom) || classToTransformFrom.equals(STRING_TYPE) ||
                    isDirectlyAssignableFrom(GSTRING_TYPE, classToTransformFrom)) {
                return true;
            }
        } else if (classToTransformTo.isArray() && classToTransformFrom.implementsInterface(TypeUtil.COLLECTION_TYPE)) {
            return isAssignableFrom(classToTransformTo.getComponentType(), classToTransformFrom.getComponentType());
        } else if (classToTransformTo.isArray() && classToTransformFrom.isArray()) {
            return isAssignableFrom(classToTransformTo.getComponentType(), classToTransformFrom.getComponentType());
        }

        if (classToTransformFrom.implementsInterface(TMAP))
            return TypeUtil.isDirectlyAssignableFrom(classToTransformTo, TypeUtil.LINKED_HASH_MAP_TYPE);

        if (classToTransformFrom.implementsInterface(TLIST))
            return TypeUtil.isDirectlyAssignableFrom(classToTransformTo, TypeUtil.ARRAY_LIST_TYPE);

        if (classToTransformFrom.implementsInterface(TTHIS)) {
            while (classToTransformFrom != null) {
                if (isDirectlyAssignableFrom(classToTransformTo, classToTransformFrom)) return true;
                if (classToTransformFrom.isStaticClass()) break;
                classToTransformFrom = classToTransformFrom.getDeclaringClass();
            }
            return false;
        }

        if (isReferenceUnboxing(classToTransformTo, classToTransformFrom)) return true;

        return isDirectlyAssignableFrom(classToTransformTo, classToTransformFrom);
    }

    public static boolean isReferenceUnboxing(ClassNode classToTransformTo, ClassNode classToTransformFrom) {
        MethodNode method = getReferenceUnboxingMethod(classToTransformFrom);
        if (method == null) return false;
        ClassNode substituted = getSubstitutedType(method.getReturnType(), method.getDeclaringClass(),
                classToTransformFrom);
        return isDirectlyAssignableFrom(classToTransformTo, substituted);
    }

    private static String[] TRANSPARENT_DEREFERENCE_CLASSES = {
            "groovy.lang.Reference",
            "java.lang.ref.Reference",
            "java.util.concurrent.atomic.AtomicReference",
            "java.util.concurrent.atomic.AtomicInteger",
            "java.util.concurrent.atomic.AtomicLong",
            "java.util.concurrent.atomic.AtomicBoolean"
    };

    public static MethodNode getReferenceUnboxingMethod(ClassNode classNode) {
        if (classNode instanceof ClosureClassNode || (classNode.getModifiers() & Opcodes.ACC_SYNTHETIC) != 0
                || ClassHelper.isPrimitiveType(classNode)) return null;
        if (!isTransparentClass(classNode)) return null;
        MethodNode method = MethodSelection.findPublicMethodInClass(classNode, "get", new ClassNode[0]);
        return method != null && method.getParameters().length == 0 && !method.getReturnType().equals(ClassHelper.VOID_TYPE)? method : null;
    }

    public static MethodNode getReferenceBoxingMethod(ClassNode classNode, ClassNode arg) {
        if (classNode instanceof ClosureClassNode || (classNode.getModifiers() & Opcodes.ACC_SYNTHETIC) != 0
                || ClassHelper.isPrimitiveType(classNode)) return null;
        if (!isTransparentClass(classNode)) return null;
        MethodNode method = MethodSelection.findPublicMethodInClass(classNode, "set", new ClassNode[] {arg});
        if (method == null || method.getParameters().length != 1) return null;
        ClassNode paramType = method.getParameters()[0].getType();
        ClassNode substituted = getSubstitutedType(paramType, method.getDeclaringClass(), classNode);
        if (!isAssignableFrom(ClassHelper.getWrapper(substituted), ClassHelper.getWrapper(arg))) return null;
        return method;
    }

    private static boolean isTransparentClass(ClassNode node) {
        for (String name : TRANSPARENT_DEREFERENCE_CLASSES) {
            if (node.getName().equals(name)) return true;
        }
        return false;
    }

    public static boolean isDirectlyAssignableFrom(ClassNode to, ClassNode from) {
        if(to.equals(ClassHelper.OBJECT_TYPE))
            return !ClassHelper.isPrimitiveType(from);
        if (from == null) return true;
        if (to.isArray() && from.isArray()) return isDirectlyAssignableFrom(to.getComponentType(), from.getComponentType());

        return from == TypeUtil.NULL_TYPE
                || (from.isDerivedFrom(to) && (!from.implementsInterface(TCLOSURE) || to.equals(ClassHelper.CLOSURE_TYPE)))
                || to.isInterface() && implementsInterface(to, from);
    }

    public static boolean isConvertibleFrom(ClassNode t1, ClassNode t2) {
        if (t1.isArray() && t2.isArray()) return isConvertibleFrom(t1.getComponentType(), t2.getComponentType());
        if (t1 instanceof ClosureClassNode) {
            if ("groovy.lang.Closure".equals(t2.getName())) return true;
        }  else if (t2 instanceof ClosureClassNode) {
            if ("groovy.lang.Closure".equals(t1.getName())) return true;
        }
        t1 = wrapSafely(t1);
        t2 = wrapSafely(t2);
        return isAssignableFrom(t1, t2) || canHaveCommonSubtype(t1, t2) || areTypesDirectlyConvertible(t1, t2);
    }

    private static boolean canHaveCommonSubtype(ClassNode t1, ClassNode t2) {
        return (t1.isInterface() && (t2.getModifiers() & Opcodes.ACC_FINAL) == 0) ||
               (t2.isInterface() && (t1.getModifiers() & Opcodes.ACC_FINAL) == 0);
    }

    public static boolean areTypesDirectlyConvertible(ClassNode t1, ClassNode t2) {
        if (t1.isArray() && t2.isArray()) return areTypesDirectlyConvertible(t1.getComponentType(), t2.getComponentType());
        t1 = wrapSafely(t1);
        t2 = wrapSafely(t2);
        return isDirectlyAssignableFrom(t1, t2) || isDirectlyAssignableFrom(t2, t1) || canHaveCommonSubtype(t1, t2);
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
        return getSubstitutedTypeToplevel(toSubstitute, accessClass, typeArgs, null);
    }

    public static ClassNode getSubstitutedType(ClassNode toSubstitute,
                                               final ClassNode declaringClass,
                                               final ClassNode accessType,
                                               final Set<String> ignoreTypeVariables) {
        ClassNode accessClass = accessType.redirect();

        ClassNode mapped = mapTypeFromSuper(toSubstitute, declaringClass.redirect(), accessClass);
        if (mapped == null) return toSubstitute;
        toSubstitute = mapped;
        final GenericsType[] typeArgs = accessType.getGenericsTypes();
        return getSubstitutedTypeToplevel(toSubstitute, accessClass, typeArgs, ignoreTypeVariables);
    }

    public static ClassNode getSubstitutedType(ClassNode toSubstitute,
                                               final MethodNode method,
                                               final ClassNode[] methodTypeArgs) {
        if (methodTypeArgs == null || methodTypeArgs.length == 0) return toSubstitute;
        GenericsType[] genericsTypes = new GenericsType[methodTypeArgs.length];
        for (int i = 0; i < genericsTypes.length; i++) {
            genericsTypes[i] = methodTypeArgs[i] == null ? null : new GenericsType(methodTypeArgs[i]);
        }
        return getSubstitutedTypeToplevelInner(toSubstitute, genericsTypes, getTypeParameterNames(method), null);
    }

    private static class RawMarker {}

    private static ClassNode RAW_CLASS = new ClassNode(RawMarker.class);
    private static GenericsType RAW_GENERIC_TYPE = new GenericsType(RAW_CLASS);

    private static ClassNode getSubstitutedTypeToplevel(ClassNode toSubstitute, ClassNode accessClass, GenericsType[] typeArgs, Set<String> ignoreTypeVariables) {
        String[] typeVariables = getTypeParameterNames(accessClass);
        if (typeVariables.length == 0) return toSubstitute;
        if (typeArgs == null) {
            typeArgs = new GenericsType[typeVariables.length];
            for (int i = 0; i < typeArgs.length; i++) {
                typeArgs[i] = RAW_GENERIC_TYPE;
            }
        }
        return getSubstitutedTypeToplevelInner(toSubstitute, typeArgs, typeVariables, ignoreTypeVariables);
    }

    private static ClassNode getSubstitutedTypeToplevelInner(ClassNode toSubstitute, GenericsType[] typeArgs, String[] typeVariables, Set<String> ignoreTypeVariables) {
        int arrayCount = 0;
        while (toSubstitute.isArray()) {
            toSubstitute = toSubstitute.getComponentType();
            arrayCount++;
        }
        if (isTypeParameterPlaceholder(toSubstitute, ignoreTypeVariables)) {
            String name = toSubstitute.getUnresolvedName();
            // This is an erased type parameter
            ClassNode binding = getBindingNormalized(name, typeVariables, typeArgs);
            if (binding == RAW_CLASS) binding = toSubstitute.redirect();
            return createArrayType(arrayCount, binding != null ? binding : toSubstitute);
        }
        if (typeVariables.length != typeArgs.length) return createArrayType(arrayCount, toSubstitute);
        return createArrayType(arrayCount, getSubstitutedTypeInner(toSubstitute, typeVariables, typeArgs, ignoreTypeVariables));
    }

    private static boolean isTypeParameterPlaceholder(ClassNode type, Set<String> ignoreTypeVariables) {
        return type.isGenericsPlaceHolder() &&
                (ignoreTypeVariables == null || !ignoreTypeVariables.contains(type.getUnresolvedName()));
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

    private static ClassNode getSubstitutedTypeInner(ClassNode toSubstitute, String[] typeVariables, GenericsType[] typeArgs, Set<String> ignoreTypeVariables) {
        GenericsType[] toSubstituteTypeArgs = toSubstitute.getGenericsTypes();
        if (toSubstituteTypeArgs == null || toSubstituteTypeArgs.length == 0) return toSubstitute;
        GenericsType[] substitutedArgs = new GenericsType[toSubstituteTypeArgs.length];
        for (int i = 0; i < toSubstituteTypeArgs.length; i++) {
            GenericsType typeArg = toSubstituteTypeArgs[i];
            if (isTypeParameterPlaceholder(typeArg.getType(), ignoreTypeVariables)) {
                GenericsType binding = getBinding(typeArg.getType().getUnresolvedName(), typeVariables, typeArgs);
                if (binding == RAW_GENERIC_TYPE) return eraseTypeArguments(toSubstitute);
                substitutedArgs[i] = binding != null ? binding : typeArg;
            } else {
                ClassNode type = getSubstitutedTypeInner(typeArg.getType(), typeVariables, typeArgs, ignoreTypeVariables);
                ClassNode oldLower = typeArg.getLowerBound();
                ClassNode lowerBound = oldLower != null ? getSubstitutedTypeInner(oldLower, typeVariables, typeArgs, ignoreTypeVariables) : oldLower;
                ClassNode[] oldUpper = typeArg.getUpperBounds();
                ClassNode[] upperBounds = null;
                if (oldUpper != null) {
                    upperBounds = new ClassNode[oldUpper.length];
                    for (int j = 0; j < upperBounds.length; j++) {
                        upperBounds[j] = getSubstitutedTypeInner(oldUpper[j], typeVariables, typeArgs, ignoreTypeVariables);

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

    public static ClassNode eraseTypeArguments(ClassNode node) {
        return withGenericTypes(node, (GenericsType[]) null);
    }

    public static ClassNode mapTypeFromSuper(ClassNode type, ClassNode aSuper, ClassNode bDerived) {
        return mapTypeFromSuper(type, aSuper, bDerived, true);
    }

    private static ClassNode mapTypeFromSuper(ClassNode type, ClassNode aSuper, ClassNode bDerived, boolean substituteOwn) {
        if (bDerived.redirect().equals(aSuper)) {
            return substituteOwn ? getSubstitutedTypeToplevel(type, bDerived.redirect(),
                        bDerived.getGenericsTypes(), null) : type;
        }
        ClassNode derivedSuperClass = bDerived.getUnresolvedSuperClass(true);
        if (derivedSuperClass != null) {
            ClassNode rec = mapTypeFromSuper(type, aSuper, derivedSuperClass, false);
            if (rec != null) {
                rec = getSubstitutedTypeToplevel(rec, derivedSuperClass.redirect(),
                        derivedSuperClass.getGenericsTypes(), null);
                return substituteOwn ? getSubstitutedTypeToplevel(rec, bDerived.redirect(),
                        bDerived.getGenericsTypes(), null) : rec;
            }
        }
        ClassNode[] interfaces = bDerived.getUnresolvedInterfaces(true);
        if (interfaces != null) {
            for (ClassNode derivedInterface : interfaces) {
                ClassNode rec = mapTypeFromSuper(type, aSuper, derivedInterface, false);
                if (rec != null) {
                    rec = getSubstitutedTypeToplevel(rec, derivedInterface.redirect(),
                            derivedInterface.getGenericsTypes(), null);
                    return substituteOwn ? getSubstitutedTypeToplevel(rec, bDerived.redirect(),
                            bDerived.getGenericsTypes(), null) : rec;
                }
            }
        }
        return null;
    }

    private static ClassNode getBindingNormalized(String name, String[] typeParameters, GenericsType[] typeArgs) {
        GenericsType genericType = getBinding(name, typeParameters, typeArgs);
        if (genericType == null) return null;
        if (isExtends(genericType)) return genericType.getUpperBounds()[0];
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
        if (ClassHelper.isPrimitiveType(baseType))
            return baseType;
        
        ClassNode newBase = makeWithoutCaching(baseType.getName());
        newBase.setRedirect(baseType);
        newBase.setGenericsTypes(genericTypes);
        return newBase;
    }

    public static ClassNode withGenericTypes(ClassNode baseType, ClassNode... typeArgs) {
        GenericsType[] existing = baseType.getGenericsTypes();
        if (existing == null) existing = baseType.redirect().getGenericsTypes(); 
        if (existing == null || existing.length != typeArgs.length) {
            // Type arguments are not correct wrt to baseType. Shouldn't modify.
            return baseType;
        }
        GenericsType[] genericsTypes = new GenericsType[typeArgs.length];
        for (int i = 0; i < genericsTypes.length; i++) {
            final ClassNode typeArg = typeArgs[i];
            genericsTypes[i] = typeArg == null ? existing[i] : new GenericsType(typeArg);
        }
        return withGenericTypes(baseType, genericsTypes);
    }

    public static ClassNode wrapSafely(ClassNode type) {
        if (ClassHelper.isPrimitiveType(type)) return ClassHelper.getWrapper(type);
        else return type;
    }
}
