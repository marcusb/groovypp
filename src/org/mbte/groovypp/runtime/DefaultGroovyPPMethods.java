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
}
