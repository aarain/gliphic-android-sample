/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.display.abstract_views;

import android.net.Network;

import gliphic.android.operation.misc.Log;
import gliphic.android.operation.server_interaction.http_server.HttpOperations;
import gliphic.android.operation.server_interaction.http_server.NetworkConnectionMonitor;

import androidx.fragment.app.Fragment;

/**
 * This fragment extends Fragment and should be the parent class of all other fragments when a contact is signed-in.
 *
 * This method allows child fragments to define behaviour for when the network connectivity status of the device
 * changes from unavailable to available. Any instance of this class MUST be attached to an activity (more
 * specifically a BaseMainActivity) since the NetworkConnectionMonitor constructor requires an activity to instantiate
 * the ConnectivityManager.
 */

public abstract class BaseMainFragment extends Fragment {

    private boolean isFirstOnNetworkAvailable = true;

    private NetworkConnectionMonitor networkConnectionMonitor = null;

    @Override
    public void onStart() {
        final BaseMainActivity baseMainActivity = (BaseMainActivity) getActivity();
        if (baseMainActivity == null) {
            return;
        }

        super.onStart();

        // Start a thread which runs once; if the onNetworkAvailable() method has not been called (yet) the
        // noNetworkOnStart() method is called, otherwise no action occurs.
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(HttpOperations.NORMAL_REQUEST_TIME_MAX);

                    if (isFirstOnNetworkAvailable) {
                        noNetworkOnStart();
                    }
                }
                catch (InterruptedException e) {
                    Log.e("BaseMainFragment", e.getMessage());
                }
            }
        };
        thread.start();

        // Register a network callback.
        networkConnectionMonitor = new NetworkConnectionMonitor(baseMainActivity) {
            @Override
            public void onAvailable(Network network) {
                final boolean startingState = isFirstOnNetworkAvailable;

                isFirstOnNetworkAvailable = false;

                onNetworkAvailable(startingState);
            }

        };
    }

    @Override
    public void onStop() {
        super.onStop();

        if (networkConnectionMonitor != null) {
            networkConnectionMonitor.unregister();
            networkConnectionMonitor = null;
        }

        isFirstOnNetworkAvailable = true;
    }

    /**
     * @see BaseMainActivity#onNetworkAvailable(boolean)
     */
    public abstract void onNetworkAvailable(boolean isFirstOnNetworkAvailable);

    /**
     * @see BaseMainActivity#noNetworkOnStart()
     */
    public abstract void noNetworkOnStart();
}
