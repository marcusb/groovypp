package groovy.util.concurrent

import groovy.util.concurrent.NonfairExecutingChannel
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger
import groovy.util.concurrent.FList

@Typed abstract class SupervisedChannel<M> extends NonfairExecutingChannel<M> {
    private static int NOT_STARTED    = 0
    private static int STARTING       = 1
    private static int STARTED        = 2
    private static int STOP_MASK      = 8
    private static int STOPPING       = STOP_MASK
    private static int STOPPED        = STOP_MASK|1
    private static int CRASHED        = STOP_MASK|2

    SupervisedChannel owner

    private volatile FList<SupervisedChannel> children = FList.emptyList

    private volatile int state

    private static final class Startup  {
        Function0 afterStartup
    }
    private static final class Shutdown {
        Function0 afterShutdown
    }
    private static final class ChildCrashed {
        SupervisedChannel who
        Throwable cause
    }

    final void startup (Function0 afterStartup = null) {
        if (owner) {
            owner.startupChild(this, afterStartup)
        }
        else {
            beginStartup ()
            if (!executor) throw new NullPointerException("executor should be set prior to startup")
            post(new Startup(afterStartup:afterStartup))
        }
    }

    final void startupChild(SupervisedChannel child, Function0 afterStartup = null) {
        child.beginStartup ()
        child.owner = this
        children.apply{ it + child }
        child.executor = child.executor ? child.executor : this.executor
        child.post(new Startup(afterStartup:afterStartup))
    }

    private void beginStartup () {
        if (!state.compareAndSet(0, STARTING)) {
            throw new IllegalStateException("${this} was started already")
        }
    }

    final void shutdown (Function0 afterShutdown = null) {
        if (owner)
            owner.shutdownChild(this, afterShutdown)
        else {
            beginShutdown ()
            post(new Shutdown(afterShutdown:afterShutdown))
        }
    }

    private void beginShutdown () {
        for (;;) {
            def s = state
            switch(s) {
                case NOT_STARTED:
                    throw new IllegalStateException("${this} was not started yet")

                case STOPPING:
                case  STOPPED:
                    throw new IllegalStateException("${this} was stopped already")

                case STARTING:
                case STARTED:
                    if(state.compareAndSet(s, STOPPING)) {
                        return
                    }
            }
        }
    }

    final void shutdownChild(SupervisedChannel child, Function0 afterShutdown = null) {
        child.beginShutdown ()
        children.apply{ it - child }
        child.post(new Shutdown(afterShutdown:afterShutdown))
    }

    protected final void onMessage(Object message) {
        try {
            doOnMessage(message)
        }
        catch(Throwable cause) {
            cause.printStackTrace()
            crash(cause)
        }
    }

    protected void doOnMessage(Object message) {
        switch(message) {
            case Startup:
                doStartup()
                message.afterStartup?.call ()
                state = STARTED
                break

            case Shutdown:
                def childsCount = children.size()
                if (childsCount) {
                    AtomicInteger cnt = [childsCount]
                    for(c in children)
                        c.shutdown {
                            if(!cnt.decrementAndGet()) {
                                doShutdown ()
                                message.afterShutdown?.call()
                                state = STOPPED
                            }
                        }
                }
                else {
                    doShutdown ()
                    message.afterShutdown?.call()
                    state = STOPPED
                }
                break

            case ChildCrashed:
                shutdownChild(message.who)
                onChildCrashed(message)
                break
        }
    }

    protected void doStartup() {}
    protected void doShutdown () {}

    protected void onChildCrashed (ChildCrashed message) {
        throw message.cause
    }

    protected final void crash(Throwable cause) {
        for(c in children)
            c.shutdown ()
        
        if(owner)
            owner.post(new ChildCrashed(who:this, cause:cause))
        else
            throw cause
    }

    final void broadcast (Object message, boolean recursive, boolean including) {
        if (including)
            post(message)

        if (recursive)
            for(c in children)
                c.broadcast(message, recursive, true)
    }

    final SupervisedChannel getRootSupervisor () {
        owner ? owner.rootSupervisor : this
    }

    protected final boolean interested(Object message) {
        message instanceof Startup  || message instanceof Shutdown || message instanceof ChildCrashed || (!(state & STOP_MASK) && checkInterest(message))
    }

    protected boolean checkInterest (Object message) { true }
}
