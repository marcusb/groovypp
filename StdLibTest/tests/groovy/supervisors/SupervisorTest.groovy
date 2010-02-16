package groovy.supervisors

import org.mbte.groovypp.remote.netty.NettyServer
import org.mbte.groovypp.remote.netty.NettyClient
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import groovy.remote.ClusterNode
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.DirectChannelBufferFactory

@Typed class SupervisorTest extends GroovyTestCase {
    void testMe () {
        SupervisedConfig config = [
            beforeStart: { println "supervisor starting" },

            afterStop: { println "supervisor stopped" },

            afterChildsCreated: {
                childs.each { c ->
                    println c
                }
            },

            childs: [
                [
                    afterStart: { println "worker started" },

                    beforeStop: { println "worker stopped" }
                ],

                [ klazz : Supervised ],
            ]
        ]

        Supervised supervisor = config.create(null)
        supervisor.start ()
        supervisor.stop ()
    }

    void testServer () {
        int k = 100000, n = 5
        CountDownLatch waitFor = [3*n*k]

        def start = System.currentTimeMillis()

        SupervisedConfig config = [
            childs : [
                [
                    server: [
                        port : 8080,
                        beforeStart: { println "server starting" },
                        afterStop: { println "server stopped" },
                        onMessage: { msg ->
                            ChannelBuffer buf = msg
                            while(buf.readableBytes() >= 8) {
                                def newBuf = DirectChannelBufferFactory.instance.getBuffer(32)
                                newBuf.writeLong(buf.readLong())
                                send(newBuf)
                                waitFor.countDown()
                            }
                        }
                    ] as NettyServer.Config
                ] as ClusterNode.Config,
                [
                    numberOfInstances : n,    
                    host : "localhost",
                    port : 8080,
                    onConnect:    { println "client connected";
                        for (i in 0..<k) {
                            def buf = DirectChannelBufferFactory.instance.getBuffer(32)
                            buf.writeLong(i)
                            send(buf)
                            waitFor.countDown()
                        }
                    },
                    onMessage: { msg ->
                        ChannelBuffer buf = msg
                        while(buf.readableBytes() >= 8) {
                            buf.readLong()
                            waitFor.countDown()
                        }
                    },
                    onDisconnect: { println "client disconnected" },
                    afterCrashed: { cause ->
                        println "client crashed '${cause?.message ?: cause?.class?.name ?: "unknown reason" }'"
                        crash(cause)
                    },
                ] as NettyClient.Config
            ]
        ]

        Supervised supervisor = config.create(null)
        supervisor.start ()

        boolean awaitResult = waitFor.await(30, TimeUnit.SECONDS)
        println "AWAITED: $awaitResult ${waitFor.count}"
        assert awaitResult

        supervisor.stop()

        println (System.currentTimeMillis() - start) 
    }
}
