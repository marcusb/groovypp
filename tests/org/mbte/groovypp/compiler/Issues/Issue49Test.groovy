package org.mbte.groovypp.compiler.Issues

public class Issue49Test extends GroovyShellTestCase {
    void testBug () {
        shell.evaluate """
            @Typed package p

            import java.util.concurrent.Callable

            static void foo() {}  // static is important

            List<Callable<Void>> callables = []
            callables << [call : { foo() }]
        """
    }
}