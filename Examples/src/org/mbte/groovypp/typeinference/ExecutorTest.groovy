package org.mbte.groovypp.typeinference

import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService

@Compile
public class ExecutorTest extends GroovyTestCase {
    void testExecutor () {

        def es = Executors.newCachedThreadPool()
        (0..99).each {
            def val = it
            es.submit {
                println "$val ${Thread.currentThread()}"
            }
        }

        Thread.sleep (1000L)
    }
}
