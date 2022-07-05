/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.operation.server_interaction.xmpp_server;

import android.content.Context;
import android.content.Intent;

import gliphic.android.operation.misc.BuildConfiguration;
import gliphic.android.operation.misc.IntentHandler;
import gliphic.android.operation.misc.Log;
import gliphic.android.operation.server_interaction.http_server.HttpOperations;

import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.ping.PingFailedListener;
import org.jivesoftware.smackx.ping.PingManager;
import org.jxmpp.jid.EntityBareJid;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import certificate_pinning.ExtendedSSLSocketFactory;
import certificate_pinning.KeyAndTrustStoresHandler;
import certificate_pinning.X509XmppCertificate;
import libraries.GeneralUtils;
import libraries.Vars;
import pojo.xmpp.XMPPMessageBody;
import xmpp.ExtendedXMPPTCPConnection;
import xmpp.PingFailedException;

/**
 * An XMPP connection instance used by the {@link ConnectionService} which handles establishing a connection to the
 * XMPP server, listening for changes in the connection state and listening for incoming XMPP messages.
 */
public class Connection implements ConnectionListener, PingFailedListener {

    // The connection state is (currently) only used for testing purposes.
    public enum ConnectionState {CONNECTED, AUTHENTICATED, DISCONNECTED}
    public static ConnectionState connectionState;

    private static final String LOG_TAG = "Connection handler";
    private static final String HOST_NAME = BuildConfiguration.isDebugBuild() ? "192.168.1.85" : Vars.DOMAIN_NAME;

    private final Context serviceContext;

    private ExtendedXMPPTCPConnection xmpptcpConnection;
    private PingManager pingManager;
    private ChatManager chatManager;
    private IncomingChatMessageListener incomingChatMessageListener = new IncomingChatMessageListener() {
        @Override
        public void newIncomingMessage(EntityBareJid from, Message message, Chat chat) {
            if (!from.equals(String.format("%s%s", Vars.HTTPS_SERVER_NAME, Vars.JID_AT_AND_DOMAIN))) {
                String s = "Incoming XMPP message not from the HTTPS server. Sender: %s";
                Log.w(HttpOperations.INCOMING_XMPP_MSG_LOG_TAG, String.format(s, from.toString()));
                return;
            }

            try {
                if (message == null || message.getBody() == null) {
                    throw new InvalidParameterException("Null message or message body.");
                }

                final XMPPMessageBody xmppMessageBody = GeneralUtils.fromJson(
                        message.getBody(),
                        XMPPMessageBody.class
                );

                if (xmppMessageBody == null || xmppMessageBody.getSubject() == null) {
                    throw new InvalidParameterException("Null XMPP message body or XMPP message body subject.");
                }

                // The message has been verified as correctly formed, so broadcast an intent to handle the message.

                final Intent intent = new Intent(xmppMessageBody.getSubject().get());

                IntentHandler.putAsIntentExtra(intent, xmppMessageBody);

                LocalBroadcastManager.getInstance(serviceContext).sendBroadcast(intent);
            }
            catch (InvalidParameterException e) {
                // This should never occur since the HTTPS server should not be sending malformed messages.
                String s = "Malformed XMPP message from the HTTPS server: %s";
                Log.e(HttpOperations.INCOMING_XMPP_MSG_LOG_TAG, String.format(s, e.getMessage()));
            }
        }
    };

    /**
     * @return  True iff the XMPP TCP connection is connected (but not necessarily authenticated).
     *
     * Assume that if the XMPP TCP connection is connected that it will be able to authenticate (an exception is thrown
     * if this is not possible). Using the isConnected() method instead of the isAuthenticated() method also ensures
     * that another authenticated connection is not established, overwriting and disconnecting the existing connection,
     * since an existing connection could be in-between states i.e. isConnected() == true, isAuthenticated() == false.
     */
    boolean isConnected() {
        return xmpptcpConnection != null && xmpptcpConnection.isConnected();
    }

    Connection(@NonNull ConnectionService service) {
        serviceContext = service;
    }

    /**
     * Check that the SSL/TLS configuration is valid, log in to the XMPP server, set up a listener for incoming XMPP
     * messages, and register a broadcast receiver to send an XMPP message whenever a valid intent is received.
     *
     * @param contactNumber             The current contact's number.
     * @param accessToken               The current contact's (valid) access token
     * @throws IOException              Thrown when attempting to connect and log-in, if an SSLException is thrown or
     *                                  when getting the XMPPTCPConnectionConfiguration.
     * @throws InterruptedException     Thrown when attempting to connect and log-in.
     * @throws XMPPException            Thrown when attempting to connect and log-in.
     * @throws SmackException           Thrown when attempting to connect and log-in.
     */
    void establishConnection(long contactNumber, @NonNull String accessToken)
            throws IOException, InterruptedException, XMPPException, SmackException {

        Log.i(LOG_TAG, String.format("Establishing connection to %s ...", HOST_NAME));

        // Ensure that the global SSL/TLS configuration is up-to-date, e.g. if the certificate has updated.
        final SSLContext sslContext;
        try {
            sslContext = new ExtendedSSLSocketFactory(
                    KeyAndTrustStoresHandler.initKeyAndTrustStores(Vars.ANDROID_KEY_STORE, new X509XmppCertificate())
            ).getSSLContext();
        }
        catch ( KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException |
                KeyManagementException e) {

            String s = "Refused to establish an XMPP connection because the SSL context setup failed with message: %s";
            throw new SSLException(String.format(s, e.getMessage()));
        }

        xmpptcpConnection = new ExtendedXMPPTCPConnection(ExtendedXMPPTCPConnection.getConfig(
                Long.toString(contactNumber),
                accessToken,
                HOST_NAME,
                sslContext
        ));
        xmpptcpConnection.addConnectionListener(this);
        xmpptcpConnection.connect();
        xmpptcpConnection.login();

        // Ping manager setup.

        final int pingInterval = 300;
        PingManager.setDefaultPingInterval(pingInterval);
        pingManager = PingManager.getInstanceFor(xmpptcpConnection);
        pingManager.setPingInterval(pingInterval);
        pingManager.registerPingFailedListener(this);

        // Message listener setup.

        chatManager = ChatManager.getInstanceFor(xmpptcpConnection);
        chatManager.addIncomingListener(incomingChatMessageListener);

        connectionState = ConnectionState.AUTHENTICATED;

        Log.i(LOG_TAG, "XMPP connection established.");
    }

