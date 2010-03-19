package org.mbte.groovypp.remote.netty;

import groovy.util.concurrent.SupervisedChannel

@Typed abstract class BroadcastThread extends SupervisedChannel {
    InetAddress group
    int         port

    private MulticastSocket socket
    private volatile boolean stopped

    abstract void loopAction ()

    void doStartup() {
        executor.execute {
            try {
                socket = new MulticastSocket(port);
                socket.joinGroup(group);
                while (!stopped)
                    loopAction ()
            }
            catch(Throwable t) {
                stopped = true
                t.printStackTrace()
                crash(t)
            }
            socket.close()
        }
    }

    void doShutdown() {
        stopped = true;
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
        Function1<byte[],?> messageTransform

        void loopAction () {
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
