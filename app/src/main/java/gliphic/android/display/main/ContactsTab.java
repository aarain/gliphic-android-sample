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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import gliphic.android.R;
import gliphic.android.adapters.ContactsAdapter;
import gliphic.android.display.libraries.EditTextImeOptions;
import gliphic.android.display.libraries.Toasts;
import gliphic.android.display.main.contact_details.ContactDetailsActivity;
import gliphic.android.display.abstract_views.BaseMainActivity;
import gliphic.android.display.libraries.RecyclerViewSetup;
import gliphic.android.display.abstract_views.BaseMainFragment;
import gliphic.android.listeners.NestedScrollViewListener;
import gliphic.android.operation.Contact;
import gliphic.android.operation.server_interaction.http_server.RequestGlobalStatic;

import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;
import libraries.GeneralUtils;

/**
 * The 'Contacts' tab fragment.
 *
 * This lists all contacts available to the user allowing further operations to be done on/with them.
 */
public class ContactsTab extends BaseMainFragment {
    private View rootView;
    private ContactsAdapter allContactsAdapter;
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
        rootView = inflater.inflate(R.layout.main_tab_contacts, container, false);

        final BaseMainActivity baseMainActivity = (BaseMainActivity) getActivity();
        if (baseMainActivity == null) {
            return rootView;
        }

        // The text within this view should not be accessed directly.
        contactSearch = rootView.findViewById(R.id.edittext_main_tab_contacts);

        EditTextImeOptions.setEnterListener(baseMainActivity, contactSearch, null);

        knownContactsBox = rootView.findViewById(R.id.checkbox_main_known_contacts);
        extendedContactsBox = rootView.findViewById(R.id.checkbox_main_extended_contacts);

        setClickableCheckBoxes();

        final NestedScrollView nestedScrollView = rootView.findViewById(R.id.nestedscrollview_main_contacts);
        final TextView         loadMoreContacts = rootView.findViewById(R.id.textview_load_more_contacts);

        baseMainActivity.addToAllClickableViews(Arrays.asList(
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
                    if (allContactsAdapter != null && allContactsAdapter.update(cs)) {
                        safeAdapterFilter();
                    }
                }
                else {
                    Toasts.showInvalidSearchPatternToast(getContext());
                }
            }
        });

        // Checkbox setup
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
    public void onDestroy() {
        super.onDestroy();

        allContactsAdapter = null;
    }

    private void checkForAppend() {
        if (allContactsAdapter != null && allContactsAdapter.append()) {
            safeAdapterAppend();
        }
    }

    private void handleCheckBoxOnClick() {
        setClickableCheckBoxes();

        if ( allContactsAdapter != null &&
             allContactsAdapter.update(knownContactsBox.isChecked(), extendedContactsBox.isChecked()) ) {

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
                final String s = "Both known and extended CheckBoxes cannot be unchecked.";
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

        final ProgressBar moreContactsProgressBar = rootView.findViewById(R.id.more_contacts_progress_bar);
        final TextView    loadMoreContacts        = rootView.findViewById(R.id.textview_load_more_contacts);

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
                    if (allContactsAdapter == null) {
                        allContactsAdapter = new ContactsAdapter(targetContacts, true);
                    }
                    else {
                        // Update the adapter with the up-to-date list of contacts.
                        allContactsAdapter.reset(
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
                                rootView.findViewById(R.id.recyclerview_main_tab_contacts),
                                ContactsTab.this,
                                ContactDetailsActivity.class,
                                allContactsAdapter
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
                ContactsAdapter.getFilteredItemCount(allContactsAdapter),
                null,
                knownContactsBox.isChecked(),
                extendedContactsBox.isChecked(),
                ContactsAdapter.getSearchString(allContactsAdapter)
        );
    }

    private void handleRequestCleanup(final ProgressBar moreContactsProgressBar) {
        moreContactsProgressBar.setVisibility(View.GONE);

        setClickableCheckBoxes();
    }

    private void showAndRemoveViews(@Nullable List<Contact> targetContacts) {
        final TextView loadMoreContacts = rootView.findViewById(R.id.textview_load_more_contacts);
        final View     itemDivider      = rootView.findViewById(R.id.itemdivider_main_tab_contacts);
        final TextView noContacts       = rootView.findViewById(R.id.textview_no_contacts);
        final TextView errorContact     = rootView.findViewById(R.id.contacts_tab_error_contact);

        if (allContactsAdapter != null && allContactsAdapter.getTotalItemCount() > 0) {
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
            }
        }
    }
}
