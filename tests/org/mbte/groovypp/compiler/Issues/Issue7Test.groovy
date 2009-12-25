package org.mbte.groovypp.compiler.Issues

public class Issue7Test extends GroovyShellTestCase {
    void testFail() {
        shouldFail {
            shell.evaluate("""
                @Typed static u ()
                {
                    def j = 0
                    (0..<5).each{ j = j + 1 }
                    j
                }
                u ()
    """)
        }
    }
}