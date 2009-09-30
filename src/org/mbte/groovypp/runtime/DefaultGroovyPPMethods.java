package org.mbte.groovypp.runtime;

import org.codehaus.groovy.runtime.typehandling.NumberMath;

public class DefaultGroovyPPMethods {
    public static Number plus (Number self, Number other) {
        return NumberMath.add(self, other);
    }

    public static Number minus (Number self, Number other) {
        return NumberMath.subtract(self, other);
    }

    public static Number multiply (Number self, Number other) {
        return NumberMath.multiply(self, other);
    }

    public static Number divide (Number self, Number other) {
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
}
