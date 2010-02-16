package org.mbte.groovypp.remote.netty

import groovy.remote.RemoteConnection
import org.jboss.netty.channel.Channel
import groovy.supervisors.Supervised

@Typed static class NettyConnection<S extends Supervised> extends RemoteConnection {
    protected Channel channel

    protected S supervised

    public void send(Object msg) {
        channel.write(msg)
    }

    public void onException(Throwable cause) {
        super.onException(cause);
        supervised?.crash (cause)
    }
}
