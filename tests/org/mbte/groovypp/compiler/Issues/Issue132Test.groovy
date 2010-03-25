package org.mbte.groovypp.compiler.Issues

import static groovy.CompileTestSupport.shouldNotCompile

public class Issue132Test extends GroovyShellTestCase {
    void testIllegalCycleInference() {
          shouldNotCompile("""
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
  """,
  "IIlegal inference inside the loop. Consider making the variable's type explicit.")
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