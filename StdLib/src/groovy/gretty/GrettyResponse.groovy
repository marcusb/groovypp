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

package groovy.gretty

import org.jboss.netty.handler.codec.http.HttpResponseStatus
import org.jboss.netty.handler.codec.http.CookieEncoder
import org.jboss.netty.handler.codec.http.CookieDecoder
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.handler.codec.http.HttpResponse
import org.jboss.netty.handler.codec.http.DefaultHttpMessage

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.*

import static org.jboss.netty.handler.codec.http.HttpVersion.*

import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.jboss.netty.handler.codec.http.DefaultHttpResponse
import org.codehaus.jackson.JsonNode

@Typed class GrettyResponse extends DefaultHttpResponse implements HttpResponse {

    final HttpRequest  request

    Object responseBody

    String charset = "UTF-8"

    GrettyResponse () {
        super(HTTP_1_1, HttpResponseStatus.FORBIDDEN)
    }

    GrettyResponse (HttpRequest request) {
        super(HTTP_1_1, HttpResponseStatus.FORBIDDEN)
        this.request  = request

        def cookieString = request.getHeader(COOKIE);
        if (cookieString) {
            def cookieDecoder = new CookieDecoder();
            def cookies = cookieDecoder.decode(cookieString);
            if(!cookies.empty) {
                def cookieEncoder = new CookieEncoder(true)
                for (cookie in cookies) {
                    cookieEncoder.addCookie(cookie)
                }
                addHeader(SET_COOKIE, cookieEncoder.encode())
            }
        }
    }

    void setContent(ChannelBuffer obj) {
        if (responseBody || content.readableBytes() > 0)
            throw new IllegalStateException("Either setContent or setResponseBody should be used")

        status = HttpResponseStatus.OK
        def cl = getHeader(CONTENT_LENGTH)
        if (!cl)
            setHeader(CONTENT_LENGTH, obj.readableBytes())
        super.setContent(obj)
    }

    void setResponseBody(Object obj) {
        status = HttpResponseStatus.OK

        if (responseBody || content.readableBytes() > 0)
            throw new IllegalStateException("Either setContent or setResponseBody should be used")

        switch(obj) {
            case String:
                content = ChannelBuffers.copiedBuffer(obj, charset)
            break

            case File:
                setHeader(HttpHeaders.Names.CONTENT_LENGTH, obj.length())
                this.responseBody = obj
            break

            default:
                this.responseBody = obj
        }
    }

    void setTest(Object body) {
        setHeader(CONTENT_TYPE, "text/plain; charset=$charset")
        content = ChannelBuffers.copiedBuffer(body.toString(), charset)
    }

    void setHtml(Object body) {
        setHeader(CONTENT_TYPE, "text/html; charset=$charset")
        content = ChannelBuffers.copiedBuffer(body.toString(), charset)
    }

    void setJson(Object body) {
        setHeader(CONTENT_TYPE, "application/json; charset=$charset")
        content = ChannelBuffers.copiedBuffer(body.toString(), charset)
    }

    void setXml(Object body) {
        setHeader(CONTENT_TYPE, "application/xml; charset=$charset")
        content = ChannelBuffers.copiedBuffer(body.toString(), charset)
    }
}
