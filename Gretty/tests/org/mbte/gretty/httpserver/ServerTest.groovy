package org.mbte.gretty.httpserver

import org.jboss.netty.channel.local.LocalAddress

import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.channel.local.DefaultLocalClientChannelFactory

import org.jboss.netty.handler.codec.http.HttpRequestEncoder
import org.jboss.netty.handler.codec.http.HttpResponseDecoder
import org.jboss.netty.channel.Channels

import org.jboss.netty.channel.SimpleChannelHandler
import org.jboss.netty.handler.codec.http.DefaultHttpRequest
import org.jboss.netty.handler.codec.http.HttpVersion
import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.handler.codec.http.HttpResponse

import groovy.util.concurrent.BindLater
import org.jboss.netty.handler.codec.http.HttpRequest

import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import org.jboss.netty.handler.codec.http.HttpChunkAggregator
import org.mbte.gretty.httpclient.GrettyClient

@Typed class ServerTest extends GroovyTestCase {

    private GrettyServer server

    protected void setUp() {
        server = [
            localAddress: new LocalAddress("test_server"),

            webContexts: [
                "/" : [
                    public: {
                        get("/data/:mapId/set/:objectId") {
                            response.addHeader("mapId", it.mapId)
                            response.addHeader("objectId",  it.objectId)
                        }

                        get("/data") {}
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
        GrettyClient client = [new LocalAddress("test_server")]
        def response = client.request(new GrettyHttpRequest("/data?msg=12&value=33")).get()

        def bytes = new byte [response.content.readableBytes()]
        response.content.getBytes(0, bytes)
        def text = new String(bytes, "UTF-8")

        assertEquals "default: path: /data", text
        assertEquals "true", response.getHeader("Default")
        assertEquals "[12]", response.getHeader("msg")
        assertEquals "[33]", response.getHeader("value")
    }

    void testMatch() {
        GrettyClient client = [new LocalAddress("test_server")]
        def response = client.request(new GrettyHttpRequest("/data/abracadabra/set/245")).get()

        assertEquals "abracadabra", response.getHeader("mapId")
        assertEquals "245", response.getHeader("objectId")
    }

    void testNoMatch() {
        GrettyClient client = [new LocalAddress("test_server")]
        def response = client.request(new GrettyHttpRequest("/data")).get()

        assertNull response.getHeader("mapId")
        assertNull response.getHeader("objectId")
    }
}
