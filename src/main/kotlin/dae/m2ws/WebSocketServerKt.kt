package dae.m2ws

import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.lang.Exception
import java.net.InetSocketAddress

abstract class WebSocketServerKt(addr: InetSocketAddress): WebSocketServer(addr) {

    abstract fun onOpenKt(conn: WebSocket, handshake: ClientHandshake)
    abstract fun onCloseKt(conn: WebSocket, code: Int, reason: String, remote: Boolean)
    abstract fun onMessageKt(conn: WebSocket, message: String)
    abstract fun onStartKt()
    abstract fun onErrorKt(conn: WebSocket, ex: Exception)

    override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
        if(conn != null && handshake != null) {
            onOpenKt(conn, handshake)
        } else {
            println("onOpen null $conn $handshake")
        }
    }

    override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
        if(conn != null && reason != null) {
            onCloseKt(conn, code, reason, remote)
        } else {
            println("onClose null $conn $code $reason $remote")
        }
    }

    override fun onMessage(conn: WebSocket?, message: String?) {
        if(conn != null && message != null) {
            onMessageKt(conn, message)
        } else {
            println("onMessage null $conn $message")
        }
    }

    override fun onStart() {
        onStartKt()
    }

    override fun onError(conn: WebSocket?, ex: Exception?) {
        if(conn != null && ex != null) {
            onErrorKt(conn, ex)
        } else {
            println("onError null $conn $ex")
        }
    }
}