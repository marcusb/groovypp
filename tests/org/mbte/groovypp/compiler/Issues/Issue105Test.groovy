package org.mbte.groovypp.compiler.Issues

import org.codehaus.groovy.control.MultipleCompilationErrorsException

public class Issue105Test extends GroovyShellTestCase {
    void testMe () {
       try {
         shell.evaluate """
           @Typed package p

           def user = null

           println user?.address?.street

           class Address {String street}
           class User {Address address}

        """
       } catch (MultipleCompilationErrorsException e) {
         def error = e.errorCollector.errors[0].cause
         assertTrue error.line > 0 && error.column > 0
       }
    }
}