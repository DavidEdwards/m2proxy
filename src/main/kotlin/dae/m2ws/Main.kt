package dae.m2ws
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.lang.Exception
import java.net.InetSocketAddress
import kotlin.coroutines.CoroutineContext

fun main(args: Array<String>) = Main().main(args)

class Main: CliktCommand(), CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    val host: String by option(help="The host / IP of the Mud server").default("mudii.co.uk")
    val port: Int by option(help="The port we should listen at for WebSocket connections").int().default(8000)
//    val name: String by option(help="The person to greet").prompt("Your name")

    private lateinit var webSocketServer: WebSocketServer

    override fun run() {
        println("Start at host $host")

        webSocketServer = object: WebSocketServerKt(InetSocketAddress(port)) {
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