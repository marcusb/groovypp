package org.mbte.groovypp.compiler.Issues

import org.codehaus.groovy.control.MultipleCompilationErrorsException

public class Issue186Test extends GroovyShellTestCase {
    void testConstructorCallOnAnInterface() {
        try {
          shell.evaluate """
            @Typed package dummy
            
            class Test186 {
                static main(args) {
                    def obj = new I186()
                }
            }
            
            interface I186 {}

          """
        } catch (MultipleCompilationErrorsException e) {
            assert e.message.contains ("You cannot create an instance from the abstract interface")
        }
    }
}