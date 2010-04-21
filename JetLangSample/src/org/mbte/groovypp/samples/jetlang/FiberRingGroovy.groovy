@Typed package org.mbte.groovypp.samples.jetlang

import java.util.concurrent.CountDownLatch
import groovy.util.concurrent.NonfairExecutingChannel
import java.util.concurrent.TimeUnit
import java.util.concurrent.Executors

def start = System.currentTimeMillis()
def pool = Executors.newFixedThreadPool(Runtime.runtime.availableProcessors())
def channels = new NonfairExecutingChannel [10000]
CountDownLatch cdl = [channels.length*500]
for (i in 0..<channels.length) {
    NonfairExecutingChannel channel = {
        if (i < channels.length-1)
          channels[i+1] << it
        cdl.countDown()
    }
    channel.executor = pool
    channels [i] = channel
}

for(i in 0..<500) {
    channels[i] << "Hi"
    for (j in 0..<i)
        cdl.countDown()
}

assert(cdl.await(100,TimeUnit.SECONDS))
assert(pool.shutdownNow().empty)
pool.awaitTermination(0L,TimeUnit.SECONDS)
println(System.currentTimeMillis()-start)
