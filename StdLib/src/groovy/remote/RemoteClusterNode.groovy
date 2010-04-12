package groovy.remote

/**
 * Remote node in the cluster
 */
@Typed class RemoteClusterNode extends MessageChannel<Serializable> {
   final RemoteConnection connection

   final UUID remoteId

   RemoteClusterNode (RemoteConnection connection, UUID remoteId) {
       this.connection = connection
       this.remoteId = remoteId
   }

   void post(Serializable message) {
        connection.send(new ToMainActor(payLoad:message))
   }

   @Typed public static class ToMainActor extends RemoteMessage {
        Serializable payLoad
   }
}
