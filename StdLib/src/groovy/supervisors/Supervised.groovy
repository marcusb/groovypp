package groovy.supervisors

import groovy.util.concurrent.CallLaterExecutors

@Typed class Supervised<C extends SupervisedConfig> {
    Supervised parent
    C          config

    DelegatingFunction0<Supervised,?> afterCreated
    DelegatingFunction0<Supervised,?> beforeStart
    DelegatingFunction0<Supervised,?> afterStart
    DelegatingFunction0<Supervised,?> beforeStop
    DelegatingFunction0<Supervised,?> afterStop
    DelegatingFunction0<Supervised,?> afterChildrenCreated
    DelegatingFunction1<Supervised,Throwable,?> afterCrashed

    protected List<Supervised> children

    void setConfig(C config) {
        this.config = config

        afterCreated       = config?.afterCreated?.clone(this)
        beforeStart        = config?.beforeStart?.clone(this)
        afterStart         = config?.afterStart?.clone(this)
        beforeStop         = config?.beforeStop?.clone(this)
        afterStop          = config?.afterStop?.clone(this)
        afterChildrenCreated = config?.afterChildrenCreated?.clone(this)
        afterCrashed       = config?.afterCrashed?.clone(this)
    }

    final void start () {
        beforeStart?.call(this)
        try {
            doStart ()
            afterStart?.call(this)
        }
        catch (Throwable t) {
        }
    }

    final void stop () {
        beforeStop?.call(this)
        doStop ()
        afterStop?.call(this)
    }

    synchronized void addChild(Supervised child) {
        child.parent = this
        if (children == null) {
            children = []
        }
        children << child
    }

    synchronized void removeChild(Supervised child) {
        child.parent = null
        children?.remove(child)
    }

    void crash (Throwable cause) {
        CallLaterExecutors.getDefaultExecutor().execute {
            afterCrashed?.call(this, cause)
            parent?.removeChild(this)
            config.create(parent).start()
        }
    }

    synchronized void doStart () {
        children?.each { child ->
            child.start ()
        }
    }

    synchronized void doStop () {
        children?.reverse()?.each { child ->
            child.stop ()
        }
    }
}
