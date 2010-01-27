package org.mbte.groovypp.compiler.Issues

public class Issue63Test extends GroovyShellTestCase {
    void testOverloadInaccessible () {
        shell.evaluate """
        @Typed package p
        class C {
          int f(Object o) {1}
          private int f(Integer i) {2}
        }
        assert 2 == new C().f(11)
        """
    }
}