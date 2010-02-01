package org.mbte.groovypp.compiler.Issues


public class Issue67Test extends GroovyShellTestCase {
    void testThisStatic () {
        shell.evaluate """
        @Typed package p
        class Test {
          static Closure cl = { String s -> s }
          static foo() {
            assert cl("Test") == "Test"
          }
        }
        Test.foo()
        """
    }

    void testThisInstance () {
        shell.evaluate """
        @Typed package p
        class Test {
          Closure cl = { String s -> s }
          def foo() {
            assert cl("Test") == "Test"
          }
        }
        new Test().foo()
        """
    }

    void testInstance () {
        shell.evaluate """
        @Typed package p
        class Test {
          Closure cl = { String s -> s }
        }
        new Test().cl("Test") == "Test"
        """
    }

    void testStatic () {
        shell.evaluate """
        @Typed package p
        class Test {
          static Closure cl = { String s -> s }
        }
        Test.cl("Test") == "Test"
        """
    }

  void testFunction2 () {
        shell.evaluate """
        @Typed package p
        class Test {
          static Function2<String, Object, String> cl = { s, obj -> s }
        }
        Test.cl("Test", []) == "Test"
        """
    }

}