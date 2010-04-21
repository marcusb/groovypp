package org.mbte.groovypp.samples.jetlang

import java.util.concurrent.{TimeUnit, CountDownLatch}
import scala.actors.Actor._
import scala.actors.scheduler.ForkJoinScheduler
import scala.actors.Reactor
import scala.actors.Scheduler

class FiberRingActor
  (i: Int, channels: Array[Reactor], cdl: CountDownLatch)
    extends Reactor {
  def act = FiberRingScala2.handle(i, channels, cdl)
  override def scheduler = FiberRingScala2.fjs
}

object FiberRingScala2 {
  val fjs = new ForkJoinScheduler(2, 2, false)
  fjs.start()

 def main(args: Array[String]) {
  val start = System.currentTimeMillis()
  val channels = new Array[Reactor](10000)
  val cdl = new CountDownLatch(channels.length * 500)

  var i: Int = 0
  while (i < channels.length) {
    /*
    val channel = actor {
      handle(i, channels, cdl)
    }
    */
    val channel =
      new FiberRingActor(i, channels, cdl)
    channel.start
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

 def handle(i:Int, channels:Array[Reactor], cdl:CountDownLatch) :Unit = {
  react {
    case x:Any =>
      if (i < channels.length -1)
        channels(i+1) ! x
      cdl.countDown()
      handle(i, channels, cdl)
  }
 }
}
