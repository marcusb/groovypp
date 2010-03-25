package org.mbte.groovypp.compiler.Issues

import static groovy.CompileTestSupport.shouldNotCompile

public class Issue186Test extends GroovyShellTestCase {
    void testConstructorCallOnAnInterface() {
        shouldNotCompile("""
            @Typed package dummy
            
            class Test186 {
                static main(args) {
                    def obj = new I186()
                }
            }
            
            interface I186 {}

          """,
         "You cannot create an instance from the abstract interface")
    }
}