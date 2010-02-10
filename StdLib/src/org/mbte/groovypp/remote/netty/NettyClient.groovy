package org.mbte.groovypp.remote.netty

import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.handler.codec.frame.FrameDecoder
import java.util.concurrent.Semaphore
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import groovy.supervisors.Supervised
import org.jboss.netty.handler.codec.serialization.ObjectEncoder
import org.jboss.netty.handler.codec.serialization.ObjectDecoder
import java.util.concurrent.Executors
import java.util.concurrent.CountDownLatch
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.ExceptionEvent
import groovy.supervisors.SupervisedConfig
import org.jboss.netty.channel.ChannelStateEvent
import org.jboss.netty.channel.Channel

@Typed class NettyClient extends Supervised<NettyClient.Config> {

    Channel channel

    void doStart() {
        final NioClientSocketChannelFactory factory =[
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool()
        ]

        ClientBootstrap bootstrap = [factory]
        SimpleChannelHandlerEx handler = [
            messageReceived: { ctx, e ->
                config.onMessage?.call(this$0,e.message)
            },

            channelConnected: { ctx, e ->
                channel = ctx.channel
                config.onConnect?.call(this$0)
            },

            channelDisconnected: { ctx, e ->
                config.onDisconnect?.call(this$0)
            },

            exceptionCaught: { ctx, e ->
                e.channel.close()
                crash(null, e.cause)
                config.onException?.call(this$0,e.cause)
            },
        ]

        bootstrap.pipeline.addLast("object.encoder", new ObjectEncoder())
        bootstrap.pipeline.addLast("object.decoder", new ObjectDecoder())
        bootstrap.pipeline.addLast("handler", handler);
        bootstrap.setOption("tcpNoDelay", true);
        bootstrap.setOption("keepAlive", true);

        bootstrap.connect(new InetSocketAddress(config.host, config.port))
    }

    void doStop () {
        channel?.close()
    }

    static class Config extends NettyConfig<NettyClient> {
        String host

        protected Supervised createSupervised() {
            new NettyClient() 
        }
    }
}