    /**
     * Disconnect the connection and then call the overridden method:
     * @see #connectionClosed()
     */
    void tearDown() {
        tearDown(true, null);
    }

    private void tearDown(boolean disconnect, @Nullable Exception disconnectionException) {
        // Ping manager teardown.
        if (pingManager != null) {
            pingManager.unregisterPingFailedListener(this);
            pingManager = null;
        }

        // Chat manager teardown.
        if (chatManager != null) {
            chatManager.removeIncomingListener(incomingChatMessageListener);
            chatManager = null;
        }

        // XMPP TCP connection teardown.
        if (xmpptcpConnection != null) {
            // Only disconnect the XMPP connection if it has not already been disconnected, otherwise an infinite loop
            // between calls to this method and the connectionClosed() method will occur.
            if (disconnect && xmpptcpConnection.isConnected()) {
                Log.i(LOG_TAG, "Disconnecting from " + HOST_NAME);

                // This method also sets the connection variable to null via a call to one of the connection-closed
                // listeners, so do not explicitly set it here.
                xmpptcpConnection.disconnect(this, disconnectionException);
            }
            else {
                xmpptcpConnection = null;
            }
        }
    }

    @Override
    public void connected(XMPPConnection connection) {
        Log.i(LOG_TAG, "Connection successful.");
    }

    @Override
    public void authenticated(XMPPConnection connection, boolean resumed) {
        Log.i(LOG_TAG, "Authentication successful.");
    }

    @Override
    public void connectionClosed() {
        connectionState = ConnectionState.DISCONNECTED;

        Log.i(LOG_TAG, "Connection closed.");

        tearDown(false, null);  // The connection has already been disconnected.

        LocalBroadcastManager.getInstance(serviceContext)
                .sendBroadcast(new Intent(ConnectionService.ACTION_CONN_CLOSED));
    }

    @Override
    public void connectionClosedOnError(Exception e) {
        connectionState = ConnectionState.DISCONNECTED;

        if (e instanceof PingFailedException) {
            Log.i(LOG_TAG, "Connection closed after failed ping request.");

            reconnect();
        }
        else if (e instanceof SmackException && e.getMessage() != null && e.getMessage().contains("END_DOCUMENT")) {
            // This occurs after the server restarts (the server may have closed the connection without sending a
            // closing stream element).
            Log.i(LOG_TAG, "Connection closed after XMPP server restart: " + e.getMessage());

            reconnect();
        }
        else if (e instanceof SSLException) {
            Log.i(LOG_TAG, "User lost internet connectivity. SSLException message: " + e.getMessage());

            tearDown(false, null);  // The connection has already been disconnected.

            LocalBroadcastManager.getInstance(serviceContext)
                    .sendBroadcast(new Intent(ConnectionService.ACTION_CONN_NO_NETWORK));
        }
        else {
            Log.w(LOG_TAG, "Unexpected connection closed: " + e.getMessage());

            if (e.getMessage() != null && e.getMessage().contains("system-shutdown")) {
                // Do nothing since the connection should reestablish itself when the server comes online again.
                return;
            }

            tearDown(false, null);  // The connection has already been disconnected.

            if (e.getMessage() != null && e.getMessage().contains("conflict")) {
                LocalBroadcastManager.getInstance(serviceContext)
                        .sendBroadcast(new Intent(ConnectionService.ACTION_CONN_ERR_CONFLICT));
            }
            else {
                LocalBroadcastManager.getInstance(serviceContext)
                        .sendBroadcast(new Intent(ConnectionService.ACTION_CONN_ERR_OTHER));
            }
        }
    }

    private void reconnect() {
        xmpptcpConnection = null;

        LocalBroadcastManager.getInstance(serviceContext)
                .sendBroadcast(new Intent(ConnectionService.ACTION_CONN_OPEN_DROPPED));
    }

    @Override
    public void pingFailed() {
        Log.w(
                LOG_TAG,
                "Ping request to the XMPP server failed; assume that the cause is one of the following: " +
                        "1 - The client has an authenticated XMPP TCP connection but the server has dropped it, " +
                        "2 - The client does not have internet connectivity, " +
                        "3 - The server is unavailable to all clients."
        );

        tearDown(true, new PingFailedException());
    }
}
