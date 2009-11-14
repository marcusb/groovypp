package org.mbte.groovypp.compiler

public class FiltersTest extends GroovyShellTestCase {
  void testSimple() {
    assertTrue shell.evaluate("""
        @Typed
        class C extends GroovyTestCase {
          def test() {
            assertEquals([0,3], (0..5).filter{it % 3 == 0})
            assertEquals(8, [1,2,3,5,8,13].find{it == 8})
            assertNull([1,2,3,5,8,13].find{it == 9})
            true
          }
        }
        new C().test()
        """)
  }
}