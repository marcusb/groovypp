package org.mbte.gretty.httpserver

abstract class GrettyWebSocketListener {
    void onDisconnect() {}

    abstract void onMessage(String message)
}
