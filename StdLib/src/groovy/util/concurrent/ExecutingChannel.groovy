package groovy.util.concurrent

import java.util.concurrent.Executor

@Typed abstract class ExecutingChannel<M> extends QueuedChannel<M> implements Runnable {
    Executor executor

    protected void signalPost(FQueue<M> oldQueue, FQueue<M> newQueue) {
        if (oldQueue !== busyEmptyQueue && newQueue.size() == 1)
            schedule ()
    }

    protected schedule() {
        executor.execute this
    }
}