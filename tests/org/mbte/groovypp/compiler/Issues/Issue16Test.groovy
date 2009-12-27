package org.mbte.groovypp.compiler.Issues

public class Issue16Test extends GroovyShellTestCase {
    void testIssue16() {
      shell.evaluate """
      @Typed class C {
        static String LINES = "a\\n\b"
        static def foo() {
          LINES.eachLine {print it}
        }
      }
      C.foo()
      """
    }

    void testStaticField() {
      shell.evaluate """
      @Typed class C {
        static String LINES = "a\\n\b"
        static def foo() {
          LINES.eachLine {
            print "\$it \$LINES"
          }
        }
      }
      C.foo()
      """
    }
}