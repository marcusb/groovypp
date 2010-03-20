package org.mbte.groovypp.remote.netty

import groovy.remote.RemoteConnection
import org.jboss.netty.channel.Channel
import groovy.supervisors.Supervised
import groovy.remote.RemoteMessage

@Typed static class NettyConnection extends RemoteConnection {
    protected Channel channel
    private Timer timer

    protected void doSend(RemoteMessage msg) {
        channel?.write(msg)
    }

    public void onConnect() {
        super.onConnect();
        timer = new Timer()
        timer.scheduleAtFixedRate({
            if (channel.isConnected())
                channel.write(new RemoteMessage.Identity(senderNodeId:clusterNode.id))
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
