package org.mbte.groovypp.compiler.Issues

import org.codehaus.groovy.control.MultipleCompilationErrorsException

public class Issue190Test extends GroovyShellTestCase {
  void testMe() {
        try {
          shell.evaluate """
  @Typed package p

  class Test {
      static main(args) {
          println Alpha.C
      }
  }
  enum Alpha { A, B }
  """
        } catch (MultipleCompilationErrorsException e) {
          assert e.message.contains("Cannot find property C of class Alpha")
        }
    }
}