package groovy.util.concurrent

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.Executors
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorCompletionService

@Typed
public class AtomicsTest extends GroovyTestCase {

    abstract static class FunctionalList<T> implements Iterable<T> {
        static final EmptyFunctionalList empty = []

        abstract T                 getHead ()
        abstract FunctionalList<T> getTail ()

        static class EmptyFunctionalList<T> extends FunctionalList<T> {
            private EmptyFunctionalList () {}

            T                 getHead () { throw new UnsupportedOperationException() }
            FunctionalList<T> getTail () { throw new UnsupportedOperationException() }

            Iterator iterator () { [hasNext:{false}, next:{throw new UnsupportedOperationException()}, remove:{throw new UnsupportedOperationException()}] }
        }

        static class Node<T> extends FunctionalList<T> implements Iterable<T> {
            T                 head
            FunctionalList<T> tail

            Iterator iterator () {
                [
                        cur: (FunctionalList<T>)this,
                        hasNext:{ cur != FunctionalList.empty },
                        next:   { def that = cur; cur = cur.tail; that.head }, 
                        remove:{throw new UnsupportedOperationException()}
                ]
            }
        }

        FunctionalList<T> add (T element) {
            (Node)[head:element, tail:this]
        }

        T call () { getHead() }
    }

    static class Atom<S> extends AtomicReference<S> {
        Atom () {}
        Atom (S state) { super(state) }
    }

    void testAtom () {
        Atom<FunctionalList<Integer>> atom = [FunctionalList.empty]
        def pool = new ExecutorCompletionService(Executors.newFixedThreadPool(5))
        def n = 100
        for(i in 0..<n)
            pool.submit {
                def newState = atom.apply { state ->
                    state.add i
                }
                (Pair)[i,newState]
            }

        for(i in 0..<n)
            println pool.take().get()

        def l = atom.get().iterator().toList()
        l.sort()
        assertEquals (0..<n, l)
    }
}