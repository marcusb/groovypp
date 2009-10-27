package org.mbte.groovypp.runtime;

import groovy.lang.GString;
import org.codehaus.groovy.runtime.DefaultGroovyMethodsSupport;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.typehandling.NumberMath;

import java.util.regex.Pattern;

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
}
