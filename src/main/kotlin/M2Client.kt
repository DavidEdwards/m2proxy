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

class M2Client(private val ws: WebSocket, private val host: String, private val port: Int = 23): CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private val tc = TelnetClient()

    private var logOutput: FileOutputStream? = null

    fun connect() {
        try {
            logOutput = FileOutputStream("spy.log", true)
        } catch (e: IOException) {
            System.err.println(
                "Exception while opening the spy file: " + e.message
            )
        }

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

            tc.registerNotifHandler { negotiation_code, option_code ->
                val command = when (negotiation_code) {
                    TelnetNotificationHandler.RECEIVED_DO -> "DO"
                    TelnetNotificationHandler.RECEIVED_DONT -> "DONT"
                    TelnetNotificationHandler.RECEIVED_WILL -> "WILL"
                    TelnetNotificationHandler.RECEIVED_WONT -> "WONT"
                    TelnetNotificationHandler.RECEIVED_COMMAND -> "COMMAND"
                    else -> Integer.toString(negotiation_code) // Should not happen
                }
                println("Received $command for option code $option_code")
            }


            println("TelnetClientExample")
            println("Type AYT to send an AYT telnet command")
            println("Type OPT to print a report of status of options (0-24)")
            println("Type REGISTER to register a new SimpleOptionHandler")
            println("Type UNREGISTER to unregister an OptionHandler")
            println("Type SPY to register the spy (connect to port 3333 to spy)")
            println("Type UNSPY to stop spying the connection")
            println("Type ^[A-Z] to send the control character; use ^^ to send ^")

            readM2()
        }
    }

    fun sendMessage(message: String) {
        val output = tc.outputStream
        val buff = message.toByteArray()
        val readBytes = buff.size

        val line = String(buff, 0, readBytes) // deliberate use of default charset
        when {
            line.startsWith("AYT") -> try {
                println("Sending AYT")

                println("AYT response:" + tc.sendAYT(5000))
            } catch (e: IOException) {
                System.err.println("Exception waiting AYT response: " + e.message)
            }
            line.startsWith("OPT") -> {
                println("Status of options:")
                for (ii in 0..24) {
                    println(
                        "Local Option " + ii + ":" + tc.getLocalOptionState(ii) +
                                " Remote Option " + ii + ":" + tc.getRemoteOptionState(ii)
                    )
                }
            }
            line.startsWith("REGISTER") -> {
                val st = StringTokenizer(String(buff))
                try {
                    st.nextToken()
                    val opcode = Integer.parseInt(st.nextToken())
                    val initlocal = java.lang.Boolean.parseBoolean(st.nextToken())
                    val initremote = java.lang.Boolean.parseBoolean(st.nextToken())
                    val acceptlocal = java.lang.Boolean.parseBoolean(st.nextToken())
                    val acceptremote = java.lang.Boolean.parseBoolean(st.nextToken())
                    val opthand = SimpleOptionHandler(
                        opcode, initlocal, initremote,
                        acceptlocal, acceptremote
                    )
                    tc.addOptionHandler(opthand)
                } catch (e: Exception) {
                    if (e is InvalidTelnetOptionException) {
                        System.err.println("Error registering option: " + e.message)
                    } else {
                        System.err.println("Invalid REGISTER command.")
                        System.err.println("Use REGISTER optcode initlocal initremote acceptlocal acceptremote")
                        System.err.println("(optcode is an integer.)")
                        System.err.println("(initlocal, initremote, acceptlocal, acceptremote are boolean)")
                    }
                }

            }
            line.startsWith("UNREGISTER") -> {
                val st = StringTokenizer(String(buff))
                try {
                    st.nextToken()
                    val opcode = st.nextToken().toInt()
                    tc.deleteOptionHandler(opcode)
                } catch (e: Exception) {
                    if (e is InvalidTelnetOptionException) {
                        System.err.println("Error unregistering option: " + e.message)
                    } else {
                        System.err.println("Invalid UNREGISTER command.")
                        System.err.println("Use UNREGISTER optcode")
                        System.err.println("(optcode is an integer)")
                    }
                }

            }
            line.startsWith("SPY") -> tc.registerSpyStream(logOutput)
            line.startsWith("UNSPY") -> tc.stopSpyStream()
            line.matches("^\\^[A-Z^]\\r?\\n?$".toRegex()) -> {
                val toSend = buff[1]
                if (toSend == '^'.toByte()) {
                    output.write(toSend.toInt())
                } else {
                    output.write(toSend - 'A'.toByte() + 1)
                }
                output.flush()
            }
            else -> try {
                output.write(buff, 0, readBytes)
                output.flush()
            } catch (e: IOException) {
                println("sendMessage error: $e")
            }
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
                    print("Input length: "+data.length)
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
            System.err.println("Exception while closing telnet:" + e.message)
        }
    }

}