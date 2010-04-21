/*
 * Copyright (c) 2009-10. MBTE Sweden AB. All Rights reserved
 */

@Typed package org.mbte.groovypp.samples.zookeeper

import org.apache.zookeeper.ZooKeeper
import java.util.concurrent.CountDownLatch
import org.apache.zookeeper.CreateMode

import org.apache.zookeeper.Watcher
import org.apache.zookeeper.ZooDefs.Ids
import org.apache.zookeeper.data.Stat
import org.apache.zookeeper.AsyncCallback
import org.apache.log4j.BasicConfigurator

class ZooKeeperEx extends ZooKeeper {
  def lock = new Object()

  ZooKeeperEx (String connection, int units) {
    super(connection, units, null)

    Watcher watcher = { event ->
      System.err.println event
      switch(event.type) {
        case Watcher.Event.EventType.None:
            switch(event.state) {
              case Watcher.Event.KeeperState.SyncConnected:
                onConnect ()
                break

              case Watcher.Event.KeeperState.Disconnected:
                onDisconnect ()
                break

              case Watcher.Event.KeeperState.Expired:
                onExpired ()
                break
            }
          break;
      }
    }

    register(watcher)
  }

  protected void onConnect    () {}
  protected void onDisconnect () {}
  protected void onExpired    () {}
}

BasicConfigurator.configure()

CountDownLatch cdl = [1]

ZooKeeperEx client2 = ['super':["127.0.0.1:2181", 200000],
  onConnect:{
      exists ("/test7", true, { int rc, String path, Object ctx, Stat stat ->
        if (!stat) {
          println "creating"
          create("/test7", "Hello, World!".bytes, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
          exists("/test7", true, this, null)
        }
        else {
          create("/test7/data", "Hello, World!".bytes, Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL,
                  { int r, String p, Object c, String name ->
                    println name
                    cdl.countDown()
                  }, null)
        }
      }, null )
  },
  onDisconnect:{},
  onExpired:{}
]

cdl.await()


def remove (ZooKeeper zoo, String path) {
  for (c in zoo.getChildren(path, false))
    remove(zoo, path + "/" +c)
  zoo.delete(path, -1)
}
