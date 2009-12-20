package groovy.util.concurrent

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.CountDownLatch

@Typed
public class AtomicsTest extends GroovyTestCase {

    void testAtom () {
        AtomicReference<FList<Integer>> atom  = [FList.emptyList]
        AtomicReference<FQueue<FList<Integer>>> queue = [FQueue.emptyQueue]
        Agent printer = []

        def pool = CallLaterExecutors.newFixedThreadPool(10)
        def n = 1000
        CountDownLatch cdl = [n]
        for(i in 0..<n) {
            callLater(pool) {
                def newState = atom.apply { state -> state + i }
                queue.apply { q -> q.addLast(newState) }
                (Pair)[i, newState]
            }.whenBound { future ->
                def string = future.get().second.reverse().asList().toString()
                printer.apply { println string; cdl.countDown() }
            }
        }

        cdl.await()
        def res = atom.get().asList()
        res.sort()
        assertEquals (0..<n, res)
    }
}