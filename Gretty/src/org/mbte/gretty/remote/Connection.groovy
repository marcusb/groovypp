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

package org.mbte.gretty.remote

import org.jboss.netty.channel.Channel

@Typed static class Connection extends RemoteConnection {
    protected Channel channel
    private Timer timer

    protected void doSend(RemoteMessage msg) {
        channel?.write(msg)
    }

    public void onConnect() {
        super.onConnect();
        timer = new Timer()
        timer.scheduleAtFixedRate({
            if (channel?.isConnected())
                channel?.write(new RemoteMessage.Identity(senderNodeId:clusterNode.id))
            else {
                timer?.cancel()
                timer = null
            }
        }, 0L, 1000L)
    }

    public void onDisconnect() {
        timer?.cancel()
        timer = null
        channel?.close()
        channel = null
        super.onDisconnect()
    }
}
