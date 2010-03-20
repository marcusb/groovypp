package groovy.remote

import groovy.util.concurrent.CallLaterExecutors
import groovy.util.concurrent.SupervisedChannel

/**
 * Local node in the claster
 */
@Typed class ClusterNode extends SupervisedChannel {

    /**
     * Unique id of this node over cluster
     */
    final UUID id = UUID.randomUUID()

    private volatile long nextObjectId

    MessageChannel mainActor

    Multiplexor<CommunicationEvent> communicationEvents = []

    protected final long allocateObjectId () {
        nextObjectId.incrementAndGet ()
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

        abstract void onDiscoveryEvent (DiscoveryEventType eventType, RemoteClusterNode node)
    }

    void onConnect(RemoteClusterNode remoteNode) {
        communicationEvents << new ClusterNode.CommunicationEvent.Connected(remoteNode:remoteNode)
    }

    void onDisconnect(RemoteConnection connection) {
        communicationEvents << new ClusterNode.CommunicationEvent.Disconnected(connection:connection)
    }

    void onMessage(RemoteClusterNode remoteNode, RemoteMessage message) {
        switch(message) {
            case RemoteClusterNode.ToMainActor:
                    mainActor?.post(((RemoteClusterNode.ToMainActor)message).payLoad)
                break;
        }
    }

    void onException(RemoteConnection connection, Throwable cause) {
        cause.printStackTrace()
    }

    void setMainActor(MessageChannel actor) {
        mainActor = actor.async(CallLaterExecutors.currentExecutor)
    }

    static class CommunicationEvent {
        static class TryingConnect extends CommunicationEvent{
            UUID uuid
            String address

            String toString () {
                "trying to connect to $uuid @ $address"
            }
        }

        static class Connected extends CommunicationEvent{
            RemoteClusterNode remoteNode

            String toString () {
                "connected to ${remoteNode.remoteId}"
            }
        }

        static class Disconnected extends CommunicationEvent{
            RemoteConnection connection

            String toString () {
                "disconnected from ${connection.remoteNode?.remoteId}"
            }
        }
    }
}
