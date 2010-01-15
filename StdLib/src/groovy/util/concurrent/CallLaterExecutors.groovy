package groovy.util.concurrent

import java.util.concurrent.*

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
    private static CallLaterPool _defaultExecutor

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

    static void execute (Executor executor, Runnable...run) {
        for(r in run)
            executor.execute r
    }

    static synchronized CallLaterPool getDefaultExecutor () {
        if (!_defaultExecutor)
            _defaultExecutor = new CallLaterPool(0, Integer.MAX_VALUE, 1L, TimeUnit.SECONDS,new SynchronousQueue<Runnable>())
        _defaultExecutor
    }

    static CallLaterPool getCurrentExecutor () {
        def t = Thread.currentThread()
        if (t instanceof CallLaterPool.GroovyThread) {
            CallLaterPool.GroovyThread gt = t
            def pool = gt.pool
            if (pool)
                return pool
        }
        defaultExecutor
    }
}
