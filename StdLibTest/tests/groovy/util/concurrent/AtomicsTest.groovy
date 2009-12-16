package groovy.util.concurrent

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorCompletionService

@Typed
public class AtomicsTest extends GroovyTestCase {

    void testAtom () {
        AtomicReference<FList<Integer>> atom = [FList.emptyList]

        def pool = new ExecutorCompletionService<Pair<Integer,FList<Integer>>>(Executors.newFixedThreadPool(5))
        def n = 100
        for(i in 0..<n)
            pool.submit {
                def newState = atom.apply { state ->
                    state.add i
                }
                (Pair)[i,newState]
            }

        for(i in 0..<n) {
            def resList  = pool.take().get().second
            def reversed = resList.reverse ()
            println resList.asList()
            println reversed.asList()
        }

        def res = atom.get().asList()
        res.sort()
        assertEquals (0..<n, res)
    }
}