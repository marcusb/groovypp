@Typed package gretty

import org.mbte.gretty.server.GrettyServer
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
//    logLevel: InternalLogLevel.DEBUG,

    webContexts: [
        "/" : [
            staticFiles: "./rootFiles",

            handler: { op -> op.html = """
<html>
    <body>
        <p>Hello, World!</p>
        <p>Unfortunately, I have no idea about page <i>${op.request.uri}</i>, which you've requested</p>
        <p>Try our <a href='/'>main page</a> please</p>
    </body>
</html>"""
            }
        ],

        "/websockets" : [
            staticFiles: "./webSocketsFiles",

            webSockets: [
               "/ws": [
                   onMessage: { msg ->
                       socket.send(msg.toUpperCase())
                   },

                   onConnect: {
                       socket.send("Welcome!")
                   }    
               ]
            ]
        ]
    ]
]
server.start()
