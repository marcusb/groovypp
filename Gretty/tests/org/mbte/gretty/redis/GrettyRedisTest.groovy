package org.mbte.gretty.redis

import org.mbte.gretty.httpserver.GrettyServer
import org.jboss.netty.channel.local.LocalAddress
import org.mbte.gretty.httpserver.HttpRequestHelper
import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.handler.codec.http.HttpVersion
import org.mbte.gretty.httpserver.GrettyHttpRequest
import org.jboss.netty.buffer.ChannelBuffers

@Typed class GrettyRedisTest extends RedisTestBase implements HttpRequestHelper {

    private GrettyServer server

    protected void setUp() {
        super.setUp()
        if(redis) {
            server = [
                localAddress: new LocalAddress("test_server"),

                public: {
                    rest("/data/:objectId") {
                        get {
                            response.async = true
                            redis.get("${it.userId}:${it.objectId}") { bound ->
                                response.json = bound.get()
                                response.complete()
                            }
                        }

                        post {
                            response.async = true
                            redis.set("${it.userId}:${it.objectId}", request.contentText) { bound ->
                                response.json = "{\"status\":\"OK\"}"
                                response.complete()
                            }
                        }
                    }
                }
            ]
            server.start()
        }
    }

    protected void tearDown() {
        server?.stop ()
        super.tearDown()
    }

    void testSetGet () {
        if(redis) {
            GrettyHttpRequest req = [HttpVersion.HTTP_1_1, HttpMethod.POST, "/data/239"]
            def message = "{code: 245, elements:[0, 4, 6]}"
            req.content = ChannelBuffers.wrappedBuffer(message.bytes)
            req.setHeader("Content-Length", req.content.readableBytes())
            doTest(req) { response ->
                println response.contentText
            }

            req = [HttpVersion.HTTP_1_1, HttpMethod.GET, "/data/239"]
            doTest(req) { response ->
                println response.contentText
                assertEquals message, response.contentText
            }
        }
    }
}
