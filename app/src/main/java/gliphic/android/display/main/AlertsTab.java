/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.display.main;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

import gliphic.android.R;
import gliphic.android.adapters.AlertsAdapter;
import gliphic.android.adapters.GroupShareItemCounts;
import gliphic.android.display.abstract_views.BaseMainActivity;
import gliphic.android.display.abstract_views.BaseMainFragment;
import gliphic.android.display.libraries.RecyclerViewSetup;
import gliphic.android.display.libraries.Toasts;
import gliphic.android.exceptions.NullStaticVariableException;
import gliphic.android.listeners.NestedScrollViewListener;
import gliphic.android.operation.Alerts;
import gliphic.android.operation.server_interaction.http_server.RequestGlobalStatic;

import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;
import pojo.account.GroupShare;

/**
 * The 'Alerts' tab fragment.
 *
 * This shows actions which they have sent to other contacts and actions which they have received.
 */
public class AlertsTab extends BaseMainFragment {
    private View rootView;
    private AlertsAdapter allAlertsAdapter;
    private CheckBox pendingReceivedOnlyBox;
    private RecyclerView recyclerView;

    void removeGroupShareAndUpdateDisplay(@NonNull List<GroupShare> groupShareList) {
        if (allAlertsAdapter == null) {     // AlertsAdapter == null ==> RecyclerView == null.
            safeAdapterReset();
        }
        else {
            allAlertsAdapter.removeGroupShareAndUpdateDisplay(groupShareList);
            showAndRemoveViews();
        }
    }

    void prependGroupShareAndUpdateDisplay(@NonNull List<GroupShare> groupShareList) {
        if (allAlertsAdapter == null) {     // AlertsAdapter == null ==> RecyclerView == null.
            safeAdapterReset();
        }
        else {
            allAlertsAdapter.removeGroupShareAndUpdateDisplay(groupShareList);
            allAlertsAdapter.prepend(groupShareList);
            showAndRemoveViews();
        }
    }

    @Override
    public void onNetworkAvailable(boolean isFirstOnNetworkAvailable) {
        new Handler(Looper.getMainLooper()).post(
                isFirstOnNetworkAvailable ? this::safeAdapterReload : this::safeAdapterReset
        );
    }

    @Override
    public void noNetworkOnStart() {
        new Handler(Looper.getMainLooper()).post(
                this::showAndRemoveViews
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.main_tab_alerts, container, false);

        if (getActivity() == null) {
            return rootView;
        }

        pendingReceivedOnlyBox = rootView.findViewById(R.id.checkbox_alerts);

        final NestedScrollView nestedScrollView = rootView.findViewById(R.id.nestedscrollview_main_alerts);
        final TextView         loadMoreAlerts   = rootView.findViewById(R.id.textview_load_more_alerts);

        ((MainActivity) getActivity()).addToAllClickableViews(Arrays.asList(
                pendingReceivedOnlyBox,
                nestedScrollView,
                loadMoreAlerts
        ));

        // Checkbox setup
        pendingReceivedOnlyBox.setOnClickListener(v -> {
            if (allAlertsAdapter != null && allAlertsAdapter.update(pendingReceivedOnlyBox.isChecked())) {
                safeAdapterFilter();
            }
        });

        // Get more contacts from the server if required.

        nestedScrollView.setOnScrollChangeListener(new NestedScrollViewListener() {
            @Override
            public void onScrollItemVisible() {
                checkForAppend();
            }
        });

        loadMoreAlerts.setOnClickListener(v -> checkForAppend());

        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        allAlertsAdapter = null;
    }

    private void checkForAppend() {
        if (allAlertsAdapter != null && allAlertsAdapter.append()) {
            safeAdapterAppend();
        }
    }

    private void safeAdapterAppend() {
        safeAdapterUpdate(true, false, false);
    }

    private void safeAdapterFilter() {
        safeAdapterUpdate(true, false, true);
    }

    private void safeAdapterReset() {
        safeAdapterUpdate(false, true, true);
    }

    private void safeAdapterReload() {
        safeAdapterUpdate(true, true, true);
    }

