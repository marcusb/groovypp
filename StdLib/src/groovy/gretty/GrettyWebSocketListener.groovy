package groovy.gretty

abstract class GrettyWebSocketListener {
    void onDisconnect() {}

    abstract void onMessage(String message)
}
