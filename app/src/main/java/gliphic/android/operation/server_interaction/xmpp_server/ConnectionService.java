/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.operation.server_interaction.xmpp_server;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;

import gliphic.android.operation.misc.Log;
import gliphic.android.operation.server_interaction.http_server.NetworkConnectionMonitor;
import gliphic.android.operation.server_interaction.http_server.RequestGlobalStatic;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * The service which handles starting, stopping and restarting a connection to the XMPP server.
 *
 * @see Connection
 */
public class ConnectionService extends Service {
    // Intent actions for monitoring an XMPP connection.
    public static final String ACTION_CONN_OPEN_DROPPED = "connectionWasOpenButDroppedByServer";
    public static final String ACTION_CONN_CLOSED       = "connectionClosed";
    public static final String ACTION_CONN_NO_NETWORK   = "connectionClosedNoInternetConnectivity";
    public static final String ACTION_CONN_ERR_CONFLICT = "connectionClosedOnError_conflict";
    public static final String ACTION_CONN_ERR_OTHER    = "connectionClosedOnError_other";

    private LocalBroadcastManager localBroadcastManager;
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_CONN_OPEN_DROPPED:
                    reconnect(ConnectionService.this);
                    break;
                case ACTION_CONN_NO_NETWORK:
                    if (NetworkConnectionMonitor.isNetworkAvailable(ConnectionService.this)) {
                        // Internet connectivity became available (again) after the XMPP connection dropped as a result
                        // of no internet connectivity.
                        break;
                    }
                case ACTION_CONN_CLOSED:
                case ACTION_CONN_ERR_CONFLICT:
                case ACTION_CONN_ERR_OTHER:
                    stopConnectionService(ConnectionService.this);
                    break;
                default:
                    break;
            }
        }
    };

    private static final String LOG_TAG = "Connection service";

    private boolean isStarted;              // Describes the state of the service.
    private HandlerThread handlerThread;    // A thread with a built-in Looper message queue keeping the thread alive.
    private Handler handler;                // Use this handler to post messages to the background thread.
    private Connection connection;

    private long contactNumber;
    private String accessToken;

    public static void startConnectionService(@NonNull Context context) {
        context.startService(new Intent(context, ConnectionService.class));
    }

    public static void stopConnectionService(@NonNull Context context) {
        context.stopService(new Intent(context, ConnectionService.class));
    }

    private void initConnection() {
        if (connection != null && connection.isConnected()) {
            return;
        }

        if (connection == null) {
            connection = new Connection(this);
        }

        try {
            connection.establishConnection(contactNumber, accessToken);
        }
        catch (InterruptedException | IOException | SmackException | XMPPException e) {
            Log.e(
                    LOG_TAG,
                    "Unable to establish an authenticated XMPP connection. " +
                            "Ensure the log-in credentials are correct and internet connectivity is available."
            );

            connection.tearDown();
            connection = null;

            stopSelf();     // Stop the service
        }
    }

    private void start() {
        Log.i(LOG_TAG,"Connection service started.");

        isStarted = true;

        if (handlerThread == null || !handlerThread.isAlive()) {
            handlerThread = new HandlerThread("ConnectionServiceHandlerThread");
            handlerThread.start();
            handler = new Handler(handlerThread.getLooper());
        }

        handler.post(this::initConnection);
    }

    private void stop() {
        Log.i(LOG_TAG, "Connection service stopped.");

        isStarted = false;

        if (handler != null) {
            handler.post(() -> {
                if (connection != null) {
                    connection.tearDown();
                    connection = null;
                }
            });
            handler = null;
        }

        if (handlerThread != null) {
            handlerThread.quitSafely();     // Note that the quit() method is not used.
            handlerThread = null;
        }
    }

    private void reconnect(@NonNull Context context) {
        Log.i(LOG_TAG, "Connection service reconnecting.");

        isStarted = false;

        if (handler != null) {
            handler.post(() -> {
                if (connection != null) {
                    connection.tearDown();
                    connection = null;
                    startConnectionService(context);
                }
            });
        }
        else {
            connection = null;
            startConnectionService(context);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * When the activity's startService() method is called this method is only called if no existing service already
     * exists.
     */
    @Override
    public void onCreate() {
        super.onCreate();
    }

    /**
     * This method is called every time the activity's startService() method is called.
     *
     * This method will return without further execution if the thread is already active.
     * @see #start()
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // If the service is killed by Android in order to save memory, ensure that it is restarted as soon as memory
        // becomes available.
        final int serviceStartMode = Service.START_REDELIVER_INTENT;

        if (isStarted) {
            return serviceStartMode;
        }

        // Register the broadcast receiver.

        if (localBroadcastManager == null) {
            IntentFilter intentFilter = new IntentFilter();

            // All of these actions should be handled by the BroadcastReceiver.
            intentFilter.addAction(ACTION_CONN_OPEN_DROPPED);
            intentFilter.addAction(ACTION_CONN_CLOSED);
            intentFilter.addAction(ACTION_CONN_NO_NETWORK);
            intentFilter.addAction(ACTION_CONN_ERR_CONFLICT);
            intentFilter.addAction(ACTION_CONN_ERR_OTHER);

            localBroadcastManager = LocalBroadcastManager.getInstance(this);
            localBroadcastManager.registerReceiver(broadcastReceiver, intentFilter);
        }

        // Obtain the current contact (number) and access token via a HTTPS request.
        RequestGlobalStatic.requestAndSetCurrentContact(
                (currentContact, accessToken) -> {
                    if (currentContact == null || accessToken == null) {
                        Log.w(LOG_TAG,
                                "Connection service not started; unable to obtain the contact's username " +
                                        "and password. This may be caused by unreliable internet connectivity."
                        );
                        return;
                    }

                    this.contactNumber = currentContact.getNumber();
                    this.accessToken = accessToken;

                    // Only start the connection service when the contact (number) and access token have been
                    // obtained.
                    start();
                },
                ConnectionService.this
        );

        return serviceStartMode;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (localBroadcastManager != null && broadcastReceiver != null) {
            localBroadcastManager.unregisterReceiver(broadcastReceiver);
            localBroadcastManager = null;
        }

        stop();
    }
}
