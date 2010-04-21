@Typed package org.mbte.groovypp.samples.jetlang

import org.jetlang.channels.MemoryChannel
import org.jetlang.fibers.ThreadFiber

MemoryChannel pingChannel = [], pongChannel = [], stopChannel = []

ThreadFiber pingThread = [], pongThread = []

[pingThread, pongThread].each { ThreadFiber fiber ->
    stopChannel.subscribe(fiber) {
        fiber.dispose ()
    }
    fiber.start ()
}


pingChannel.subscribe(pingThread) { int msg ->
    println "$msg: ping"
    if (msg)
        pongChannel.publish (msg-1)
    else
        stopChannel.publish(0)
}

pongChannel.subscribe(pongThread) { int msg ->
    println "$msg: pong"
    pingChannel.publish (msg)
}

pingChannel.publish 100

pingThread.join()
pingThread.join()
