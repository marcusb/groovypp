package org.mbte.groovypp.compiler

public class CompareTest extends GroovyShellTestCase{
  void testMath () {
    shell.evaluate("""
      @Compile
      def u () {
        assert (10l == 10)
        assert (10 > 5G)
        assert !(10 < 5.0f)
        assert (10 >= 5)
        assert !(10d <= 5)
        assert (10l != 5d)
      }
      u ()
    """)
  }
}