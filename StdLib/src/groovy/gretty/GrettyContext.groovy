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

import org.jboss.netty.handler.codec.http.HttpHeaders

import org.jboss.netty.handler.codec.http.HttpResponseStatus

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.*

import static org.jboss.netty.handler.codec.http.HttpResponseStatus.*
import org.jboss.netty.handler.codec.http.HttpMethod

@Typed class GrettyContext {
    String               staticFiles
    protected String     webPath

    ClassLoader          classLoader
    String               classLoaderPath

    GrettyHandler handler

    Map<String,GrettyWebSocketHandler> webSockets = [:]

    void handleHttpRequest(GrettyResponse op) {
        def staticFile = op.request.method == HttpMethod.GET ? findStaticFile(op.request.uri) : null
        if (staticFile) {
            op.status = staticFile.second
            if (staticFile.second.code != 200) {
                op.setHeader(CONTENT_TYPE, "text/html; charset=UTF-8")
                op.responseBody = "Failure: ${staticFile.second}\r\n"
            }
            else {
                op.responseBody = staticFile.first
            }
        }
        else {
            if (handler) {
                handler.handle(op)
            }
            else {
                op.status = NOT_FOUND
                op.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8")
                op.responseBody = "Failure: ${NOT_FOUND}\r\n"
            }
        }
    }

    public void initContext (String path) {
        if(staticFiles) {
            def file = new File(staticFiles)
            if(!file.exists() || !file.directory || file.hidden) {
                throw new IOException("directory $staticFiles does not exists or hidden")
            }

            staticFiles = file.absoluteFile.canonicalPath
            webPath = path
        }

        if (webSockets) {
            for(ws in webSockets.entrySet()) {
                ws.value.socketPath = ws.key
            }
        }
    }

    private Pair<File,HttpResponseStatus> findStaticFile(String uri) {
        if (!staticFiles)
            return null

        uri = URLDecoder.decode(uri, "UTF-8")
        uri = uri.replace('/', File.separatorChar).substring(webPath.length())

        if(uri.startsWith('/'))
            uri = uri.substring(1)

        if (uri.contains(File.separator + ".") || uri.contains("." + File.separator) || uri.startsWith(".") || uri.endsWith(".")) {
            return [null,FORBIDDEN]
        }

        uri = "$staticFiles/$uri".replace('/', File.separatorChar)

        File file = [uri]
        if (!file.exists()) {
            return null
        }
        if (file.hidden) {
            return [null,FORBIDDEN]
        }
        if (!file.file) {
            def indexFiles = file.listFiles { dir, name -> name.startsWith("index.") }
            if (indexFiles?.length == 1)
                file = indexFiles[0]
            else
                return handler ? null : [null,FORBIDDEN]
        }

        [file,OK]
    }
}