    private void safeAdapterUpdate(final boolean forceServerRequest,
                                   final boolean resetAdapterLists,
                                   final boolean resetDisplayedItemCount) {

        final BaseMainActivity baseMainActivity = (BaseMainActivity) getActivity();
        if (baseMainActivity == null) {
            return;
        }

        final ProgressBar moreAlertsProgressBar = rootView.findViewById(R.id.more_alerts_progress_bar);
        final TextView    loadMoreAlerts        = rootView.findViewById(R.id.textview_load_more_alerts);

        moreAlertsProgressBar.setVisibility(View.VISIBLE);
        loadMoreAlerts.setVisibility(View.GONE);

        // Get the current item counts required to send a request to the server, using any existing adapter.

        // All of the default values are zero.
        GroupShareItemCounts groupShareItemCounts = new GroupShareItemCounts();
        if (allAlertsAdapter != null) {
            groupShareItemCounts = allAlertsAdapter.verifyPendingAndCompletedShares();
        }
        long pendingReceivedItemCount   = groupShareItemCounts.getPendingReceivedItemCount();
        long pendingSentItemCount       = groupShareItemCounts.getPendingSentItemCount();
        long completedReceivedItemCount = groupShareItemCounts.getSuccessReceivedItemCount();
        long completedSentItemCount     = groupShareItemCounts.getSuccessAndFailedSentItemCount();

        // Allow users to modify the RecyclerView and related filter options whilst waiting for an asynchronous server
        // response, providing a more seamless user experience when selecting and filtering RecyclerView items.
        // This is done by not deactivating clickable views here and (re)activating them when the response is received.

        RequestGlobalStatic.requestGroupShareAlerts(
                (groupShareList, groupShareRequestInProgress) -> {
                    final Context context = getContext();
                    if (context == null) {
                        return;
                    }

                    if (groupShareRequestInProgress) {
                        handleRequestCleanup(moreAlertsProgressBar);
                        return;
                    }

                    // Initialise an adapter if one does not already exist.
                    if (allAlertsAdapter == null) {
                        allAlertsAdapter = new AlertsAdapter(groupShareList, pendingReceivedOnlyBox.isChecked());
                    }
                    else {
                        // Update the adapter with the up-to-date list of group-shares.
                        allAlertsAdapter.modify(resetAdapterLists, resetDisplayedItemCount, groupShareList);
                    }

                    if (groupShareList == null) {
                        Toasts.showCannotLoadAlertsToast(context);
                    }

                    if (recyclerView == null) {
                        recyclerView = RecyclerViewSetup.setupStandardRecyclerView(
                                rootView.findViewById(R.id.recyclerview_main_tab_alerts),
                                context,
                                allAlertsAdapter
                        );
                    }

                    // Update the Alerts tab title and views (if required).
                    // This also handles showing and removing views depending on the number of alerts in the adapter.
                    MainActivity.sendBroadcastToUpdateAlertsTab(baseMainActivity);

                    handleRequestCleanup(moreAlertsProgressBar);
                },
                baseMainActivity,
                forceServerRequest,
                pendingReceivedItemCount,
                pendingReceivedOnlyBox.isChecked() ? null : pendingSentItemCount,
                pendingReceivedOnlyBox.isChecked() ? null : completedReceivedItemCount,
                pendingReceivedOnlyBox.isChecked() ? null : completedSentItemCount
        );
    }

    void showAndRemoveViews() {
        final View     itemDivider    = rootView.findViewById(R.id.itemdivider_main_tab_alerts);
        final TextView noAlerts       = rootView.findViewById(R.id.textview_no_alerts);
        final TextView errorAlert     = rootView.findViewById(R.id.alerts_tab_error_item);
        final TextView loadMoreAlerts = rootView.findViewById(R.id.textview_load_more_alerts);

        if (allAlertsAdapter != null && allAlertsAdapter.getItemCount() > 0) {
            pendingReceivedOnlyBox.setVisibility(View.VISIBLE);
            itemDivider.setVisibility(View.VISIBLE);
            noAlerts.setVisibility(View.GONE);
            errorAlert.setVisibility(View.GONE);
            loadMoreAlerts.setVisibility(View.VISIBLE);
        }
        else {
            boolean showErrorViews;

            try {
                // Show and hide views depending on whether there are any alerts loaded.
                Alerts.getGroupShares();

                showErrorViews = false;
            }
            catch (NullStaticVariableException e) {
                showErrorViews = true;
            }

            if (showErrorViews) {
                pendingReceivedOnlyBox.setVisibility(View.GONE);
                itemDivider.setVisibility(View.GONE);
                noAlerts.setVisibility(View.GONE);
                errorAlert.setVisibility(View.VISIBLE);
                loadMoreAlerts.setVisibility(View.GONE);
            }
            else {
                pendingReceivedOnlyBox.setVisibility(View.VISIBLE);
                itemDivider.setVisibility(View.VISIBLE);
                noAlerts.setVisibility(View.VISIBLE);
                errorAlert.setVisibility(View.GONE);
                loadMoreAlerts.setVisibility(View.VISIBLE);
            }
        }
    }

    private void handleRequestCleanup(@NonNull final ProgressBar moreAlertsProgressBar) {
        moreAlertsProgressBar.setVisibility(View.GONE);
    }
}
