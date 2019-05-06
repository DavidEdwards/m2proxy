
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.lang.Exception
import java.net.InetAddress
import java.net.InetSocketAddress
import kotlin.coroutines.CoroutineContext


fun main(args: Array<String>) {
    Main().start(args.first())
}

class Main: CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private val WS_PORT = 8002
    private lateinit var webSocketServer: WebSocketServer

    fun start(host: String) {
        println("Start at host $host")

        webSocketServer = object: WebSocketServerKt(InetSocketAddress(WS_PORT)) {
            override fun onOpenKt(conn: WebSocket, handshake: ClientHandshake) {
                println("onOpen")

                val client = M2Client(conn, host)
                conn.setAttachment(client)
                client.connect()
            }

            override fun onCloseKt(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
                println("onClose: $code $reason $remote")
                conn.getAttachment<M2Client>()?.let { client ->
                    client.stop()
                }
            }

            override fun onMessageKt(conn: WebSocket, message: String) {
                println("onMessage: $message")
                conn.getAttachment<M2Client>()?.let { client ->
                    client.sendMessage(message+"\n")
                }
            }

            override fun onStartKt() {
                println("onStart")
            }

            override fun onErrorKt(conn: WebSocket, ex: Exception) {
                System.err.println("onError: $ex")
            }

        }
        webSocketServer.start()
    }
}