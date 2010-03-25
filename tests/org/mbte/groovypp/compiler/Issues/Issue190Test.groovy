package org.mbte.groovypp.compiler.Issues

import static groovy.CompileTestSupport.shouldNotCompile

public class Issue190Test extends GroovyShellTestCase {
  void testMe() {
        shouldNotCompile("""
  @Typed package p

  class Test {
      static main(args) {
          println Alpha.C
      }
  }
  enum Alpha { A, B }
  """,
  "Cannot find property C of class Alpha")
    }
}