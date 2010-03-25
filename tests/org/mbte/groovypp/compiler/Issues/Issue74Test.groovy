package org.mbte.groovypp.compiler.Issues

import static groovy.CompileTestSupport.shouldNotCompile

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
      shouldNotCompile(script, 'Duplicate @Typed annotation found')
    }
}