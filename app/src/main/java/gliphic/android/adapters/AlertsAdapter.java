/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.adapters;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import gliphic.android.R;
import gliphic.android.display.CreateGroupActivity;
import gliphic.android.display.abstract_views.BaseMainActivity;
import gliphic.android.display.libraries.AlertDialogs;
import gliphic.android.display.libraries.DateTime;
import gliphic.android.display.libraries.EditTextImeOptions;
import gliphic.android.display.libraries.LoadingDialog;
import gliphic.android.display.main.MainActivity;
import gliphic.android.exceptions.GroupException;
import gliphic.android.exceptions.NoStoredObjectException;
import gliphic.android.operation.Alerts;
import gliphic.android.operation.Group;
import gliphic.android.operation.misc.Log;
import gliphic.android.operation.server_interaction.http_server.HttpOperations;
import gliphic.android.operation.server_interaction.http_server.RequestGlobalStatic;
import gliphic.android.operation.storage_handlers.IsStoredAndDataObject;
import gliphic.android.operation.storage_handlers.SharedPreferencesHandler;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.DecoderException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import libraries.Base256;
import libraries.BouncyCastleInterpreter;
import libraries.GeneralUtils;
import libraries.Vars;
import libraries.Vars.GroupShareStatus;
import pojo.account.GroupShare;
import pojo.group.AcceptGroupRequest;
import pojo.group.GetPubEncGroupKeyResponse;
import pojo.load.LoadChosenContactsRequest;
import pojo.load.LoadChosenContactsResponse;
import pojo.load.LoadContactObject;
import pojo.load.LoadGroupObject;
import pojo.misc.AccessTokenAndGroupNumber;

import static libraries.Vars.GroupShareStatus.PENDING_RECEIVED;

/**
 * The adapter for a list of Contacts.
 *
 * Facilitates creating a RecyclerView to list a user's Contacts.
 */
public class AlertsAdapter extends RecyclerView.Adapter<AlertsAdapter.AlertsItemViewHolder> {
    private IsStoredAndDataObject statusAndDeviceKey = null;

    public static final String GENERIC_LOAD_ALERTS_FAILED_MSG = "Unable to load alerts at this time.";

    public static final int MAX_NUM_OF_ITEMS_APPENDED = 50;

    private int maxNumOfItemsDisplayed = MAX_NUM_OF_ITEMS_APPENDED;

    // All group-shares available to this adapter before any filters are applied.
    // Note that this list is not sorted.
    private List<GroupShare> completeList = new ArrayList<>();

    // The list of group-shares available after all filters are applied.
    // Note that this list should be sorted.
    private List<GroupShare> filteredList = new ArrayList<>();

    // The list of group-shares to display after all filters are applied and the item display limit is applied.
    // Note that this list should be sorted.
    private List<GroupShare> displayedList = new ArrayList<>();

    // Filter(s) for this adapter.
    private boolean pendingReceivedOnlyIsChecked;

    /**
     * Constructor for initialising the adapter with group-share requests sent and received by this contact.
     *
     * @param shareList                     The list of loaded group-shares for this contact. If this is null it is
     *                                      assumed that the app could not load any groups from either the local cache
     *                                      or from the server.
     * @param pendingReceivedOnlyIsChecked  Set to true iff the only show pending-received alerts CheckBox is selected.
     */
    public AlertsAdapter(List<GroupShare> shareList, boolean pendingReceivedOnlyIsChecked) {
        this.pendingReceivedOnlyIsChecked = pendingReceivedOnlyIsChecked;

        // Assume that the calling method handles the else-case.
        if (!isNullOrContainsNulls(shareList)) {
            verifyPendingAndCompletedShares(shareList);

            completeList.addAll(shareList);
            setFilteredList(true);
            setDisplayedList(true);
        }
    }

    class AlertsItemViewHolder extends RecyclerView.ViewHolder {
        private TextView groupShareMessage;
        private Button   acceptButton;
        private Button   declineButton;

