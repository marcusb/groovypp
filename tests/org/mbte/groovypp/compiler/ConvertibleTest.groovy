package org.mbte.groovypp.compiler

import static groovy.CompileTestSupport.shouldNotCompile

class ConvertibleTest extends GroovyShellTestCase {
  void testFinalClassToInterface() {
    shouldNotCompile ("""
      @Typed package p
      final class C {}
      interface I {}
      I i = new C()
    """, "Cannot convert C to I")
  }

  void testInterfaceToFinalClass() {
    shouldNotCompile ("""
      @Typed package p
      final class C {}
      interface I {}
      C foo (I i) { (C)i }
      foo(null)
    """, "Cannot convert I to C")
  }
}
