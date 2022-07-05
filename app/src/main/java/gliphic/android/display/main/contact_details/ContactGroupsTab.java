/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.display.main.contact_details;

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

import gliphic.android.R;
import gliphic.android.adapters.GroupsAdapter;
import gliphic.android.display.abstract_views.BaseMainActivity;
import gliphic.android.display.libraries.EditTextImeOptions;
import gliphic.android.display.libraries.Toasts;
import gliphic.android.display.main.group_details.GroupDetailsActivity;
import gliphic.android.display.libraries.RecyclerViewSetup;
import gliphic.android.display.abstract_views.BaseMainFragment;
import gliphic.android.listeners.NestedScrollViewListener;
import gliphic.android.operation.Contact;
import gliphic.android.operation.Group;
import gliphic.android.operation.server_interaction.http_server.RequestGlobalStatic;

import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;
import libraries.GeneralUtils;

/**
 * The 'Groups' tab fragment.
 *
 * This lists all of the groups which the user has in common with this contact.
 */
public class ContactGroupsTab extends BaseMainFragment {
    private View rootView;
    private GroupsAdapter commonGroupsAdapter;
    private Contact viewedContact;
    private RecyclerView recyclerView;

    @Override
    public void onNetworkAvailable(boolean isFirstOnNetworkAvailable) {
        new Handler(Looper.getMainLooper()).post(
                this::safeAdapterReset
        );
    }

    @Override
    public void noNetworkOnStart() {
        new Handler(Looper.getMainLooper()).post(() ->
                showAndRemoveViews(null)
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.contact_details_tab_groups, container, false);

        final ContactDetailsActivity contactDetailsActivity = (ContactDetailsActivity) getActivity();
        if (contactDetailsActivity == null) {
            return rootView;
        }

        viewedContact = contactDetailsActivity.viewedContact;

        final EditText         groupSearch      = rootView.findViewById(R.id.edittext_contact_groups);
        final NestedScrollView nestedScrollView = rootView.findViewById(R.id.nestedscrollview_contact_groups);
        final TextView         loadMoreGroups   = rootView.findViewById(R.id.textview_load_more_contact_groups);

        contactDetailsActivity.addToAllClickableViews(Arrays.asList(groupSearch, nestedScrollView, loadMoreGroups));

        EditTextImeOptions.setEnterListener(contactDetailsActivity, groupSearch, null);

        // Search text listener
        groupSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence cs, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable editable) {}

            @Override
            public void onTextChanged(CharSequence cs, int start, int before, int count) {
                // The CharSequence should never be null.
                if (GeneralUtils.isValidGroupSearchString(cs.toString(), false)) {
                    if (commonGroupsAdapter != null && commonGroupsAdapter.update(cs)) {
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
    public void onStop() {
        super.onStop();

        if (commonGroupsAdapter != null) {
            commonGroupsAdapter.clear();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        commonGroupsAdapter = null;
    }

    private void checkForAppend() {
        if (commonGroupsAdapter != null && commonGroupsAdapter.append()) {
            safeAdapterAppend();
        }
    }

    private void safeAdapterAppend() {
        safeAdapterUpdate(true, false, false, false);
    }

    private void safeAdapterFilter() {
        safeAdapterUpdate(true, false, false, true);
    }

    private void safeAdapterReset() {
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

        final ProgressBar moreContactsProgressBar = rootView.findViewById(R.id.more_contact_groups_progress_bar);
        final TextView    loadMoreGroups          = rootView.findViewById(R.id.textview_load_more_contact_groups);

        moreContactsProgressBar.setVisibility(View.VISIBLE);
        loadMoreGroups.setVisibility(View.GONE);

        // Allow users to modify the RecyclerView and related filter options whilst waiting for an asynchronous server
        // response, providing a more seamless user experience when selecting and filtering RecyclerView items.
        // This is done by not deactivating clickable views here and (re)activating them when the response is received.

        RequestGlobalStatic.requestAndSetKnownGroups(
                (commonGroups, knownGroupsRequestInProgress) -> {
                    if (knownGroupsRequestInProgress) {
                        handleRequestCleanup(moreContactsProgressBar);
                        return;
                    }

                    // Initialise an adapter if one does not already exist.
                    if (commonGroupsAdapter == null) {
                        commonGroupsAdapter = new GroupsAdapter(commonGroups);
                    }
                    else {
                        // Update the adapter with the up-to-date list of groups.
                        commonGroupsAdapter.update(resetAdapterLists, resetDisplayedItemCount, commonGroups);
                    }

                    final Context context = getContext();
                    if (commonGroups == null && context != null) {
                        Toasts.showCannotLoadGroupsToast(context);
                    }

                    // Show/Remove views depending on the number of groups in the adapter and whether the list of
                    // common groups has been obtained.
                    showAndRemoveViews(commonGroups);

                    if (recyclerView == null) {
                        recyclerView = RecyclerViewSetup.setupGroupsRecyclerView(
                                rootView.findViewById(R.id.recyclerview_contact_details_tab_groups),
                                ContactGroupsTab.this,
                                GroupDetailsActivity.class,
                                commonGroupsAdapter
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
                GroupsAdapter.getFilteredItemCount(commonGroupsAdapter),
                viewedContact,
                GroupsAdapter.getSearchString(commonGroupsAdapter),
                true,
                false
        );
    }

    private void handleRequestCleanup(final ProgressBar moreContactsProgressBar) {
        moreContactsProgressBar.setVisibility(View.GONE);
    }

    private void showAndRemoveViews(@Nullable List<Group> commonGroups) {
        final TextView loadMoreGroups = rootView.findViewById(R.id.textview_load_more_contact_groups);
        final EditText groupSearch    = rootView.findViewById(R.id.edittext_contact_groups);
        final TextView noGroups       = rootView.findViewById(R.id.textview_no_common_groups);
        final TextView errorGroup     = rootView.findViewById(R.id.contact_groups_tab_error_group);

        // Enable standard functionality if there are existing item(s) in the adapter.
        if (commonGroupsAdapter != null && commonGroupsAdapter.getTotalItemCount() > 0) {
            groupSearch.setVisibility(View.VISIBLE);
            noGroups.setVisibility(View.GONE);
            errorGroup.setVisibility(View.GONE);
            loadMoreGroups.setVisibility(View.VISIBLE);
        }
        else {
            groupSearch.setVisibility(View.GONE);

            if (commonGroups == null) {
                noGroups.setVisibility(View.GONE);
                errorGroup.setVisibility(View.VISIBLE);
                loadMoreGroups.setVisibility(View.GONE);
            }
            else {
                noGroups.setVisibility(View.VISIBLE);
                errorGroup.setVisibility(View.GONE);
                loadMoreGroups.setVisibility(View.VISIBLE);
            }
        }
    }
}
