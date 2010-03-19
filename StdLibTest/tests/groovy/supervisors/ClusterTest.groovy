package groovy.supervisors

import groovy.remote.ClusterNode
import org.mbte.groovypp.remote.netty.NettyServer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@Typed class ClusterTest extends GroovyTestCase {
    void testStartStop () {
        def n = 10
        def stopCdl = new CountDownLatch(n)
        def connectCdl = new CountDownLatch(n*(n-1))
        def disconnectCdl = new CountDownLatch(n*(n-1))
        for(i in 0..<n) {
            ClusterNode cluster = [
                    server: [port: 8000 + i] as NettyServer,
            ]
            cluster.communicationEvents.subscribe { msg ->
                println "${cluster.id} $msg"
            }
            cluster.communicationEvents.subscribe { msg ->
                switch (msg) {
                    case ClusterNode.CommunicationEvent.Connected:
                        ((ClusterNode.CommunicationEvent.Connected)msg).remoteNode << "Hello!"
                        connectCdl.countDown()
                    break
                    case ClusterNode.CommunicationEvent.Disconnected:
                        disconnectCdl.countDown()
                    break
                }
            }
            cluster.mainActor = { msg ->
                @Field int counter=1
                println "${cluster.id} received '$msg' $counter"
                counter++
                if (counter == n) {
                    println "${cluster.id} stopping"
                    cluster.shutdown ()
                    stopCdl.countDown()
                    println "${cluster.id} stopped"
                }
            }
            cluster.startup()
        }
        assertTrue(stopCdl.await(60,TimeUnit.SECONDS))
        assertTrue(connectCdl.await(60,TimeUnit.SECONDS))
        assertTrue(disconnectCdl.await(60,TimeUnit.SECONDS))
    }
}
