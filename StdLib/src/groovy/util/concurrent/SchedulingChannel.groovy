package groovy.util.concurrent

@Typed abstract class SchedulingChannel<M> extends MessageChannel<M> implements Runnable {
    protected volatile FQueue<M> queue = FQueue.emptyQueue

    void post(M message) {
        for (;;) {
            def q = queue
            def newQ = queue.addLast(message)
            if (queue.compareAndSet(q, newQ)) {
                if (q.size() < concurrencyLevel)
                    schedule ()
                return
            }
        }
    }

    void run () {
        for (;;) {
            def q = queue
            def newQ = queue.removeFirst()
            if (queue.compareAndSet(q, newQ.second)) {
                onMessage(newQ.first)
                if (q.size() > concurrencyLevel)
                    schedule ()
                return
            }
        }
    }

    abstract void onMessage(M message)

    abstract void schedule ()

    protected int getConcurrencyLevel () { 1 }
}
