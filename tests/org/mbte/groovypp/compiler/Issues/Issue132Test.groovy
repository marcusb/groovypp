package org.mbte.groovypp.compiler.Issues

import static groovy.CompileTestSupport.shouldNotCompile
import org.codehaus.groovy.control.MultipleCompilationErrorsException

public class Issue132Test extends GroovyShellTestCase {
    void testIllegalCycleInference() {
        try {
          shell.evaluate """
  @Typed class Test {
      static main(args) {
          def a = 1
          def b = 2
          def c

          for (i in a..b) {
              c = i
          }
      }
  }
  """
        } catch (MultipleCompilationErrorsException e) {
          assert e.message.contains ("IIlegal inference inside the loop. Consider making the variable's type explicit.")
        }
    }

  void testWeirdShouldNotHappen() {
        shell.evaluate """
@Typed
int xx() {
 def i = 0
 for (a in 2..100) {
   for (b in 2..100) {
     i++
     break;
   }
   break;
 }
}
xx()
"""
    }
}