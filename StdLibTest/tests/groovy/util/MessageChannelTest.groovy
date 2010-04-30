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
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch

import java.util.concurrent.TimeUnit
import groovy.util.concurrent.FThreadPool
import java.util.concurrent.Executor
import groovy.channels.MessageChannel
import groovy.channels.MultiplexorChannel
import static groovy.channels.Channels.channel
import groovy.channels.Channels
import groovy.channels.ExecutingChannel

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

    void testFilter () {
        Reference one = [0]
        def c = channel{ int m -> one += m }.filter{ int m -> m & 1}
        for(i in 0..5)
          c << i
        assertEquals 9, one.get()
    }

    void testTransform () {
        def one = []
        def c = channel{ String m -> one << m.toUpperCase() }.map{ int m -> "" + 2*m }
        for(i in 0..4)
          c << i
        assertEquals(["0", "2", "4", "6", "8"], one)
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
        def multiplexor = new MultiplexorChannel ()
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
            ExecutingChannel channel = [onMessage: { msg ->
                if (msg instanceof Integer) {
                  results << msg
                  cdl.countDown()
                }
                else
                  super.onMessage(msg)
            },
            executor:pool]

            for (i in 0..<100)
                channel << i

            cdl.await(10,TimeUnit.SECONDS)
            assertEquals 0..<100, results
        }
    }

    void testExecute () {
        testWithFixedPool {
            def cdl = new CountDownLatch(100)
            CopyOnWriteArrayList results = []
            ExecutingChannel channel = [executor:pool, runFair:true]
            for (i in 0..<100)
                channel.execute {
                  results << i
                  cdl.countDown()
                }

            cdl.await(10,TimeUnit.SECONDS)
            assertEquals 0..<100, results
        }
    }

    void testRingFair () {
        testWithFixedPool {
          runRing(pool,true)
        }
    }

    void testRingNonFair () {
        testWithFixedPool {
          runRing(pool,false)
        }
    }

    void testRingFairFastPool () {
        FThreadPool pool = []
        runRing(pool,true)
        assertTrue(pool.shutdownNow().empty)
        assertTrue(pool.awaitTermination(10,TimeUnit.SECONDS))
    }

    void testRingNonFairFastPool () {
        FThreadPool pool = []
        runRing(pool,false)
        assertTrue(pool.shutdownNow().empty)
        assertTrue(pool.awaitTermination(10,TimeUnit.SECONDS))
    }

    private void runRing (Executor pool, boolean fair) {
        def start = System.currentTimeMillis()
        MessageChannel prev
        int nMessages = 500
        int nActors = 10000
        CountDownLatch cdl = [nActors*nMessages]
        for (i in 0..<nActors) {
            ExecutingChannel channel = [
                onMessage: {
                    if (it instanceof String) {
                      prev?.post it
                      cdl.countDown()
                    }
                    else {
                      super.onMessage(it)
                    }
                },
                toString: {
                    "Channel $i"
                },
                executor:pool,
                runFair:fair
            ]
            prev = channel
        }
        for(i in 0..<nMessages)
            prev << "Hi"

        assertTrue(cdl.await(300,TimeUnit.SECONDS))
        println("runRing($fair): ${pool.class.simpleName} ${System.currentTimeMillis()-start}")
    }
}