        private AlertsItemViewHolder(View view) {
            super(view);

            this.groupShareMessage = view.findViewById(R.id.alert_item_msg);
            this.acceptButton      = view.findViewById(R.id.alert_item_accept);
            this.declineButton     = view.findViewById(R.id.alert_item_decline);

            acceptButton.setOnClickListener(v -> {
                final GroupShare            groupShare    = getGroupShareItem(getAdapterPosition());
                final long                  groupNumber   = groupShare.getGroup().getNumber();
                final BaseMainActivity      activity      = (BaseMainActivity) view.getContext();

                if (CreateGroupActivity.isGroupsLimitReached(activity)) {
                    return;
                }

                // Ask the contact to choose a description for the group.

                // Ignore the warning; the AlertDialog should have a null parent because it does not have a root view.
                @SuppressLint("InflateParams")
                final View alertDialogView = activity.getLayoutInflater().inflate(R.layout.edittext_view, null);

                final EditText editText = alertDialogView.findViewById(R.id.alertdialog_edittext);

                EditTextImeOptions.setEnterListener(activity, editText, null);

                // Required to combine the 'text' and 'textMultiLine' input types (set in XML).
                editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
                editText.setRawInputType(InputType.TYPE_CLASS_TEXT);

                final String message = "Enter a unique description for this group:";
                final AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
                activity.setMAlertDialog(alertDialog);

                alertDialog.setTitle("Group description required");
                alertDialog.setMessage(message);
                alertDialog.setView(alertDialogView);
                alertDialog.setButton(
                        AlertDialog.BUTTON_NEGATIVE,
                        activity.getResources().getString(android.R.string.cancel),
                        (dialog, which) -> {
                            dialog.dismiss();
                            if (alertDialog.equals(activity.getMAlertDialog())) {
                                activity.setMAlertDialog(null);
                            }
                        });
                alertDialog.setButton(
                        AlertDialog.BUTTON_POSITIVE,
                        activity.getResources().getString(android.R.string.ok),
                        (dialog, which) -> {
                            // Do nothing here since this method is overridden below.
                        });
                alertDialog.setCancelable(false);
                alertDialog.setCanceledOnTouchOutside(false);
                alertDialog.show();

                // Prevent the AlertDialog from closing automatically after the button is clicked by using this manual
                // override.
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v1 -> {
                    final String groupDescription = editText.getText().toString();

                    try {
                        Group.checkValidDescription(groupDescription, true);
                    }
                    catch (GroupException e) {
                        // Set the error message text and color.

                        SpannableStringBuilder ssBuilder = new SpannableStringBuilder(message + "\n\n");

                        int spanStart = ssBuilder.length();
                        ssBuilder.append(e.getMessage());
                        int spanEnd = ssBuilder.length();
                        ssBuilder.setSpan(
                                new ForegroundColorSpan(ContextCompat.getColor(activity, R.color.colorAccent)),
                                spanStart,
                                spanEnd,
                                0
                        );

                        alertDialog.setMessage(ssBuilder);

                        // Perform the custom shake animation on the EditText view.

                        editText.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.shake));

                        return;
                    }

                    // The group description should be valid at this point.

                    alertDialog.dismiss();
                    if (alertDialog.equals(activity.getMAlertDialog())) {
                        activity.setMAlertDialog(null);
                    }

                    activity.deactivateClickableViews();

                    final LoadingDialog loadingDialog = new LoadingDialog(activity);

                    RequestGlobalStatic.requestAndSetAccessToken(
                            accessToken -> {
                                if (accessToken == null) {
                                    activity.activateClickableViews();
                                    return;
                                }

                                final AccessTokenAndGroupNumber atagn = new AccessTokenAndGroupNumber(
                                        accessToken,
                                        groupNumber
                                );

                                HttpOperations.post(
                                        HttpOperations.URI_GROUP_ACCEPT_GET_KEY,
                                        atagn,
                                        activity,
                                        response1 -> {
                                            final GetPubEncGroupKeyResponse gpegkr;
                                            final byte[] dataEncryptionKey;
                                            final byte[] privateKey;
                                            final byte[] groupKey;

                                            try {
                                                gpegkr = GeneralUtils.fromJson(
                                                        response1,
                                                        GetPubEncGroupKeyResponse.class
                                                );

                                                dataEncryptionKey =
                                                        SharedPreferencesHandler.getDataEncryptionKey(activity);

                                                privateKey = BouncyCastleInterpreter.aesOperate(
                                                        false,
                                                        Base64.decode(gpegkr.getEncryptedPrivateKeyString()),
                                                        Base64.decode(gpegkr.getPrivateKeyIvString()),
                                                        dataEncryptionKey
                                                );
                                            }
                                            catch ( NoStoredObjectException | IOException | GeneralSecurityException |
                                                    DecoderException | InvalidCipherTextException |
                                                    NullPointerException e ) {

                                                handleFailedAcceptedRequest(loadingDialog, activity);
                                                return;
                                            }

                                            /*
                                             * If the group key was encrypted by the source contact using an invalid
                                             * public key then this contact will be unable to decrypt it and an
                                             * exception will be thrown. This failure case is handled separately to
                                             * internal errors as this is (likely to be) a malicious share request.
                                             *
                                             * See the main design document for more details.
                                             */
                                            try {
                                                groupKey = BouncyCastleInterpreter.rsaOperate(
                                                        false,
                                                        Base64.decode(gpegkr.getEncryptedGroupKeyString()),
                                                        privateKey
                                                );
                                            }
                                            catch (IOException | InvalidCipherTextException e) {
                                                HttpOperations.post(
                                                        HttpOperations.URI_GROUP_ACCEPT_FAILED,
                                                        atagn,
                                                        activity,
                                                        response2 -> {
                                                            // Remove the GroupShare object and display (item).

                                                            loadingDialog.dismissDialog();

                                                            Alerts.safeRemoveGroupShare(groupShare);

                                                            removeGroupShareAndUpdateDisplay(groupShare);

                                                            MainActivity.sendBroadcastToUpdateAlertsTab(activity);

                                                            String s = "You were unable to join group %s (group ID " +
                                                                    "%s) due to %s (contact ID %s) supplying " +
                                                                    "invalid group information. " +
                                                                    "You must receive another share request before " +
                                                                    "you can join the group.";
                                                            AlertDialogs.genericConfirmationDialog(
                                                                    activity,
                                                                    "Accept request failed",
                                                                    String.format(
                                                                            s,
                                                                            groupShare.getGroup().getName(),
                                                                            Base256.fromBase64(
                                                                                    groupShare.getGroup().getIdBase64()
                                                                            ),
                                                                            groupShare.getContact().getName(),
                                                                            groupShare.getContact().getId()
                                                                    )
                                                            );

                                                            activity.activateClickableViews();
                                                        },
                                                        error2 -> {
                                                            loadingDialog.dismissDialog();

                                                            HttpOperations.handleStandardRequestOnErrorResponse(
                                                                    error2,
                                                                    activity,
                                                                    false
                                                            );

                                                            activity.activateClickableViews();
                                                        }
                                                );

                                                return;
                                            }

                                            // Continue with accepting the group share request.

                                            // Get the stored device (account recovery) key.
                                            try {
                                                // Assume that the application key already exists.

                                                statusAndDeviceKey = SharedPreferencesHandler.getDeviceKey(
                                                        activity,
                                                        false
                                                );
                                            }
                                            catch ( IOException | GeneralSecurityException | DecoderException |
                                                    NullPointerException | NoStoredObjectException e ) {

                                                handleFailedAcceptedRequest(loadingDialog, activity);
                                                return;
                                            }

                                            final AcceptGroupRequest acceptGroupRequest;
                                            try {
                                                final byte[] groupKeyIv         = GeneralUtils.generateIv();
                                                final byte[] recoveryGroupKeyIv = GeneralUtils.generateIv();

                                                final byte[] encryptedGroupKey = BouncyCastleInterpreter.aesOperate(
                                                        true,
                                                        groupKey,
                                                        groupKeyIv,
                                                        dataEncryptionKey
                                                );

                                                final byte[] recovEncGroupKey = BouncyCastleInterpreter.aesOperate(
                                                        true,
                                                        groupKey,
                                                        recoveryGroupKeyIv,
                                                        statusAndDeviceKey.getData()
                                                );

                                                // The device key is no longer required.
                                                statusAndDeviceKey = null;

                                                acceptGroupRequest = new AcceptGroupRequest(
                                                        accessToken,
                                                        groupNumber,
                                                        groupDescription,
                                                        Base64.toBase64String(encryptedGroupKey),
                                                        Base64.toBase64String(groupKeyIv),
                                                        Base64.toBase64String(recovEncGroupKey),
                                                        Base64.toBase64String(recoveryGroupKeyIv)
                                                );
                                            }
                                            catch (InvalidCipherTextException e) {
                                                handleFailedAcceptedRequest(loadingDialog, activity);
                                                return;
                                            }

                                            HttpOperations.post(
                                                    HttpOperations.URI_GROUP_ACCEPT_CONFIRM,
                                                    acceptGroupRequest,
                                                    activity,
                                                    response2 -> {
                                                        // Attempt to request the target contacts for the received
                                                        // group, and store these and the received group.
                                                        // If storing target contacts fails then store only the group.
                                                        // If storing the group fails then still display a success
                                                        // message since the contact has still successfully joined the
                                                        // group at this point.

                                                        final LoadGroupObject lgo = GeneralUtils.fromJson(
                                                                response2,
                                                                LoadGroupObject.class
                                                        );

                                                        final long[] groupContacts =
                                                                lgo.getTargetContactNumbersNullSafe();

                                                        if (groupContacts.length == 0) {
                                                            handleSuccessfulAcceptedRequest(
                                                                    loadingDialog,
                                                                    activity,
                                                                    groupShare,
                                                                    lgo,
                                                                    null
                                                            );

                                                            return;
                                                        }

                                                        HttpOperations.post(
                                                                HttpOperations.URI_LOAD_CHOSEN_CONTACTS,
                                                                new LoadChosenContactsRequest(
                                                                        accessToken,
                                                                        groupContacts
                                                                ),
                                                                activity,
                                                                response3 -> {
                                                                    final LoadChosenContactsResponse lccr =
                                                                            GeneralUtils.fromJson(
                                                                                    response3,
                                                                                    LoadChosenContactsResponse.class
                                                                            );

                                                                    List<LoadContactObject> targetContacts =
                                                                            new ArrayList<>();

                                                                    targetContacts.addAll(
                                                                            lccr.getKnownContacts()
                                                                    );
                                                                    targetContacts.addAll(
                                                                            lccr.getExtendedContacts()
                                                                    );

                                                                    handleSuccessfulAcceptedRequest(
                                                                            loadingDialog,
                                                                            activity,
                                                                            groupShare,
                                                                            lgo,
                                                                            targetContacts
                                                                    );
                                                                },
                                                                error3 -> {
                                                                    logError(
                                                                            lgo.getNumber(),
                                                                            String.format(
                                                                                    "Error response from %s",
                                                                                    HttpOperations
                                                                                            .URI_LOAD_CHOSEN_CONTACTS
                                                                            )
                                                                    );

                                                                    handleSuccessfulAcceptedRequest(
                                                                            loadingDialog,
                                                                            activity,
                                                                            groupShare,
                                                                            lgo,
                                                                            null
                                                                    );
                                                                }
                                                        );
                                                    },
                                                    error2 -> {
                                                        loadingDialog.dismissDialog();

                                                        HttpOperations.handleAddGroupOnErrorResponse(
                                                                null,
                                                                error2,
                                                                activity
                                                        );

                                                        activity.activateClickableViews();
                                                    }
                                            );
                                        },
                                        error1 -> {
                                            loadingDialog.dismissDialog();

                                            HttpOperations.handleStandardRequestOnErrorResponse(
                                                    error1,
                                                    activity,
                                                    false
                                            );

                                            activity.activateClickableViews();
                                        }
                                );
                            },
                            activity,
                            loadingDialog,
                            false
                    );
                });
            });

            declineButton.setOnClickListener(v -> {
                final GroupShare       groupShare  = getGroupShareItem(getAdapterPosition());
                final long             groupNumber = groupShare.getGroup().getNumber();
                final BaseMainActivity activity    = (BaseMainActivity) view.getContext();

                activity.deactivateClickableViews();

                final LoadingDialog loadingDialog = new LoadingDialog(activity);

                RequestGlobalStatic.requestAndSetAccessToken(
                        accessToken -> {
                            if (accessToken == null) {
                                activity.activateClickableViews();
                                return;
                            }

                            final AccessTokenAndGroupNumber atagn = new AccessTokenAndGroupNumber(
                                    accessToken,
                                    groupNumber
                            );

                            HttpOperations.post(
                                    HttpOperations.URI_GROUP_DECLINE_CONFIRM,
                                    atagn,
                                    activity,
                                    response -> {
                                        // Remove the GroupShare object and display (item).

                                        loadingDialog.dismissDialog();

                                        Alerts.safeRemoveGroupShare(groupShare);

                                        removeGroupShareAndUpdateDisplay(groupShare);

                                        MainActivity.sendBroadcastToUpdateAlertsTab(activity);

                                        activity.activateClickableViews();
                                    },
                                    error -> {
                                        loadingDialog.dismissDialog();

                                        HttpOperations.handleStandardRequestOnErrorResponse(
                                                error,
                                                activity,
                                                false
                                        );

                                        activity.activateClickableViews();
                                    }
                            );
                        },
                        activity,
                        loadingDialog,
                        false
                );
            });
        }

        private void handleSuccessfulAcceptedRequest(@NonNull LoadingDialog loadingDialog,
                                                     @NonNull BaseMainActivity activity,
                                                     @NonNull GroupShare groupShare,
                                                     @NonNull LoadGroupObject loadGroupObject,
                                                     @Nullable final List<LoadContactObject> loadContactObjectList) {

            /* Store the group and any available contacts. */

            try {
                if (loadContactObjectList == null) {
                    RequestGlobalStatic.storeGroup(loadGroupObject);
                }
                else {
                    RequestGlobalStatic.storeGroupAndTargetContacts(loadGroupObject, loadContactObjectList);
                }
            }
            catch (GroupException e) {
                logErrorOnLoadGroupFailure(loadGroupObject, e);
            }

            /* Update the GroupShare object and display (item). */

            // Assume that the GroupShare object is statically stored in the relevant global list.
            groupShare.setShareStatus(GroupShareStatus.SUCCESS_RECEIVED);

            updateItem(groupShare);

            MainActivity.sendBroadcastToUpdateAlertsTab(activity);
            MainActivity.sendBroadcastToUpdateGroupsTab(activity);

            /* Handle displayed views. */

            loadingDialog.dismissDialog();

            AlertDialogs.genericConfirmationDialog(
                    activity,
                    "Accept request successful",
                    String.format(
                            "You have successfully joined group %s.",
                            // The LoadGroupObject should be non-null but it is checked just to be safe.
                            loadGroupObject != null ? loadGroupObject.getName() : groupShare.getGroup().getName()
                    )
            );

            activity.activateClickableViews();
        }

        private void handleFailedAcceptedRequest(@NonNull LoadingDialog loadingDialog,
                                                 @NonNull BaseMainActivity activity) {

            statusAndDeviceKey = null;

            loadingDialog.dismissDialog();

            AlertDialogs.genericConfirmationDialog(
                    activity,
                    "Accept request failed",
                    "Unable to accept the group request at this time."
            );

            activity.activateClickableViews();
        }

        private void handleFailedDeclinedRequest(@NonNull LoadingDialog loadingDialog,
                                                 @NonNull BaseMainActivity activity) {

            loadingDialog.dismissDialog();

            AlertDialogs.genericConfirmationDialog(
                    activity,
                    "Decline request failed",
                    "Unable to decline the group request at this time."
            );

            activity.activateClickableViews();
        }

        private void logErrorOnLoadGroupFailure(@NonNull LoadGroupObject loadGroupObject, @NonNull GroupException e) {
            logError(
                    loadGroupObject.getNumber(),
                    "Exception message: " + e.getMessage()
            );
        }

        private void logError(long groupNumber, @Nullable String logMessageSuffix) {
            // Do not display an error message since the group was successfully joined but unsuccessfully loaded.
            String msg = "Successfully joined group %d but unable to load it. %s";
            Log.e("Accept group request", String.format(msg, groupNumber, logMessageSuffix));
        }
    }

    @Override
    @NonNull
    public AlertsItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alert, parent, false);
        return new AlertsItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlertsItemViewHolder groupShareItemViewHolder, int position) {
        GroupShare groupShareItem = getGroupShareItem(position);

        String prefix;
        String middle;  // One of LoadContactObject or LoadGroupObject is null iff this string is set to null below.
        String suffix;
        boolean displayContactBeforeGroup;

        GroupShareStatus shareStatus = groupShareItem.getShareStatus();

        switch (shareStatus) {
            case PENDING_RECEIVED:
                prefix = "[%s] Pending:\nYou have received a request from ";
                middle = " to join group ";
                suffix = ".";
                displayContactBeforeGroup = true;
                break;
            case PENDING_SENT:
                prefix = "[%s] Pending:\nYou have sent a request to ";
                middle = " to join group ";
                suffix = " and are awaiting their response.";
                displayContactBeforeGroup = true;
                break;
            case SUCCESS_RECEIVED:
                // A NullPointerException is thrown when attempting to access LoadContactObject members for this case.
                prefix = "[%s] Successful:\nYou joined group ";
                middle = null;
                suffix = ".";
                displayContactBeforeGroup = false;
                break;
            case SUCCESS_SENT:
                prefix = "[%s] Successful:\nYou shared group ";
                middle = " with ";
                suffix = ".";
                displayContactBeforeGroup = false;
                break;
            case FAILED_SENT:
                prefix = "[%s] Failed:\nYour attempt to share group ";
                middle = " with ";
                suffix = " failed. This could be the result of malicious activity using your account.";
                displayContactBeforeGroup = false;
                break;
            case FAILED_RECEIVED:
                throw new IllegalArgumentException(String.format("Invalid share status '%s'.", shareStatus.name()));
            default:
                throw new IllegalArgumentException(String.format("Unknown share status '%s'.", shareStatus.name()));
        }

        String contactIdMsg = "(contact ID %s)";
        String groupIdMsg   = "(group ID %s)";

        String stringFormat1;
        String objectName1;
        String objectId1;
        // Setting these to null is only to prevent the dumb compiler error: "... might not have been initialized".
        String stringFormat2 = null;
        String objectName2   = null;
        String objectId2     = null;

        if (displayContactBeforeGroup) {
            stringFormat1 = contactIdMsg;
            objectName1   = groupShareItem.getContact().getName();
            objectId1     = groupShareItem.getContact().getId();
        }
        else {
            stringFormat1 = groupIdMsg;
            objectName1   = groupShareItem.getGroup().getName();
            objectId1     = Base256.fromBase64(groupShareItem.getGroup().getIdBase64());
        }

        if (middle != null) {   // Avoid a NullPointerException when attempting to access undefined members.
            if (displayContactBeforeGroup) {
                stringFormat2 = groupIdMsg;
                objectName2   = groupShareItem.getGroup().getName();
                objectId2     = Base256.fromBase64(groupShareItem.getGroup().getIdBase64());
            }
            else {
                stringFormat2 = contactIdMsg;
                objectName2   = groupShareItem.getContact().getName();
                objectId2     = groupShareItem.getContact().getId();
            }
        }

        SpannableStringBuilder ssBuilder = new SpannableStringBuilder(String.format(
                prefix,
                DateTime.getDateTime(groupShareItem.getShareTime())
        ));

        int spanStart = ssBuilder.length();
        ssBuilder.append(objectName1);
        int spanEnd = ssBuilder.length();
        ssBuilder.setSpan(new StyleSpan(Typeface.ITALIC), spanStart, spanEnd, 0);

        ssBuilder.append(" ");

        spanStart = ssBuilder.length();
        ssBuilder.append(String.format(stringFormat1, objectId1));
        spanEnd = ssBuilder.length();
        ssBuilder.setSpan(new ForegroundColorSpan(Color.DKGRAY), spanStart, spanEnd, 0);

        if (middle != null) {   // Only display a secondary object's name and ID if it is available.
            ssBuilder.append(middle);

            spanStart = ssBuilder.length();
            ssBuilder.append(objectName2);
            spanEnd = ssBuilder.length();
            ssBuilder.setSpan(new StyleSpan(Typeface.ITALIC), spanStart, spanEnd, 0);

            ssBuilder.append(" ");

            spanStart = ssBuilder.length();
            ssBuilder.append(String.format(stringFormat2, objectId2));
            spanEnd = ssBuilder.length();
            ssBuilder.setSpan(new ForegroundColorSpan(Color.DKGRAY), spanStart, spanEnd, 0);
        }

        ssBuilder.append(suffix);

        groupShareItemViewHolder.groupShareMessage.setText(ssBuilder);

        // Hide views depending on the share type.
        if (groupShareItem.getShareStatus().equals(PENDING_RECEIVED)) {
            groupShareItemViewHolder.itemView.findViewById(R.id.alert_item_action_layout).setVisibility(View.VISIBLE);
        }
        else {
            groupShareItemViewHolder.itemView.findViewById(R.id.alert_item_action_layout).setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return (displayedList != null ? displayedList.size() : 0);
    }

    /**
     * Get the total filtered number of group-shares stored in this adapter.
     *
     * @return  Return the size of the filtered group-shares list, if the list is null then return 0.
     */
    public int getFilteredItemCount() {
        return (filteredList != null ? filteredList.size() : 0);
    }

    /**
     * Get the total unfiltered number of group-shares stored in this adapter.
     *
     * @return  Return the size of the complete group-shares list, if the list is null then return 0.
     */
    public int getTotalItemCount() {
        return (completeList != null ? completeList.size() : 0);
    }

    private boolean isNullOrContainsNulls(List<GroupShare> shareList) {
        if (shareList == null) {
            return true;
        }

        for (GroupShare groupShare : shareList) {
            if (groupShare == null) {
                return true;
            }
        }

        return false;
    }

    /**
     * Return all of the item counts from this adapter instance required to request more alerts from the server.
     *
     * Note that this method should not throw an exception since all objects in the complete list should have already
     * had their share statuses verified.
     *
     * @see #verifyPendingAndCompletedShares(List)
     *
     * @return  Return counts for all objects stored by this adapter with the following group-share statuses:
     *          PENDING_RECEIVED, PENDING_SENT, SUCCESS_RECEIVED and SUCCESS_SENT/FAILED_SENT.
     */
    public GroupShareItemCounts verifyPendingAndCompletedShares() {
        return verifyPendingAndCompletedShares(completeList);
    }

    private GroupShareItemCounts verifyPendingAndCompletedShares(@NonNull List<GroupShare> shareList) {
        GroupShareItemCounts groupShareItemCounts = new GroupShareItemCounts();

        for (GroupShare groupShare : shareList) {
            switch (groupShare.getShareStatus()) {
                case PENDING_RECEIVED:
                    groupShareItemCounts.incrementPendingReceivedItemCount();
                    break;
                case PENDING_SENT:
                    groupShareItemCounts.incrementPendingSentItemCount();
                    break;
                case SUCCESS_RECEIVED:
                    groupShareItemCounts.incrementSuccessReceivedItemCount();
                    break;
                case SUCCESS_SENT:
                case FAILED_SENT:
                    groupShareItemCounts.incrementSuccessAndFailedSentItemCount();
                    break;
                default:
                    String exceptionMessage;

                    if (groupShare.getShareStatus().equals(Vars.GroupShareStatus.FAILED_RECEIVED)) {
                        exceptionMessage = "Invalid share status '%s'.";
                    }
                    else {
                        exceptionMessage = "Unknown share status '%s'.";
                    }

                    throw new IllegalArgumentException(String.format(
                            exceptionMessage,
                            groupShare.getShareStatus().name()
                    ));
            }
        }

        return groupShareItemCounts;
    }

    /**
     * Return the group-share at the given position in the displayed list.
     *
     * @param position  The zero-indexed position of the group-share to return.
     * @return          The group-share stored at the given position in the displayed list.
     */
    private GroupShare getGroupShareItem(int position) {
        return displayedList.get(position);
    }

    /**
     * Return the position in the displayed list which corresponds to the given group-share.
     *
     * @param groupShare    The group-share to find in the displayed list.
     * @return              Either the (non-negative) position or -1 if the group-share does not correspond with any of
     *                      the displayed items.
     */
    private int getItemPositionFromGroupShare(GroupShare groupShare) {
        for (int i = 0; i < displayedList.size(); i++) {
            if (displayedList.get(i).equals(groupShare)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Return a boolean depending on whether the given integer represents a valid position in the displayed items list.
     *
     * @param itemPosition  The position of the item in the displayed list. This must be obtained via the method:
     *                      @see #getItemPositionFromGroupShare(GroupShare)
     * @return              True iff the item position is valid.
     */
    private boolean isValidItemPosition(int itemPosition) {
        return itemPosition >= 0 && itemPosition <= displayedList.size();
    }

    /**
     * Update the group-share in the display using the given object. This method does not sort the displayed items by
     * share-time so if the share-time is updated items may be out of order until the RecyclerView is refreshed.
     *
     * No structural changes are made in this method, only item changes.
     *
     * @param groupShare    The object to update.
     *                      If the item matching this object is not displayed this method does nothing.
     */
    private void updateItem(@NonNull GroupShare groupShare) {
        final boolean removeFilteredItem =
                pendingReceivedOnlyIsChecked && !Alerts.isActionableGroupShare(groupShare.getShareStatus());

        // The item position might have changed so find it again (before removing the object from lists).
        final int itemPosition = getItemPositionFromGroupShare(groupShare);

        if (removeFilteredItem) {
            filteredList.remove(groupShare);
            displayedList.remove(groupShare);
        }

        if (isValidItemPosition(itemPosition)) {
            if (removeFilteredItem) {
                maxNumOfItemsDisplayed--;

                notifyItemRemoved(itemPosition);
            }
            else {
                notifyItemChanged(itemPosition);
            }
        }
    }

    /**
     * @see #removeGroupShareAndUpdateDisplay(List)
     */
    private void removeGroupShareAndUpdateDisplay(@Nullable GroupShare groupShare) {
        if (groupShare == null) {
            return;
        }

        removeGroupShareAndUpdateDisplay(Collections.singletonList(groupShare));
    }

    /**
     * Remove a list of GroupShare objects from all lists in this adapter instance and remove the corresponding items
     * from the display.
     *
     * If the given list is null, do nothing.
     * If an object within the list is null, ignore it and proceed to the next object.
     *
     * @param groupShares   The objects/items to remove from the adapter lists/display.
     */
    public void removeGroupShareAndUpdateDisplay(@Nullable List<GroupShare> groupShares) {
        if (groupShares == null) {
            return;
        }

        for (GroupShare gs : groupShares) {
            if (gs == null) {
                continue;
            }

            // The item position might have changed so find it again (before removing the object from lists).
            final int itemPosition = getItemPositionFromGroupShare(gs);

            completeList.remove(gs);
            filteredList.remove(gs);
            displayedList.remove(gs);

            if (!isValidItemPosition(itemPosition)) {
                continue;
            }

            maxNumOfItemsDisplayed--;

            notifyItemRemoved(itemPosition);
        }
    }

    /**
     * Add the given list of objects to the complete adapter list and prepend objects matching the filter to the
     * display.
     *
     * This method does not sort the new display items with the old display items but blindly prepends them.
     *
     * @param groupShareList    The list of objects to add.
     */
    public void prepend(@NonNull List<GroupShare> groupShareList) {
        completeList.addAll(0, groupShareList);

        List<GroupShare> newDisplayedGroupShares = new ArrayList<>();

        for (GroupShare groupShare : groupShareList) {
            if (!pendingReceivedOnlyIsChecked || Alerts.isActionableGroupShare(groupShare.getShareStatus())) {
                newDisplayedGroupShares.add(groupShare);
            }
        }

        if (newDisplayedGroupShares.isEmpty()) {
            return;
        }

        Alerts.reverseSortByShareTime(newDisplayedGroupShares);

        if (newDisplayedGroupShares.size() > MAX_NUM_OF_ITEMS_APPENDED) {
            modify(true, true, new ArrayList<>(completeList));
        }
        else {
            maxNumOfItemsDisplayed += newDisplayedGroupShares.size();

            filteredList.addAll(0, newDisplayedGroupShares);
            displayedList.addAll(0, newDisplayedGroupShares);

            notifyItemRangeInserted(0, newDisplayedGroupShares.size());
        }
    }

    /**
     * Filter and set the list of group-shares shown based on the status of the CheckBox selected.
     *
     * Also sort this list using this adapter's comparator.
     *
     * @param sortList  True to sort the filtered list by group share time, false to not sort.
     */
    private void setFilteredList(boolean sortList) {
        filteredList.clear();

        if (pendingReceivedOnlyIsChecked) {
            for (GroupShare groupShare : completeList) {
                if (groupShare.getShareStatus().equals(PENDING_RECEIVED)) {
                    filteredList.add(groupShare);
                }
            }
        }
        else {
            filteredList.addAll(completeList);
        }

        if (sortList) {
            Alerts.reverseSortByShareTime(filteredList);
        }
    }

    /**
     * @see #setDisplayedList(int, int, boolean)
     */
    private void setDisplayedList(boolean sortList) {
        setDisplayedList(0, Math.min(filteredList.size(), maxNumOfItemsDisplayed), sortList);
    }

    /**
     * Set the list of displayed group-shares by taking a sublist of the filtered list with the given int indexes.
     *
     * Also sort this list using this adapter's comparator.
     *
     * @param sublistFromIndex  The fromIndex supplied to the subList() method called on the filtered list.
     * @param sublistToIndex    The toIndex supplied to the subList() method called on the filtered list.
     * @param sortList          True to sort the filtered list by group share time, false to not sort.
     */
    private void setDisplayedList(int sublistFromIndex, int sublistToIndex, boolean sortList) {
        displayedList.clear();
        displayedList.addAll(filteredList.subList(sublistFromIndex, sublistToIndex));

        if (sortList) {
            Alerts.reverseSortByShareTime(displayedList);
        }
    }

    /**
     * Reset the maximum number of items on display.
     */
    private void resetMaxDisplaySize() {
        maxNumOfItemsDisplayed = MAX_NUM_OF_ITEMS_APPENDED;
    }

    /**
     * Clear the adapter by clearing all lists and removing all items bound to the view holder.
     */
    public void clear() {
        resetMaxDisplaySize();

        pendingReceivedOnlyIsChecked = true;

        completeList.clear();
        filteredList.clear();
        displayedList.clear();

        notifyDataSetChanged();
    }

    /**
     * Update a RecyclerView to show a filtered list of items using the given CheckBox filter.
     *
     * A maximum of maxNumOfItemsDisplayed will be displayed.
     *
     * @param pendingReceivedOnly   The boolean status of the pending-received only CheckBox to filter items with.
     * @return                      Return true if a server request is necessary after this method terminates,
     *                              false otherwise.
     */
    public boolean update(boolean pendingReceivedOnly) {
        this.pendingReceivedOnlyIsChecked = pendingReceivedOnly;

        setFilteredList(false);

        // Assume that no device shows at least MAX_NUM_OF_ITEMS_APPENDED items on one screen.
        // Displaying fewer than this number implies that more items should be requested from the server.

        boolean serverRequestRequired = false;

        final int numOfDisplayableItems = filteredList
                .subList(0, Math.min(filteredList.size(), maxNumOfItemsDisplayed))
                .size();

        if (numOfDisplayableItems < MAX_NUM_OF_ITEMS_APPENDED) {
            serverRequestRequired = true;
        }

        // Always reset the maximum number of items which are initially displayed to allow the view to be smoothly
        // redrawn, regardless of if a server request is required after this method returns.

        resetMaxDisplaySize();

        setDisplayedList(false);

        notifyDataSetChanged();

        return serverRequestRequired;
    }

    /**
     * Modify a RecyclerView to show a either append, filter or reset a given list of group-shares.
     *
     * The stored filterSearch string is applied to this new list.
     *
     * To achieve the following goals for the given list, set the following boolean values:
     * ----------------------------------------------------------
     *            resetAdapterLists | resetDisplayedItemCount   |
     * * Append :   false           |   false                   |
     * * Filter :   false           |   true                    |
     * * Reset  :   true            |   true                    |
     *
     * @param resetAdapterLists         Set to true to reset the entire data set or false to add group-shares to the
     *                                  existing complete list.
     * @param resetDisplayedItemCount   True to reset the items displayed or false to append to the display.
     * @param newCompleteList           The list of group-shares to display in the RecyclerView.
     */
    public void modify(boolean resetAdapterLists,
                       boolean resetDisplayedItemCount,
                       @Nullable List<GroupShare> newCompleteList) {

        if (isNullOrContainsNulls(newCompleteList)) {
            return;
        }

        if (newCompleteList != completeList) {
            verifyPendingAndCompletedShares(newCompleteList);

            // Either reset the entire data set or append to the existing display.
            if (resetAdapterLists) {
                completeList.clear();
                completeList.addAll(newCompleteList);
            }
            else {
                // This is required for the notifyItemRangeInserted method called in the append method.
                Alerts.appendNewGroupShares(completeList, newCompleteList);
            }
        }

        // Apply filters to the complete list of group-shares.
        // Also sort alerts if resetDisplayedItemCount is true.
        setFilteredList(resetDisplayedItemCount);

        // Either reset the displayed groups or append to the existing display.
        if (resetDisplayedItemCount) {
            resetMaxDisplaySize();

            setDisplayedList(true);

            notifyDataSetChanged();
        }
        else {
            append(0);
        }
    }

    /**
     * Append the maximum number of items possible to the RecyclerView display. If fewer than this maximum are
     * available to append then no items are appended and a server request should be made to acquire more.
     *
     * This method is typically called within a call to a RecyclerView's addOnScrollListener() method when a user has
     * scrolled near/to the bottom of the RecyclerView.
     *
     * @return  Returns true if a new server request should be made to get more alerts and false otherwise.
     */
    public boolean append() {
        // If MAX_NUM_OF_ITEMS_APPENDED (or more) items are available to append then they should be appended without
        // requiring a server request.
        return append(MAX_NUM_OF_ITEMS_APPENDED - 1);
    }

    /**
     * Append items to the RecyclerView display based on the given threshold.
     *
     * @param appendedItemCountThreshold    The number of items available to append must be larger than this number.
     *                                      If this is not positive, items will always be appended if they are loaded.
     * @return                              Returns true if a new server request should be made to get more alerts and
     *                                      false otherwise.
     *                                      If a server request has already been made, this return value can be safely
     *                                      ignored.
     */
    private boolean append(int appendedItemCountThreshold) {
        final int oldListSize = getItemCount();
        final int newListSize = Math.min(filteredList.size(), oldListSize + MAX_NUM_OF_ITEMS_APPENDED);
        final int appendedItemCount = newListSize - oldListSize;

        if (appendedItemCount > Math.max(0, appendedItemCountThreshold)) {
            maxNumOfItemsDisplayed += MAX_NUM_OF_ITEMS_APPENDED;

            setDisplayedList(0, newListSize, false);

            // This method is more efficient than notifyDataSetChanged() so use it when possible.
            notifyItemRangeInserted(oldListSize, appendedItemCount);

            return false;
        }
        else {
            // If the number of items available to append is not larger than the threshold, request more from the
            // server before updating the displayed list.
            return true;
        }
    }
}
