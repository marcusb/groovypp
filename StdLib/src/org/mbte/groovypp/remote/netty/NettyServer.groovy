package org.mbte.groovypp.remote.netty

import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import org.jboss.netty.bootstrap.ServerBootstrap
import java.util.concurrent.Executors
import org.jboss.netty.handler.codec.serialization.ObjectEncoder
import org.jboss.netty.handler.codec.serialization.ObjectDecoder
import org.jboss.netty.channel.Channel
import groovy.remote.ClusterNodeServer
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import org.jboss.netty.bootstrap.ClientBootstrap
import groovy.remote.ClusterNode

@Typed class NettyServer extends ClusterNodeServer {
    int port

    final NioServerSocketChannelFactory serverFactory = [
        Executors.newCachedThreadPool(),
        Executors.newCachedThreadPool()
    ]

    final NioClientSocketChannelFactory clientFactory =[
            Executors.newCachedThreadPool(),
            Executors.newCachedThreadPool()
    ]

    private Channel serverChannel
    private Broadcast broadcast

    void start() {
        startServer()
        startBroadcast ()
    }

    void stop () {
        stopBroadcast()
        stopServer()
    }

    private void startServer() {
        ServerBootstrap bootstrap = [serverFactory]

        bootstrap.setOption("child.tcpNoDelay", true)
        bootstrap.setOption("child.keepAlive", true)

        SimpleChannelHandlerEx handler = [
                createConnection: {ctx ->
                    new NettyConnection(channel: ctx.channel, clusterNode:clusterNode)
                }
        ]

        bootstrap.pipeline.addLast("object.encoder", new ObjectEncoder())
        bootstrap.pipeline.addLast("object.decoder", new ObjectDecoder())
        bootstrap.pipeline.addLast("handler", handler)

        // Bind and start to accept incoming connections.
        serverChannel = bootstrap.bind(new InetSocketAddress(port))
    }

    private void startBroadcast() {
        broadcast = [clusterNode.id, (InetSocketAddress)serverChannel.getLocalAddress()]
        broadcast.start ()
    }

    private void stopServer() {
        serverChannel?.close()
        serverFactory.releaseExternalResources()
    }

    private void stopBroadcast() {
        broadcast.stop()
    }

    HashMap<UUID,NettyClient> clients = [:]

    class Broadcast extends BroadcastDiscovery {
        Broadcast (UUID uid, InetSocketAddress address) {
            super (uid, address)
        }

        protected void onDiscovery(UUID uuid, SocketAddress address) {
            if (uuid > getUid()) {
                synchronized(clients) {
                    if(!clients.containsKey(uuid)) {
                        clusterNode.communicationEvents << new ClusterNode.CommunicationEvent.TryingConnect(uuid:uuid, address:address)
                        NettyClient client = new NettyClient()
                        clients.put(uuid, client)
                        client.start (clientFactory, address, clusterNode)
                    }
                }
            }
        }
    }

    static class NettyClient {
        NettyConnection connection

        void start(NioClientSocketChannelFactory clientFactory, SocketAddress address, ClusterNode clusterNode) {
            ClientBootstrap bootstrap = [clientFactory]
            SimpleChannelHandlerEx handler = [
                createConnection: { ctx ->
                    new NettyConnection(channel: ctx.channel, clusterNode:clusterNode)
                }
            ]

            bootstrap.pipeline.addLast("object.encoder", new ObjectEncoder())
            bootstrap.pipeline.addLast("object.decoder", new ObjectDecoder())
            bootstrap.pipeline.addLast("handler", handler);

            bootstrap.setOption("tcpNoDelay", true);
            bootstrap.setOption("keepAlive", true);

            bootstrap.connect(address)
        }

        void stop () {
            connection?.channel?.close()
        }
    }
}
