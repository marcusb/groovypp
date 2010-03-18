package org.mbte.groovypp.remote.netty

import groovy.util.concurrent.NonfairExecutingChannel
import java.util.concurrent.Executor
import groovy.util.concurrent.FList
import java.util.concurrent.atomic.AtomicReference

@Typed abstract class SupervisedChannel extends NonfairExecutingChannel {
    SupervisedChannel owner

    private AtomicReference<FList<SupervisedChannel>> childs = [FList.emptyList]

    private static final class Start {}
    private static final class Stop  {}
    private static final class Crash {
        SupervisedChannel who
        Throwable      cause
    }

    final void start () {
        if (owner)
            owner.startChild(this)
        else
            post(new Start())
    }

    final void stop () {
        if (owner)
            owner.stopChild(this)
        else
            post(new Stop())
    }

    protected final void startChild(SupervisedChannel child, Executor executor = null) {
        child.owner = this
        child.executor = executor ? executor : this.executor
        childs.apply{ it + child }
        child.post(new Start())
    }

    protected final void stopChild(SupervisedChannel child) {
        child.owner = null
        childs.apply{ it - child }
        child.post(new Stop())
    }

    protected void onMessage(Object message) {
        switch(message) {
            case Start: doStart(); break

            case Stop:
                childs*.stop ()
                break

            case Crash:
                Crash crash = (Crash) message
                stopChild(crash.who)
                onChildCrashed(crash)
                break
        }
    }

    protected void doStart() {}

    protected void doStop () {}

    protected void onChildCrashed (Crash message) {
        crash(message.cause)
    }

    protected final void crash(Throwable cause) {
        childs*.stop ()
        if(owner)
            owner.post(new Crash(who:this, cause:cause))
        else
            throw cause
    }
}
