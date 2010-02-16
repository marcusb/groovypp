package groovy.remote

@Typed abstract class RemoteConnection {
    ClusterNode clusterNode

    Config config

    void onConnect () {
        onConnect?.call()
    }

    void onDisconnect () {
        onDisconnect?.call()
    }

    void onMessage (Object msg) {
        onMessage?.call(msg)
    }

    void onException (Throwable cause) {
        onException?.call(cause)
    }

    void setConfig(Config config) {
        onConnect    = config?.onConnect?.clone(this)
        onDisconnect = config?.onDisconnect?.clone(this)
        onMessage    = config?.onMessage?.clone(this)
        onException  = config?.onException?.clone(this)
    }

    abstract void send(Object msg)

    DelegatingFunction0<RemoteConnection,?>               onConnect
    DelegatingFunction0<RemoteConnection,?>               onDisconnect
    DelegatingFunction1<RemoteConnection,Object,?>        onMessage
    DelegatingFunction1<RemoteConnection,Throwable,?>     onException

    @Trait abstract class Config {
        DelegatingFunction0<RemoteConnection,?>               onConnect
        DelegatingFunction0<RemoteConnection,?>               onDisconnect
        DelegatingFunction1<RemoteConnection,Object,?>        onMessage
        DelegatingFunction1<RemoteConnection,Throwable,?>     onException
    }
}