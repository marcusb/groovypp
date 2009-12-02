package groovy

import static groovy.CompileTestSupport.shouldCompile
import static groovy.CompileTestSupport.shouldNotCompile

class ForInTest extends GroovyShellTestCase {

  void testForInList() {
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

  void testForInIntArray() {
    def res = shell.evaluate("""
      @Typed
      public def foo() {
        def res = []
        int[] arr = [0,1,2]
        for (i in arr) res << i.byteValue()
        res
      }
      foo()
    """)
    assertEquals([0,1,2], res)
  }

  void testForIterator() {
    def res = shell.evaluate("""
      @Typed
      public def foo() {
        def res = []
        def it = [0,1,2].iterator()
        for (i in it) res << i.byteValue()
        res
      }
      foo()
    """)
    assertEquals([0,1,2], res)
  }

  void testForReader() {
    def res = shell.evaluate("""
      @Typed
      public def foo(Reader r) {
        def res = []
        for (str in r) res << str.toLowerCase()
        res
      }
      foo(new StringReader("Schwiitzi\\nNati"))
    """)
    assertEquals(["schwiitzi", "nati"], res)
  }

}