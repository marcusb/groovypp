package groovy.util.concurrent

import java.util.concurrent.*
import org.junit.Assert

/**
 * Provide infrastructure for convinient work with thread pools
 *
 * Each thread knows which executor it belongs to so objects can easily submit tasks for the same
 * executor
 *
 * There is default executor, which is cached thread pool (of 1 sec keep alive threads) created on demand
 *
 * Groovy executors knows how to deal with CallLater type of tasks, which are more effective
 */
@Typed
class CallLaterExecutors {
    static CallLaterPool newFixedThreadPool(int nThreads) {
        new CallLaterPool(nThreads, nThreads,0L, TimeUnit.SECONDS,new LinkedBlockingQueue<Runnable>())
    }

    static CallLaterPool newCachedThreadPool() {
        new CallLaterPool(0, Integer.MAX_VALUE, 1L, TimeUnit.SECONDS,new SynchronousQueue<Runnable>())
    }

    static <T> BindLater<T> callLater (Executor executor, CallLater<T> future) {
        executor.execute future
        future
    }

    static <T> BindLater<T> apply (Executor executor, CallLater<T> future) {
        executor.execute future
        future
    }

    static void execute (Executor executor, Runnable...run) {
        for(r in run)
            executor.execute r
    }

    static void testWithFixedPool (Object self, int nThreads = Runtime.getRuntime().availableProcessors(), TestWithPool test) {
        test.pool = newFixedThreadPool(nThreads)
        test.test ()
    }

    static void testWithCachedPool (Object self, TestWithPool test) {
        test.pool = newCachedThreadPool()
        test.test ()
    }

    abstract static class TestWithPool extends Assert implements Runnable {
        CallLaterPool pool

        final void test () {
            pool.rejectedExecutionHandler = { run, pool ->
                throw new RejectedExecutionException("Task rejected: $run");
            }
            run ()
            assertTrue(pool.shutdownNow().empty)
            assertTrue(pool.awaitTermination(10,TimeUnit.SECONDS))
        }
    }
}
