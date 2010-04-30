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

package org.mbte.groovypp.remote.inet

import groovy.remote.ClusterNode
import org.mbte.groovypp.remote.ClientConnector
import org.mbte.groovypp.remote.Server

@Typed class MulticastClusterNode extends ClusterNode {
  InetAddress multicastGroup = InetAddress.getByAddress(230,0,0,239)
  int         multicastPort  = 4238

  protected void doStartup() {
      super.doStartup();
      startupChild(server) {
        if (multicastGroup && multicastPort) {
          server.startupChild (new MulticastChannel.Sender([
                  multicastGroup: multicastGroup,
                  multicastPort:  multicastPort,
                  dataToTransmit: InetDiscoveryInfo.toBytes(id, (InetSocketAddress)server.address)
          ]))
        }
      }
      startupChild(clientConnector) {
        if (multicastGroup && multicastPort) {
          clientConnector.startupChild(new MulticastChannel.Receiver([
                  multicastGroup: multicastGroup,
                  multicastPort: multicastPort,
          ]))
        }
      }
  }
}
