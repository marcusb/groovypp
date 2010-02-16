package org.mbte.groovypp.remote.netty

import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.buffer.ChannelBuffer
import java.util.concurrent.Executors
import groovy.supervisors.Supervised
import org.jboss.netty.handler.codec.serialization.ObjectEncoder
import org.jboss.netty.handler.codec.serialization.ObjectDecoder
import org.jboss.netty.channel.Channel
import java.util.concurrent.CountDownLatch
import groovy.remote.ClusterNodeServer
import groovy.remote.RemoteConnection
import groovy.remote.RemoteMessage

@Typed class NettyServer extends ClusterNodeServer<NettyServer.Config> {

    final NioServerSocketChannelFactory factory = [
        Executors.newCachedThreadPool(),
        Executors.newCachedThreadPool()
    ]

    private Channel serverChannel

    void doStart() {
        ServerBootstrap bootstrap = [factory]

        bootstrap.setOption("child.tcpNoDelay", true)
        bootstrap.setOption("child.keepAlive", true)

        SimpleChannelHandlerEx handler = [
            createConnection: { ctx ->
                new NettyConnection(config:config, channel:ctx.channel)
            }
        ]

//        bootstrap.pipeline.addLast("object.encoder", new ObjectEncoder())
//        bootstrap.pipeline.addLast("object.decoder", new ObjectDecoder())
        bootstrap.pipeline.addLast("handler", handler)

        // Bind and start to accept incoming connections.
        serverChannel = bootstrap.bind(new InetSocketAddress(config.port))
    }

    void doStop () {
        serverChannel?.close()

        factory.releaseExternalResources()
    }

    static class Config implements ClusterNodeServer.Config<NettyServer>, RemoteConnection.Config {
        int port

        Supervised createSupervised() {
            new NettyServer() 
        }
    }
}
