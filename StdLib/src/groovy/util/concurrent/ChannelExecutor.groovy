package groovy.util.concurrent

import java.util.concurrent.Executor
import java.util.concurrent.CountDownLatch
import java.util.concurrent.locks.LockSupport

@Typed class ChannelExecutor extends SchedulingChannel<Runnable> implements Executor {
    final int threadNumber

    private volatile FList<Thread> waitingThread = FList.emptyList

    ChannelExecutor (int threadNumber) {
        this.threadNumber = threadNumber
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

    void onMessage(Runnable command) {
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
            if (!q.size()) {
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
    }

    public void schedule() {
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

    public void execute(Runnable command) {
        post command
    }

    protected int getConcurrencyLevel () {
        threadNumber
    }
}
