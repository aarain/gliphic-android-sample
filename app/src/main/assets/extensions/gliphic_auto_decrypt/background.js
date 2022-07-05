"use strict";

const NATIVE_APP = "gliphic_auto_decrypt";  // TODO: Delete this constant and reference it from the application.

// Establish a connection with app.
const portToApp = browser.runtime.connectNative(NATIVE_APP);

portToApp.onMessage.addListener(function(response) {
    portFromCS.postMessage(response);
});

// Receive a connection from the content script.
let portFromCS;

function connected(port) {
    portFromCS = port;

    portFromCS.onMessage.addListener(function(jo) {
        portToApp.postMessage(jo);
    });
}

browser.runtime.onConnect.addListener(connected);