package org.mbte.gretty.server

abstract class GrettyWebSocketListener {
    void onDisconnect() {}

    abstract void onMessage(String message)
}
