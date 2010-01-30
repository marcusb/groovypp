package org.mbte.groovypp.compiler.Issues

public class Issue2Test extends GroovyShellTestCase {
    void testBug () {
        shell.evaluate """
            @Typed package p 
            def u (def v = null) {
                v
            }

            assert !u()
        """
    }
}