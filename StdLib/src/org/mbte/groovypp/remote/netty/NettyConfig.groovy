package org.mbte.groovypp.remote.netty

import groovy.supervisors.SupervisedConfig
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.ChannelStateEvent

static class NettyConfig<T> extends SupervisedConfig {
    int    port

    DelegatingFunction0<T,?>           onConnect
    DelegatingFunction0<T,?>           onDisconnect
    DelegatingFunction1<T,Object,?>    onMessage
    DelegatingFunction1<T,Throwable,?> onException
}
