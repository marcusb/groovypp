package groovy

import static groovy.CompileTestSupport.shouldCompile
import static groovy.CompileTestSupport.shouldNotCompile

class ForInTest extends GroovyShellTestCase {

  void testForIn() {
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


}