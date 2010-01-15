package org.mbte.groovypp.compiler.Issues

@Typed class Issue9Test extends GroovyShellTestCase {
    void testEquals () {
        shell.evaluate """
        @Typed def u () {
            def x = []
            def y = [], z = x
            assert !(x != y)
            assert x == y
            assert x !== y
            assert z === x
        }
        u ()
        """
    }
}