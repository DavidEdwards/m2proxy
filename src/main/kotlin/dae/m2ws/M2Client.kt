package dae.m2ws

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.apache.commons.net.telnet.*
import org.java_websocket.WebSocket

import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import kotlin.coroutines.CoroutineContext

class M2Client(private val ws: WebSocket, private val host: String, private val port: Int = 23) : CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private val tc = TelnetClient()

    fun connect() {
        val ttopt = TerminalTypeOptionHandler("VT100", false, false, true, false)
        val echoopt = EchoOptionHandler(true, false, true, false)
        val gaopt = SuppressGAOptionHandler(true, true, true, true)

        try {
            tc.addOptionHandler(ttopt)
            tc.addOptionHandler(echoopt)
            tc.addOptionHandler(gaopt)
        } catch (e: InvalidTelnetOptionException) {
            System.err.println("Error registering option handlers: " + e.message)
        }

        launch(Dispatchers.IO) {
            tc.connect(host, port)

//            tc.registerNotifHandler { negotiation_code, option_code ->
//                val command = when (negotiation_code) {
//                    TelnetNotificationHandler.RECEIVED_DO -> "DO"
//                    TelnetNotificationHandler.RECEIVED_DONT -> "DONT"
//                    TelnetNotificationHandler.RECEIVED_WILL -> "WILL"
//                    TelnetNotificationHandler.RECEIVED_WONT -> "WONT"
//                    TelnetNotificationHandler.RECEIVED_COMMAND -> "COMMAND"
//                    else -> Integer.toString(negotiation_code) // Should not happen
//                }
//                println("Received $command for option code $option_code")
//            }

            readM2()
        }
    }

    fun sendMessage(message: String) {
        val output = tc.outputStream
        val buff = message.toByteArray()
        val readBytes = buff.size

        try {
            output.write(buff, 0, readBytes)
            output.flush()
        } catch (e: IOException) {
            println("sendMessage error: $e")
        }
    }

    private fun readM2() = launch(Dispatchers.IO) {
        val input = tc.inputStream

        try {
            val buff = ByteArray(1024 * 20)
            var readBytes: Int

            do {
                readBytes = input.read(buff)
                if (readBytes > 0) {
                    val data = String(buff, 0, readBytes)
//                    println("Input length: " + data.length)
                    ws.send(data)
                }
            } while (readBytes >= 0)
        } catch (e: IOException) {
            System.err.println("Exception while reading socket:" + e.message)
        }

        stop()
    }

    fun stop() {
        ws.close(1000, "Stopped")
        try {
            tc.disconnect()
        } catch (e: IOException) {
            System.err.println("Exception while closing telnet: " + e.message)
        }
    }

}