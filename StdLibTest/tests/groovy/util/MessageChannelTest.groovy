/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



package groovy.util

import groovy.util.concurrent.FQueue
import groovy.util.concurrent.FairExecutingChannel
import groovy.util.concurrent.NonfairExecutingChannel
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch

import java.util.concurrent.TimeUnit
import groovy.util.concurrent.FThreadPool
import java.util.concurrent.Executor

@Typed class MessageChannelTest extends GroovyTestCase {

    void testAfter () {
        Reference one = [0], two = [-1], count = [0]
        MessageChannel c = { int m -> one += m + count; count = count+1 }
        c = c.addAfter{ int m -> two += count + m; count = count +1 }
        c << 1
        assertEquals 1, one.get()
        assertEquals 1, two.get()
        assertEquals 2, count.get()
    }

    void testBefore () {
        Reference one = [0], two = [-1], count = [0]
        MessageChannel c = { int m -> one += m + count; count = count+1 }
        c = c.addBefore{ int m -> two += count + m; count = count +1 }
        c << 1
        assertEquals 2, one.get()
        assertEquals 0, two.get()
        assertEquals 2, count.get()
    }

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
        testWithFixedPool {
            def cdl = new CountDownLatch(100)
            CopyOnWriteArrayList results = []
            FairExecutingChannel channel = { int msg ->
                println msg
                results << msg
                cdl.countDown()
            }

            channel.executor = pool
            for (i in 0..<100)
                channel << i

            cdl.await(10,TimeUnit.SECONDS)
            assertEquals 0..<100, results
        }
    }

    void testRingFair () {
        testWithFixedPool {
            runRingFair(pool)
        }
    }

    void testRingNonFair () {
        testWithFixedPool {
            runRingNonFair(pool)
        }
    }

    void testRingFairFastPool () {
        FThreadPool pool = []
        runRingFair(pool)
        assertTrue(pool.shutdownNow().empty)
        assertTrue(pool.awaitTermination(10,TimeUnit.SECONDS))
    }

    void testRingNonFairFastPool () {
        FThreadPool pool = []
        runRingNonFair(pool)
        assertTrue(pool.shutdownNow().empty)
        assertTrue(pool.awaitTermination(10,TimeUnit.SECONDS))
    }

    private void runRingFair (Executor pool) {
        def start = System.currentTimeMillis()
        MessageChannel prev
        int nMessages = 500
        int nActors = 10000
        CountDownLatch cdl = [nActors*nMessages]
        for (i in 0..<nActors) {
            FairExecutingChannel channel = [
                onMessage: {
                    prev?.post it
                    cdl.countDown()
                },
                toString: {
                    "Channel $i"
                }
            ]
            channel.executor = pool
            prev = channel
        }
        for(i in 0..<nMessages)
            prev << "Hi"

        assertTrue(cdl.await(300,TimeUnit.SECONDS))
        println("runRingFair: ${pool.class.simpleName} ${System.currentTimeMillis()-start}")
    }

    private void runRingNonFair (Executor pool) {
        def start = System.currentTimeMillis()
        MessageChannel prev
        int nMessages = 500
        int nActors = 10000
        CountDownLatch cdl = [nActors*nMessages]
        for (i in 0..<nActors) {
            NonfairExecutingChannel channel = [
                onMessage: {
                    prev?.post it
                    cdl.countDown()
                },
                toString: {
                    "Channel $i"
                }
            ]
            channel.executor = pool
            prev = channel
        }
        for(i in 0..<nMessages)
            prev << "Hi"

        assertTrue(cdl.await(300,TimeUnit.SECONDS))
        println("runRingNonFair: ${pool.class.simpleName} ${System.currentTimeMillis()-start}")
    }
}