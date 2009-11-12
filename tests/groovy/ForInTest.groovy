package groovy

import static groovy.CompileTestSupport.shouldCompile
import static groovy.CompileTestSupport.shouldNotCompile

class ForInTest extends GroovyShellTestCase {

  void testForInString() {
    def res = shell.evaluate("""
      @Typed
      public def foo(List<String> l) {
        def res = []
        for (el in l) res << el.toLowerCase()
        res
      }
      foo(["Es", "GEHT"])
    """)
    assertEquals(["es", "geht"], res)
  }
  void testForInIntRange() {
    def res = shell.evaluate("""
      @Typed
      public def foo() {
        def res = []
        for (i in 0..2) res << i.byteValue()
        res
      }
      foo()
    """)
    assertEquals([0,1,2], res)
  }

}