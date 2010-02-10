package groovy.remote

/**
 * Remode node in the claster
 */
class RemoteNode {
   protected final ClusterNode clusterNode

   final UUID remoteId

   RemoteNode (ClusterNode clusterNode, UUID remoteId) {
       this.clusterNode = clusterNode
       this.remoteId = remoteId
   }

   final void sendMessage (Object message) {
       clusterNode.sendMessage (remoteId, message)
   }
}
