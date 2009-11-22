package org.mbte.groovypp.typeinference

import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.CountDownLatch


public class ExecutorTest extends GroovyTestCase {

    @Compile
    void testExecutor () {
        def es = Executors.newFixedThreadPool(10)
        def queue = new LinkedBlockingQueue ()
        def done = new CountDownLatch (1), start = new CountDownLatch(1)

        es.submit {
            start.countDown()

            int i = 0
            while (!(i == 9998)) {
                def obj = (PrimeRes)queue.take()
                if (obj) {
                    if (!(NONE == obj)) {
                        if (obj && obj.prime) {
                            println obj
                        }
                    }

                    ++i
                }
            }

            done.countDown()
        }

        start.await()
        (2..9999).each {
            long val = it
            es.submit {
                int divisor = 2
                while (true) {
                    if (divisor == val) {
                        queue.put (new PrimeRes(value:val, prime:true))
                        break;
                    }
                    if (val % divisor == 0) {
                        queue.put (new PrimeRes(value:val, prime:false))
                        break;
                    }
                    divisor++
                }
            }
        }

        done.await()
        es.shutdown()
    }

    void testExecutorGroovy () {
        def es = Executors.newFixedThreadPool(10)
        def queue = new LinkedBlockingQueue ()
        def done = new CountDownLatch (1), start = new CountDownLatch(1)

        es.submit {
            start.countDown()

            int i = 0
            while (!(i == 9998)) {
                def obj = (PrimeRes)queue.take()
                if (obj) {
                    if (!(NONE == obj)) {
                        if (obj && obj.prime) {
                            System.out.println obj
                        }
                    }

                    ++i
                }
            }

            done.countDown()
        }

        start.await()
        (2..9999).each {
            long val = it
            es.submit {
                int divisor = 2
                while (true) {
                    if (divisor == val) {
                        queue.put (new PrimeRes(value:val, prime:true))
                        break;
                    }
                    if (val % divisor == 0) {
                        queue.put (new PrimeRes(value:val, prime:false))
                        break;
                    }
                    divisor++
                }
            }
        }

        done.await()
        es.shutdown()
    }

    static private Object NONE = new Object ()
}

@Compile
class PrimeRes {
    int value
    boolean prime

    String toString () {
        "$value :$prime"
    }
}