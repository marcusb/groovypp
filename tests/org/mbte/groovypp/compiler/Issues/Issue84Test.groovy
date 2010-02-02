package org.mbte.groovypp.compiler.Issues

public class Issue84Test extends GroovyShellTestCase {
    void testMe () {
        shell.evaluate """
        @Typed
        class Test {
            static main(args) {
                assert [0, 1][-1] == 1
                assert [a:10]["a"] == 10
            }
        }
        """
    }
}