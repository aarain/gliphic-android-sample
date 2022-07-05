/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.display.main.group_details;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import gliphic.android.R;
import gliphic.android.adapters.ContactsAdapter;
import gliphic.android.display.abstract_views.BaseMainActivity;
import gliphic.android.display.libraries.EditTextImeOptions;
import gliphic.android.display.libraries.Toasts;
import gliphic.android.display.main.contact_details.ContactDetailsActivity;
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
import libraries.Vars;

/**
 * The 'Profile' tab fragment.
 *
 * This lists the profile details of the selected contact.
 */
public class GroupContactsTab extends BaseMainFragment {
    private View rootView;
    private ContactsAdapter commonContactsAdapter;
    private Group viewedGroup;
    private EditText contactSearch;
    private CheckBox knownContactsBox;
    private CheckBox extendedContactsBox;
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
        rootView = inflater.inflate(R.layout.group_details_tab_contacts, container, false);

        final GroupDetailsActivity groupDetailsActivity = (GroupDetailsActivity) getActivity();
        if (groupDetailsActivity == null) {
            return rootView;
        }

        this.viewedGroup = groupDetailsActivity.viewedGroup;

        // The text within this view should not be accessed directly.
        contactSearch = rootView.findViewById(R.id.edittext_group_contacts);

        EditTextImeOptions.setEnterListener(groupDetailsActivity, contactSearch, null);

        knownContactsBox = rootView.findViewById(R.id.checkbox_group_known_contacts);
        extendedContactsBox = rootView.findViewById(R.id.checkbox_group_extended_contacts);

        setClickableCheckBoxes();

        final NestedScrollView nestedScrollView = rootView.findViewById(R.id.nestedscrollview_group_contacts);
        final TextView         loadMoreContacts = rootView.findViewById(R.id.textview_load_more_group_contacts);

        groupDetailsActivity.addToAllClickableViews(Arrays.asList(
                contactSearch,
                nestedScrollView,
                loadMoreContacts,
                knownContactsBox,
                extendedContactsBox
        ));

