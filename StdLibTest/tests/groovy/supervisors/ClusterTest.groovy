package groovy.supervisors

import groovy.remote.ClusterNode
import org.mbte.groovypp.remote.Server
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.mbte.groovypp.remote.ClientConnector
import groovy.util.concurrent.CallLaterExecutors
import org.mbte.groovypp.remote.inet.InetClusterNode

@Typed class ClusterTest extends GroovyTestCase {
    int findFreePort() {
      def server = new ServerSocket(0)
      def port = server.getLocalPort()
      server.close()
      port
    }


    void testStartStop () {
        def n = 3
        def stopCdl = new CountDownLatch(n)
        def connectCdl = new CountDownLatch(n*(n-1))
        def disconnectCdl = new CountDownLatch(n*(n-1))
        def pool = CallLaterExecutors.newCachedThreadPool()
        for(i in 0..<n) {
            InetClusterNode cluster = [
                doStartup: {
                    startupChild(new Server(address : new InetSocketAddress(InetAddress.getLocalHost(), findFreePort())))
                    startupChild(new Server(address : new InetSocketAddress(InetAddress.getLocalHost(), findFreePort())))

                    startupChild(new ClientConnector())
                }
            ]
            cluster.executor = pool
            cluster.communicationEvents.subscribe { msg ->
                println "${cluster.id} $msg"
            }
            cluster.communicationEvents.subscribe { msg ->
                switch (msg) {
                    case ClusterNode.CommunicationEvent.Connected:
                        msg.remoteNode << "Hello!"
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
                    cluster.shutdown {
                        stopCdl.countDown()
                        println "${cluster.id} stopped"
                    }
                }
            }

            cluster.startup()
        }
        assertTrue(stopCdl.await(100,TimeUnit.SECONDS))
        assertTrue(connectCdl.await(100,TimeUnit.SECONDS))
        assertTrue(disconnectCdl.await(100,TimeUnit.SECONDS))
    }
}
