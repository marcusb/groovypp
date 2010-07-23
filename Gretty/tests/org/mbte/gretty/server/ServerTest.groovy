package org.mbte.gretty.server

import org.jboss.netty.channel.local.LocalAddress
import java.util.concurrent.CountDownLatch
import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.channel.local.DefaultLocalClientChannelFactory
import org.jboss.netty.handler.stream.ChunkedWriteHandler
import org.jboss.netty.handler.codec.http.HttpResponseEncoder
import org.jboss.netty.handler.codec.http.HttpRequestDecoder
import org.jboss.netty.handler.codec.http.HttpRequestEncoder
import org.jboss.netty.handler.codec.http.HttpResponseDecoder
import org.jboss.netty.channel.Channels
import org.mbte.gretty.remote.SimpleChannelHandlerEx
import org.jboss.netty.channel.SimpleChannelHandler
import org.jboss.netty.handler.codec.http.DefaultHttpRequest
import org.jboss.netty.handler.codec.http.HttpVersion
import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.handler.codec.http.HttpResponse
import java.util.concurrent.atomic.AtomicReference
import groovy.util.concurrent.BindLater
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.handler.codec.http.QueryStringDecoder

@Typed class ServerTest extends GroovyTestCase {

    private GrettyServer server

    protected void setUp() {
        server = [
            localAddress: new LocalAddress("test_server"),

            webContexts: [
                "/" : [
                    public: {
                        get("/data/:i") {

                        }
                    },

                    default: {
                        response.addHeader "Default", "true"
                        for(p in request.parameters.entrySet())
                            response.addHeader(p.key, p.value.toString())
                        response.text = "default: path: ${request.path}"
                    }
                ]
            ]
        ]
        server.start()
    }

    protected void tearDown() {
        server.stop ()
    }

    void testDefault () {
        def response = testRequest(new LocalAddress("test_server"), new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/data?msg=12&value=33"))

        def bytes = new byte [response.content.readableBytes()]
        response.content.getBytes(0, bytes)
        def text = new String(bytes, "UTF-8")

        assertEquals "default: path: /data", text
        assertEquals "true", response.getHeader("Default")
        assertEquals "[12]", response.getHeader("msg")
        assertEquals "[33]", response.getHeader("value")
    }

    private HttpResponse testRequest(SocketAddress remoteAddress, HttpRequest request) {
        BindLater<HttpResponse> result = []

        ClientBootstrap bootstrap = [new DefaultLocalClientChannelFactory()]
        bootstrap.setOption("tcpNoDelay", true)
        bootstrap.setOption("keepAlive",  true)
        bootstrap.pipelineFactory = { ->
            def pipeline = Channels.pipeline()

            pipeline.addLast("http.request.decoder", new HttpResponseDecoder())
            pipeline.addLast("http.request.encoder", new HttpRequestEncoder())

            pipeline.addLast("http.application", (SimpleChannelHandler)[
                channelConnected: { ctx, msg ->
                    ctx.channel.write(request)
                },

                messageReceived: { ctx, msg ->
                    ctx.channel.close().addListener {
                        result.set((HttpResponse)msg.message)
                    }
                }
            ])
                    
            pipeline
        }
        bootstrap.connect(remoteAddress)
        result.get()
    }
}
