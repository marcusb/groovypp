package groovy.util.concurrent

import java.util.concurrent.Executor

@Typed abstract static class ExecutingChannel<M> implements MessageChannel<M>, Runnable  {
    protected volatile FQueue<M> queue = FQueue.emptyQueue

    private static final FQueue dummy = FQueue.emptyQueue.addFirst(null)

    Executor executor

    ExecutingChannel() {
        this.executor = CallLaterExecutors.getCurrentExecutor()
    }

    ExecutingChannel(Executor executor) {
        this.executor = executor
    }

    MessageChannel<M> post(M message) {
        for (;;) {
            def q = queue
            def newQ = queue.addLast(message)
            if (queue.compareAndSet(q, newQ)) {
                if (!q.size())
                    executor.execute(this)
                return this
            }
        }
    }

    void run () {
        for (;;) {
            def q = queue
            if (queue.compareAndSet(q, dummy)) {
                for(m in q) {
                    if (m)
                        onMessage(m)
                }
                if(!queue.compareAndSet(dummy, FQueue.emptyQueue)) {
                    executor.execute this
                }
                break
            }
        }
    }

    abstract void onMessage(M message)
}
