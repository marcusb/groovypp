package org.mbte.groovypp.compiler.Issues

public class Issue66Test extends GroovyShellTestCase {
    void testMe () {
        shell.evaluate """
        @Typed package p
        class C {
          Object result
          void setResult(Object result) {
            this.result = result
          }
        }
        new C().setResult(null)
        """
    }
}