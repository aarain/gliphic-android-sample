/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.display.abstract_views;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Network;
import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import gliphic.android.R;
import gliphic.android.display.browser.BrowserActivity;
import gliphic.android.display.libraries.AlertDialogs;
import gliphic.android.display.main.MainActivity;
import gliphic.android.operation.misc.Log;
import gliphic.android.operation.server_interaction.http_server.HttpOperations;
import gliphic.android.operation.server_interaction.http_server.NetworkConnectionMonitor;
import gliphic.android.operation.server_interaction.http_server.RequestGlobalStatic;
import gliphic.android.operation.server_interaction.xmpp_server.ConnectionService;
import gliphic.android.operation.storage_handlers.ForcedDialogs;

/**
 * This activity is a child of AppCompatActivity and should be the parent class of all other activities which are
 * started after a contact signs in.
 *
 * This activity has the ability to register, unregister and receive LocalBroadcasts, for example when an XMPP
 * connection has been terminated.
 */
public abstract class BaseMainActivity extends BaseActivity {

    /**
     * Clear any stored browsing history.
     */
    public void clearBrowserState() {
        BrowserActivity.Companion.clearStorage(getApplicationContext());
    }

    private boolean isOnFirstNetworkAvailable = true;

    private NetworkConnectionMonitor networkConnectionMonitor = null;

    private LocalBroadcastManager localBroadcastManager;
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String intentAction = intent.getAction();

            final ForcedDialogs.ForcedSignOutAction forcedSignOutAction;

            switch (intentAction) {
                case ConnectionService.ACTION_CONN_ERR_CONFLICT:
                    forcedSignOutAction = ForcedDialogs.ForcedSignOutAction.CONNECTION_CLOSED_CONFLICT;
                    break;
                case ConnectionService.ACTION_CONN_ERR_OTHER:
                    forcedSignOutAction = ForcedDialogs.ForcedSignOutAction.CONNECTION_CLOSED_OTHER;
                    break;
                default:
                    return;
            }

            // This method handles stopping the connection service.
            AlertDialogs.signOutDialog(BaseMainActivity.this, forcedSignOutAction, true);
        }
    };

    private void setFloatingActionButtonOnClickListener(final FloatingActionButton floatingActionButton,
                                                        @NonNull final Class<?> activityStarted) {

        if (floatingActionButton != null) {
            floatingActionButton.setOnClickListener(view -> {
                Intent intent = new Intent(this, activityStarted);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
            });
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        setFloatingActionButtonOnClickListener(findViewById(R.id.fab_to_webView), BrowserActivity.class);
        setFloatingActionButtonOnClickListener(findViewById(R.id.fab_to_editView), MainActivity.class);

        // Start a thread which runs once; if the onNetworkAvailable() method has not been called (yet) the
        // noNetworkOnStart() method is called, otherwise no action occurs.
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(HttpOperations.NORMAL_REQUEST_TIME_MAX);

                    if (isOnFirstNetworkAvailable) {
                        noNetworkOnStart();
                    }
                }
                catch (InterruptedException e) {
                    Log.e("BaseMainActivity", e.getMessage());
                }
            }
        };
        thread.start();

        /* Register the broadcast receiver */

        if (localBroadcastManager == null) {
            localBroadcastManager = LocalBroadcastManager.getInstance(this);
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectionService.ACTION_CONN_ERR_CONFLICT);
        intentFilter.addAction(ConnectionService.ACTION_CONN_ERR_OTHER);

        localBroadcastManager.registerReceiver(broadcastReceiver, intentFilter);

        /* Register the network connection monitor */

        networkConnectionMonitor = new NetworkConnectionMonitor(BaseMainActivity.this) {
            @Override
            public void onAvailable(Network network) {
                final boolean startingState = isOnFirstNetworkAvailable;

                isOnFirstNetworkAvailable = false;

                // This method also handles starting the connection service (since the contact must be signed in).
                // Note that this method does not block the main thread, so execution of this method continues before a
                // server response is received.
                RequestGlobalStatic.requestSignInTimeAndForceSignOutOnMismatch(BaseMainActivity.this);

                onNetworkAvailable(startingState);
            }
        };
    }

    @Override
    public void onStop() {
        /* Unregister the network connection monitor */

        if (networkConnectionMonitor != null) {
            networkConnectionMonitor.unregister();
            networkConnectionMonitor = null;
        }

        /* Unregister the broadcast receiver */

        if (localBroadcastManager != null && broadcastReceiver != null) {
            localBroadcastManager.unregisterReceiver(broadcastReceiver);
        }

        isOnFirstNetworkAvailable = true;

        super.onStop();
    }

//    /**
//     * This method exists as a fix for the following error:
//     *
//     *     "E/ConnectivityManager.CallbackHandler: callback not found for RELEASED message"
//     *
//     * When a source activity starts a destination activity, the source activity's onStop method is NOT called before
//     * the destination activity's onStart method. This causes a conflict when multiple NetworkCallbacks are
//     * registered at the same time.
//     *
//     * @param destination
//     * @param <T>
//     */
//    public <T extends BaseMainActivity> void startActivityFromBaseMainActivity(Class<T> destination) {
//        unregisterNetworkConnectionMonitor();
//
//        Intent intent = new Intent(this, destination);
//        this.startActivity(intent);
//    }

    /**
     * This method is triggered whenever the network connectivity status of the device changes from unavailable to
     * available.
     *
     * If the overridden method has a chance of calling an adapter's onBindViewHolder() method, it should use a Handler
     * to update the RecyclerView UI in order to prevent an IllegalStateException from being raised when the adapter's
     * onBindViewHolder() method is called from multiple sources. For example:
     *
     * new Handler(Looper.getMainLooper()).post(
     *     // do something
     * );
     */
    public abstract void onNetworkAvailable(boolean isFirstOnNetworkAvailable);

    /**
     * This method is triggered if no network is available for the first X seconds after the onStart() method is
     * triggered, where X the value satisfying the Thread.sleep(X) method called above.
     *
     * If the overridden method has a chance of calling a view's setVisibility() method, it should use a Handler
     * to update the RecyclerView UI in order to prevent the following exception from being raised:
     * * android.view.ViewRootImpl$CalledFromWrongThreadException:
     * * Only the original thread that created a view hierarchy can touch its views.
     */
    public abstract void noNetworkOnStart();
}
