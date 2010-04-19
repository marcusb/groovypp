/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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