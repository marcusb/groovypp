package groovy.supervisors

import groovy.remote.ClusterNode
import org.mbte.groovypp.remote.netty.NettyServer

@Typed class ClusterTest extends GroovyTestCase {
    void testStartStop () {
        int n = 5
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
                    break
                }
            }
            cluster.mainActor = { msg ->
                println "${cluster.id} received '$msg'"
            }
            cluster.start()
        }
        Thread.sleep(5000)
    }
}
