package org.mbte.groovypp.compiler.Issues

public class Issue51Test extends GroovyShellTestCase {
    void testStatic () {
        shell.evaluate """
            @Typed package p

            class C {
              static int N
              def foo() {
                Runnable r = [run : { def n = N }]
              }
            }

            new C().foo()
        """
    }

    void testNonStatic () {
        shell.evaluate """
            @Typed package p

            class C {
              int N
              def foo() {
                Runnable r = [run : { def n = N }]
              }
            }

            new C().foo()
        """
    }
}