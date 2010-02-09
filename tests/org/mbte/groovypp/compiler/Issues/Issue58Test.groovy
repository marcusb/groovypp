package org.mbte.groovypp.compiler.Issues

public class Issue58Test extends GroovyShellTestCase {
    void testMe () {
        shell.evaluate """
        @Typed package p
        def x = { int i -> if (i > 0) doCall(i -1) }
        x(1000000)
        """
    }
}