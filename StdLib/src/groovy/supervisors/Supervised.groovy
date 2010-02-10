package groovy.supervisors

import groovy.util.concurrent.CallLater
import groovy.util.concurrent.CallLaterExecutors

@Typed class Supervised<C extends SupervisedConfig> {
    Supervised parent
    C          config

    protected List<Supervised> childs

    final void start () {
        config.beforeStart?.call(this)
        doStart ()
        config.afterStart?.call(this)
    }

    final void stop () {
        config.beforeStop?.call(this)
        doStop ()
        config.afterStop?.call(this)
    }

    synchronized void addChild(Supervised child) {
        child.parent = this
        if (childs == null) {
            childs = []
        }
        childs << child
    }

    synchronized void removeChild(Supervised child) {
        child.parent = null
        childs?.remove(child)
    }

    void crash (String message = null, Throwable cause = null) {
        CallLaterExecutors.getDefaultExecutor().execute {
            config.afterCrashed?.call(this$0, message, cause)
            def parent = this$0.parent
            parent?.removeChild(this$0)
            config.create(parent).start()
        }
    }

    synchronized void doStart () {
        childs?.each { child ->
            child.start ()
        }
    }

    synchronized void doStop () {
        childs?.reverse()?.each { child ->
            child.stop ()
        }
    }
}
