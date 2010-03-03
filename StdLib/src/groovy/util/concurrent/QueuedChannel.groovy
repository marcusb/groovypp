package groovy.util.concurrent

@Typed abstract class QueuedChannel<M> implements MessageChannel<M> {

    protected volatile FQueue<M> queue = FQueue.emptyQueue

    protected static final FQueue busyEmptyQueue = FQueue.emptyQueue + null

    MessageChannel<M> post(M message) {
        for (;;) {
            def oldQueue = queue
            def newQueue = (oldQueue === busyEmptyQueue ? FQueue.emptyQueue : oldQueue) + message
            if (queue.compareAndSet(oldQueue, newQueue)) {
                signalPost(oldQueue, newQueue)
                return this
            }
        }
    }

    protected abstract void signalPost (FQueue<M> oldQueue, FQueue<M> newQueue)

    abstract void onMessage(M message)
}