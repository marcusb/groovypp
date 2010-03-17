package org.mbte.groovypp.remote.netty

import groovy.remote.RemoteConnection
import org.jboss.netty.channel.Channel
import groovy.supervisors.Supervised
import groovy.remote.RemoteMessage

@Typed static class NettyConnection extends RemoteConnection {
    protected Channel channel

    protected void doSend(RemoteMessage msg) {
        channel?.write(msg)
    }

    protected void disconnect() {
        channel?.disconnect()
        channel = null
    }
}
