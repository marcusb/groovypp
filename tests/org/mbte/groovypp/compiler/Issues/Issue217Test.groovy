package org.mbte.groovypp.compiler.Issues

import static groovy.CompileTestSupport.shouldCompile

@Typed
public class Issue217Test extends GroovyShellTestCase {
    void testMe () {
        shouldCompile """
@Typed package p
abstract class Base {
  private int f
}

class Derived extends Base {
  private int f
  static void f() {
    int iii = new Derived().@f
  }
}
Derived.f()
        """
    }
}