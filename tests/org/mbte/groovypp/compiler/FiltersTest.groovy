package org.mbte.groovypp.compiler

public class FiltersTest extends GroovyShellTestCase {
  void testSimple() {
    assertTrue shell.evaluate("""
        @Typed
        class C extends GroovyTestCase {
          def test() {
            assertEquals([0,3], (0..5).filter{it % 3 == 0})
            true
          }
        }
        new C().test()
        """)
  }
}