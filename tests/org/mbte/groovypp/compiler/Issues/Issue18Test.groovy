package org.mbte.groovypp.compiler.Issues

public class Issue18Test extends GroovyShellTestCase {
    void testFail () {
        shell.evaluate """
            @Typed def u () {
                def j = 0
                for (int i in (0..4).step(2)) {
                   j += i
                }
                assert j == 6
            }
        """
    }
}