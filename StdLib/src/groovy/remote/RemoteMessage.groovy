package groovy.remote

class RemoteMessage implements Externalizable {
    /**
     * Id of remote node
     */
    UUID senderNodeId

    /**
    * First message send by client when connected to the server
    */
    static class Identity extends RemoteMessage {}

    void writeExternal(ObjectOutput out) {
        out.writeObject(senderNodeId);
    }

    void readExternal(ObjectInput input) {
        senderNodeId = input.readObject()
    }
}