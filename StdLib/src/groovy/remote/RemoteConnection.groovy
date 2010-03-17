package groovy.remote

@Typed abstract class RemoteConnection {
    ClusterNode       clusterNode
    RemoteClusterNode remoteNode

    void onConnect () {
        send(new RemoteMessage.Identity())
    }

    void onDisconnect () {
        clusterNode.onDisconnect(this)
    }

    void onMessage (Object message) {
        RemoteMessage msg = message
        if (!remoteNode) {
            if (msg instanceof RemoteMessage.Identity) {
                remoteNode = new RemoteClusterNode(this, msg.senderNodeId)
                clusterNode.onConnect(remoteNode)
            }
            else {
                throw new RuntimeException("protocol error: $msg.senderNodeId")
            }
        }
        else {
            clusterNode.onMessage(remoteNode, msg)
        }
    }

    void onException (Throwable cause) {
        disconnect()
        clusterNode.onException(this, cause)
    }

    final void send(RemoteMessage msg) {
        msg.senderNodeId = clusterNode.id
        doSend (msg)
    }

    protected abstract void doSend(RemoteMessage msg)

    protected abstract void disconnect()
}