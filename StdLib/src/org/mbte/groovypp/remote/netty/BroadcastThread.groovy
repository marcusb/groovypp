package org.mbte.groovypp.remote.netty;

import groovy.util.concurrent.SupervisedChannel

@Typed abstract class BroadcastThread extends SupervisedChannel {
    InetAddress multicastGroup
    int         multicastPort

    private MulticastSocket socket
    private volatile boolean stopped

    protected abstract void doLoopAction ()

    void doStartup() {
        executor.execute {
            try {
                socket = new MulticastSocket(multicastPort)
                socket.joinGroup(multicastGroup)
                while (!stopped)
                    doLoopAction ()
            }
            catch(Throwable t) {
                stopped = true
                crash(t)
            }
            socket.close()
        }
    }

    void doShutdown() {
        stopped = true;
    }

    static class Sender extends BroadcastThread {
        byte [] dataToTransmit

        void doLoopAction () {
            socket.send ([dataToTransmit, dataToTransmit.length, multicastGroup, multicastPort])
        }
    }

    static class Receiver extends BroadcastThread {
        Function1<byte[],?> messageTransform

        void doLoopAction () {
            def buffer = new byte [512]
            def packet = new DatagramPacket(buffer, buffer.length)
            socket.receive(packet)

            def msg = buffer
            if (messageTransform)
                msg = messageTransform(buffer)
            if (msg)
                (owner ? owner : this).post(msg)
        }
    }
}
