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

package org.mbte.gretty.httpclient

import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.channel.local.DefaultLocalClientChannelFactory
import org.jboss.netty.channel.local.LocalAddress
import org.jboss.netty.handler.codec.http.HttpChunkAggregator
import org.jboss.netty.handler.codec.http.HttpRequestEncoder
import org.jboss.netty.handler.codec.http.HttpResponseDecoder
import org.mbte.gretty.GrettyShared
import org.jboss.netty.channel.*

@Typed class AbstractHttpClient extends SimpleChannelHandler {
    protected volatile Channel channel

    protected final SocketAddress remoteAddress;

    AbstractHttpClient(SocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress
    }

    ChannelFuture connect () {
        ClientBootstrap bootstrap = [remoteAddress instanceof LocalAddress ? new DefaultLocalClientChannelFactory() : GrettyShared.nioClientFactory ]
        bootstrap.setOption("tcpNoDelay", true)
        bootstrap.setOption("keepAlive",  true)
        bootstrap.pipelineFactory = { ->
            def pipeline = Channels.pipeline()

            pipeline.addLast("http.response.decoder", new HttpResponseDecoder())
            pipeline.addLast("http.response.aggregator", new HttpChunkAggregator(Integer.MAX_VALUE))
            pipeline.addLast("http.request.encoder", new HttpRequestEncoder())
            pipeline.addLast("http.application", this)

            pipeline
        }

        def connectFuture = bootstrap.connect(remoteAddress)
        connectFuture.addListener { future ->
            if(future.success) {
                channel = future.channel
                onConnect ()
            }
            else {
               future.channel.close ()
               onConnectFailed ()
            }
        }
        connectFuture
    }

    void connect(ChannelFutureListener listener) {
        connect().addListener listener
    }

    void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        ctx.channel.close ()
    }

    void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) {
        if(channel != null) {
            channel = null
            onDisconnect()
        }
    }

    void disconnect() {
        channel?.close()
    }

    protected void onConnect () {}

    protected void onConnectFailed () {}

    protected void onDisconnect () {}
}
