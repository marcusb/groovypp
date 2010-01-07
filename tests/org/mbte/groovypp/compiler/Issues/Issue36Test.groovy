package org.mbte.groovypp.compiler.Issues

import static groovy.CompileTestSupport.shouldCompile

public class Issue36Test extends GroovyShellTestCase {
    void testBug () {
        shouldCompile("""
            @Typed
            class B {
              final Map<String, Long> someMap = new HashMap<String, Long>()

              Long getSomeMap ( String s ) { return null }
            }

            @Typed
            class A {
              static Map<String, Long> top ( int n, Map<String, Long> map ) {
                return null
              }
              def foo() {
               top( 10, new B().getSomeMap())
              }
            }
            new A().foo()
        """)
    }
}