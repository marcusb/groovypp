package org.mbte.groovypp.remote.netty

import org.jboss.netty.channel.ChannelPipelineCoverage
import org.jboss.netty.channel.SimpleChannelHandler
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.channel.ChannelStateEvent
import org.jboss.netty.channel.ExceptionEvent
import groovy.remote.RemoteConnection
import org.jboss.netty.channel.ChannelEvent

@ChannelPipelineCoverage("all")
@Typed abstract class SimpleChannelHandlerEx extends SimpleChannelHandler {

    abstract RemoteConnection createConnection (ChannelHandlerContext ctx)

    void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
        RemoteConnection conn = ctx.attachment
        if (!conn) {
            conn = createConnection(ctx)
            ctx.attachment = conn
        }
        super.handleUpstream(ctx, e)
    }

    void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
        RemoteConnection conn = ctx.attachment
        conn.onConnect()
    }

    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
        RemoteConnection conn = ctx.attachment
        conn.onDisconnect()
    }

    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) {
        super.channelClosed(ctx, e);
    }

    void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        RemoteConnection conn = ctx.attachment
        conn.onMessage(e.message)
    }

    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        e.channel.close()
        RemoteConnection conn = ctx.attachment
        conn?.onException(e.cause)
    }
}
