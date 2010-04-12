package org.mbte.groovypp.remote.inet

import groovy.remote.ClusterNode
import org.mbte.groovypp.remote.ClientConnector
import org.mbte.groovypp.remote.Server

@Typed class InetClusterNode extends ClusterNode {
  InetAddress multicastGroup = InetAddress.getByAddress(230,0,0,239)
  int         multicastPort  = 4238

  void startServerSniffer(ClientConnector clientConnector) {
    if (multicastGroup && multicastPort) {
      clientConnector.startupChild(new BroadcastThread.Receiver([
              multicastGroup: multicastGroup,
              multicastPort: multicastPort,
              messageTransform: { byte [] buf -> InetDiscoveryInfo.fromBytes(buf) }
      ]))
    }
  }

  void startServerBroadcaster(Server server) {
    if (multicastGroup && multicastPort)
      server.startupChild (new BroadcastThread.Sender([
              multicastGroup: multicastGroup,
              multicastPort:  multicastPort,
              dataToTransmit: InetDiscoveryInfo.toBytes(id, (InetSocketAddress)server.address)
      ]))
  }
}
