package org.mbte.groovypp.compiler.Issues

public class Issue28Test extends GroovyShellTestCase {
    void testBug () {
        shell.evaluate("""
            @Typed
            def foo () {
                [3,1,7,9,5].sort()
            }
            assert [1,3,5,7,9] == foo()
        """)
    }
}