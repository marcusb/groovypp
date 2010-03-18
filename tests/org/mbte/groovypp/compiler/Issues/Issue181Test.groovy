package org.mbte.groovypp.compiler.Issues

import static groovy.CompileTestSupport.shouldNotCompile

public class Issue181Test extends GroovyShellTestCase {
  void testMe() {
        shouldNotCompile """
@Typed
class Outer {
  int r = 0
  static void main(args) {
    new Inner().foo()
  }

  static class Inner {
    def foo() { r = 0 }
  }
}
        """
    }
}

