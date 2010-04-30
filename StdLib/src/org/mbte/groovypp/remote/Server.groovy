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

package org.mbte.groovypp.remote

import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import org.jboss.netty.bootstrap.ServerBootstrap
import java.util.concurrent.Executors
import org.jboss.netty.handler.codec.serialization.ObjectEncoder
import org.jboss.netty.handler.codec.serialization.ObjectDecoder
import org.jboss.netty.channel.Channel
import groovy.remote.ClusterNode
import groovy.channels.SupervisedChannel

@Typed class Server extends SupervisedChannel {
    SocketAddress address

    ClusterNode clusterNode
    NioServerSocketChannelFactory serverFactory

    private Channel serverChannel

    protected void doStartup() {
        super.doStartup()

        if (!clusterNode) {
            if(owner instanceof ClusterNode)
                clusterNode = (ClusterNode)owner
            else
                throw new IllegalStateException("Server requires clusterNode")
        }

        if (!address) {
            address = new InetSocketAddress(InetAddress.getLocalHost(), findFreePort())
        }

        serverFactory = [Executors.newCachedThreadPool(),Executors.newCachedThreadPool()]

        ServerBootstrap bootstrap = [serverFactory]

        bootstrap.setOption("child.tcpNoDelay", true)
        bootstrap.setOption("child.keepAlive", true)

        SimpleChannelHandlerEx handler = [
            createConnection: {ctx ->
                new Connection(channel: ctx.channel, clusterNode:clusterNode)
            }
        ]

        bootstrap.pipeline.addLast("object.encoder", new ObjectEncoder())
        bootstrap.pipeline.addLast("object.decoder", new ObjectDecoder())
        bootstrap.pipeline.addLast("handler", handler)

        // Bind and startup to accept incoming connections.
        serverChannel = bootstrap.bind(address)
    }

    protected void doShutdown () {
        serverChannel?.close()
        serverFactory.releaseExternalResources()
        serverFactory = null
    }

    static int findFreePort() {
        def server = new ServerSocket(0)
        def port = server.getLocalPort()
        server.close()
        port
    }
}
