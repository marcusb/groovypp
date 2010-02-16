package org.mbte.groovypp.remote.netty

import groovy.remote.RemoteMessage
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.handler.codec.serialization.ObjectEncoder
import org.jboss.netty.handler.codec.serialization.ObjectDecoder
import groovy.remote.RemoteConnection
import groovy.supervisors.Supervised
import org.jboss.netty.channel.Channel
import groovy.supervisors.SupervisedConfig
import java.util.concurrent.Executors

@Typed class NettyClient extends Supervised<NettyClient.Config> {
    NettyConnection connection

    final NioClientSocketChannelFactory factory =[
            Executors.newCachedThreadPool(),
            Executors.newCachedThreadPool()
    ]

    void doStart() {
        ClientBootstrap bootstrap = [factory]
        SimpleChannelHandlerEx handler = [
            createConnection: { ctx ->
                connection = new NettyConnection(channel:ctx.channel, config:config, supervised:this$0)
            }
        ]

//        bootstrap.pipeline.addLast("object.encoder", new ObjectEncoder())
//        bootstrap.pipeline.addLast("object.decoder", new ObjectDecoder())
        bootstrap.pipeline.addLast("handler", handler);

        bootstrap.setOption("tcpNoDelay", true);
        bootstrap.setOption("keepAlive", true);

        bootstrap.connect(new InetSocketAddress(config.host, config.port))
    }

    void doStop () {
        connection?.channel?.close()

        factory.releaseExternalResources()
    }

    static class Config implements SupervisedConfig, RemoteConnection.Config {
        int    port
        String host

        Supervised createSupervised() {
            new NettyClient() 
        }
    }
}
