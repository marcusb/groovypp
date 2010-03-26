package org.mbte.groovypp.compiler.Issues

public class Issue198Test extends GroovyShellTestCase {
    void testMe() {
        try {
          shell.evaluate """
     @Typed class Test {
       static int a
       static main(args) {
          a = ""
       }
     }
  """
        } catch (ClassCastException cce) {
          assert cce.message.contains("Cannot cast object '' with class 'java.lang.String'")
        }
    }
}