@Typed package org.mbte.groovypp.samples.jetlang

import org.jetlang.fibers.ThreadFiber
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

ThreadFiber fiber = []
fiber.start()

def n = 10
def latch = new CountDownLatch(n)
for (i in 0..<n)
    fiber.execute {
        print "$i "
        latch.countDown()
    }

latch.await(10, TimeUnit.SECONDS)

//shutdown thread
fiber.dispose()
