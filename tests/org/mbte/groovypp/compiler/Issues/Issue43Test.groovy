package org.mbte.groovypp.compiler.Issues

import static groovy.CompileTestSupport.shouldNotCompile

public class Issue43Test extends GroovyShellTestCase {
    void testUnassignableReturn () {
    shouldNotCompile """
      @Typed int foo () {
         new Date()
      }
      foo()
      """
  }
}