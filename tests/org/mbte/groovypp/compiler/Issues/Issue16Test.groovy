package org.mbte.groovypp.compiler.Issues

import static groovy.CompileTestSupport.shouldNotCompile

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

    void testIssue17() {
      shouldNotCompile """
        @Typed
        class C {
          def foo() {}
          static def bar() {foo()}
        }
        C.bar()
      """
    }

    void testIssue17a() {
      shouldNotCompile """
        @Typed
        class C {
          def foo() {}
          static def bar() {C.foo()}
        }
        C.bar()
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