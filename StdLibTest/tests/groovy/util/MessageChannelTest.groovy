package groovy.util

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import groovy.util.concurrent.FairExecutingChannel
import groovy.util.concurrent.ConcurrentlyExecutingChannel
import groovy.util.concurrent.ChannelExecutor
import java.util.concurrent.ExecutorService
import groovy.util.concurrent.FQueue
import groovy.util.concurrent.FairExecutingChannel
import groovy.util.concurrent.NonfairExecutingChannel
import groovy.util.concurrent.NonfairExecutingChannel
import groovy.util.concurrent.CallLaterExecutors

@Typed class MessageChannelTest extends GroovyTestCase {

    void testQueue () {
        FQueue q = FQueue.emptyQueue
        q = q.addLast(1)
        q = q.addLast(2)
        q = q.addLast(3)
        q = q.addLast(4)

        def r = q.removeFirst()
        assertEquals(1, r.first)
        q = r.second

        r = q.removeFirst()
        assertEquals(2, r.first)
        q = r.second

        r = q.removeFirst()
        assertEquals(3, r.first)
        q = r.second

        r = q.removeFirst()
        assertEquals(4, r.first)
    }

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
        FairExecutingChannel channel = { int msg ->
            println msg
            results << msg
            cdl.countDown()
        }
        channel.executor = CallLaterExecutors.currentExecutor
        for (i in 0..<100)
            channel << i

        cdl.await(10,TimeUnit.SECONDS)

        assertEquals 0..<100, results
    }

    void testConcurrentExecutor () {
//        def cdl = new CountDownLatch(100)
//        CopyOnWriteArrayList results = []
//        ConcurrentlyExecutingChannel channel = [
//          'super': [5],
//          onMessage: { msg ->
//            println msg
//            results << msg
//            cdl.countDown()
//        }]
//
//        for (i in 0..<100)
//            channel << i
//
//        cdl.await(10,TimeUnit.SECONDS)
//
//        assertEquals (0..<100, results.iterator().asList().sort())
    }

    void testRingFair () {
        runRingFair(new ChannelExecutor(Runtime.runtime.availableProcessors()))
    }

    void testRingFairStd () {
        runRingFair(Executors.newFixedThreadPool(Runtime.runtime.availableProcessors()))
    }

    void testRingNonFair () {
        runRingNonFair(new ChannelExecutor(Runtime.runtime.availableProcessors()))
    }

    void testRingNonFairStd () {
        runRingNonFair(Executors.newFixedThreadPool(Runtime.runtime.availableProcessors()))
    }

    private void runRingFair (ExecutorService pool) {
        def start = System.currentTimeMillis()
        MessageChannel last
        CountDownLatch cdl = [10000*500]
        for (i in 0..<10000) {
            FairExecutingChannel channel = {
                last?.post it
                cdl.countDown()
            }
            channel.executor = pool
            last = channel
        }
        for(i in 0..<500)
            last << "Hi"

        assertTrue(cdl.await(100,TimeUnit.SECONDS))
        assertTrue(pool.shutdownNow().empty)
        pool.awaitTermination(0L,TimeUnit.SECONDS)
        println(System.currentTimeMillis()-start) 
    }

    private void runRingNonFair (ExecutorService pool) {
        def start = System.currentTimeMillis()
        MessageChannel last
        CountDownLatch cdl = [10000*500]
        for (i in 0..<10000) {
            NonfairExecutingChannel channel = {
                last?.post it
                cdl.countDown()
            }
            channel.executor = pool
            last = channel
        }
        for(i in 0..<500)
            last << "Hi"

        assertTrue(cdl.await(100,TimeUnit.SECONDS))
        assertTrue(pool.shutdownNow().empty)
        pool.awaitTermination(0L,TimeUnit.SECONDS)
        println(System.currentTimeMillis()-start)
    }
}