package groovy.supervisors

import org.mbte.groovypp.remote.netty.NettyServer
import org.mbte.groovypp.remote.netty.NettyClient
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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

        Supervised supervisor = config.create()
        supervisor.start ()
        supervisor.stop ()
    }

    void testServer () {
        int n = 250
        CountDownLatch waitFor = [n]

        SupervisedConfig config = [
            childs: [
                [
                    numberOfInstances : n,
                    host : "localhost",
                    port : 8080,    
                    afterCreated: { println "client created"      },
                    beforeStart:  { println "client starting"     },
                    afterStop:    { println "client stopped"      },
                    onConnect:    { println "client connected"; channel.write "Hello"    },
                    onDisconnect: { println "client disconnected" },
                    afterCrashed: { msg, cause ->
                        println "client crashed '${cause?.message ?: msg}'"
                        cause?.printStackTrace ()
                    },
                ] as NettyClient.Config,
                [
                    port : 8080,    
                    beforeStart: { println "server starting" },
                    afterStop: { println "server stopped" },
                    onMessage: { msg -> println msg; waitFor.countDown() }
                ] as NettyServer.Config,
            ]
        ]

        Supervised supervisor = config.create()
        supervisor.start ()

        waitFor.await(30, TimeUnit.SECONDS)

        supervisor.stop()
    }
}