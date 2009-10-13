package org.mbte.groovypp.compiler

public class NullCompareTest extends GroovyShellTestCase {

  void testNullCompare() {
    shell.evaluate """

      @Typed
      def u() {
        def a = new Object()
        println a != null
        assert a != null
      }

      u()
    """
  }
}