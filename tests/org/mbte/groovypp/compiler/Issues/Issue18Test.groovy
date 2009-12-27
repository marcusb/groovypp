package org.mbte.groovypp.compiler.Issues

public class Issue18Test extends GroovyShellTestCase {
    void testFail () {
        shell.evaluate """
            @Typed(debug=true) def u () {
                def j = 0
                for (int i in (0..12).step(2)) {
                   j += i
                }
            }
        """
    }
}