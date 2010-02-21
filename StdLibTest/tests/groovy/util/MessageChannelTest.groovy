package groovy.util

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import groovy.util.concurrent.ExecutingChannel
import groovy.util.concurrent.ConcurrentlyExecutingChannel
import groovy.util.concurrent.ChannelExecutor

@Typed class MessageChannelTest extends GroovyTestCase {
    void testMulti () {
        Reference count = [10]
        def multiplexor = new Multiplexor ()
        multiplexor.subscribe { int msg ->
            count = count-msg
            multiplexor.unsubscribe(this)
        }{ int msg ->
            count = count-msg
        }{ int msg ->
            count = count-msg
        }.subscribe { int msg ->
            count = count-1
            multiplexor.unsubscribe(this)
        }
        multiplexor << 2
        assertEquals 3, count.get ()

        multiplexor << 5
        assertEquals (-7, count.get ())
    }

    void testExecutor () {
        def cdl = new CountDownLatch(100)
        CopyOnWriteArrayList results = []
        ExecutingChannel channel = { int msg ->
            println msg
            results << msg
            cdl.countDown()
        }

        for (i in 0..<100)
            channel << i

        cdl.await(10,TimeUnit.SECONDS)

        assertEquals 0..<100, results
    }

    void testConcurrentExecutor () {
        def cdl = new CountDownLatch(100)
        CopyOnWriteArrayList results = []
        ConcurrentlyExecutingChannel channel = [
          'super': [5],
          onMessage: { msg ->
            println msg
            results << msg
            cdl.countDown()
        }]

        for (i in 0..<100)
            channel << i

        cdl.await(10,TimeUnit.SECONDS)

        assertEquals (0..<100, results.iterator().asList().sort())
    }

    void testRing () {
        runRing(new ChannelExecutor(Runtime.runtime.availableProcessors()))
    }

    void testRingStd () {
        runRing(Executors.newFixedThreadPool(Runtime.runtime.availableProcessors()))
    }

    private void runRing (Executor pool) {
        MessageChannel last
        CountDownLatch cdl = [10000*500]
        for (i in 0..<10000) {
            ExecutingChannel channel = {
                last?.post it
                cdl.countDown()
            }
            channel.executor = pool
            last = channel
        }
        for(i in 0..<500)
            last << "Hi"

        assertTrue(cdl.await(100,TimeUnit.SECONDS))
    }
}