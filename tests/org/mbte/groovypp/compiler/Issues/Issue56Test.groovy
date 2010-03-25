package org.mbte.groovypp.compiler.Issues

import static groovy.CompileTestSupport.shouldNotCompile

public class Issue56Test extends GroovyShellTestCase {
    void testMe () {
        shouldNotCompile("""
                @Typed
                class Test {
                    static main(String[] args) {
                        foo(new Date())
                    }
                    static foo(Integer x) {
                        println "foo(Integer) called - \$x"
                    }
                }
            """)
    }
}