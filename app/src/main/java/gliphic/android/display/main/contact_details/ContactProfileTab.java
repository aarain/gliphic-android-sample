/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.display.main.contact_details;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import gliphic.android.R;
import gliphic.android.display.GroupShareActivity;
import gliphic.android.display.abstract_views.BaseActivity;
import gliphic.android.display.abstract_views.BaseMainActivity;
import gliphic.android.display.abstract_views.BaseMainFragment;
import gliphic.android.display.libraries.AlertDialogs;
import gliphic.android.display.libraries.Views;
import gliphic.android.display.libraries.LoadingDialog;
import gliphic.android.operation.Contact;
import gliphic.android.exceptions.ContactException;
import gliphic.android.operation.server_interaction.http_server.HttpOperations;
import gliphic.android.operation.server_interaction.http_server.RequestGlobalStatic;

import java.util.Arrays;
import java.util.Collections;

import androidx.annotation.NonNull;
import libraries.GeneralUtils;
import libraries.Vars.ContactType;
import pojo.account.RemoveContactResponse;
import pojo.misc.AccessTokenAndContactNumber;

/**
 * The 'Profile' tab fragment.
 *
 * This lists the profile details of the selected contact.
 */
public class ContactProfileTab extends BaseMainFragment {
    private View rootView;
    private Contact viewedContact;

    private ImageView contactImage;
    private TextView contactName;
    private TextView contactId;

    /**
     * Use the current group information to set the image, name and description.
     */
    private void updateContactSettings() {
        viewedContact.getImage().setImageView(contactImage);
        contactName.setText(viewedContact.getName());
        contactId.setText(viewedContact.getId());

        configureButtons(viewedContact.isKnownContact());
    }

    @Override
    public void onNetworkAvailable(boolean isFirstOnNetworkAvailable) {}

    @Override
    public void noNetworkOnStart() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.contact_details_tab_profile, container, false);

        final ContactDetailsActivity contactDetailsActivity = (ContactDetailsActivity) getActivity();
        if (contactDetailsActivity == null) {
            return rootView;
        }

        viewedContact = contactDetailsActivity.viewedContact;

        contactImage = rootView.findViewById(R.id.imageview_contact_details_tab_profile);
        contactName  = rootView.findViewById(R.id.contact_name_contact_details_tab_profile);
        contactId    = rootView.findViewById(R.id.contact_id_contact_details_tab_profile);

        final Button addRemove             = rootView.findViewById(R.id.btn_contact_details_add_remove);
        final Button shareGroupWithContact = rootView.findViewById(R.id.btn_contact_details_share);

        ((BaseMainActivity) getActivity()).addToAllClickableViews(Arrays.asList(addRemove, shareGroupWithContact));

        // Set up OnClickListeners.

        addRemove.setOnClickListener(view -> {
            final BaseActivity baseActivity1 = (BaseActivity) getActivity();
            if (baseActivity1 == null) {
                return;
            }

            baseActivity1.deactivateClickableViews();

            final String url;
            final String errMsg;

            if (viewedContact.isKnownContact()) {
                errMsg = "Unable to remove the contact at this time.";
                url = HttpOperations.URI_REMOVE_CONTACT;
            }
            else {
                errMsg = "Unable to add the contact at this time.";
                url = HttpOperations.URI_ADD_CONTACT;
            }

            final LoadingDialog loadingDialog = new LoadingDialog(baseActivity1);

            RequestGlobalStatic.requestAndSetAccessToken(
                    accessToken -> {
                        final BaseActivity baseActivity2 = (BaseActivity) getActivity();
                        if (baseActivity2 == null) {
                            return;
                        }

                        if (accessToken == null) {
                            baseActivity2.activateClickableViews();
                            return;
                        }

                        final AccessTokenAndContactNumber atacn = new AccessTokenAndContactNumber(
                                accessToken,
                                viewedContact.getNumber()
                        );

                        HttpOperations.post(
                                url,
                                atacn,
                                baseActivity2,
                                response -> {
                                    final BaseActivity baseActivity3 = (BaseActivity) getActivity();
                                    if (baseActivity3 == null) {
                                        return;
                                    }

                                    // This contact's type should be updated, and if this contact is in the
                                    // local list of known or extended contacts it should be added/removed
                                    // to/from the relevant list.
                                    try {
                                        if (viewedContact.isKnownContact()) {
                                            // The contact has been removed by the server.

                                            final RemoveContactResponse removeContactResponse = GeneralUtils.fromJson(
                                                    response,
                                                    RemoveContactResponse.class
                                            );

                                            if (removeContactResponse.isUnknownContact()) {
                                                viewedContact.changeType(ContactType.UNKNOWN);
                                            }
                                            else {
                                                viewedContact.changeType(ContactType.EXTENDED);
                                            }
                                        }
                                        else {
                                            // TODO: Request contact groups from the server.

                                            // The contact has been added by the server.

                                            viewedContact.changeType(ContactType.KNOWN);
                                        }
                                    }
                                    catch (ContactException e) {
                                        loadingDialog.dismissDialog();

                                        AlertDialogs.internalErrorDialog(baseActivity3, false, errMsg);

                                        baseActivity3.activateClickableViews();

                                        return;
                                    }

                                    loadingDialog.dismissDialog();

                                    AlertDialogs.addedOrRemovedContactDialog(
                                            baseActivity3,
                                            viewedContact.isKnownContact(),
                                            viewedContact.getName()
                                    );

                                    baseActivity3.activateClickableViews();

                                    // This must be called after reactivating clickable views so that certain views
                                    // can be disabled.
                                    configureButtons(viewedContact.isKnownContact());
                                },
                                error -> {
                                    final BaseActivity baseActivity4 = (BaseActivity) getActivity();
                                    if (baseActivity4 == null) {
                                        return;
                                    }

                                    loadingDialog.dismissDialog();

                                    HttpOperations.handleStandardRequestOnErrorResponse(
                                            error,
                                            baseActivity4,
                                            false
                                    );

                                    baseActivity4.activateClickableViews();
                                }
                        );
                    },
                    baseActivity1,
                    loadingDialog,
                    false
            );
        });

        shareGroupWithContact.setOnClickListener(v -> {
            final BaseMainActivity baseMainActivity = (BaseMainActivity) getActivity();
            if (baseMainActivity != null) {
                GroupShareActivity.startActivityShareGroup(baseMainActivity, viewedContact);
            }
        });

        // Note that this must be done after calling the setOnClickListener method because if the view which
        // setOnClickListener is called on is intended to be not enabled, setOnClickListener sets it to be enabled.
        updateContactSettings();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        updateContactSettings();
    }

    private void configureButtons(boolean isKnownContact) {
        final Button addOrRemoveButton = rootView.findViewById(R.id.btn_contact_details_add_remove);
        final Button shareGroupButton  = rootView.findViewById(R.id.btn_contact_details_share);

        Views.setAlphaAndEnabled(Collections.singletonList(shareGroupButton), isKnownContact);

        if (isKnownContact) {
            addOrRemoveButton.setText(R.string.contact_details_remove_btn);
        }
        else {
            addOrRemoveButton.setText(R.string.contact_details_add_btn);
        }
    }
}
