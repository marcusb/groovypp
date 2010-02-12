package org.mbte.groovypp.compiler.Issues

@Typed
class Issue46Test extends GroovyShellTestCase {

    void testMe()
    {
        shell.evaluate """
    @Typed package pack

import java.util.concurrent.Executor
import java.util.concurrent.Executors

    static class MyThread extends Thread
    {
        MyThread ( Runnable target )
        {
            super( target );
            println "MyThread created"
        }

        String toString () { "thread: \${super.toString()}"}
    }


    def pool = Executors.newFixedThreadPool( 3, [
        newThread : { Runnable r ->
           println "new thread";
           new MyThread( r )
        } ] 
    );

    [ 1, 2, 3 ].iterator().each(pool) { it ->
        try {
            println Thread.currentThread()
        }
        catch (Throwable t) {
            t.printStackTrace ()
        }
    }.get()
        """
    }
}

