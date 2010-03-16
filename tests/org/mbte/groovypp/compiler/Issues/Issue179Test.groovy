package org.mbte.groovypp.compiler.Issues

public class Issue179Test extends GroovyShellTestCase {
  void testMe() {
        shell.evaluate """
          @Typed package p
          class A {
            static def foo() {}
          }

          class B extends A {
            def foo() {}
            static def bar() { foo() }
          }
          B.bar()
        """
    }
}