package org.mbte.groovypp.compiler.Issues

@Typed
class Issue99Test extends GroovyShellTestCase {

    void testMe()
    {
        shell.evaluate """
          @Typed package p
          assert 0.6 == 1.0 + 2.0 - 3.0 * 4 / 5
        """
    }
}