package groovy.util.concurrent

import java.util.concurrent.Executor

@Typed abstract class FairExecutingChannel<M> extends QueuedChannel<M> implements Runnable {
    Executor executor

    void run () {
        for (;;) {
            def q = queue
            def removed = q.removeFirst()
            if (q.size() == 1) {
                if (queue.compareAndSet(q, busyEmptyQueue)) {
                    onMessage removed.first
                    if (!queue.compareAndSet(busyEmptyQueue, FQueue.emptyQueue)) {
                        executor.execute this
                    }
                    return
                }
            }
            else {
                if (queue.compareAndSet(q, removed.second)) {
                    onMessage removed.first
                    executor.execute this
                    return
                }
            }
        }
    }

    protected void signalPost(FQueue<M> oldQueue, FQueue<M> newQueue) {
        if (oldQueue !== busyEmptyQueue && newQueue.size() == 1)
            executor.execute this
    }
}
