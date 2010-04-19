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



package groovy.supervisors

import groovy.remote.ClusterNode
import org.mbte.groovypp.remote.Server
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.mbte.groovypp.remote.ClientConnector
import groovy.util.concurrent.CallLaterExecutors
import org.mbte.groovypp.remote.inet.MulticastClusterNode

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
            MulticastClusterNode cluster = [
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
