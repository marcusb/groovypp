package groovy.remote

abstract class RemoteConnection {
    protected final ClusterNode clusterNode

    RemoteConnection (ClusterNode clusterNode) {
        this.clusterNode = clusterNode
    }

    void onMessage (RemoteMessage msg) {
    }

    void send (RemoteMessage msg) {
        msg.senderNodeId = clusterNode.id
    }
}