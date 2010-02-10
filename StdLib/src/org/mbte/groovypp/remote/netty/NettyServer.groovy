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

@Typed class NettyServer extends Supervised<NettyServer.Config> {
    private Channel channel

    NettyServer () {
    }

    void doStart() {
        NioServerSocketChannelFactory factory =
            [Executors.newCachedThreadPool(),Executors.newCachedThreadPool()]

        ServerBootstrap bootstrap = [factory]

        bootstrap.setOption("child.tcpNoDelay", true)
        bootstrap.setOption("child.keepAlive", true)

        SimpleChannelHandlerEx handler = [
            messageReceived: { ctx, e ->
                config.onMessage?.call(this$0,e.message)
            },

            channelConnected: { ctx, e ->
                config.onConnect?.call(this$0)
            },

            channelDisconnected: { ctx, e ->
                config.onDisconnect?.call(this$0)
            },

            exceptionCaught: { ctx, e ->
                e.channel.close()
                config.onException?.call(this$0,e.cause)
            },
        ]

        bootstrap.pipeline.addLast("object.encoder", new ObjectEncoder())
        bootstrap.pipeline.addLast("object.decoder", new ObjectDecoder())
        bootstrap.pipeline.addLast("handler", handler)

        // Bind and start to accept incoming connections.
        channel = bootstrap.bind(new InetSocketAddress(config.port))
    }

    void doStop () {
        channel?.close()
    }

    static class Config extends NettyConfig<NettyServer> {
        protected Supervised createSupervised() {
            new NettyServer() 
        }
    }
}