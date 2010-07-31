package org.mbte.gretty

import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import java.util.concurrent.Executors
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import java.util.concurrent.Executor

class GrettyShared {
    private static final Executor nioExecutor = Executors.newFixedThreadPool(128)

    final static NioServerSocketChannelFactory nioServerFactory = [nioExecutor, nioExecutor]

    final static NioClientSocketChannelFactory nioClientFactory = [nioExecutor, nioExecutor]
}
