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

    default: { response.html = template("./templates/404.ftl", [user:"Dear Unknown User"]) },

    public: {
        get("googlib/:path") {
            redirect "http://ajax.googleapis.com/ajax/libs/" + it.path
        }
    },

    webContexts: [
        "/websockets" : [
            static: "./webSocketsFiles",

            default: {
                response.redirect("http://${request.getHeader('Host')}/websockets/")
            },

            public: {
                get("/:none") { args ->
                    if(!args.none.empty)
                        response.redirect("http://${request.getHeader('Host')}/websockets/")
                    else
                        response.responseBody = new File("./webSocketsFiles/ws.html")
                }

                websocket("/ws",[
                    onMessage: { msg ->
                        socket.send(msg.toUpperCase())
                    },

                    onConnect: {
                        socket.send("Welcome!")
                    }
                ])
            },
        ],

        "/life" : [
            default: { response.responseBody = new File("./lifeFiles/life.html") },

            public: {
                rest("/game/:user/:gameId") {
                    get{ args ->
                        def user = User.users[userId]
                        if(!user) {
                            response.json = [error:'No such user $userId']
                        }
                        else {
                            def gameId = args.gameId
                            if(!gameId) {
                                response.json = user.games
                            }
                            else {
                                def game = user.games[gameId]
                                if (!game) {
                                    response.json = [error:'No such game $userId/$gameId']
                                }
                                else {
                                    response.json = game
                                }
                            }
                        }
                    }

                    put{ args ->
                        def userId = args.user
                        def user = User.users[userId]
                        if(!user) {
                            response.json = [error:'No such user $userId']
                        }
                        else {
                            def game = user.games[args.gameId]
                            if(!game) {
                                response.json = [error:'No such game $userId/$gameId']
                            }
                            else {
                                game.fromJson(request.)
                            }
                        }
                    }

                    delete{ args ->

                    }

                    post{ args ->
                        
                    }
                }

                websocket("/life",[
                    onMessage: { msg ->
                      println msg
                    }
                ])
            }
        ]
    ]
]
server.start()

class User {
    static final Map<String,User> users = [:]

    String name, password, id

    Map<String,Game> games = []

    Game newGame (int width, int height) {
        Game game = [id: UUID.randomUUID(), width:width, height:height]
        games[game.id] = game
    }
}

class Game {
    String id

    int width, height

    List<Pair<Integer,Integer>> liveCells = []
}