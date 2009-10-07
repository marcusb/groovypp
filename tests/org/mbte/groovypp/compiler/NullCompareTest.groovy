package org.mbte.groovypp.compiler

public class NullCompareTest extends GroovyShellTestCase {

  void testNullCompare() {
      shell.evaluate  """

      @Typed
      def u() {
        def a = new Object()
        assert a != null
      }

      u()
    """
  }
}