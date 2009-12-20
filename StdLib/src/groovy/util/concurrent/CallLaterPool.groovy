package groovy.util.concurrent

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

@Typed
class CallLaterPool extends ThreadPoolExecutor {
    CallLaterPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, new DefaultThreadFactory())
        ((DefaultThreadFactory)getThreadFactory()).pool = this
    }

    public <T> BindLater<T> callLater (CallLater<T> future) {
        execute future
        future
    }

    protected static class GroovyThread extends Thread {
       final ExecutorService pool

       GroovyThread (ExecutorService pool, ThreadGroup group, Runnable r, String name) {
           super(group, r, name, 0)
           this.@pool = pool
           setPriority(Thread.NORM_PRIORITY)
           setDaemon(false)
       }
    }

    private static class DefaultThreadFactory implements ThreadFactory {
        static final AtomicInteger poolNumber = [1]

        final AtomicInteger threadNumber = [1]
        final ThreadGroup group
        final String namePrefix

        CallLaterPool pool

        DefaultThreadFactory() {
            def s = System.getSecurityManager();
            this.@group = s ? s.getThreadGroup() : Thread.currentThread().threadGroup
            this.@namePrefix = "groovy-pool-" + poolNumber.getAndIncrement() + "-thread-"
        }

        GroovyThread newThread(Runnable r) {
            [pool, group, r, namePrefix + threadNumber.getAndIncrement()]
        }
    }
}
