package org.mbte.groovypp.compiler.Issues

public class Issue48Test extends GroovyShellTestCase{
    void testMe () {
        shell.evaluate """
        @Typed package p

        import java.util.concurrent.*

        int             n    = 2
        def pool = Executors.newFixedThreadPool( n );

        ( 0..<5 ).iterator().each( pool )
        {
            println "[\${ new Date()}]: [\$it]: [\${ Thread.currentThread() }] started";
            long t = System.currentTimeMillis();
            sleep(3000)
            println "[\${ new Date()}]: [\$it]: [\${ Thread.currentThread() }] finished - (\${ System.currentTimeMillis() - t } ms)";
        }.get();

        println "[\${ new Date()}]: all threads finished"
        pool.shutdown();
        """
    }
}