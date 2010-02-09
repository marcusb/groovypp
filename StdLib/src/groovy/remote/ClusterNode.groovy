package groovy.remote

import java.util.concurrent.CopyOnWriteArraySet

/**
 * Local node in the claster
 */
class ClusterNode {
    /**
     * Unique id of this node over cluster
     */
    final UUID id = UUID.randomUUID()

    private volatile long nextObjectId

    protected long allocateObjectId () {
        nextObjectId.incrementAndGet ()
    }

    private final CopyOnWriteArraySet<MessageListener> messageListeners = []

    /**
     * Add message listener to the cluster node
     */
    ClusterNode addMessageListener (MessageListener listener) {
        messageListeners.add(listener)
    }

    /**
     * Remove message listener from the cluster node
     */
    ClusterNode removeMessageListener (MessageListener listener) {
        messageListeners.remove(listener)
    }

    /**
     * Listener for communication messages
     */
    abstract static class MessageListener {
        abstract void onMessage (RemoteNode from, Object message)
    }

    /**
     * Listener for discovery events
     */
    abstract static class DiscoveryListener {
        enum DiscoveryEventType {
            JOINED,
            LEFT,
            CRASHED
        }

        protected ClusterNode clusterNode

        abstract void onDiscoveryEvent (DiscoveryEventType eventType, RemoteNode node)
    }
}
