@Typed package org.mbte.groovypp.samples.jetlang

import org.jetlang.channels.MemoryChannel
import org.jetlang.fibers.ThreadFiber
import org.jetlang.core.Callback
import org.jetlang.core.Disposable
import org.jetlang.channels.Channel

@Trait abstract static class ThreadActor {
    ThreadFiber   fiber    = []
    MemoryChannel channel  = []

    abstract <T> void onMsg (T msg)

    void start () {
        subscribe(channel) { msg ->
            onMsg (msg)
        }
        fiber.start ()
    }

    final <T> Disposable subscribe (Channel<T> channel, Callback<T> callback) {
        channel.subscribe(fiber, callback)
    }

    ThreadActor leftShift(msg) {
        channel.publish msg
    }

    void dispose () {
        fiber.dispose ()
    }

    void join () {
        fiber.join ()
    }
}

MemoryChannel stopChannel = []

@Field ThreadActor pingActor = { int msg ->
    println "$msg: ping"
    if (msg)
        pongActor << (msg-1)
    else
        stopChannel.publish(0)
}

@Field ThreadActor pongActor =  { int msg ->
    println "$msg: pong"
    pingActor << msg
}

[pingActor, pongActor].each { ThreadActor actor ->
    actor.subscribe(stopChannel) {
        actor.dispose ()
    }
    actor.start ()
}

pingActor << 100

pingActor.join()
pongActor.join()
