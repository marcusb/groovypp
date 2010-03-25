package org.mbte.groovypp.compiler.Issues

import static groovy.CompileTestSupport.shouldNotCompile

public class Issue192Test extends GroovyShellTestCase {
  void testMe() {
        shouldNotCompile("""
  @Typed
  class Test {
      static main(args) {
          new A()
      }
      class A {
          A() {}
      }
  }
  """,
  'No enclosing instance passed in constructor call of a non-static inner class')
    }
}