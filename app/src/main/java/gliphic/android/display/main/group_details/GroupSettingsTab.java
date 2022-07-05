/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.display.main.group_details;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import gliphic.android.R;
import gliphic.android.display.GroupShareActivity;
import gliphic.android.display.abstract_views.BaseActivity;
import gliphic.android.display.abstract_views.BaseMainActivity;
import gliphic.android.display.GroupSelectionActivity;
import gliphic.android.display.libraries.AlertDialogs;
import gliphic.android.display.libraries.Views;
import gliphic.android.display.libraries.EditTextImeOptions;
import gliphic.android.display.libraries.LoadingDialog;
import gliphic.android.display.abstract_views.BaseMainFragment;
import gliphic.android.display.pictures.DisplayPicturesActivity;
import gliphic.android.operation.Group;
import gliphic.android.exceptions.GroupException;
import gliphic.android.operation.server_interaction.http_server.HttpOperations;
import gliphic.android.operation.server_interaction.http_server.RequestGlobalStatic;

import java.util.Arrays;
import java.util.Collections;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import libraries.Vars;
import pojo.misc.AccessTokenAndGroupNumber;
import pojo.set.SetGroupString;

/**
 * The 'Profile' tab fragment.
 *
 * This lists the profile details of the selected contact.
 */
public class GroupSettingsTab extends BaseMainFragment {
    private Group viewedGroup;

    private ImageView imageViewGroupImg;
    private EditText editTextGroupName;
    private EditText editTextGroupDesc;
    private TextView groupId;

    /**
     * Use the current group information to set the image, name and description.
     */
    private void updateGroupSettings() {
        viewedGroup.getImage().setImageView(imageViewGroupImg);
        editTextGroupName.setText(viewedGroup.getName());
        editTextGroupDesc.setText(viewedGroup.getDescription());
        groupId.setText(viewedGroup.getId());
    }

    private void setClickableButton(boolean clickable, Button button) {
        Views.setAlphaAndEnabled(Collections.singletonList(button), clickable);
    }

    @Override
    public void onNetworkAvailable(boolean isFirstOnNetworkAvailable) {}

