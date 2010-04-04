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

            testWithFixedPool {
                CountDownLatch cdl = [1]
                assert 21 == calculation (10, 11, pool){ bl ->
                    assert 21 == bl.get()
                    cdl.countDown ()
                }.get ()
                assert cdl.await(10L,TimeUnit.SECONDS)
            }
        """
    }
}