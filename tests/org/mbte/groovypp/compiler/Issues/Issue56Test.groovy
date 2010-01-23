package org.mbte.groovypp.compiler.Issues

import org.codehaus.groovy.control.MultipleCompilationErrorsException

public class Issue56Test extends GroovyShellTestCase {
    void testMe () {
        try {
            shell.evaluate """
                @Typed
                class Test {
                    static main(String[] args) {
                        foo("1")
                    }
                    static foo(Integer x) {
                        println "foo(Integer) called - \$x"
                    }
                }
            """
        }
        catch (MultipleCompilationErrorsException err) {
            Object error = err.errorCollector.errors[0].cause
            assertTrue error.line != -1 && error.column != -1 
        }
    }
}