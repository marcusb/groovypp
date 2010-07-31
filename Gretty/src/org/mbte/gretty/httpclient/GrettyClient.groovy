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

import org.jboss.netty.channel.SimpleChannelHandler
import org.jboss.netty.handler.codec.http.HttpResponse
import org.jboss.netty.handler.codec.http.HttpRequestEncoder
import org.jboss.netty.handler.codec.http.HttpChunkAggregator
import org.jboss.netty.handler.codec.http.HttpResponseDecoder
import org.jboss.netty.channel.Channels
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import org.jboss.netty.channel.local.DefaultLocalClientChannelFactory
import org.jboss.netty.channel.local.LocalAddress
import org.jboss.netty.bootstrap.ClientBootstrap
import groovy.util.concurrent.BindLater
import org.jboss.netty.handler.codec.http.HttpRequest
import groovy.util.concurrent.FList
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.ChannelStateEvent
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.channel.Channel
import groovy.util.concurrent.FQueue
import java.util.concurrent.Executors
import org.jboss.netty.channel.ExceptionEvent
import java.nio.channels.ClosedChannelException
import org.jboss.netty.channel.ChannelFuture
import org.mbte.gretty.GrettyShared
import groovy.util.concurrent.BindLater.Listener

@Typed class GrettyClient extends AbstractHttpClient {

    private volatile BindLater<HttpResponse> pendingRequest

    GrettyClient(SocketAddress remoteAddress) {
        super(remoteAddress)
    }

    BindLater<HttpResponse> request(HttpRequest request) {
        def later = new BindLater()
        assert pendingRequest.compareAndSet(null, later)
        channel.write(request)
        later
    }

    void request(HttpRequest request, BindLater.Listener<HttpResponse> action) {
        assert pendingRequest.compareAndSet(null, new BindLater().whenBound(action))
        channel.write(request)
    }

    void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        pendingRequest.getAndSet(null).set((HttpResponse)e.message)
    }
}
