$(function() {
  var ansi_up = new AnsiUp;

  var protocol = "ws";
  var host = "localhost";
  var port = 8000;

  var wsUri = protocol+"://"+host+":"+port+"/";
  var output = $("#output");
  var command = $("#command");
  writeToScreen("Attempting to connect to "+host+"...\n");
  var websocket = new WebSocket(wsUri);
  var state = 0;

  websocket.onopen = function(evt) {
    //writeToScreen("CONNECTED");
    command.focus();
    state = 1;
  };

  websocket.onclose = function(evt) {
    console.log("Close event", evt);
    if(state > 0) {
      writeToScreen("DISCONNECTED");
    } else {
      writeToScreen("Could not connect to "+host+". It might be offline.");
    }
    state = 0;
  };

  websocket.onmessage = function(evt) {
    writeToScreen(evt.data);
  };

  websocket.onerror = function(evt) {
    console.error("Error event", evt);
    if(evt.data) {
      writeToScreen(evt.data);
    }
  };

  function writeToScreen(message) {
    var html = ansi_up.ansi_to_html(message);
    html = html.replace(/[\n\r]+/g, "<br />");

    var lines = html.split("\n");
    for (var i = 0; i < lines.length; i++) {
      /*var span = document.createElement("span");
      span.innerHTML = lines[i];*/
      output.append(lines[i]);

      output[0].scrollTop = output[0].scrollHeight;
    }
  }

  command.keypress(function(e) {
    if (e.keyCode === 13) {
      var text = $(this).val();
      websocket.send(text);
      $(this).select();
    }
  });

  document.addEventListener("keydown", function(e) {
    if ((window.navigator.platform.match("Mac") ? e.metaKey : e.ctrlKey)) {
      if (e.keyCode === 187 || e.keyCode === 107) {
        e.preventDefault();
        increaseFontSizeBy1px();
      } else if (e.keyCode === 189 || e.keyCode === 109) {
        e.preventDefault();
        decreaseFontSizeBy1px();
      }
    }
  }, false);

  function increaseFontSizeBy1px() {
    var style = window.getComputedStyle(output[0], null).getPropertyValue('font-size');
    var currentSize = parseFloat(style);
    currentSize++;
    output[0].style.fontSize = currentSize+"px";
  }

  function decreaseFontSizeBy1px() {
    var style = window.getComputedStyle(output[0], null).getPropertyValue('font-size');
    var currentSize = parseFloat(style);
    currentSize--;
    output[0].style.fontSize = currentSize+"px";
  }

});