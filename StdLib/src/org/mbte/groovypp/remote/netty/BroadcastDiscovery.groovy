package org.mbte.groovypp.remote.netty;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.UUID
import groovy.util.concurrent.NonfairExecutingChannel;

@Typed class BroadcastDiscovery extends SupervisedChannel {
    private static InetAddress GROUP;
    private static final int PORT = 4238;
    private static final long MAGIC = 0x23982392L;

    abstract static class BroadcastThread extends SupervisedChannel {
        InetAddress group
        int         port

        private MulticastSocket socket
        private volatile boolean stopped

        abstract void loopAction ()

        void doStart() {
            executor.execute {
                try {
                    socket = new MulticastSocket(port);
                    socket.joinGroup(group);
                    while (!stopped)
                        loopAction ()
                }
                catch(Throwable t) {
                    stopped = true
                    crash(t)
                }
                socket.close()
            }
        }

        void doStop() {
            stopped = true;
        }
    }

    static class Sender extends BroadcastThread {
        long    sleepPeriod = 1000L
        byte [] dataToTransmit

        void loopAction () {
            socket.send ([dataToTransmit, dataToTransmit.length, group, port])
            Thread.currentThread().sleep(sleepPeriod);
        }
    }

    static class Receiver extends BroadcastThread {
        void loopAction () {
            def buffer = new byte [512]
            def packet = new DatagramPacket(buffer, buffer.length)
            socket.receive(packet)
            owner?.post(buffer)
        }
    }

    UUID uid
    InetSocketAddress address

    static {
        try {
            GROUP = InetAddress.getByName("230.0.0.239");
        } catch (UnknownHostException ignored) {
            GROUP = null;
        }
    }

    void doStart() {
        Receiver receiver = [group:GROUP,port:PORT]
        startChild (receiver)

        Sender sender = [group:GROUP,port:PORT,dataToTransmit: createDataToTransmit()]
        startChild (sender)
    }

    protected void onMessage(Object message) {
        if (message instanceof byte[]) {
            byte [] buf = message
            final DataInputStream input = new DataInputStream(new ByteArrayInputStream(buf));
            if (input.readLong() == MAGIC) {
                def uuid = new UUID(input.readLong(), input.readLong())
                def port = input.readInt()
                def addrLen = input.readInt()
                def addrBuf = new byte [addrLen]
                input.read(addrBuf)
                onDiscovery(uuid, new InetSocketAddress(InetAddress.getByAddress(addrBuf), port))
            }
        }
        else
            super.onMessage(message)
    }

    private byte [] createDataToTransmit() {
        def out = new ByteArrayOutputStream();
        def stream = new DataOutputStream(out);

        stream.writeLong(MAGIC);
        stream.writeLong(uid.getMostSignificantBits());
        stream.writeLong(uid.getLeastSignificantBits());
        stream.writeInt(address.getPort());
        def addrBytes = address.getAddress().getAddress();
        stream.writeInt(addrBytes.length);
        stream.write(addrBytes);
        stream.close();
        out.toByteArray()
    }

    protected void onDiscovery(final UUID uuid, final SocketAddress address) {
    }
}