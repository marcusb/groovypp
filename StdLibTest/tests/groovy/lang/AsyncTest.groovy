package groovy.lang

public class AsyncTest extends GroovyShellTestCase {
    void testAsync () {
        shell.evaluate """
            @Typed package p

            import java.util.concurrent.*
            import groovy.util.concurrent.*

            @Async int calculation (int a, int b) {
               a + b
            }

            assert 21 == calculation (10, 11, CallLaterExecutors.currentExecutor){ bl ->
                assert 21 == bl.get()
            }.get ()
        """
    }

    void testRemote () {
        shell.evaluate """
            @Typed package p

            import java.util.concurrent.*
            import groovy.util.concurrent.*

            @Async int calculation (int a, int b) {
               a + b
            }

            assert 21 == calculation (10, 11, CallLaterExecutors.currentExecutor){ bl ->
                assert 21 == bl.get()
            }.get ()
        """
    }
}