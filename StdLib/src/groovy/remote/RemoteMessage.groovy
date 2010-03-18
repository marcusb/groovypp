package groovy.remote

@Typed class RemoteMessage implements Externalizable {
    /**
     * Id of remote node
     */
    UUID senderNodeId

    /**
    * First message send by client when connected to the server
    */
    static class Identity extends RemoteMessage {}

    static class ForwardMessage extends RemoteMessage {
        UUID          recipient
        RemoteMessage payLoad
    }
}