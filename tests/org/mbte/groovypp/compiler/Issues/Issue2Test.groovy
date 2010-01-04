package org.mbte.groovypp.compiler.Issues

public class Issue2Test extends GroovyShellTestCase {
    void testBug () {
        shell.evaluate """
            @Typed def u (def v) {
                v
            }

            @Typed def u2 () {
                assert !u()
            }
        """
    }
}