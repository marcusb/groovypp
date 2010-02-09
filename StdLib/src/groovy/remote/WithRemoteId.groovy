package groovy.remote

/**
 * Distributed address for object on remote node
 */
@Trait abstract class WithRemoteId {
    UUID remoteNodeId
    long id

    void setId (ClusterNode node) {
        remoteNodeId = node.id
        id = node.allocateObjectId ()
    }
}