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

package org.mbte.gretty

import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.channel.local.DefaultLocalClientChannelFactory
import org.jboss.netty.channel.local.LocalAddress
import org.jboss.netty.channel.*

@Typed class AbstractClient extends SimpleChannelHandler implements ChannelPipelineFactory {
    protected volatile Channel channel

    protected final SocketAddress remoteAddress;

    AbstractClient(SocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress
    }

    ChannelFuture connect () {
        ClientBootstrap bootstrap = [remoteAddress instanceof LocalAddress ?
            new DefaultLocalClientChannelFactory() :
            GrettyShared.nioClientFactory ]
        bootstrap.pipelineFactory = this
        bootstrap.setOption("tcpNoDelay", true)
        bootstrap.setOption("keepAlive",  true)

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

    ChannelPipeline getPipeline () {
        def pipeline = Channels.pipeline()
        buildPipeline(pipeline)
        pipeline
    }

    protected void buildPipeline(ChannelPipeline pipeline) {
        pipeline.addFirst("clientItself", this)
    }
}
