package groovy.remote

class RemoteMessage implements Serializable {
    /**
     * Id of remote node
     */
    UUID senderNodeId

    /**
    * First message send by client when connected to the server
    */
    static class Identity extends RemoteMessage {}
}