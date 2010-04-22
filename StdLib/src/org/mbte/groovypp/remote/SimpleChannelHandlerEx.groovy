/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbte.groovypp.remote

import groovy.remote.RemoteConnection
import org.jboss.netty.channel.*

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
