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



@Typed
package org.mbte.groovypp.samples.echo

import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import org.jboss.netty.channel.*
import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import org.jboss.netty.bootstrap.ClientBootstrap

import java.util.concurrent.Executors;
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.Semaphore
import org.jboss.netty.handler.codec.frame.FrameDecoder
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.channel.ChannelPipelineCoverage

class ThroughputMonitor {
    static AtomicLong transferredBytes = []

    static void addBytesStat (int bytes) {
        transferredBytes.addAndGet(bytes)
    }

    static void startReporting () {
        // Start performance monitor.
        Thread.startDaemon {
            long oldCounter = transferredBytes
            def startTime = System.currentTimeMillis()
            for (;;) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace()
                }

                def endTime = System.currentTimeMillis()
                long newCounter = transferredBytes
                System.err.format(
                        "%4.3f MiB/s%n",
                        (newCounter - oldCounter) * 1000.0d / (endTime - startTime) /
                        1048576.0d)
                oldCounter = newCounter
                startTime = endTime
            }
        }
    }
}

@ChannelPipelineCoverage("all")
class SimpleChannelHandlerEx extends SimpleChannelHandler {}

class EchoServer {
    public static void main(String[] args) throws Exception {
        NioServerSocketChannelFactory factory =
            [Executors.newCachedThreadPool(),Executors.newCachedThreadPool()]

        ServerBootstrap bootstrap = [factory]

        bootstrap.setOption("child.tcpNoDelay", true)
        bootstrap.setOption("child.keepAlive", true)

        SimpleChannelHandlerEx handler = [
            messageReceived: { /*ChannelHandlerContext*/ ctx, /*MessageEvent*/ e ->
                // Send back the received message to the remote peer.
                ThroughputMonitor.addBytesStat(((ChannelBuffer) e.message).readableBytes())
                e.channel.write(e.message)
            },

            exceptionCaught: { /*ChannelHandlerContext*/ ctx, /*ExceptionEvent*/ e ->
                // Close the connection when an exception is raised.
                e.cause.printStackTrace(System.err)
                e.channel.close()
            }
        ]

        bootstrap.pipeline.addLast("handler", handler)

        // Bind and start to accept incoming connections.
        bootstrap.bind(new InetSocketAddress(8080))

        // Start performance monitor.
        ThroughputMonitor.startReporting ()
    }
}

class EchoClient {

    static void main(String[] args) throws Exception {
        // Print usage if no argument is specified.
        if (args.length < 2 || args.length > 3) {
            System.err.println "Usage: ${EchoClient.class.simpleName} <host> <port> [<first message size>]"
            return
        }

        // Parse options.
        def host = args[0]
        def port = Integer.parseInt(args[1])

        def firstMessageSize = args.length == 3 ? Integer.parseInt(args[2]) : 256

        def workers = Executors.newFixedThreadPool(200)
        Semaphore semaphor = [10]

        final NioClientSocketChannelFactory factory =[Executors.newCachedThreadPool(),Executors.newCachedThreadPool()]

        for (i in 0..100000) {
            workers.execute {
                semaphor.acquire()

                def firstMessage = ChannelBuffers.buffer(firstMessageSize)
                for (int j = 0; j < firstMessage.capacity(); j ++) {
                    firstMessage.writeByte((byte) j)
                }

                ClientBootstrap bootstrap = [factory]
                FrameDecoder handler = [
                    channelConnected: { /*ChannelHandlerContext*/ ctx, /*ChannelStateEvent*/ e ->
                        println "$i connected"
                        e.channel.write(firstMessage)
                        semaphor.release()
                    },

                    exceptionCaught: { /*ChannelHandlerContext*/ ctx, /*ExceptionEvent*/ e ->
                        // Close the connection when an exception is raised.
                        System.err.println "err @ $i ${e.cause.message}"
                        e.channel.close()
                        semaphor.release()
                    },

                    decode: { /*ChannelHandlerContext*/ ctx, /*Channel*/ channel, /*ChannelBuffer*/ buffer ->
                        if (buffer.readableBytes() < firstMessage.capacity())
                            return null

                        buffer.readBytes(firstMessage.capacity())
                        ThroughputMonitor.addBytesStat(firstMessage.capacity())
//                        channel.close ()
                        channel.write(firstMessage)
                        return ""
                    }
                ]

                bootstrap.pipeline.addLast("handler", handler);
                bootstrap.setOption("tcpNoDelay", true);
                bootstrap.setOption("keepAlive", true);

                bootstrap.connect(new InetSocketAddress(host, port))
            }
        }

        // Start performance monitor.
        ThroughputMonitor.startReporting()
    }
}
