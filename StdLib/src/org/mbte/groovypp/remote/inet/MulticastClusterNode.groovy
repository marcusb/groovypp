package org.mbte.groovypp.remote.inet

import groovy.remote.ClusterNode
import org.mbte.groovypp.remote.ClientConnector
import org.mbte.groovypp.remote.Server

@Typed class MulticastClusterNode extends ClusterNode {
  InetAddress multicastGroup = InetAddress.getByAddress(230,0,0,239)
  int         multicastPort  = 4238

  void startServerSniffer(ClientConnector clientConnector) {
    if (multicastGroup && multicastPort) {
      clientConnector.startupChild(new MulticastChannel.Receiver([
              multicastGroup: multicastGroup,
              multicastPort: multicastPort,
      ]))
    }
  }

  void startServerBroadcaster(Server server) {
    if (multicastGroup && multicastPort)
      server.startupChild (new MulticastChannel.Sender([
              multicastGroup: multicastGroup,
              multicastPort:  multicastPort,
              dataToTransmit: InetDiscoveryInfo.toBytes(id, (InetSocketAddress)server.address)
      ]))
  }
}
