package org.mbte.groovypp.compiler.Issues

import org.mbte.groovypp.compiler.DebugContext

public class Issue175Test extends GroovyShellTestCase {
  void testMe() {
        def baos = new ByteArrayOutputStream()
        DebugContext.outputStream = new PrintStream(baos)
        shell.evaluate """
          @Typed(debug = true)
          static def foo() {
            print "foo"
          }
          foo()
        """
        assert baos.toString().indexOf("forName") < 0
    }
}