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

package org.mbte.gretty.httpserver

import java.util.concurrent.Executor

import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory

import org.jboss.netty.channel.*
import org.jboss.netty.handler.codec.http.*
import org.jboss.netty.handler.stream.ChunkedWriteHandler
import org.jboss.netty.logging.InternalLogLevel

import org.jboss.netty.bootstrap.ServerBootstrap
import java.util.concurrent.Executors
import org.jboss.netty.util.internal.ExecutorUtil
import org.jboss.netty.channel.local.LocalAddress
import org.jboss.netty.channel.local.DefaultLocalServerChannelFactory
import org.jboss.netty.channel.group.DefaultChannelGroup
import org.mbte.gretty.remote.SimpleChannelHandlerEx
import org.jboss.netty.buffer.ChannelBuffer

@Typed class GrettyServer {
    int              ioWorkerCount      = 2*Runtime.getRuntime().availableProcessors()
    int              serviceWorkerCount = 4*Runtime.getRuntime().availableProcessors()

    InternalLogLevel logLevel

    SocketAddress    localAddress = new InetSocketAddress(8080)

    Map<String,GrettyContext> webContexts = [:]

    final PseudoWebSocketManager pseudoWebSocketManager = []

    protected Executor threadPool
    protected Channel channel

    protected final DefaultChannelGroup allConnected = []

    private void initContexts () {
        webContexts = webContexts.sort { me1, me2 -> me2.key <=> me1.key }

        for(e in webContexts.entrySet()) {
            e.value.initContext(e.key)
        }
    }

    void start () {
        initContexts ()

        def bossExecutor = Executors.newCachedThreadPool()
        def ioExecutor   = Executors.newFixedThreadPool(ioWorkerCount)
        threadPool       = Executors.newFixedThreadPool(serviceWorkerCount)

        def isLocal = localAddress instanceof LocalAddress

        def channelFactory = isLocal ? new DefaultLocalServerChannelFactory () : (NioServerSocketChannelFactory )[bossExecutor, ioExecutor]

        ServerBootstrap bootstrap = [channelFactory]
        bootstrap.setOption("child.tcpNoDelay", true)
        bootstrap.setOption("child.keepAlive",  true)

        def logger = logLevel ? new HttpLoggingHandler(logLevel) : null

        bootstrap.pipelineFactory = { ->
            def pipeline = createPipeline()
            if (logger)
                pipeline.addBefore("http.application", "http.logger", logger)
            pipeline
        }

        channel = bootstrap.bind(localAddress)
        channel.closeFuture.addListener {
            ExecutorUtil.terminate(bossExecutor, ioExecutor, threadPool) 
        }
    }

    protected ChannelPipeline createPipeline() {
        def pipeline = Channels.pipeline()

        pipeline.addLast("http.request.decoder", new GrettyRequestDecoder())
        pipeline.addLast("http.request.encoder", new HttpResponseEncoder())

        pipeline.addLast("chunkedWriter", new ChunkedWriteHandler())
        pipeline.addLast("fileWriter", new FileWriteHandler())

        pipeline.addLast("http.application", new GrettyAppHandler(this))

        pipelineCreated(pipeline)
        pipeline
    }

    protected void pipelineCreated (ChannelPipeline pipeline) {
    }

    void stop() {
        channel?.close ()
    }
}
