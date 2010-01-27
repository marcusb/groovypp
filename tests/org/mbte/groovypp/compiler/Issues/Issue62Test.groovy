package org.mbte.groovypp.compiler.Issues

public class Issue62Test extends GroovyShellTestCase {
    void testOverload () {
        shell.evaluate """
        @Typed package p
        int f(Object o) {1}
        int f(List l) {2}
        assert 2 == f([])
        """
    }
}