    @Override
    public void noNetworkOnStart() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.group_details_tab_settings, container, false);

        final GroupDetailsActivity groupDetailsActivity = (GroupDetailsActivity) getActivity();
        if (groupDetailsActivity == null) {
            return rootView;
        }

        viewedGroup = groupDetailsActivity.viewedGroup;

        imageViewGroupImg = rootView.findViewById(R.id.group_details_img);
        editTextGroupName = rootView.findViewById(R.id.group_details_name);
        editTextGroupDesc = rootView.findViewById(R.id.group_details_description);
        groupId           = rootView.findViewById(R.id.group_id_group_settings_tab_profile);
        final Button selectBtn = rootView.findViewById(R.id.btn_group_details_select);
        final Button shareBtn  = rootView.findViewById(R.id.btn_group_details_share);
        final Button leaveBtn  = rootView.findViewById(R.id.btn_group_details_leave);

        groupDetailsActivity.addToAllClickableViews(Arrays.asList(
                editTextGroupName,
                editTextGroupDesc,
                selectBtn,
                shareBtn,
                leaveBtn
        ));

        EditTextImeOptions.setEnterListener(groupDetailsActivity, editTextGroupName, null);
        EditTextImeOptions.setEnterListener(groupDetailsActivity, editTextGroupDesc, null);

        // Required to combine the 'text' and 'textMultiLine' input types (set in XML).
        editTextGroupDesc.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editTextGroupDesc.setRawInputType(InputType.TYPE_CLASS_TEXT);

        if (viewedGroup.getNumber() != Vars.DEFAULT_GROUP_NUMBER) {
            imageViewGroupImg.setOnClickListener(v -> DisplayPicturesActivity.launchActivityForGroup(
                    getActivity(),
                    viewedGroup.getNumber()
            ));
        }

        editTextGroupName.setOnFocusChangeListener((v, hasFocus) -> {
            String oldGroupName = viewedGroup.getName();
            String newGroupName = editTextGroupName.getText().toString();
            if (!hasFocus && !oldGroupName.equals(newGroupName)) {
                editTextGroupNameChange(editTextGroupName, oldGroupName, newGroupName);
            }
        });

        editTextGroupDesc.setOnFocusChangeListener((v, hasFocus) -> {
            String oldGroupDesc = viewedGroup.getDescription();
            String newGroupDesc = editTextGroupDesc.getText().toString();
            if (!hasFocus && !oldGroupDesc.equals(newGroupDesc)) {
                editTextGroupDescChange(editTextGroupDesc, oldGroupDesc, newGroupDesc);
            }
        });

        selectBtn.setOnClickListener(v -> {
            final BaseMainActivity baseMainActivity = (BaseMainActivity) getActivity();
            if (baseMainActivity != null) {
                GroupSelectionActivity.selectGroup(baseMainActivity, viewedGroup);
            }
        });

        shareBtn.setOnClickListener(v -> {
            final BaseMainActivity baseMainActivity = (BaseMainActivity) getActivity();
            if (baseMainActivity != null) {
                GroupShareActivity.startActivityShareGroup(baseMainActivity, viewedGroup);
            }
        });

        leaveBtn.setOnClickListener(v -> leaveGroup());

        // The default group cannot be shared or left.
        if (viewedGroup.getNumber() == Vars.DEFAULT_GROUP_NUMBER) {
            setClickableButton(false, shareBtn);
            setClickableButton(false, leaveBtn);
        }
        else {
            setClickableButton(true, shareBtn);
            setClickableButton(true, leaveBtn);
        }

        // Note that this must be done after calling the setOnClickListener method because if the view which
        // setOnClickListener is called on is intended to be not clickable, setOnClickListener sets it to be clickable.
        updateGroupSettings();

        return rootView;
    }

    /**
     * This is called whenever the user changes text in the editTextGroupName EditText view,
     * and the user either changes focus away from the EditText or taps the Enter key.
     *
     * @param editTextGroupName     The EditText view containing the group name.
     * @param oldName               The text-string before user-modification.
     * @param newName               The text-string after user-modification.
     */
    private void editTextGroupNameChange(final EditText editTextGroupName,
                                         final String oldName,
                                         final String newName) {

        final BaseActivity baseActivity1 = (BaseActivity) getActivity();
        if (baseActivity1 == null) {
            return;
        }

        final String GENERIC_SET_GROUP_NAME_FAILED_MSG = "Unable to change the group name at this time.";

        // The default group cannot be modified.
        if (viewedGroup.getNumber() == Vars.DEFAULT_GROUP_NUMBER) {
            editTextGroupName.setText(oldName);
            AlertDialogs.cannotModifyDefaultGroupDialog(baseActivity1);
            return;
        }

        try {
            Group.checkValidName(newName);
        }
        catch (GroupException e) {
            editTextGroupName.setText(oldName);
            AlertDialogs.invalidInputDialog(baseActivity1, e.getMessage(), false);
            return;
        }

        final AlertDialog groupNameChanged = new AlertDialog.Builder(baseActivity1).create();
        baseActivity1.setMAlertDialog(groupNameChanged);
        groupNameChanged.setTitle("Change group name?");
        groupNameChanged.setMessage("Confirming this change will modify the group name for all users.");
        groupNameChanged.setButton(
                AlertDialog.BUTTON_POSITIVE,
                getResources().getString(R.string.confirm),
                (dialog, which) -> {
                    dialog.dismiss();
                    if (groupNameChanged.equals(baseActivity1.getMAlertDialog())) {
                        baseActivity1.setMAlertDialog(null);
                    }

                    baseActivity1.deactivateClickableViews();

                    final LoadingDialog loadingDialog = new LoadingDialog(baseActivity1);

                    RequestGlobalStatic.requestAndSetAccessToken(
                            accessToken -> {
                                final BaseActivity baseActivity2 = (BaseActivity) getActivity();
                                if (baseActivity2 == null) {
                                    return;
                                }

                                if (accessToken == null) {
                                    editTextGroupName.setText(oldName);

                                    baseActivity2.activateClickableViews();

                                    return;
                                }

                                SetGroupString setGroupString = new SetGroupString(
                                        accessToken,
                                        viewedGroup.getNumber(),
                                        newName
                                );

                                HttpOperations.post(
                                        HttpOperations.URI_SET_GROUP_NAME,
                                        setGroupString,
                                        baseActivity2,
                                        response -> {
                                            final BaseActivity baseActivity3 = (BaseActivity) getActivity();
                                            if (baseActivity3 == null) {
                                                return;
                                            }

                                            loadingDialog.dismissDialog();

                                            // Set the group name internally.
                                            try {
                                                viewedGroup.setName(newName);
                                            }
                                            catch (GroupException e) {
                                                AlertDialogs.internalErrorDialog(
                                                        baseActivity3,
                                                        false,
                                                        GENERIC_SET_GROUP_NAME_FAILED_MSG
                                                );

                                                editTextGroupName.setText(oldName);

                                                baseActivity3.activateClickableViews();

                                                return;
                                            }

                                            AlertDialogs.setGroupNameDialog((BaseActivity) getActivity());

                                            baseActivity3.activateClickableViews();
                                        },
                                        error -> {
                                            final BaseActivity baseActivity4 = (BaseActivity) getActivity();
                                            if (baseActivity4 == null) {
                                                return;
                                            }

                                            loadingDialog.dismissDialog();

                                            HttpOperations.handleSetGroupDataOnErrorResponse(error, baseActivity4);

                                            editTextGroupName.setText(oldName);

                                            baseActivity4.activateClickableViews();
                                        }
                                );
                            },
                            baseActivity1,
                            loadingDialog,
                            false
                    );
                });
        groupNameChanged.setButton(
                AlertDialog.BUTTON_NEGATIVE,
                getResources().getString(android.R.string.no),
                (dialog, which) -> {
                    editTextGroupName.setText(oldName);
                    dialog.dismiss();
                    if (groupNameChanged.equals(baseActivity1.getMAlertDialog())) {
                        baseActivity1.setMAlertDialog(null);
                    }
                });
        groupNameChanged.setCancelable(false);
        groupNameChanged.setCanceledOnTouchOutside(false);
        groupNameChanged.show();
    }

    /**
     * This is called whenever the user changes text in the editTextGroupDesc EditText view,
     * and the user either changes focus away from the EditText or taps the Enter key.
     *
     * @param editTextGroupDesc     The EditText view containing the group description.
     * @param oldDesc               The text-string before user-modification.
     * @param newDesc               The text-string after user-modification.
     */
    private void editTextGroupDescChange(final EditText editTextGroupDesc,
                                         final String oldDesc,
                                         final String newDesc) {

        final BaseActivity baseActivity1 = (BaseActivity) getActivity();
        if (baseActivity1 == null) {
            return;
        }

        final String GENERIC_SET_GROUP_DESC_FAILED_MSG = "Unable to change the group description at this time.";

        // The default group cannot be modified.
        if (viewedGroup.getNumber() == Vars.DEFAULT_GROUP_NUMBER) {
            editTextGroupDesc.setText(oldDesc);
            AlertDialogs.cannotModifyDefaultGroupDialog(baseActivity1);
            return;
        }

        try {
            Group.checkValidDescription(newDesc, true);
        }
        catch (GroupException e) {
            editTextGroupDesc.setText(oldDesc);
            AlertDialogs.invalidInputDialog(baseActivity1, e.getMessage(), false);
            return;
        }

        final AlertDialog groupDescChanged = new AlertDialog.Builder(baseActivity1).create();
        baseActivity1.setMAlertDialog(groupDescChanged);
        groupDescChanged.setTitle("Change group description?");
        String msg = "Confirming this change will only modify your personal description of the group.";
        groupDescChanged.setMessage(msg);
        groupDescChanged.setButton(
                AlertDialog.BUTTON_POSITIVE,
                getResources().getString(R.string.confirm),
                (dialog, which) -> {
                    dialog.dismiss();
                    if (groupDescChanged.equals(baseActivity1.getMAlertDialog())) {
                        baseActivity1.setMAlertDialog(null);
                    }

                    baseActivity1.deactivateClickableViews();

                    final LoadingDialog loadingDialog = new LoadingDialog(baseActivity1);

                    RequestGlobalStatic.requestAndSetAccessToken(
                            accessToken -> {
                                final BaseActivity baseActivity2 = (BaseActivity) getActivity();
                                if (baseActivity2 == null) {
                                    return;
                                }

                                if (accessToken == null) {
                                    editTextGroupDesc.setText(oldDesc);

                                    baseActivity2.activateClickableViews();

                                    return;
                                }

                                SetGroupString setGroupString = new SetGroupString(
                                        accessToken,
                                        viewedGroup.getNumber(),
                                        newDesc
                                );

                                HttpOperations.post(
                                        HttpOperations.URI_SET_GROUP_DESCRIPTION,
                                        setGroupString,
                                        baseActivity2,
                                        response -> {
                                            final BaseActivity baseActivity3 = (BaseActivity) getActivity();
                                            if (baseActivity3 == null) {
                                                return;
                                            }

                                            loadingDialog.dismissDialog();

                                            // Set the group description internally.
                                            try {
                                                viewedGroup.setDescription(
                                                        newDesc,
                                                        true
                                                );
                                            }
                                            catch (GroupException e) {
                                                AlertDialogs.internalErrorDialog(
                                                        baseActivity3,
                                                        false,
                                                        GENERIC_SET_GROUP_DESC_FAILED_MSG
                                                );

                                                editTextGroupDesc.setText(oldDesc);

                                                baseActivity3.activateClickableViews();

                                                return;
                                            }

                                            AlertDialogs.setGroupDescriptionDialog(baseActivity3);

                                            baseActivity3.activateClickableViews();
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

                                            editTextGroupDesc.setText(oldDesc);

                                            baseActivity4.activateClickableViews();
                                        }
                                );
                            },
                            baseActivity1,
                            loadingDialog,
                            false
                    );
                });
        groupDescChanged.setButton(
                AlertDialog.BUTTON_NEGATIVE,
                getResources().getString(android.R.string.no),
                (dialog, which) -> {
                    editTextGroupDesc.setText(oldDesc);
                    dialog.dismiss();
                    if (groupDescChanged.equals(baseActivity1.getMAlertDialog())) {
                        baseActivity1.setMAlertDialog(null);
                    }
                });
        groupDescChanged.setCancelable(false);
        groupDescChanged.setCanceledOnTouchOutside(false);
        groupDescChanged.show();
    }

    private void leaveGroup() {
        final BaseActivity baseActivity1 = (BaseActivity) getActivity();
        if (baseActivity1 == null) {
            return;
        }

        final AlertDialog alertDialog = new AlertDialog.Builder(baseActivity1).create();
        baseActivity1.setMAlertDialog(alertDialog);
        alertDialog.setTitle("Leave group");
        alertDialog.setMessage(String.format("Are you sure you want to leave %s?", viewedGroup.getName()));
        alertDialog.setButton(
                AlertDialog.BUTTON_NEGATIVE,
                baseActivity1.getResources().getString(android.R.string.no),
                (dialog, which) -> {
                    dialog.dismiss();
                    if (alertDialog.equals(baseActivity1.getMAlertDialog())) {
                        baseActivity1.setMAlertDialog(null);
                    }
                });
        alertDialog.setButton(
                AlertDialog.BUTTON_POSITIVE,
                baseActivity1.getResources().getString(android.R.string.yes),
                (dialog, which) -> {
                    dialog.dismiss();
                    if (alertDialog.equals(baseActivity1.getMAlertDialog())) {
                        baseActivity1.setMAlertDialog(null);
                    }

                    baseActivity1.deactivateClickableViews();

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

                                final AccessTokenAndGroupNumber atagn = new AccessTokenAndGroupNumber(
                                        accessToken,
                                        viewedGroup.getNumber()
                                );

                                HttpOperations.post(
                                        HttpOperations.URI_LEAVE_GROUP,
                                        atagn,
                                        baseActivity2,
                                        response -> {
                                            viewedGroup.removeGroup();

                                            final BaseActivity baseActivity3 = (BaseActivity) getActivity();
                                            if (baseActivity3 == null) {
                                                return;
                                            }

                                            loadingDialog.dismissDialog();

                                            // Finishing the activity means there is no need to reactivate clickable
                                            // views.
                                            baseActivity3.finish();
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
        alertDialog.setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();

        updateGroupSettings();
    }
}
