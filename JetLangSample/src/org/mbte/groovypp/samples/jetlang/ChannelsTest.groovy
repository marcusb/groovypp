@Typed package org.mbte.groovypp.samples.jetlang

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.jetlang.fibers.ThreadFiber
import org.jetlang.channels.MemoryChannel

// start thread backed receiver.
// Lighweight fibers can also be created using a thread pool
def receiver = new ThreadFiber()
receiver.start()

// number of passes
def n = 10

// create java.util.concurrent.CountDownLatch to notify when message arrives
def latch = new CountDownLatch(n)

// create channel to message between threads
def channel = new MemoryChannel<String>();

//add subscription for message on receiver thread
for (i in 0..<n) {
    channel.subscribe(receiver) { msg ->
        println "$i: $msg"
        latch.countDown();
    }
}

//publish message to receive thread. the publish method is thread safe.
channel.publish("Hello");

//wait for receiving thread to receive message
latch.await(10, TimeUnit.SECONDS);

//shutdown thread
receiver.dispose();
