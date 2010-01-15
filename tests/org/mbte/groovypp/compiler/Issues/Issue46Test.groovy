package org.mbte.groovypp.compiler.Issues

@Typed
class Issue46Test extends GroovyShellTestCase {

    void testMe()
    {
        shell.evaluate """
    @Typed(debug=true) package pack

import java.util.concurrent.Executor
import java.util.concurrent.Executors

    static class MyThread extends Thread
    {
        MyThread ( target )
        {
            super( target );
            println "MyThread created"
        }

        String toString () { "thread: \${super.toString()}"}
    }


        Executor pool = Executors.newFixedThreadPool( 3, [ newThread : { Runnable r -> println "new thread"; new MyThread( r ) } ] );

        [ 1, 2, 3 ].iterator().each(pool) { it ->
            println Thread.currentThread();
        }
        """
    }
}

