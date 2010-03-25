package org.mbte.groovypp.compiler.Issues

import org.mbte.groovypp.compiler.DebugContext

public class Issue175Test extends GroovyShellTestCase {
  void testMe() {
        def baos = new ByteArrayOutputStream()
        def oldStream = DebugContext.outputStream
        try {
          DebugContext.outputStream = new PrintStream(baos)
          shell.evaluate """
            @Typed(debug=true)  // We check for debug output below!!!
            static def foo() {
              print "foo"
            }
            foo()
          """
        } finally {
          DebugContext.outputStream = oldStream
        }

        assert baos.toString().indexOf("forName") < 0
    }
}