        // Search text listener
        contactSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence cs, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable editable) {}

            @Override
            public void onTextChanged(CharSequence cs, int start, int before, int count) {
                // The CharSequence should never be null.
                if (GeneralUtils.isValidContactSearchString(cs.toString(), false)) {
                    if (commonContactsAdapter != null && commonContactsAdapter.update(cs)) {
                        safeAdapterFilter();
                    }
                }
                else {
                    Toasts.showInvalidSearchPatternToast(getContext());
                }
            }
        });

        knownContactsBox.setOnClickListener(v -> handleCheckBoxOnClick());

        extendedContactsBox.setOnClickListener(v -> handleCheckBoxOnClick());

        // Get more contacts from the server if required.

        nestedScrollView.setOnScrollChangeListener(new NestedScrollViewListener() {
            @Override
            public void onScrollItemVisible() {
                checkForAppend();
            }
        });

        loadMoreContacts.setOnClickListener(v -> checkForAppend());

        return rootView;
    }

    @Override
    public void onStop() {
        super.onStop();

        if (commonContactsAdapter != null) {
            commonContactsAdapter.clear();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        commonContactsAdapter = null;
    }

    private void checkForAppend() {
        if (commonContactsAdapter != null && commonContactsAdapter.append()) {
            safeAdapterAppend();
        }
    }

    private void handleCheckBoxOnClick() {
        setClickableCheckBoxes();

        if ( commonContactsAdapter != null &&
             commonContactsAdapter.update(knownContactsBox.isChecked(), extendedContactsBox.isChecked()) ) {

            safeAdapterFilter();
        }
    }

    private void setClickableCheckBoxes() {
        // Prevent both the known and extended CheckBoxes from being unchecked.
        if (knownContactsBox.isChecked()) {
            if (extendedContactsBox.isChecked()) {
                knownContactsBox.setClickable(true);
            }
            else {
                knownContactsBox.setClickable(false);
            }

            extendedContactsBox.setClickable(true);
        }
        else {
            if (extendedContactsBox.isChecked()) {
                knownContactsBox.setClickable(true);
            }
            else {
                final String s = "Both group-known and group-extended CheckBoxes cannot be unchecked.";
                throw new Error(new IllegalStateException(s));
            }

            extendedContactsBox.setClickable(false);
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

        final ProgressBar moreContactsProgressBar = rootView.findViewById(R.id.more_group_contacts_progress_bar);
        final TextView    loadMoreContacts        = rootView.findViewById(R.id.textview_load_more_group_contacts);

        moreContactsProgressBar.setVisibility(View.VISIBLE);
        loadMoreContacts.setVisibility(View.GONE);

        // Allow users to modify the RecyclerView and related filter options whilst waiting for an asynchronous server
        // response, providing a more seamless user experience when selecting and filtering RecyclerView items.
        // This is done by not deactivating clickable views here and (re)activating them when the response is received.

        RequestGlobalStatic.requestAndSetTargetContacts(
                (targetContacts, targetContactsRequestInProgress) -> {

                    if (targetContactsRequestInProgress) {
                        handleRequestCleanup(moreContactsProgressBar);
                        return;
                    }

                    // Initialise an adapter if one does not already exist.
                    if (commonContactsAdapter == null) {
                        commonContactsAdapter = new ContactsAdapter(
                                viewedGroup,
                                targetContacts,
                                true
                        );
                    }
                    else {
                        // Update the adapter with the up-to-date list of contacts.
                        commonContactsAdapter.reset(
                                targetContacts,
                                resetAdapterLists,
                                resetDisplayedItemCount,
                                knownContactsBox.isChecked(),
                                extendedContactsBox.isChecked()
                        );
                    }

                    final Context context = getContext();
                    if (targetContacts == null && context != null) {
                        Toasts.showCannotLoadContactsToast(context);
                    }

                    // Show/Remove views depending on the number of contacts in the adapter and
                    // whether the list of target contacts has been obtained.
                    showAndRemoveViews(targetContacts);

                    if (recyclerView == null) {
                        recyclerView = RecyclerViewSetup.setupContactsRecyclerView(
                                rootView.findViewById(R.id.recyclerview_group_details_tab_target_contacts),
                                GroupContactsTab.this,
                                ContactDetailsActivity.class,
                                commonContactsAdapter
                        );
                    }

                    // The RecyclerView will be null if the context cannot be obtained from this fragment.
                    if (recyclerView == null) {
                        return;
                    }

                    handleRequestCleanup(moreContactsProgressBar);
                },
                baseMainActivity,
                forceServerRequest,
                ignoreFilters,
                ContactsAdapter.getFilteredItemCount(commonContactsAdapter),
                viewedGroup,
                knownContactsBox.isChecked(),
                extendedContactsBox.isChecked(),
                ContactsAdapter.getSearchString(commonContactsAdapter)
        );
    }

    private void handleRequestCleanup(final ProgressBar moreContactsProgressBar) {
        moreContactsProgressBar.setVisibility(View.GONE);

        setClickableCheckBoxes();
    }

    private void showAndRemoveViews(@Nullable List<Contact> targetContacts) {
        final TextView loadMoreContacts = rootView.findViewById(R.id.textview_load_more_group_contacts);
        final View     itemDivider      = rootView.findViewById(R.id.itemdivider_group_contacts);
        final TextView noContacts       = rootView.findViewById(R.id.textview_no_group_contacts);
        final TextView errorContact     = rootView.findViewById(R.id.group_contacts_tab_error_contact);

        if (commonContactsAdapter != null && commonContactsAdapter.getTotalItemCount() > 0) {
            contactSearch.setVisibility(View.VISIBLE);
            knownContactsBox.setVisibility(View.VISIBLE);
            extendedContactsBox.setVisibility(View.VISIBLE);
            itemDivider.setVisibility(View.VISIBLE);
            noContacts.setVisibility(View.GONE);
            errorContact.setVisibility(View.GONE);
            loadMoreContacts.setVisibility(View.VISIBLE);
        }
        else {
            contactSearch.setVisibility(View.GONE);
            knownContactsBox.setVisibility(View.GONE);
            extendedContactsBox.setVisibility(View.GONE);
            itemDivider.setVisibility(View.GONE);

            if (targetContacts == null) {
                noContacts.setVisibility(View.GONE);
                errorContact.setVisibility(View.VISIBLE);
                loadMoreContacts.setVisibility(View.GONE);
            }
            else {
                noContacts.setVisibility(View.VISIBLE);
                errorContact.setVisibility(View.GONE);
                loadMoreContacts.setVisibility(View.VISIBLE);

                // The default group is assumed to have no known or extended contacts stored.
                if (viewedGroup.getNumber() == Vars.DEFAULT_GROUP_NUMBER) {
                    noContacts.setText(R.string.default_group_contacts);
                }
                else {
                    noContacts.setText(R.string.no_group_contacts);
                }
            }
        }
    }
}
