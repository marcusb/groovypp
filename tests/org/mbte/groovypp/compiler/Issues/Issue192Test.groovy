package org.mbte.groovypp.compiler.Issues

import org.codehaus.groovy.control.MultipleCompilationErrorsException

public class Issue192Test extends GroovyShellTestCase {
  void testMe() {
        try {
          shell.evaluate """
  @Typed
  class Test {
      static main(args) {
          new A()
      }
      class A {
          A() {}
      }
  }
  """
        } catch (MultipleCompilationErrorsException e) {
          assert e.message.contains ('No enclosing instance passed in constructor call of a non-static inner class')
        }
    }
}