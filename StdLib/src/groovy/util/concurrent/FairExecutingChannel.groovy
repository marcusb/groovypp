package groovy.util.concurrent

@Typed abstract class FairExecutingChannel<M> extends ExecutingChannel<M> implements Runnable {
    void run () {
        for (;;) {
            def q = queue
            def removed = q.removeFirst()
            if (q.size() == 1) {
                if (queue.compareAndSet(q, busyEmptyQueue)) {
                    onMessage removed.first
                    if (!queue.compareAndSet(busyEmptyQueue, FQueue.emptyQueue)) {
                        schedule ()
                    }
                    return
                }
            }
            else {
                if (queue.compareAndSet(q, removed.second)) {
                    onMessage removed.first
                    schedule ()
                    return
                }
            }
        }
    }
}
