package groovy.util.concurrent

import java.util.concurrent.Executor

@Typed abstract class SupervisedChannel extends NonfairExecutingChannel {

    SupervisedChannel owner

    private volatile FList<SupervisedChannel> children = FList.emptyList

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
            owner.startupChild(this)
        else {
            if (!executor)
                executor = CallLaterExecutors.currentExecutor
            post(Startup.instance)
        }
    }

    final void shutdown () {
        if (owner)
            owner.shutdownChild(this)
        else
            post(Shutdown.instance)
    }

    final void startupChild(SupervisedChannel child, Executor executor = null) {
        child.owner = this
        children.apply{ it + child }
        child.executor = executor ? executor : (child.executor ? child.executor : this.executor)
        child.post(Startup.instance)
    }

    final void shutdownChild(SupervisedChannel child) {
        children.apply{ it - child }
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
                for(c in children)
                    c.shutdown ()
                doShutdown ()
                break

            case ChildCrashed:
                def crash = (ChildCrashed) message
                shutdownChild(crash.who)
                onChildCrashed(crash)
                break
        }
    }

    protected void doStartup() {}
    protected void doShutdown () {}

    protected void onChildCrashed (ChildCrashed message) {
        throw message.cause
    }

    final void crash(Throwable cause) {
        for(c in children)
            c.shutdown ()
        if(owner)
            owner.post(new ChildCrashed(who:this, cause:cause))
        else
            throw cause
    }

    final void post (Object message, boolean recursive, boolean including) {
        if (including)
            post(message)

        for(c in children)
            c.post(message)
    }

    final getRootSupervisor () {
        owner ? owner.getRootSupervisor() : this
    }

    protected boolean interested(Object message) {
        message && (
            message instanceof Startup  ||
            message instanceof Shutdown ||
            message instanceof ChildCrashed
        )
    }
}
