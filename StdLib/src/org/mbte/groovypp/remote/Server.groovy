package org.mbte.groovypp.remote

import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import org.jboss.netty.bootstrap.ServerBootstrap
import java.util.concurrent.Executors
import org.jboss.netty.handler.codec.serialization.ObjectEncoder
import org.jboss.netty.handler.codec.serialization.ObjectDecoder
import org.jboss.netty.channel.Channel
import groovy.remote.ClusterNode
import groovy.util.concurrent.SupervisedChannel

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

        clusterNode.startServerBroadcaster(this)
    }

    protected void doShutdown () {
        serverChannel?.close()
        serverFactory.releaseExternalResources()
        serverFactory = null
    }
}
