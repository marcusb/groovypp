@Typed package gretty

import org.mbte.gretty.httpserver.GrettyServer
import java.util.logging.Level
import java.util.logging.ConsoleHandler
import java.util.logging.LogManager
import org.jboss.netty.logging.InternalLogLevel
import org.jboss.netty.handler.codec.http.websocket.WebSocketFrame
import org.jboss.netty.handler.codec.http.websocket.DefaultWebSocketFrame

def rootLogger = LogManager.logManager.getLogger("")
rootLogger.setLevel(Level.FINE)
rootLogger.addHandler(new ConsoleHandler(level:Level.FINE))

GrettyServer server = [
    logLevel: InternalLogLevel.DEBUG,

    static: "./rootFiles",

    default: { response.html = template("./templates/404.ftl", [user:"Dear Unknow User"]) },

    public: {
        get("googlib/:path") {
            redirect "http://ajax.googleapis.com/ajax/libs/" + it.path
        }
    },

    webContexts: [
        "/websockets" : [
            static: "./webSocketsFiles",

            public: {
                websocket("/ws",[
                    onMessage: { msg ->
                        socket.send(msg.toUpperCase())
                    },

                    onConnect: {
                        socket.send("Welcome!")
                    }
                ])
            },
        ]
    ]
]
server.start()
