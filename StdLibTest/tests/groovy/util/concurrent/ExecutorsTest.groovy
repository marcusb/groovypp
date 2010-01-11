package groovy.util.concurrent

public class ExecutorsTest extends GroovyShellTestCase {
    void testCompile () {
        shell.evaluate """
            import java.util.concurrent.*

            @Typed def u () {
               Executors.newFixedThreadPool(2).execute{}{}
            }
        """
    }
}