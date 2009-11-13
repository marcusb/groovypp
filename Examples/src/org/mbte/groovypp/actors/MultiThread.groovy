import java.util.concurrent.ExecutorService
import java.util.concurrent.Executor
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import groovy.util.concurrent.ReadWriteLockable

@Typed
package org.mbte.groovypp.actors

class MultiThread {

}

class WorkerThread extends Thread {
    Actor currentActor
}

class Scheduler implements Executor {

    public void execute(Runnable runnable) {
    }

    private static final ThreadLocal<Actor> current = [:]

    static Actor getCurrentActor () {
        def currentThread = Thread.currentThread()
        if (currentThread instanceof WorkerThread) {
            currentThread.currentActor
        }
        else
            current.get()
    }


    static void setCurrentActor (Actor actor) {
        if (actor)
            assert !getCurrentActor()

        def currentThread = Thread.currentThread()
        if (currentThread instanceof WorkerThread) {
            currentThread.currentActor = actor
        }
        else
            current.set(actor)
    }
}

@Trait
abstract class MessageChannel extends ReadWriteLockable {
    final LinkedList queue = new LinkedList()

    void addLast (def message) {
         withWriteLock {
             queue << message
         }
    }

    def removeFirst () {
         withWriteLock {
             queue.removeFirst()
         }
    }

    def any (PartialFunction op) {
        withReadLock {
            queue.any { op.isDefined(it) }
        }
    }
}

@Trait
abstract class Actor {
}

/**
 * Message class
 */
class ActorMessage<T> extends Pair<T,Actor> {
    ActorMessage (T payLoad, Actor sender = Scheduler.getCurrentThreadProcessor()) {
        super(payLoad,  sender)
    }

    Actor getSender () {
        second
    }

    T getPayLoad () {
        first
    }
}