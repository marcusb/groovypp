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

package org.mbte.groovypp.samples.zookeeper

import groovy.remote.ClusterNode
import org.apache.zookeeper.Watcher
import org.apache.zookeeper.ZooKeeper
import org.apache.zookeeper.WatchedEvent
import org.apache.zookeeper.data.Stat
import org.apache.zookeeper.CreateMode
import org.mbte.groovypp.remote.inet.InetDiscoveryInfo
import org.apache.zookeeper.data.ACL
import org.apache.zookeeper.ZooDefs.Ids
import org.apache.zookeeper.AsyncCallback.StringCallback
import org.apache.zookeeper.AsyncCallback

@Typed class ZooClusterNode extends ClusterNode implements Watcher {
  protected ZooKeeper zoo

  private volatile boolean connected

  String rootPath       = "/cluster"
  String connectString  = "localhost:2181"
  int    sessionTimeout = 2000

  ZooKeeper getZoo () {
    zoo
  }

  protected void doStartup() {
      startupChild(server) {
        zoo = createZoo()
      }
  }

  private ZooKeeper createZoo() {
    [connectString, sessionTimeout, this]
  }

  protected void doShutdown() {
    zoo?.close()
    zoo = null
  }

  void onZooConnect() {
      zoo.exists (rootPath, true, { int rc, String path, Object ctx, Stat stat ->
          if (!stat) {
            zooCreate(rootPath, CreateMode.PERSISTENT) { r, p, c, name ->
              createEphemeral()
            }
          }
          else {
            createEphemeral()
          }
        }, null )
  }

  private void createEphemeral () {
      startupChild(clientConnector) {
        observeCluster()
      }
      zooCreate("$rootPath/${id}", InetDiscoveryInfo.toBytes(id, (InetSocketAddress)server.address), CreateMode.EPHEMERAL) { r, p, c, name -> }
  }

  void onZooExpired() {
      connected = false
      zoo?.close ()
      zoo = createZoo()
  }

  void zooCreate (String path, byte [] data = "No data".bytes, List<ACL> acl = Ids.OPEN_ACL_UNSAFE, CreateMode createMode, StringCallback cb) {
    zoo.create(path, data, acl, createMode, cb, null)
  }

  def void onZooNodeChildrenChanged(String p) {
    observeCluster()
  }

  private def observeCluster() {
    zoo?.getChildren(rootPath, true, {
      int rc, String path, Object ctx, List<String> children ->
        for (c in children) {
          if (c != id.toString()) {
            zoo?.getData("$rootPath/$c", false, [processResult: {
              int r, String p, Object ctx_, byte[] data, Stat stat ->
                clientConnector << InetDiscoveryInfo.fromBytes(data)
            }], null)
          }
        }
    }, null)
  }

  void process(WatchedEvent event) {
    schedule {
      println event
      switch (event.type) {
        case Watcher.Event.EventType.NodeCreated:
            onZooNodeCreated(event.path)
          break;

        case Watcher.Event.EventType.NodeDeleted:
            onZooNodeDeleted(event.path)
          break;

        case Watcher.Event.EventType.NodeDataChanged:
            onZooNodeDataChanged(event.path)
          break;

        case Watcher.Event.EventType.NodeChildrenChanged:
            onZooNodeChildrenChanged(event.path)
          break;

        case Watcher.Event.EventType.None:
          switch (event.state) {
            case Watcher.Event.KeeperState.SyncConnected:
              onZooConnect()
              break

            case Watcher.Event.KeeperState.Disconnected:
              onZooDisconnect()
              break

            case Watcher.Event.KeeperState.Expired:
              onZooExpired()
              break
          }
          break;
      }
    }
  }

  void onZooDisconnect () {}
  void onZooNodeCreated(String path) {}
  void onZooNodeDeleted(String path) {}
  void onZooNodeDataChanged(String path) {}
}
