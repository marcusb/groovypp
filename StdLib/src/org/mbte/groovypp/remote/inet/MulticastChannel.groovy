package org.mbte.groovypp.remote.inet

import groovy.util.concurrent.LoopChannel

@Typed abstract class MulticastChannel extends LoopChannel {
    InetAddress multicastGroup
    int         multicastPort

    private MulticastSocket socket

    void doStartup() {
        socket = new MulticastSocket(multicastPort)
        socket.joinGroup(multicastGroup)
        super.doStartup()
    }

    void doShutdown() {
        socket.close()
        super.doShutdown()
    }

    static class Sender extends MulticastChannel {
        byte [] dataToTransmit

        void doLoopAction () {
            socket.send ([dataToTransmit, dataToTransmit.length, multicastGroup, multicastPort])
        }
    }

    static class Receiver extends MulticastChannel {
        void doLoopAction () {
          def buffer = new byte[512]
          def packet = new DatagramPacket(buffer, buffer.length)
          socket.receive(packet)

          (owner ?: this).post(InetDiscoveryInfo.fromBytes(buffer))
        }
    }
}