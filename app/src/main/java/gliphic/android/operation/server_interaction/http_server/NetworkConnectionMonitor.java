/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.operation.server_interaction.http_server;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import androidx.annotation.Nullable;
import gliphic.android.display.abstract_views.BaseMainActivity;

import androidx.annotation.NonNull;

/**
 * Allow the application to receive broadcasts from the device to notify the device that it can communicate with the
 * server.
 */
public abstract class NetworkConnectionMonitor extends ConnectivityManager.NetworkCallback {

    // TODO: Add more NetworkCapabilities?
    private static final int[] NETWORK_CAPABILITIES = {
            NetworkCapabilities.NET_CAPABILITY_INTERNET,
            NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED,
            NetworkCapabilities.NET_CAPABILITY_VALIDATED
//            NetworkCapabilities.TRANSPORT_CELLULAR,
//            NetworkCapabilities.TRANSPORT_ETHERNET
//            NetworkCapabilities.TRANSPORT_VPN,
//            NetworkCapabilities.TRANSPORT_WIFI,
    };

    private static final int[] TRANSPORT_MECHANISMS = {
            NetworkCapabilities.TRANSPORT_CELLULAR,
            NetworkCapabilities.TRANSPORT_ETHERNET,
            NetworkCapabilities.TRANSPORT_VPN,
            NetworkCapabilities.TRANSPORT_WIFI
    };

    /**
     * Check if the device has internet connectivity.
     *
     * @param context   The calling context.
     * @return          True if internet connectivity is confirmed, false otherwise.
     */
    public static boolean isNetworkAvailable(@NonNull Context context) {
        final ConnectivityManager connectivityManager = getConnectivityManager(context);
        if (connectivityManager == null) {
            return false;
        }

        final Network network = connectivityManager.getActiveNetwork();
        if (network == null) {
            return false;
        }

        final NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);
        if (networkCapabilities == null) {
            return false;
        }

        for (int transport : TRANSPORT_MECHANISMS) {
            if (networkCapabilities.hasTransport(transport)) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    private static ConnectivityManager getConnectivityManager(@NonNull Context context) {
        return (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    private BaseMainActivity holdingActivity;

    /**
     * Initialise the ConnectivityManager and automatically register a NetworkCallback with an instance of this class.
     *
     * @param activity      The calling activity.
     */
    public NetworkConnectionMonitor(@NonNull BaseMainActivity activity) {
        holdingActivity = activity;

        NetworkRequest.Builder builder = new NetworkRequest.Builder();

        for (int networkCapability : NETWORK_CAPABILITIES) {
            builder = builder.addCapability(networkCapability);
        }

        final ConnectivityManager connectivityManager = getConnectivityManager(holdingActivity);

        if (connectivityManager != null) {
            connectivityManager.registerNetworkCallback(builder.build(), this);
        }
    }

    /**
     * Execute all necessary methods to unregister the NetworkCallback.
     */
    public void unregister() {
        final ConnectivityManager connectivityManager = getConnectivityManager(holdingActivity);

        if (connectivityManager != null) {
            connectivityManager.unregisterNetworkCallback(this);
        }
    }
}
