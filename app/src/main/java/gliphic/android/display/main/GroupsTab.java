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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Arrays;

import gliphic.android.adapters.GroupsAdapter;
import gliphic.android.display.libraries.EditTextImeOptions;
import gliphic.android.display.libraries.Toasts;
import gliphic.android.display.main.group_details.GroupDetailsActivity;
import gliphic.android.R;
import gliphic.android.display.libraries.RecyclerViewSetup;
import gliphic.android.display.abstract_views.BaseMainActivity;
import gliphic.android.display.abstract_views.BaseMainFragment;
import gliphic.android.listeners.NestedScrollViewListener;
import gliphic.android.operation.server_interaction.http_server.RequestGlobalStatic;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;
import libraries.GeneralUtils;

/**
 * The 'Groups' tab fragment.
 *
 * This lists all groups available to the user allowing further operations to be done on/with them.
 */
public class GroupsTab extends BaseMainFragment {
    private View rootView;
    private GroupsAdapter allGroupsAdapter;
    private RecyclerView recyclerView;

    @Override
    public void onNetworkAvailable(boolean isFirstOnNetworkAvailable) {
        new Handler(Looper.getMainLooper()).post(
                this::safeAdapterReset
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
        rootView = inflater.inflate(R.layout.main_tab_groups, container, false);

        final BaseMainActivity baseMainActivity = (BaseMainActivity) getActivity();
        if (baseMainActivity == null) {
            return rootView;
        }

        final EditText         groupSearch      = rootView.findViewById(R.id.edittext_main_tab_groups);
        final NestedScrollView nestedScrollView = rootView.findViewById(R.id.nestedscrollview_main_groups);
        final TextView         loadMoreGroups   = rootView.findViewById(R.id.textview_load_more_groups);

        baseMainActivity.addToAllClickableViews(Arrays.asList(groupSearch, nestedScrollView, loadMoreGroups));

        // Search text listener
        EditTextImeOptions.setEnterListener(baseMainActivity, groupSearch, null);
        groupSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence cs, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable editable) {}

            @Override
            public void onTextChanged(CharSequence cs, int start, int before, int count) {
                // The CharSequence should never be null.
                if (GeneralUtils.isValidGroupSearchString(cs.toString(), false)) {
                    if (allGroupsAdapter != null && allGroupsAdapter.update(cs)) {
                        safeAdapterFilter();
                    }
                }
                else {
                    Toasts.showInvalidSearchPatternToast(getContext());
                }
            }
        });

        // Get more groups from the server if required.

        nestedScrollView.setOnScrollChangeListener(new NestedScrollViewListener() {
            @Override
            public void onScrollItemVisible() {
                checkForAppend();
            }
        });

        loadMoreGroups.setOnClickListener(v -> checkForAppend());

        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        allGroupsAdapter = null;
    }

    private void checkForAppend() {
        if (allGroupsAdapter != null && allGroupsAdapter.append()) {
            safeAdapterAppend();
        }
    }

//    private boolean checkForAppend() {
//        if (allGroupsAdapter != null && allGroupsAdapter.append()) {
//            safeAdapterAppend();
//            return true;
//        }
//
//        return false;
//    }

    private void safeAdapterAppend() {
        safeAdapterUpdate(true, false, false, false);
    }

    private void safeAdapterFilter() {
        safeAdapterUpdate(true, false, false, true);
    }

    void safeAdapterReset() {
        safeAdapterUpdate(false, true, true, true);
    }

    private void safeAdapterUpdate(final boolean forceServerRequest,
                                   final boolean ignoreFilters,
                                   final boolean resetAdapterLists,
                                   final boolean resetDisplayedItemCount) {

        final BaseMainActivity baseMainActivity = (BaseMainActivity) getActivity();
        if (baseMainActivity == null) {
            return;
        }

//        // Initialise an adapter if one does not already exist.
//        if (allGroupsAdapter == null) {
//            // This adapter assignment prevents the call to this method below looping indefinitely.
//            allGroupsAdapter = new GroupsAdapter(null);
//
//            // If there are unloaded items available to append in a non-full RecyclerView, load them (by calling this
//            // method again) before returning immediately.
//            if (checkForAppend()) {
//                return;
//            }
//        }

        final ProgressBar moreContactsProgressBar = rootView.findViewById(R.id.more_groups_progress_bar);
        final TextView    loadMoreGroups          = rootView.findViewById(R.id.textview_load_more_groups);

        moreContactsProgressBar.setVisibility(View.VISIBLE);
        loadMoreGroups.setVisibility(View.GONE);

        // Allow users to modify the RecyclerView and related filter options whilst waiting for an asynchronous server
        // response, providing a more seamless user experience when selecting and filtering RecyclerView items.
        // This is done by not deactivating clickable views here and (re)activating them when the response is received.

        RequestGlobalStatic.requestAndSetKnownGroups(
                (knownGroups, knownGroupsRequestInProgress) -> {
                    if (knownGroupsRequestInProgress) {
                        handleRequestCleanup(moreContactsProgressBar);
                        return;
                    }

                    // Initialise an adapter if one does not already exist.
                    if (allGroupsAdapter == null) {
                        allGroupsAdapter = new GroupsAdapter(knownGroups);
                    }
                    else {
                        // Update the adapter with the up-to-date list of groups.
                        allGroupsAdapter.update(resetAdapterLists, resetDisplayedItemCount, knownGroups);
                    }

                    final Context context = getContext();
                    if (knownGroups == null && context != null) {
                        Toasts.showCannotLoadGroupsToast(context);
                    }

                    // Show/Remove views depending on whether the list of known groups has been obtained.
                    showAndRemoveViews();

                    if (recyclerView == null) {
                        recyclerView = RecyclerViewSetup.setupGroupsRecyclerView(
                                rootView.findViewById(R.id.recyclerview_main_tab_groups),
                                GroupsTab.this,
                                GroupDetailsActivity.class,
                                allGroupsAdapter
                        );
                    }

                    // The RecyclerView will be null if the context cannot be obtained from this fragment.
                    if (recyclerView == null) {
                        return;
                    }

                    handleRequestCleanup(moreContactsProgressBar);
                },
                baseMainActivity,
                null,
                null,
                forceServerRequest,
                ignoreFilters,
                GroupsAdapter.getFilteredItemCount(allGroupsAdapter),
                null,
                GroupsAdapter.getSearchString(allGroupsAdapter),
                false,
                false
        );
    }

    private void handleRequestCleanup(final ProgressBar moreContactsProgressBar) {
        moreContactsProgressBar.setVisibility(View.GONE);
    }

    private void showAndRemoveViews() {
        final TextView loadMoreGroups = rootView.findViewById(R.id.textview_load_more_groups);
        final EditText groupSearch    = rootView.findViewById(R.id.edittext_main_tab_groups);
        final TextView errorGroup     = rootView.findViewById(R.id.groups_tab_error_group);

        // Only disable standard functionality if there are no existing items in the adapter.
        if (allGroupsAdapter != null && allGroupsAdapter.getTotalItemCount() > 0) {
            groupSearch.setVisibility(View.VISIBLE);
            errorGroup.setVisibility(View.GONE);
            loadMoreGroups.setVisibility(View.VISIBLE);
        }
        else {
            // Every contact must have access to at least the default group, so 0 adapter items implies
            // that group loading was unsuccessful.
            groupSearch.setVisibility(View.GONE);
            errorGroup.setVisibility(View.VISIBLE);
            loadMoreGroups.setVisibility(View.GONE);
        }
    }
}