package org.mbte.groovypp.compiler.Issues

public class Issue177Test extends GroovyShellTestCase {
  void testMe() {
        shell.evaluate """
          @Typed(TypePolicy.MIXED)
          def foo(p) {
            p[0]
          }
          assert foo([10, 1]) == 10
        """
    }
}