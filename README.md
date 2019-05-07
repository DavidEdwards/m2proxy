# Mud Proxy

This is a very simple project that combines a Kotlin based WebSocket server and Telnet client, and an HTML WebSocket client.

## Server

The server is written it Kotlin. It runs a WebSocket server on (by default) port 8000.

When a user connects to that, it creates a Telnet client (curtosy of Apache Commons). This client connects to the Mud server. The Telnet client and WebSocket server act as a proxy, relaying data between the real user web client and the Mud server.

A runnable JAR is provided in `/dist`.

## Client

The client is an extremely basic WebSocket client. It prints out data given to it from the WebSocket server. It has an ANSI colour interpretter (https://github.com/drudru/ansi_up).

## Usage

Run the server with (changing the command line parameters as necessary):
```bash
java -jar m2ws.jar -i mud.address.com -P mudPort23 -p websocketPort8000
```

Run it in the background and redirect log output as appropriate. This server should ideally run on the same server as the actual mud server itself. This will reduce lag between the user and the main Mud server.

The client JS file needs to be changed, such that the lines:

```js
var host = "localhost";
var port = 8000;
```

Point to the correct place. 