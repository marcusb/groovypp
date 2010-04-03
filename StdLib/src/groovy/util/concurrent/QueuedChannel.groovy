package groovy.util.concurrent

/**
 * Message channel with incoming queue
 */
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

    /**
     * Filter for incoming messages
     * Only messages the channel is interested in will be places in to processing queue
     */
    protected boolean interested (M message) { true }

    /**
     * Action (normally scheduling logic) to be taken after a message placed in to incoming queue
     */
    protected abstract void signalPost (FQueue<M> oldQueue, FQueue<M> newQueue)

    /**
     * Asynchronious message processing callback
     */
    protected abstract void onMessage(M message)
}