package org.mbte.groovypp.compiler.Issues

public class Issue179Test extends GroovyShellTestCase {
  void testMe() {
        shell.evaluate """
          @Typed package p
          class A {
            static def foo() {"Afoo"}
          }

          class B extends A {
            def foo() {"Bfoo"}
            static def bar() { foo() }
          }
          assert "Afoo" == B.bar()
        """
    }
}