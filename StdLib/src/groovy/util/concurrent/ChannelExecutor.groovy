package groovy.util.concurrent

import groovy.util.concurrent.FList
import groovy.util.concurrent.FQueue
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.CountDownLatch
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.LockSupport

@Typed class ChannelExecutor extends AbstractExecutorService {
    protected volatile FQueue<Runnable> queue = FQueue.emptyQueue

    private final int threadNumber

    private final CountDownLatch terminationCdl

    private volatile FList<Thread> waitingThread = FList.emptyList

    // 0 - running
    // 1 - shuting down
    private volatile int state

    ChannelExecutor (int threadNumber) {
        this.threadNumber = threadNumber
        terminationCdl = [threadNumber]
        def startLock = new CountDownLatch(threadNumber)
        for (i in 0..<threadNumber) {
            Thread t = [
                run: {
                    initThread (startLock)
                    loopThread ()
                },
                daemon:true
            ]
            t.start ()
        }
        startLock.await()
    }

    private void initThread (CountDownLatch startLock) {
        def thisThread = Thread.currentThread()
        for (;;) {
            def wt = waitingThread
            if (waitingThread.compareAndSet(wt, wt + thisThread)) {
                startLock.countDown()
                LockSupport.park()
                break
            }
        }
    }

    private void loopThread () {
        def thisThread = Thread.currentThread()
        for (;;) {
            def q = queue
            if (q.empty) {
                if (!state)
                    break;
                
                def wt = waitingThread
                if (waitingThread.compareAndSet(wt, wt + thisThread)) {
                    LockSupport.park()
                }
                continue
            }

            def newQ = q.removeFirst()
            if (queue.compareAndSet(q, newQ.second)) {
                newQ.first.run ()
            }
        }
        terminationCdl.countDown()
    }

    void schedule() {
        for (;;) {
            def wt = waitingThread
            if (!wt.size())
                break;

            if (waitingThread.compareAndSet(wt, wt.tail)) {
                LockSupport.unpark(wt.head)
                break;
            }
        }
    }

    void execute(Runnable command) {
        if (state)
            throw new RejectedExecutionException()

        for (;;) {
            def q = queue
            def newQ = queue.addLast(command)
            if (queue.compareAndSet(q, newQ)) {
                if (q.size() < threadNumber)
                    schedule ()
                break
            }
        }
    }

    void shutdown() {
        state = 1
        for (;;) {
            def wt = waitingThread
            if (!wt.size())
                break;

            if (waitingThread.compareAndSet(wt, wt.tail)) {
                LockSupport.unpark(wt.head)
            }
        }
    }

    public List<Runnable> shutdownNow() {
        state = 1
        for (;;) {
            def q = queue
            if (queue.compareAndSet(q, FQueue.emptyQueue)) {
                shutdown ()
                return q.iterator().asList ()
            }
        }
    }

    boolean isShutdown() {
        !state
    }

    boolean isTerminated() {
        !terminationCdl.count
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) {
        terminationCdl.await(timeout, unit)
    }
}
