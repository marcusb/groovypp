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

package groovy.remote

@Typed abstract class RemoteConnection {
    volatile ClusterNode       clusterNode
    volatile RemoteClusterNode remoteNode

    void onConnect () {
        send(new RemoteMessage.Identity())
    }

    void onDisconnect () {
        clusterNode?.onDisconnect(this)
        clusterNode = null
    }

    void onMessage (Object message) {
        RemoteMessage msg = message
        if (!remoteNode) {
            if (msg instanceof RemoteMessage.Identity) {
                remoteNode = new RemoteClusterNode(this, msg.senderNodeId)
                clusterNode.onConnect(remoteNode)
            }
            else {
                throw new RuntimeException("protocol error: $msg.senderNodeId")
            }
        }
        else {
            clusterNode.onMessage(remoteNode, msg)
        }
    }

    void onException (Throwable cause) {
      clusterNode?.onException(this, cause)
      onDisconnect()
    }

    final void send(RemoteMessage msg) {
        msg.senderNodeId = clusterNode.id
        doSend (msg)
    }

    protected abstract void doSend(RemoteMessage msg)
}