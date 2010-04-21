@Typed package org.mbte.groovypp.samples.jetlang

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.jetlang.fibers.ThreadFiber
import org.jetlang.channels.MemoryChannel
import org.jetlang.fibers.PoolFiber
import java.util.concurrent.Executors
import org.jetlang.fibers.PoolFiberFactory

def start = System.currentTimeMillis()

def pool = Executors.newFixedThreadPool(Runtime.runtime.availableProcessors())
def fiberFactory = new PoolFiberFactory(pool)

def channels = new MemoryChannel[10000]
CountDownLatch cdl = [channels.length*500]
for (i in 0..<channels.length) {
    def fiber = fiberFactory.create()
    def channel = new MemoryChannel()
    channel.subscribe(fiber)  {
        if (i < channels.length-1)
            channels[i+1].publish(it)
        cdl.countDown()
    }
    channels [i] = channel
    fiber.start()
}
for(i in 0..<500) {
    channels[i].publish("Hi")
    for (j in 0..<i)
        cdl.countDown()
}

assert(cdl.await(100,TimeUnit.SECONDS))
pool.shutdown()
println (System.currentTimeMillis() - start)