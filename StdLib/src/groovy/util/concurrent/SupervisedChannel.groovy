package groovy.util.concurrent

import groovy.util.concurrent.NonfairExecutingChannel
import java.util.concurrent.Executor
import groovy.util.concurrent.FList

@Typed abstract class SupervisedChannel extends NonfairExecutingChannel {

    SupervisedChannel owner

    private volatile FList<SupervisedChannel> childs = FList.emptyList

    private static final class Startup  {
        static Startup instance = []
    }
    private static final class Shutdown {
        static Shutdown instance = []
    }
    private static final class ChildCrashed {
        SupervisedChannel who
        Throwable cause
    }

    final void startup () {
        if (owner)
            owner.startChild(this)
        else {
            if (!executor)
                executor = CallLaterExecutors.currentExecutor
            post(Startup.instance)
        }
    }

    final void shutdown () {
        if (owner)
            owner.stopChild(this)
        else
            post(Shutdown.instance)
    }

    protected final void startChild(SupervisedChannel child, Executor executor = null) {
        child.owner = this
        childs.apply{ it + child }
        child.executor = executor ? executor : (child.executor ? child.executor : this.executor)
        child.post(Startup.instance)
    }

    protected final void stopChild(SupervisedChannel child) {
        childs.apply{ it - child }
        child.post(Shutdown.instance)
    }

    protected final void onMessage(Object message) {
        try {
            doOnMessage(message)
        }
        catch(Throwable cause) {
            crash(cause)
        }
    }

    protected void doOnMessage(Object message) {
        switch(message) {
            case Startup:
                doStartup()
                break

            case Shutdown:
                childs*.shutdown ()
                doShutdown ()
                break

            case ChildCrashed:
                def crash = (ChildCrashed) message
                stopChild(crash.who)
                onChildCrashed(crash)
                break
        }
    }

    protected void doStartup() {}
    protected void doShutdown () {}

    protected void onChildCrashed (ChildCrashed message) {
        throw message.cause
    }

    protected final void crash(Throwable cause) {
        childs*.shutdown ()
        if(owner)
            owner.post(new ChildCrashed(who:this, cause:cause))
        else
            throw cause
    }
}
