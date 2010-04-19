package org.mbte.groovypp.runtime;

import groovy.lang.GString;
import groovy.lang.GroovyRuntimeException;
import org.codehaus.groovy.runtime.DefaultGroovyMethodsSupport;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;
import org.codehaus.groovy.runtime.typehandling.NumberMath;

import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import static org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation.*;

public class DefaultGroovyPPMethods extends DefaultGroovyMethodsSupport {
    public static Number plus(Number self, Number other) {
        return NumberMath.add(self, other);
    }

    public static Number minus(Number self, Number other) {
        return NumberMath.subtract(self, other);
    }

    public static Number multiply(Number self, Number other) {
        return NumberMath.multiply(self, other);
    }

    public static Number div(Number self, Number other) {
        return NumberMath.divide(self, other);
    }

    public static Boolean box(boolean value) {
        return value ? Boolean.TRUE : Boolean.FALSE;
    }

    public static Byte box(byte value) {
        return value;
    }

    public static Character box(char value) {
        return value;
    }

    public static Short box(short value) {
        return value;
    }

    public static Integer box(int value) {
        return value;
    }

    public static Long box(long value) {
        return value;
    }

    public static Float box(float value) {
        return value;
    }

    public static Double box(double value) {
        return value;
    }

    public static Class getClass(byte value) {
        return Byte.TYPE;
    }

    public static Class getClass(short value) {
        return Short.TYPE;
    }

    public static Class getClass(int value) {
        return Integer.TYPE;
    }

    public static Class getClass(char value) {
        return Character.TYPE;
    }

    public static Class getClass(long value) {
        return Long.TYPE;
    }

    public static Class getClass(float value) {
        return Float.TYPE;
    }

    public static Class getClass(double value) {
        return Double.TYPE;
    }

    public static Class getClass(boolean value) {
        return Boolean.TYPE;
    }

    public static String[] gstringArrayToStringArray(GString[] data) {
        if (data == null)
            return null;

        String[] strings = new String[data.length];
        for (int i = 0; i < strings.length; i++) {
            strings[i] = data[i].toString();
        }

        return strings;
    }

    public static Pattern bitwiseNegate(GString self) {
        return DefaultGroovyMethods.bitwiseNegate(self.toString());
    }

    public static Iterator<String> iterator(String self) {
        return DefaultGroovyMethods.toList(self).iterator();
    }

    public static void print(Writer self, Object value) {
        DefaultGroovyMethods.print(self, value);
    }

    public static void println(Writer self, Object value) {
        DefaultGroovyMethods.println(self, value);
    }

    public static boolean equals(Object left, Object right) {
        return left == right || !(left == null || right == null) && left.equals(right);
    }

    public static boolean equals(List left, Object right) {
        if (left == right) return true;
        if (left == null || right == null) return false;
        Class rightClass = right.getClass();
        if (rightClass.isArray()) {
            return compareArrayEqual(left, right);
        }
        if (right instanceof List)
            return DefaultGroovyMethods.equals(left, (List)right);
        return false;
    }

    public static boolean compareEqual(Object left, Object right) {
        if (left == right) return true;
        if (left == null || right == null) return false;

        if (left instanceof Comparable) {
            return compareToWithEqualityCheck(left, right) == 0;
        }

        // handle arrays on both sides as special case for efficiency
        Class leftClass = left.getClass();
        Class rightClass = right.getClass();
        if (leftClass.isArray() && rightClass.isArray()) {
            return compareArrayEqual(left, right);
        }
        if (leftClass.isArray() && leftClass.getComponentType().isPrimitive()) {
            left = primitiveArrayToList(left);
        }
        if (rightClass.isArray() && rightClass.getComponentType().isPrimitive()) {
            right = primitiveArrayToList(right);
        }
        if (left instanceof Object[] && right instanceof List) {
            return DefaultGroovyMethods.equals((Object[]) left, (List) right);
        }
        if (left instanceof List && right instanceof Object[]) {
            return DefaultGroovyMethods.equals((List) left, (Object[]) right);
        }
        if (left instanceof List && right instanceof List) {
            return DefaultGroovyMethods.equals((List) left, (List) right);
        }
        return left.equals(right);
    }

    private static boolean isValidCharacterString(Object value) {
        if (value instanceof String) {
            String s = (String) value;
            if (s.length() == 1) {
                return true;
            }
        }
        return false;
    }

    public static int compareToWithEqualityCheck(Object left, Object right) {
        if (left == right) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        else if (right == null) {
            return 1;
        }
        if (left instanceof Comparable) {
            if (left instanceof Number) {
                if (isValidCharacterString(right)) {
                    return DefaultGroovyMethods.compareTo((Number) left, (Character) box(castToChar(right)));
                }
                if (right instanceof Character || right instanceof Number) {
                    return DefaultGroovyMethods.compareTo((Number) left, castToNumber(right));
                }
            }
            else if (left instanceof Character) {
                if (isValidCharacterString(right)) {
                    return DefaultGroovyMethods.compareTo((Character)left,(Character)box(castToChar(right)));
                }
                if (right instanceof Number) {
                    return DefaultGroovyMethods.compareTo((Character)left,(Number)right);
                }
            }
            else if (right instanceof Number) {
                if (isValidCharacterString(left)) {
                    return DefaultGroovyMethods.compareTo((Character)box(castToChar(left)),(Number) right);
                }
            }
            else if (left instanceof String && right instanceof Character) {
                return ((String) left).compareTo(right.toString());
            }
            else if (left instanceof String && right instanceof GString) {
                return ((String) left).compareTo(right.toString());
            }
            if (left.getClass().isAssignableFrom(right.getClass())
                    || (right.getClass() != Object.class && right.getClass().isAssignableFrom(left.getClass())) //GROOVY-4046
                    || (left instanceof GString && right instanceof String)) {
                Comparable comparable = (Comparable) left;
                return comparable.compareTo(right);
            }
        }

        return -1; // anything other than 0
    }
}
