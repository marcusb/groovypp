package org.mbte.groovypp.runtime;

import org.codehaus.groovy.runtime.DefaultGroovyMethodsSupport;
import org.codehaus.groovy.runtime.InvokerHelper;

public class DefaultGroovyPPStaticMethods extends DefaultGroovyMethodsSupport {
    public static void print(Object self, Object value) {
        System.out.print(InvokerHelper.toString(value));
    }

    public static void println(Object self, Object value) {
        System.out.println(InvokerHelper.toString(value));
    }
}
