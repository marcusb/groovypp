package org.mbte.groovypp.compiler.Issues

import org.codehaus.groovy.control.MultipleCompilationErrorsException

public class Issue74Test extends GroovyTestCase {
    void testDuplicateTyped1 () {
        runAndVerifyError """
            @Typed(TypePolicy.STATIC)
            @Typed(TypePolicy.DYNAMIC)
            package test

            class Foo {}
        """
    }

    void testDuplicateTyped2 () {
        runAndVerifyError """
            @Typed(TypePolicy.STATIC)
            @Typed(TypePolicy.DYNAMIC)
            class Foo {}
        """
    }

    void testDuplicateTyped3 () {
        runAndVerifyError """
            class Foo {
                @Typed(TypePolicy.STATIC)
                @Typed(TypePolicy.DYNAMIC)
                def Foo() {}
            }
        """
    }
    
    void testDuplicateTyped4 () {
        runAndVerifyError """
            class Foo {
                @Typed(TypePolicy.STATIC)
                @Typed(TypePolicy.DYNAMIC)
                def bar() {}
            }
        """
    }
    
    private runAndVerifyError(script) {
        try {
            GroovyShell shell = new GroovyShell()
            shell.evaluate(script)
            fail('Compilation should have failed as script has duplicate Typed annotation specified')
          } catch (MultipleCompilationErrorsException e) {
              def error = e.errorCollector.errors[0].cause
              assertTrue error.message.contains('Duplicate @Typed annotation found') 
              assertTrue error.line > 0 && error.column > 0
          }
    }
}