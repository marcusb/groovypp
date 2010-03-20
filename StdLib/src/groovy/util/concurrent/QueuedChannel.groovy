package groovy.util.concurrent

@Typed abstract class QueuedChannel<M> extends MessageChannel<M> {

    protected volatile FQueue<M> queue = FQueue.emptyQueue

    protected static final FQueue busyEmptyQueue = FQueue.emptyQueue + null

    final void post(M message) {
        if (interested(message)) {
            for (;;) {
                def oldQueue = queue
                def newQueue = (oldQueue === busyEmptyQueue ? FQueue.emptyQueue : oldQueue) + message
                if (queue.compareAndSet(oldQueue, newQueue)) {
                    signalPost(oldQueue, newQueue)
                    return
                }
            }
        }
    }

    protected boolean interested (M message) { true }

    protected abstract void signalPost (FQueue<M> oldQueue, FQueue<M> newQueue)

    protected abstract void onMessage(M message)
}