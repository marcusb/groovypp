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



package groovy.util.concurrent

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.CountDownLatch

@Typed
public class AtomicsTest extends GroovyTestCase {

    private CallLaterPool pool

    private final int n = 10000

    protected void setUp() {
//        super.setUp();
        pool = CallLaterExecutors.newFixedThreadPool(10)
    }

    protected void tearDown() {
        pool.shutdown()
//        super.tearDown();
    }

    void testAtom () {
        AtomicReference<FList<Integer>> atom  = [FList.emptyList]

        CountDownLatch cdl = [n]
        for(i in 0..<n) {
            pool.callLater {
                atom.apply { state -> state + i }
                i
            }.whenBound { future ->
                def value = future.get()
                if (value % 1000 == 999)
                    println value
                cdl.countDown()
            }
        }

        cdl.await()
        def res = atom.get().asList()
        res.sort()
        assertEquals (0..<n, res)
    }

    void testQueue () {
        AtomicReference<FQueue<Integer>> queue = [FQueue.emptyQueue]

        CountDownLatch cdl = [n]
        for(i in 0..<n) {
            pool.callLater {
                queue.apply { q -> q.addLast(i) }
                i
            }.whenBound { future ->
                def value = future.get()
                if (value % 1000 == 999)
                    println value
                cdl.countDown()
            }
        }

        cdl.await()
        FQueue q = queue.get()
        assertEquals (n, q.size ())
        def res = q.asList ()
        res.sort()
        assertEquals (0..<n, res)
    }

    void testMap () {
        AtomicReference<FHashMap<Integer,Integer>> map = [FHashMap.emptyMap]

        CountDownLatch cdl = [n]
        for(i in 0..<n) {
            pool.callLater {
                map.apply { m -> m.put(i,i) }
                i
            }.whenBound { future ->
                def value = future.get()
                if (value % 1000 == 999)
                    println value
                cdl.countDown()
            }
        }

        cdl.await()
        assertEquals (n, map.get().size ())
    }
}