@Typed package org.mbte.groovypp.samples.jetlang

import org.jetlang.fibers.ThreadFiber
import org.jetlang.channels.MemoryChannel

def fiber = new ThreadFiber ()
fiber.start ()

def channel = new MemoryChannel ()

channel.subscribe(fiber) { msg ->
    switch(msg) {
        case "The End":
            println "I will be back..."
            fiber.dispose()
            fiber.join()
        break

        case "Terminate":
          println "Hastala vista baby!!!"
        break

        default:
          println "You are terminated******"    
    }
}

channel.publish("Terminate")
channel.publish("Buy me icecream")
channel.publish("The End")
channel.publish("Terminate")