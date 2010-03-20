package groovy.remote

@Typed abstract class RemoteConnection {
    ClusterNode       clusterNode
    RemoteClusterNode remoteNode

    void onConnect () {
        send(new RemoteMessage.Identity())
    }

    void onDisconnect () {
        clusterNode?.onDisconnect(this)
        clusterNode = null
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
        onDisconnect()
        if (!(cause instanceof IOException))
            clusterNode.onException(this, cause)
    }

    final void send(RemoteMessage msg) {
        msg.senderNodeId = clusterNode.id
        doSend (msg)
    }

    protected abstract void doSend(RemoteMessage msg)
}