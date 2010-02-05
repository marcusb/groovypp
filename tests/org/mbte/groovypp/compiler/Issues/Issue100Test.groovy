package org.mbte.groovypp.compiler.Issues

import static groovy.CompileTestSupport.shouldNotCompile

@Typed
class Issue100Test extends GroovyShellTestCase {

    void testMe()
    {
        shouldNotCompile """
          @Typed package p
          class Person {
            String firstname
            String lastname
          }

          def props = [firstname: "F", lastname: "L"]

          def p = new Person(*:props)
        """
    }
}