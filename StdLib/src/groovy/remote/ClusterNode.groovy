package groovy.remote

import groovy.util.concurrent.CallLaterExecutors

/**
 * Local node in the claster
 */
@Typed class ClusterNode {
    /**
     * Unique id of this node over cluster
     */
    final UUID id = UUID.randomUUID()

    private volatile long nextObjectId

    private ClusterNodeServer server

    MessageChannel mainActor

    Multiplexor<CommunicationEvent> communicationEvents = []

    void setServer(ClusterNodeServer server) {
        server.clusterNode = this
        this.server = server
    }

    protected final long allocateObjectId () {
        nextObjectId.incrementAndGet ()
    }

    void start () {
        server.start ()
    }

    void stop () {
        server.stop ()
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
        mainActor = actor.async(CallLaterExecutors.defaultExecutor)
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
