package org.mbte.groovypp.compiler.Issues

public class Issue51Test extends GroovyShellTestCase {
    void testBug () {
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
}