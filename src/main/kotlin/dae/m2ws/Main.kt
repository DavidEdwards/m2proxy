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

class Main : CliktCommand(), CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    val host: String by option("-i", "--ip", help = "The host / IP of the Mud server").default("mudii.co.uk")
    val port: Int by option(
        "-p",
        "--port",
        help = "The port we should listen at for WebSocket connections"
    ).int().default(8000)
    val mudport: Int by option("-P", help = "The port we should connect to for Mud").int().default(23)
//    val name: String by option(help="The person to greet").prompt("Your name")

    private lateinit var webSocketServer: WebSocketServer

    override fun run() {
        println("Listen at $port")
        println("Connect to $host:$mudport")

        webSocketServer = object : WebSocketServerKt(InetSocketAddress(port)) {
            override fun onOpenKt(conn: WebSocket, handshake: ClientHandshake) {
                println("onOpen ${conn.remoteSocketAddress.address.hostAddress}")

                val client = M2Client(conn, host, mudport)
                conn.setAttachment(client)
                client.connect()
            }

            override fun onCloseKt(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
                println("onClose: code=$code reason=$reason remote=$remote ip=${conn.remoteSocketAddress.address.hostAddress}")
                conn.getAttachment<M2Client>()?.let { client ->
                    client.stop()
                }
            }

            override fun onMessageKt(conn: WebSocket, message: String) {
//                println("onMessage: $message")
                conn.getAttachment<M2Client>()?.let { client ->
                    client.sendMessage(message + "\n")
                }
            }

            override fun onStartKt() {
                println("onStart")
            }

            override fun onErrorKt(conn: WebSocket, ex: Exception) {
                System.err.println("onError: $ex ip=${conn.remoteSocketAddress.address.hostAddress}")
            }

        }
        webSocketServer.start()
    }
}