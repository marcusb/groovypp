package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ClassHelper;
import static org.codehaus.groovy.ast.ClassHelper.*;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;

import java.util.*;

import groovy.lang.TypedClosure;
import groovy.lang.Range;

public class TypeUtil {
    private static final ClassNode Number_TYPE = ClassHelper.make(Number.class);
    public static final String DTT_INTERNAL = BytecodeHelper.getClassInternalName(DefaultTypeTransformation.class.getName());
    public static final ClassNode LINKED_HASH_MAP_TYPE = make(LinkedHashMap.class);
    public static final ClassNode ARRAY_LIST_TYPE = make(ArrayList.class);
    public static final ClassNode COLLECTION_TYPE = make(Collection.class);
    public static final ClassNode TCLOSURE = make(TypedClosure.class);
    public static final ClassNode RANGE_TYPE = make(Range.class);

    private static class Null {}
    public static final ClassNode NULL_TYPE = new ClassNode(Null.class);

    public static boolean isAssignableFrom(ClassNode classToTransformTo, ClassNode classToTransformFrom) {
        if (classToTransformFrom == null) return true;
        if (classToTransformTo.equals(classToTransformFrom)) return true;
        if (classToTransformTo.equals(OBJECT_TYPE)) return true;

        classToTransformTo = getWrapper(classToTransformTo);
        classToTransformFrom = getWrapper(classToTransformFrom);
        if (classToTransformTo == classToTransformFrom) return true;

        // note: there is no coercion for boolean and char. Range matters, precision doesn't
        if (classToTransformTo == Integer_TYPE) {
            if (classToTransformFrom == Integer_TYPE
                    || classToTransformFrom == Short_TYPE
                    || classToTransformFrom == Byte_TYPE
                    || classToTransformFrom == BigInteger_TYPE)
                return true;
        } else if (classToTransformTo == Double_TYPE) {
            if (classToTransformFrom == Double_TYPE
                    || classToTransformFrom == Integer_TYPE
                    || classToTransformFrom == Long_TYPE
                    || classToTransformFrom == Short_TYPE
                    || classToTransformFrom == Byte_TYPE
                    || classToTransformFrom == Float_TYPE
                    || classToTransformFrom == BigDecimal_TYPE
                    || classToTransformFrom == BigInteger_TYPE)
                return true;
        } else if (classToTransformTo == BigDecimal_TYPE) {
            if (classToTransformFrom == Double_TYPE
                    || classToTransformFrom == Integer_TYPE
                    || classToTransformFrom == Long_TYPE
                    || classToTransformFrom == Short_TYPE
                    || classToTransformFrom == Byte_TYPE
                    || classToTransformFrom == Float_TYPE
                    || classToTransformFrom == BigDecimal_TYPE
                    || classToTransformFrom == BigInteger_TYPE)
                return true;
        } else if (classToTransformTo == BigInteger_TYPE) {
            if (classToTransformFrom == Integer_TYPE
                    || classToTransformFrom == Long_TYPE
                    || classToTransformFrom == Short_TYPE
                    || classToTransformFrom == Byte_TYPE
                    || classToTransformFrom == BigInteger_TYPE)
                return true;
        } else if (classToTransformTo == Long_TYPE) {
            if (classToTransformFrom == Long_TYPE
                    || classToTransformFrom == Integer_TYPE
                    || classToTransformFrom == Short_TYPE
                    || classToTransformFrom == Byte_TYPE)
                return true;
        } else if (classToTransformTo == Float_TYPE) {
            if (classToTransformFrom == Float_TYPE
                    || classToTransformFrom == Integer_TYPE
                    || classToTransformFrom == Long_TYPE
                    || classToTransformFrom == Short_TYPE
                    || classToTransformFrom == Byte_TYPE)
                return true;
        } else if (classToTransformTo == Short_TYPE) {
            if (classToTransformFrom == Short_TYPE
                    || classToTransformFrom == Byte_TYPE)
                return true;
        } else if (classToTransformTo.equals(STRING_TYPE)) {
            if (classToTransformFrom.equals(STRING_TYPE) ||
                    isDirectlyAssignableFrom(GSTRING_TYPE, classToTransformFrom)) {
                return true;
            }
        }

        return isDirectlyAssignableFrom(classToTransformTo, classToTransformFrom);
    }

    public static boolean isDirectlyAssignableFrom(ClassNode type, ClassNode type1) {
        return type1.isDerivedFrom(type) || type.isInterface() && implementsInterface(type, type1);
    }

    private static boolean implementsInterface(ClassNode type, ClassNode type1) {
        return type1.implementsInterface(type);
    }

    public static boolean isIntegralType(ClassNode expr) {
        return expr == Integer_TYPE || expr == Byte_TYPE || expr == Short_TYPE || expr == Character_TYPE || expr == Boolean_TYPE;
    }

    public static boolean isNumericalType(ClassNode paramType) {
        return paramType == char_TYPE
         || paramType == byte_TYPE
         || paramType == short_TYPE
         || paramType == int_TYPE
         || paramType == float_TYPE
         || paramType == long_TYPE
         || paramType == double_TYPE
         || paramType == boolean_TYPE
         || paramType.equals(Byte_TYPE)
         || paramType.equals(Character_TYPE)
         || paramType.equals(Short_TYPE)
         || paramType.equals(Integer_TYPE)
         || paramType.equals(Float_TYPE)
         || paramType.equals(Long_TYPE)
         || paramType.equals(Boolean_TYPE)
         || paramType.equals(Double_TYPE)
         || paramType.equals(BigDecimal_TYPE)
         || paramType.equals(BigInteger_TYPE);
    }

    public static ClassNode commonType(ClassNode type1, ClassNode type2) {
        if (type1 == null || type2 == null)
          throw new RuntimeException("Internal Error");

        if (type1 == NULL_TYPE)
           return type2;

        if (type2 == NULL_TYPE)
           return type1;

        if (type1.equals(ClassHelper.OBJECT_TYPE) || type2.equals(ClassHelper.OBJECT_TYPE))
          return ClassHelper.OBJECT_TYPE;

        type1 = ClassHelper.getWrapper(type1);
        type2 = ClassHelper.getWrapper(type2);

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
		return type == BigDecimal_TYPE;
	}

    public static boolean isBigInteger(ClassNode type) {
		return type == BigInteger_TYPE;
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
        if (isBigInteger(r) || isBigInteger(r)) {
            return BigInteger_TYPE;
        }
        if (isLong(l) || isLong(r)){
            return long_TYPE;
        }
        return int_TYPE;
    }

    static Set<ClassNode> getAllTypes (ClassNode cn) {
        Set<ClassNode> set = new LinkedHashSet<ClassNode> ();

        LinkedList<ClassNode> ifaces = new LinkedList<ClassNode> ();
        if (!cn.isInterface()) {
            for ( ClassNode c = cn; !c.equals(ClassHelper.OBJECT_TYPE); c = c.getSuperClass()) {
                set.add (c);
                ifaces.addAll(Arrays.asList(cn.getInterfaces()));
            }
        }
        else {
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
}
