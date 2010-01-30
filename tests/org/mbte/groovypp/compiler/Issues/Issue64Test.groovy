package org.mbte.groovypp.compiler.Issues

public class Issue64Test extends GroovyShellTestCase {
    void testMe () {
        shell.evaluate """
        @Typed package p
        class C {
          final int f
          C(int f) { this.f = f }
        }
        new C(0)
        """
    }
}