/*
 * Copyright (c) 2009-10. MBTE Sweden AB. All Rights reserved
 */

@Typed package org.mbte.groovypp.samples.zookeeper

import org.apache.zookeeper.*
import java.util.concurrent.CountDownLatch

import java.util.concurrent.TimeUnit
import org.apache.log4j.PropertyConfigurator
import groovy.remote.ClusterNode

PropertyConfigurator.configure("log4j.properties")

testWithFixedPool(20) {
  def n = 3
  def stopCdl = new CountDownLatch(n)
  def connectCdl = new CountDownLatch(n*(n-1))
  def disconnectCdl = new CountDownLatch(n*(n-1))
  for(i in 0..<n) {
      ZooClusterNode cluster = [
              executor:pool
      ]
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
  assert stopCdl.await(100,TimeUnit.SECONDS)
  assert connectCdl.await(100,TimeUnit.SECONDS)
  assert disconnectCdl.await(100,TimeUnit.SECONDS)
}

def remove (ZooKeeper zoo, String path) {
  for (c in zoo.getChildren(path, false))
    remove(zoo, path + "/" +c)
  zoo.delete(path, -1)
}
