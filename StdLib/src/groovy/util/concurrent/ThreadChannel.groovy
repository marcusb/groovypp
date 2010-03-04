package groovy.util.concurrent

import java.util.concurrent.locks.LockSupport

@Typed abstract class ThreadChannel<M> extends QueuedChannel<M> implements Runnable {
    private volatile Thread thread

    protected void signalPost(FQueue<M> oldQueue, FQueue<M> newQueue) {
        if (thread != Thread.currentThread()) {
            if (oldQueue !== busyEmptyQueue && newQueue.size() == 1) {
                LockSupport.unpark(thread)
            }
        }
    }

    public void run() {
        thread = Thread.currentThread()
        while(thread) {
            def q = queue
            if (q === busyEmptyQueue) {
                queue.compareAndSet(busyEmptyQueue, FQueue.emptyQueue)
                continue
            }

            if (q.empty) {
                LockSupport.park()
                continue
            }

            if (queue.compareAndSet(q, busyEmptyQueue)) {
                for(m in q) {
                    if (m)
                        onMessage m
                }
            }
        }
    }

    void stop () {
    }

    List<M> stopNow () {
    }
}