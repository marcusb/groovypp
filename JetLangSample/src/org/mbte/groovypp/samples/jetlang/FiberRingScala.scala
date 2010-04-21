package org.mbte.groovypp.samples.jetlang

import java.util.concurrent.{TimeUnit, CountDownLatch}
import actors.Actor._
import scala.actors.Actor
import scala.actors.Scheduler

object FiberRingScala {
 def main(args: Array[String]) {
  val start = System.currentTimeMillis()
  val channels = new Array[Actor](10000)
  val cdl = new CountDownLatch(channels.length * 500)

  var i: Int = 0
  while (i < channels.length) {
    val channel = actor {
      handle(i, channels, cdl)
    }
    channels(i) = channel
    i += 1
  }
  i = 0
  while (i < 500) {
    channels(i) ! "Hi"
    var j : Int = 0
    while (j < i) {
      cdl.countDown()
      j = j+1
    }
    i += 1
  }

  cdl.await(1000, TimeUnit.SECONDS)
  Scheduler.shutdown

  println(System.currentTimeMillis() - start)
 }

 def handle(i:Int, channels:Array[Actor], cdl:CountDownLatch) :Unit = {
  react {
    case x:Any =>
      if (i < channels.length -1)
        channels(i+1) ! x
      cdl.countDown()
      handle(i, channels, cdl)
  }
 }
}