package org.mbte.groovypp.compiler

public class ElvisTest extends GroovyShellTestCase {
  void testElvis() {
    def res = shell.evaluate("""
        @Typed
        def u () {
            def x = "1234", y = null
            [ x?: "unknown", y?:"unknown"]
        }

        u()
""")
    assertEquals(["1234", "unknown"], res)
  }
}