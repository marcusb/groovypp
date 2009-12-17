package groovy.util.concurrent

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorCompletionService

@Typed
public class AtomicsTest extends GroovyTestCase {

    void testAtom () {
        AtomicReference<FList<Integer>> atom  = [FList.emptyList]
        AtomicReference<FQueue<FList<Integer>>> queue = [FQueue.emptyQueue]

        def pool = new ExecutorCompletionService<FQueue<FList<Integer>>>(Executors.newFixedThreadPool(5))
        def n = 100
        for(i in 0..<n) {
            pool.submit {
                def newState = atom.apply { state -> state + i }
                queue.apply { q -> q.addLast(newState) }
            }
        }

        for(i in 0..<n) {
            pool.take().get()
            def resList = queue.apply { q -> q.removeFirst() } { oldq, newq -> oldq.first }.second
            def reversed = resList.reverse ()
            println reversed.asList()
        }

        def res = atom.get().asList()
        res.sort()
        assertEquals (0..<n, res)
    }
}