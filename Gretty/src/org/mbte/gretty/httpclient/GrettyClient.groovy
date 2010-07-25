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

@Typed class GrettyClient extends SimpleChannelHandler {
    private volatile State state

    GrettyClient(SocketAddress remoteAddress) {
        state = [queue:FQueue.emptyQueue]

        ClientBootstrap bootstrap = [remoteAddress instanceof LocalAddress ? new DefaultLocalClientChannelFactory() : new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()) ]
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
        bootstrap.connect(remoteAddress).addListener { future ->
            future.channel.closeFuture.addListener {
                bootstrap.factory.releaseExternalResources()
            }
        }
    }

    BindLater<HttpResponse> request(HttpRequest request) {
        RequestResponse responseFuture = [request]
        for (;;) {
            def s = state
            def ns = s.clone()
            if(s?.channel?.connected && !s.pendingRequest) {
                assert s.queue.empty
                ns.pendingRequest = responseFuture
                if(state.compareAndSet(s, ns)) {
                    s.channel.write(request)
                    return responseFuture
                }
            }
            else {
                ns.queue = s.queue.addLast(responseFuture)
                if(state.compareAndSet(s, ns))
                    return responseFuture
            }
        }
    }

    void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
        for (;;) {
            def s = state
            assert !s.channel
            assert !s.pendingRequest

            def ns = s.clone()
            ns.channel = e.channel

            if(s.queue.empty) {
                if(state.compareAndSet(s, ns))
                    break
            }
            else {
                def removed = s.queue.removeFirst()
                ns.queue = removed.second
                ns.pendingRequest = removed.first
                if(state.compareAndSet(s, ns)) {
                    ns.channel.write(ns.pendingRequest)
                    break
                }
            }
        }
    }

    void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        e.channel.close()
    }

    void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) {
        for (;;) {
            def s = state
            assert s.pendingRequest

            def ns = s.clone()
            ns.channel = null
            ns.queue   = FQueue.emptyQueue
            ns.pendingRequest = null

            if(state.compareAndSet(s, ns)) {
                def exception = new ClosedChannelException()
                s.pendingRequest?.setException(exception)
                for(pr in s.queue) {
                    pr.setException(exception)
                }
                break
            }
        }
    }

    void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        HttpResponse response = e.message
        for (;;) {
            def s = state
            assert s.pendingRequest

            def ns = s.clone()
            if(s.queue.empty) {
                ns.pendingRequest = null
                if(state.compareAndSet(s, ns)) {
                    s.pendingRequest.set(response)
                    break
                }
            }
            else {
                def removed = s.queue.removeFirst()
                ns.queue = removed.second
                ns.pendingRequest = removed.first
                if(state.compareAndSet(s, ns)) {
                    s.pendingRequest.set(response)
                    ns.channel.write(ns.pendingRequest)
                    break
                }
            }
        }
    }

    private static class State implements Cloneable {
        Channel                 channel
        FQueue<RequestResponse> queue
        RequestResponse         pendingRequest

        protected State clone() {
            return super.clone()
        }
    }

    static class RequestResponse extends BindLater<HttpResponse> {
        final HttpRequest  request

        RequestResponse(HttpRequest request) {
            this.request = request
        }
    }
}
