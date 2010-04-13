package org.mbte.groovypp.remote.inet

@Typed static class InetDiscoveryInfo {
    UUID clusterId
    InetSocketAddress serverAddress

    static final long MAGIC = 0x23982392L;

    static byte [] toBytes(UUID clusterId, InetSocketAddress serverAddress) {
        def out = new ByteArrayOutputStream()
        def stream = new DataOutputStream(out)

        stream.writeLong(MAGIC)
        stream.writeLong(clusterId.getMostSignificantBits())
        stream.writeLong(clusterId.getLeastSignificantBits())
        stream.writeInt(serverAddress.port)
        def addrBytes = serverAddress.address.address
        stream.writeInt(addrBytes.length)
        stream.write(addrBytes)
        stream.close()
        out.toByteArray()
    }

    static InetDiscoveryInfo fromBytes (byte [] buf) {
        def input = new DataInputStream(new ByteArrayInputStream(buf))
        if (input.readLong() == MAGIC) {
            def uuid = new UUID(input.readLong(), input.readLong())
            def port = input.readInt()
            def addrLen = input.readInt()
            def addrBuf = new byte [addrLen]
            input.read(addrBuf)
            [clusterId:uuid, serverAddress:new InetSocketAddress(InetAddress.getByAddress(addrBuf), port)]
        }
    }

    void post(MessageChannel channel) {
        channel?.post(this) 
    }
}
