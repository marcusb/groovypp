package groovy.util

import org.codehaus.groovy.runtime.DefaultGroovyMethods
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.Executor
import groovy.util.concurrent.BindLater
import groovy.util.concurrent.FList

/**
 * Utility methods to iterate over objects of standard types.
 */
@Typed
abstract class Iterations {
    /**
     * Computes the aggregate of the given iterator applying the operation to the first iterator element first.
     * @param self input iterator.
     * @param init the initial value to be passed on op first invocation.
     * @param op the function that computes aggregate given the current element and its own result computed so far.
     * @return computed aggregated value.
     */
    static <T, R> R foldLeft(Iterator<T> self, R init, Function2<T, R, R> op) {
        self.hasNext() ? foldLeft(self, op.call(self.next(), init), op) : init
    }

    /**
     * Computes the aggregate of the given iterator applying the operation to the first element first.
     * @param self input @link{Iterable} object.
     * @param init the initial value to be passed on op first invocation.
     * @param op the function that computes aggregate given the current element and its own result computed so far.
     * @return computed aggregated value.
     */
    static <T, R> R foldLeft(Iterable<T> self, R init, Function2<T, R, R> op) {
        foldLeft(self.iterator(), init, op)
    }

    static <T, R> R foldLeft(T[] self, R init, Function2<T, R, R> op) {
        foldLeft(self.asList(), init, op)
    }

    /**
     * Computes the aggregate of the given iterator applying the operation to the last iterator element first.
     * @param self input iterator.
     * @param init the initial value to be passed on op first invocation.
     * @param op the function that computes aggregate given the current element and its own result computed so far.
     * @return computed aggregated value.
     */
    static <T, R> R foldRight(Iterator<T> self, R init, Function2<T, R, R> op) {
      def rev = FList.emptyList
      while(self.hasNext()) {
        rev = rev + self.next()
      }
      foldLeft(rev.iterator(), init, op)
    }

    /**
     * Computes the aggregate of the given iterator applying the operation to the last element first.
     * @param self input @link{Iterable} object.
     * @param init the initial value to be passed on op first invocation.
     * @param op the function that computes aggregate given the current element and its own result computed so far.
     * @return computed aggregated value.
     */
    static <T, R> R foldRight(Iterable<T> self, R init, Function2<T, R, R> op) {
        foldRight(self.iterator(), init, op)
    }

    static <T, R> R foldRight(T[] self, R init, Function2<T, R, R> op) {
        foldRight(self.asList(), init, op)
    }

    /**
     * Applies given function to each iterator element. The result of function application is discarded.
     * @param self input iterator.
     * @param op function to be applied.
     */
    static <T> void each(Iterator<T> self, Function1<T, Object> op) {
        while (self.hasNext()) op.call(self.next())
    }

    /**
     * Applies given function to each iterator element. The result of function application is discarded.
     * @param self input iterator.
     * @param op function to be applied.
     */
    static <T> BindLater each(Iterator<T> self, Executor executor, int concurrency = 0, Function1<T, Object> op) {
        if (!concurrency)
            concurrency = Runtime.getRuntime().availableProcessors()

        BindLater.Group result = [concurrency]
        for (j in 0..concurrency) {
            result.attach(executor.callLater {
                while(!result.isDone()) {
                    T el
                    synchronized (self) {
                        if (!self)
                            break

                        el = self.next()
                    }

                    op [el]
                }
            })
        }
        result
    }

    /**
     * Applies given function to each element. The result of function application is discarded.
     * @param self input @link{Iterable} object.
     * @param op function to be applied.
     */
    static <T> void each(Iterable<T> self, Function1<T, Object> op) {
        each(self.iterator(), op)
    }

    /**
     * Applies given function to each array element. The result of function application is discarded.
     * @param self input array.
     * @param op function to be applied.
     */
    static <T> void each(T[] self, Function1<T, Object> op) {
        each(self.iterator(), op)
    }

    /**
     * Applies given function to each enumeration element. The result of function application is discarded.
     * @param self input {java.util.Enumeration} object.
     * @param op function to be applied.
     */
    static <T> void each(Enumeration<T> self, Function1<T, Object> op) {
        each(self.iterator(), op)
    }

    /**
     * Applies given function to each map element. The result of function application is discarded.
     * @param self input {java.util.Map} object.
     * @param op function to be applied.
     */
    static <K, V> void each(Map<K, V> self, Function2<K, V, Object> op) {
        def it = self.entrySet().iterator()
        while (it.hasNext()) {
            def el = it.next()
            op.call(el.key, el.value)
        }
    }

    /**
     * Default dynamic iteration.
     */
    static <T> T each(T self, Closure closure) {
        return DefaultGroovyMethods.each(self, closure)
    }

    static <T> BlockingQueue<T> leftShift(BlockingQueue<T> queue, Iterator<T> iter) {
        for (el in iter) {
            queue << iter
        }
    }

    static <T> SupplyStream<T> supply(Iterator<T> iterator) {
        [
            take: { synchronized (iterator) { iterator ? iterator.next() : null } },
            poll: {long timeout, TimeUnit unit -> take () }
        ]
    }

    static <T> SupplyStream<T> supply(Iterable<T> iterable) {
        iterable.iterator().supply ()
    }
}
