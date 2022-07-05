/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.display.main;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import gliphic.android.R;
import gliphic.android.adapters.GroupsAdapter;
import gliphic.android.display.abstract_views.BaseActivity;
import gliphic.android.display.GroupSelectionActivity;
import gliphic.android.display.abstract_views.BaseMainActivity;
import gliphic.android.display.abstract_views.BaseMainFragment;
import gliphic.android.display.browser.DecryptedJsonObject;
import gliphic.android.exceptions.ContactException;
import gliphic.android.exceptions.GroupException;
import gliphic.android.exceptions.NoStoredObjectException;
import gliphic.android.exceptions.UnknownGroupIdException;
import gliphic.android.interfaces.BooleanCallback;
import gliphic.android.display.libraries.AlertDialogs;
import gliphic.android.display.libraries.LoadingDialog;
import gliphic.android.display.libraries.RecyclerViewSetup;
import gliphic.android.exceptions.GroupKeyException;
import gliphic.android.operation.PublishedText;
import gliphic.android.exceptions.PublishedTextException;
import gliphic.android.operation.misc.ContactGroupAssociation;
import gliphic.android.operation.misc.Log;
import gliphic.android.operation.server_interaction.http_server.HttpOperations;
import gliphic.android.operation.server_interaction.http_server.RequestGlobalStatic;
import gliphic.android.operation.storage_handlers.SharedPreferencesHandler;

import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.DecoderException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import libraries.Base256Exception;
import libraries.BouncyCastleInterpreter;
import libraries.GeneralUtils;
import libraries.Vars;
import pojo.load.LoadChosenContactsRequest;
import pojo.load.LoadChosenContactsResponse;
import pojo.load.LoadContactObject;
import pojo.load.LoadGroupObject;
import pojo.load.LoadGroupsFromIdsRequest;
import pojo.load.LoadGroupsFromIdsResponse;
import pojo.misc.AccessTokenAndGroupNumber;
import pojo.text.GroupKeyResponse;
import pojo.text.TextDecryptRequest;
import pojo.text.TextDecryptRequestObject;
import pojo.text.TextDecryptResponse;
import pojo.text.TextDecryptResponseObject;
import pojo.text.TextEncryptRequest;
import pojo.text.TextEncryptResponse;

/**
 * The 'Workspace' tab fragment.
 *
 * This allows the user to perform basic encrypt/decrypt operations.
 */
public class WorkspaceTab extends BaseMainFragment {
    private View rootView;
    private GroupsAdapter selectedGroupAdapter;
    private RecyclerView recyclerView;

    private static final String GENERIC_ENCRYPT_FAILED_MSG =
            "Unable to encrypt text at this time.";
    private static final String GENERIC_DECRYPT_FAILED_MSG =
            "Unable to decrypt text at this time.";
    private static final String GROUP_INACTIVE_AND_ACCESS_DENIED_MSG =
            "You do not have permission to access the group which created this message, and it is currently inactive.";
    private static final String GROUP_INACTIVE_MSG =
            "The group which created this message is currently inactive; you must activate it to use it.";
    private static final String GROUP_ACCESS_DENIED_MSG =
            "You do not have permission to access the group which created this message.";
    private static final String BAD_PUBLISHED_MSG_MSG =
            "Check that the input message is not malformed.";
    private static final String TIME_OUT_EXPIRED_MSG =
            "The message time-out has expired.";

    @Override
    public void onNetworkAvailable(boolean isFirstOnNetworkAvailable) {
        new Handler(Looper.getMainLooper()).post(
                this::safeAdapterUpdate
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
        rootView = inflater.inflate(R.layout.main_tab_workspace, container, false);

        final MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity == null) {
            return rootView;
        }

        final EditText editTextEncrypt    = rootView.findViewById(R.id.edittext_encrypt);
        final EditText editTextDecrypt    = rootView.findViewById(R.id.edittext_decrypt);
        final Button   buttonEncrypt      = rootView.findViewById(R.id.btn_encrypt);
        final Button   buttonEncryptShare = rootView.findViewById(R.id.btn_encrypt_and_copy);
        final Button   buttonDecrypt      = rootView.findViewById(R.id.btn_decrypt);
        final Button   buttonDecryptShare = rootView.findViewById(R.id.btn_decrypt_and_copy);

        mainActivity.addToAllClickableViews(Arrays.asList(
                rootView.findViewById(R.id.recyclerview_main_tab_selected_group),
                editTextEncrypt,
                editTextDecrypt,
                buttonEncrypt,
                buttonEncryptShare,
                buttonDecrypt,
                buttonDecryptShare
        ));

        // This is necessary when data is shared to the parent activity and the data needs to be displayed in this
        // fragment.
        mainActivity.handleSharedText(mainActivity.getIntent(), editTextEncrypt, editTextDecrypt);

        /* Button listeners  */

        buttonEncrypt.setOnClickListener(v -> encryptText(
                isSuccessful -> {
                    // The callback boolean is not used.

                    mainActivity.activateClickableViews();
                },
                editTextEncrypt,
                editTextDecrypt
        ));

        buttonDecrypt.setOnClickListener(v -> decryptText(
                isSuccessful -> {
                    // The callback boolean is not used.

                    mainActivity.activateClickableViews();
                },
                (BaseActivity) getActivity(),
                editTextEncrypt,
                editTextDecrypt
        ));

        buttonEncryptShare.setOnClickListener(v -> encryptText(
                isSuccessful -> {
                    if (isSuccessful) {
                        // Assume that the editTextDecrypt has been updated.
                        String publishedText = editTextDecrypt.getText().toString();

                        Intent sendIntent = new Intent();
                        sendIntent.setAction(Intent.ACTION_SEND);
                        sendIntent.putExtra(Intent.EXTRA_TEXT, publishedText);
                        sendIntent.setType("text/plain");
                        startActivity(Intent.createChooser(
                                sendIntent,
                                getResources().getString(R.string.encrypt_and_share_header)
                        ));
                    }

                    mainActivity.activateClickableViews();
                },
                editTextEncrypt,
                editTextDecrypt
        ));

        buttonDecryptShare.setOnClickListener(v -> decryptText(
                isSuccessful -> {
                    if (isSuccessful) {
                        // Assume that the editTextEncrypt has been updated.
                        String plainText = editTextEncrypt.getText().toString();

                        Intent sendIntent = new Intent();
                        sendIntent.setAction(Intent.ACTION_SEND);
                        sendIntent.putExtra(Intent.EXTRA_TEXT, plainText);
                        sendIntent.setType("text/plain");
                        startActivity(Intent.createChooser(
                                sendIntent,
                                getResources().getString(R.string.decrypt_and_share_header)
                        ));
                    }

                    mainActivity.activateClickableViews();
                },
                (BaseActivity) getActivity(),
                editTextEncrypt,
                editTextDecrypt
        ));

        return rootView;
    }

    private void safeAdapterUpdate() {
        final BaseMainActivity baseMainActivity = (BaseMainActivity) getActivity();
        if (baseMainActivity == null) {
            return;
        }

        RequestGlobalStatic.requestAndSetSelectedGroup(
                selectedGroup -> {
                    // Initialise an adapter if one does not already exist.
                    // Both null and non-null selectedGroup possibilities are accounted for by the adapter.
                    if (selectedGroupAdapter == null) {
                        selectedGroupAdapter = new GroupsAdapter(Collections.singletonList(selectedGroup));
                    }
                    else {
                        selectedGroupAdapter.update(
                                true,
                                true,
                                Collections.singletonList(selectedGroup)
                        );
                    }

                    if (selectedGroup == null) {
                        selectedGroupAdapter.clear();
                    }

                    // Show/Remove views depending on whether the selected group has been obtained.
                    showAndRemoveViews();

                    if (recyclerView == null) {
                        recyclerView = RecyclerViewSetup.setupGroupsRecyclerView(
                                rootView.findViewById(R.id.recyclerview_main_tab_selected_group),
                                WorkspaceTab.this,
                                GroupSelectionActivity.class,
                                selectedGroupAdapter
                        );
                    }
                },
                baseMainActivity,
                null,
                null
        );
    }

    private void showAndRemoveViews() {
        final TextView errorGroup = rootView.findViewById(R.id.workspace_tab_error_group);

        if (selectedGroupAdapter != null && selectedGroupAdapter.getTotalItemCount() > 0) {
            errorGroup.setVisibility(View.GONE);
        }
        else {
            errorGroup.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        selectedGroupAdapter = null;
    }

    /**
     * Encrypt a plain text string from an EditText instance, encrypt it, and output it in another EditText instance.
     *
     * @param callback          Returns a boolean success status for this method.
     * @param editTextEncrypt   The input EditText instance, i.e. the encrypted text section.
     * @param editTextDecrypt   The output EditText instance, i.e. the decrypted text section.
     */
    private void encryptText(final BooleanCallback callback,
                             EditText editTextEncrypt,
                             final EditText editTextDecrypt) {

        final BaseMainActivity baseMainActivity = (BaseMainActivity) getActivity();
        if (baseMainActivity == null) {
            callback.onReturn(false);
            return;
        }

        baseMainActivity.deactivateClickableViews();

        final String plainText = editTextEncrypt.getText().toString();

        // Perform preliminary checks before creating a JSONObject to send to the server.

        if (plainText.isEmpty()) {
            final String s = "Enter a non-empty message to encrypt.";
            AlertDialogs.invalidInputDialog(baseMainActivity, s, false);
            callback.onReturn(false);
            return;
        }

        final LoadingDialog loadingDialog = new LoadingDialog(baseMainActivity);

        RequestGlobalStatic.requestAndSetSelectedGroup(
                (selectedGroup, accessToken) -> {
                    final BaseActivity baseActivity2 = (BaseActivity) getActivity();
                    if (baseActivity2 == null) {
                        callback.onReturn(false);
                        return;
                    }

                    if (selectedGroup == null || accessToken == null) {
                        callback.onReturn(false);
                        return;
                    }

                    // Check that the selected group does not meet any of the failure conditions.

                    boolean condition1 = !selectedGroup.getPermissions().isActive();
                    boolean condition2 = selectedGroup.getPermissions().isDenied();
                    String invalidInputErrMsg = null;
                    if (condition1 && condition2) {
                        invalidInputErrMsg = HttpOperations.ERROR_MSG_403_INACTIVE_AND_DENIED_SELECT;
                    }
                    else if (condition1) {
                        invalidInputErrMsg = HttpOperations.ERROR_MSG_403_INACTIVE_SELECT;
                    }
                    else if (condition2) {
                        invalidInputErrMsg = HttpOperations.ERROR_MSG_403_DENIED_SELECT;
                    }

                    if (invalidInputErrMsg != null) {
                        loadingDialog.dismissDialog();
                        AlertDialogs.invalidInputDialog(baseActivity2, invalidInputErrMsg, true);
                        callback.onReturn(false);
                        return;
                    }

                    final byte[] dataEncryptionKey;
                    try {
                        dataEncryptionKey = SharedPreferencesHandler.getDataEncryptionKey(baseActivity2);
                    }
                    catch ( NoStoredObjectException | IOException | GeneralSecurityException | DecoderException |
                            NullPointerException e ) {

                        loadingDialog.dismissDialog();
                        AlertDialogs.internalErrorDialog(baseActivity2, false, GENERIC_ENCRYPT_FAILED_MSG);
                        callback.onReturn(false);
                        return;
                    }

                    // Now a HTTP request for the encrypted group key can be sent to the server.

                    AccessTokenAndGroupNumber accessTokenAndGroupNumber = new AccessTokenAndGroupNumber(
                            accessToken,
                            selectedGroup.getNumber()
                    );

                    HttpOperations.post(
                            HttpOperations.URI_GET_GROUP_KEY,
                            accessTokenAndGroupNumber,
                            baseActivity2,
                            response1 -> {
                                final BaseActivity baseActivity3 = (BaseActivity) getActivity();
                                if (baseActivity3 == null) {
                                    callback.onReturn(false);
                                    return;
                                }

                                try {
                                    final GroupKeyResponse groupKeyResponse = GeneralUtils.fromJson(
                                            response1,
                                            GroupKeyResponse.class
                                    );

                                    byte[] groupKey = BouncyCastleInterpreter.aesOperate(
                                            false,
                                            Base64.decode(groupKeyResponse.getEncryptedGroupKeyString()),
                                            Base64.decode(groupKeyResponse.getIvString()),
                                            dataEncryptionKey
                                    );

                                    // IMPORTANT: Ensure that the group key for the selected group is always set to
                                    // null after use.
                                    selectedGroup.setKey(groupKey);
                                }
                                catch ( DataLengthException | InvalidCipherTextException | GroupKeyException |
                                        DecoderException e ) {

                                    selectedGroup.setNullKey();

                                    loadingDialog.dismissDialog();

                                    AlertDialogs.internalErrorDialog(
                                            baseActivity3,
                                            false,
                                            GENERIC_ENCRYPT_FAILED_MSG
                                    );

                                    callback.onReturn(false);

                                    return;
                                }

                                final PublishedText pt;
                                final TextEncryptRequest textEncryptRequest;
                                try {
                                    pt = new PublishedText(
                                            plainText,
                                            Vars.NO_TIME_OUT,   // TODO: Allow message time-outs.
                                            selectedGroup
                                    );

                                    // The group key is no longer required.
                                    selectedGroup.setNullKey();

                                    textEncryptRequest = new TextEncryptRequest(
                                            accessToken,
                                            pt.getGroup().getNumber(),
                                            Base64.toBase64String(pt.getIv()),
                                            Base64.toBase64String(pt.getRawCipherText()),
                                            pt.getTimeOut()
                                    );
                                }
                                catch (InvalidCipherTextException | PublishedTextException e) {
                                    selectedGroup.setNullKey();

                                    loadingDialog.dismissDialog();

                                    // The PublishedTextExceptions caught are either checked before or
                                    // currently impossible (i.e. a non-zero message time-out) so display a
                                    // generic error message.
                                    AlertDialogs.internalErrorDialog(
                                            baseActivity3,
                                            false,
                                            GENERIC_ENCRYPT_FAILED_MSG
                                    );

                                    callback.onReturn(false);

                                    return;
                                }

                                HttpOperations.post(
                                        HttpOperations.URI_TEXT_ENCRYPT,
                                        textEncryptRequest,
                                        baseActivity3,
                                        response2 -> {
                                            final BaseActivity baseActivity4 = (BaseActivity) getActivity();
                                            if (baseActivity4 == null) {
                                                callback.onReturn(false);
                                                return;
                                            }

                                            try {
                                                final TextEncryptResponse textEncryptResponse = GeneralUtils.fromJson(
                                                        response2,
                                                        TextEncryptResponse.class
                                                );

                                                pt.constructPublishedText(
                                                        Base64.decode(textEncryptResponse.getEncMsg())
                                                );
                                            }
                                            catch (IOException e) {
                                                loadingDialog.dismissDialog();
                                                AlertDialogs.internalErrorDialog(
                                                        baseActivity4,
                                                        false,
                                                        GENERIC_ENCRYPT_FAILED_MSG
                                                );
                                                callback.onReturn(false);
                                                return;
                                            }

                                            editTextDecrypt.setText(pt.getPublishedText());

                                            callback.onReturn(true);

                                            loadingDialog.dismissDialog();
                                        },
                                        error -> {
                                            callback.onReturn(false);

                                            final BaseActivity baseActivity5 = (BaseActivity) getActivity();
                                            if (baseActivity5 == null) {
                                                return;
                                            }

                                            loadingDialog.dismissDialog();

                                            HttpOperations.handleEncryptOnErrorResponse(error, baseActivity5);
                                        }
                                );
                            },
                            error -> {
                                callback.onReturn(false);

                                selectedGroup.setNullKey();

                                final BaseActivity baseActivity6 = (BaseActivity) getActivity();
                                if (baseActivity6 == null) {
                                    return;
                                }

                                loadingDialog.dismissDialog();

                                HttpOperations.handleGetGroupKeyOnErrorResponse(error, baseActivity6);
                            }
                    );
                },
                baseMainActivity,
                loadingDialog,
                GENERIC_ENCRYPT_FAILED_MSG
        );
    }

    /**
     * Decrypt a published text string from an EditText instance, decrypt it, and output it in another EditText
     * instance.
     *
     * @param activity                      The calling activity.
     * @param callback                      Returns a boolean success status for this method.
     * @param editTextEncrypt               The input EditText instance, i.e. the encrypted text section.
     * @param editTextDecrypt               The output EditText instance, i.e. the decrypted text section.
     */
    public static void decryptText(@NonNull final BooleanCallback callback,
                                   @NonNull final BaseActivity activity,
                                   @NonNull final EditText editTextEncrypt,
                                   @NonNull final EditText editTextDecrypt) {

        decryptText(
                activity,
                callback,
                null,
                null,
                editTextEncrypt,
                editTextDecrypt
        );
    }

    /**
     * Decrypt a collection of published text string(s) from the automatic-decrypt browser extension and update the
     * given {@link DecryptedJsonObject} with the decrypted plain text(s).
     *
     * @param activity                      The calling activity.
     * @param callback                      Returns a boolean success status for this method.
     * @param decryptedJsonObject           The object containing the {@link org.json.JSONObject} created by the
     *                                      automatic-decrypt browser extension. This object will be updated with the
     *                                      decrypted plain text(s) before this method's return.
     * @param autoDecryptPublishedTexts     The list of published text strings to decrypt.
     *                                      Set this to null if the user manually chose to decrypt a single message.
     *                                      Failures/Exceptions will be ignored iff this not is null.
     *                                      If this is null, the following failures should divert the intended code
     *                                      execution path and display exception messages to the user:
     *                                      * The number of text-decrypt objects in the request and response are not
     *                                        equal.
     *                                      * A non-success status is received from the server.
     */
    public static void decryptText(@NonNull final BooleanCallback callback,
                                   @NonNull final BaseActivity activity,
                                   @NonNull final DecryptedJsonObject decryptedJsonObject,
                                   @NonNull final List<String> autoDecryptPublishedTexts) {

        decryptText(
                activity,
                callback,
                decryptedJsonObject,
                autoDecryptPublishedTexts,
                null,
                null
        );
    }

    private static void decryptText(@NonNull  final BaseActivity activity,
                                    @NonNull  final BooleanCallback callback,
                                    @Nullable final DecryptedJsonObject decryptedJsonObject,
                                    @Nullable final List<String> autoDecryptPublishedTexts,
                                    @Nullable final EditText editTextEncrypt,
                                    @Nullable final EditText editTextDecrypt) {

        final boolean displayFailures = autoDecryptPublishedTexts == null;

        if (displayFailures) {
            activity.deactivateClickableViews();
        }

        final byte[] dataEncryptionKey;
        try {
            dataEncryptionKey = SharedPreferencesHandler.getDataEncryptionKey(activity);
        }
        catch ( NoStoredObjectException | IOException | GeneralSecurityException | DecoderException |
                NullPointerException e ) {

            handleGenericDecryptFailed(activity, displayFailures, null, callback);
            return;
        }

        final LoadingDialog loadingDialog = displayFailures ? new LoadingDialog(activity) : null;

        RequestGlobalStatic.requestAndSetAccessToken(
                accessToken -> {
                    if (accessToken == null) {
                        // The LoadingDialog should be dismissed in the requestAndSetAccessToken method.
                        callback.onReturn(false);
                        return;
                    }

                    final List<String> publishedTextInputs;
                    if (displayFailures) {
                        // Initialise the PublishedText instance with the user input.
                        publishedTextInputs = Collections.singletonList(editTextDecrypt.getText().toString());
                    }
                    else {
                        publishedTextInputs = new ArrayList<>(autoDecryptPublishedTexts);
                    }

                    try {
                        checkGroupIdsLoadedAndRequestTextDecryption(
                                activity,
                                displayFailures,
                                decryptedJsonObject,
                                publishedTextInputs,
                                loadingDialog,
                                callback,
                                editTextEncrypt,
                                dataEncryptionKey,
                                accessToken
                        );
                    }
                    catch (UnknownGroupIdException e) {
                        final LoadGroupsFromIdsRequest loadGroupsFromIdsRequest;
                        try {
                            loadGroupsFromIdsRequest = new LoadGroupsFromIdsRequest(
                                    accessToken,
                                    e.getGroupIdsBase64()
                            );
                        }
                        catch (Base256Exception e2) {
                            handleGenericDecryptFailed(activity, displayFailures, loadingDialog, callback);
                            return;
                        }

                        HttpOperations.post(
                                HttpOperations.URI_LOAD_GROUPS_FROM_IDS,
                                loadGroupsFromIdsRequest,
                                activity,
                                response1 -> {
                                    final LoadGroupsFromIdsResponse lgfir = GeneralUtils.fromJson(
                                            response1,
                                            LoadGroupsFromIdsResponse.class
                                    );

                                    // Assume that only one LoadGroupObject is returned since only one group ID was
                                    // sent.
                                    if (lgfir.getLoadGroupObjectList().size() == 0) {
                                        if (displayFailures) {
                                            loadingDialog.dismissDialog();

                                            String s = "Input message is encrypted with an unknown group.";
                                            AlertDialogs.invalidInputDialog(activity, s, true);
                                        }

                                        callback.onReturn(false);
                                        return;
                                    }

                                    final List<LoadGroupObject> loadGroupObjects = lgfir.getLoadGroupObjectList();

                                    List<long[]> groupContactsList = new ArrayList<>();

                                    for (LoadGroupObject lgo : loadGroupObjects) {
                                        groupContactsList.add(lgo.getTargetContactNumbersNullSafe());
                                    }

                                    final long[] groupContactsArray =
                                            GeneralUtils.collectionToArray(
                                                GeneralUtils.arrayToSet(    // Remove duplicate contact numbers.
                                                    GeneralUtils.concatenateArrays(groupContactsList)
                                                )
                                            );

                                    if (groupContactsArray.length == 0) {
                                        storeLoadedObjectsAndRequestDecrypt(
                                                activity,
                                                displayFailures,
                                                decryptedJsonObject,
                                                loadGroupObjects,
                                                null,
                                                loadingDialog,
                                                callback,
                                                publishedTextInputs,
                                                editTextEncrypt,
                                                dataEncryptionKey,
                                                accessToken
                                        );

                                        return;
                                    }

                                    HttpOperations.post(
                                            HttpOperations.URI_LOAD_CHOSEN_CONTACTS,
                                            new LoadChosenContactsRequest(accessToken, groupContactsArray),
                                            activity,
                                            response2 -> {
                                                final LoadChosenContactsResponse lccr = GeneralUtils.fromJson(
                                                        response2,
                                                        LoadChosenContactsResponse.class
                                                );

                                                List<LoadContactObject> targetContacts = new ArrayList<>();

                                                targetContacts.addAll(lccr.getKnownContacts());
                                                targetContacts.addAll(lccr.getExtendedContacts());

                                                storeLoadedObjectsAndRequestDecrypt(
                                                        activity,
                                                        displayFailures,
                                                        decryptedJsonObject,
                                                        loadGroupObjects,
                                                        targetContacts,
                                                        loadingDialog,
                                                        callback,
                                                        publishedTextInputs,
                                                        editTextEncrypt,
                                                        dataEncryptionKey,
                                                        accessToken
                                                );
                                            },
                                            error2 -> {
                                                if (displayFailures) {
                                                    // Do not display an error message since the group was successfully
                                                    // loaded from the ID but target contacts were unsuccessfully
                                                    // loaded.
                                                    Log.e(
                                                            "Accept group request",
                                                            String.format(
                                                                    "Successfully requested group %d from its ID " +
                                                                            "but unable to load its target " +
                                                                            "contacts. Error response from %s",
                                                                    lgfir.getLoadGroupObjectList().get(0).getNumber(),
                                                                    HttpOperations.URI_LOAD_CHOSEN_CONTACTS
                                                            )
                                                    );
                                                }

                                                storeLoadedObjectsAndRequestDecrypt(
                                                        activity,
                                                        displayFailures,
                                                        decryptedJsonObject,
                                                        loadGroupObjects,
                                                        null,
                                                        loadingDialog,
                                                        callback,
                                                        publishedTextInputs,
                                                        editTextEncrypt,
                                                        dataEncryptionKey,
                                                        accessToken
                                                );
                                            }
                                    );
                                },
                                error1 -> {
                                    callback.onReturn(false);

                                    if (displayFailures) {
                                        loadingDialog.dismissDialog();

                                        HttpOperations.handleStandardRequestOnErrorResponse(
                                                error1,
                                                activity,
                                                false
                                        );
                                    }
                                }
                        );
                    }
                },
                activity,
                loadingDialog,
                false
        );
    }

    private static void storeLoadedObjectsAndRequestDecrypt(@NonNull BaseActivity activity,
                                                            final boolean displayFailures,
                                                            @Nullable final DecryptedJsonObject decryptedJsonObject,
                                                            @NonNull List<LoadGroupObject> loadGroupObjects,
                                                            @Nullable List<LoadContactObject> loadContactObjectList,
                                                            @NonNull LoadingDialog loadingDialog,
                                                            @NonNull BooleanCallback callback,
                                                            @NonNull List<String> publishedTextInputs,
                                                            @NonNull EditText editTextEncrypt,
                                                            @NonNull byte[] dataEncryptionKey,
                                                            @NonNull String accessToken) {

        try {
            if (loadContactObjectList == null) {
                ContactGroupAssociation.storeGroupsAndSetAssociations(loadGroupObjects);
            }
            else {
                ContactGroupAssociation.storeContactsAndGroupsAndSetAssociations(
                        loadContactObjectList,
                        loadGroupObjects
                );
            }

            checkGroupIdsLoadedAndRequestTextDecryption(
                    activity,
                    displayFailures,
                    decryptedJsonObject,
                    publishedTextInputs,
                    loadingDialog,
                    callback,
                    editTextEncrypt,
                    dataEncryptionKey,
                    accessToken
            );
        }
        catch (ContactException | GroupException | UnknownGroupIdException e) {
            handleGenericDecryptFailed(activity, displayFailures, loadingDialog, callback);
        }
    }

    /**
     * Check that all the groups required to decrypt the given published text strings are loaded; if they are then
     * text decryption is requested, if they are not then a {@link UnknownGroupIdException} exception is thrown
     * containing the group ID(s) required to request before calling this method again.
     *
     * This method determines if group is loaded by attempting to initialise a {@link PublishedText} object from a
     * published text input string.
     *
     * All other (expected) exceptions are caught and handled within this method.
     *
     * @param activity                  The calling activity.
     * @param displayFailures           Set to true for manual decryption.
     * @param djo                       The object to update upon successful decryption, if the request is automatic.
     * @param publishedTextInputs       All strings to create the PublishedText objects from, if the request is
     *                                  automatic.
     * @param loadingDialog             The LoadingDialog on display during the user's decrypt request, if the request
     *                                  is manual.
     * @param callback                  The BooleanCallback supplied to the decrypt method.
     * @param editTextEncrypt           The EditText to output the decrypted message to, if the request is manual.
     * @param dataEncryptionKey         The key used decrypt the encrypted group key(s).
     * @param accessToken               The most recent access token used for server requests.
     * @throws UnknownGroupIdException  Thrown when attempting to initialise a {@link PublishedText} object with a
     *                                  published text string.
     *                                  When thrown, this object contains either a single group ID or a list of all
     *                                  group IDs, not currently loaded by the current contact, to request before
     *                                  requesting message decryption.
     */
    private static void checkGroupIdsLoadedAndRequestTextDecryption(@NonNull BaseActivity activity,
                                                                    final boolean displayFailures,
                                                                    @Nullable final DecryptedJsonObject djo,
                                                                    @NonNull List<String> publishedTextInputs,
                                                                    @NonNull LoadingDialog loadingDialog,
                                                                    @NonNull BooleanCallback callback,
                                                                    @NonNull EditText editTextEncrypt,
                                                                    @NonNull byte[] dataEncryptionKey,
                                                                    @NonNull String accessToken)
            throws UnknownGroupIdException {

        if (displayFailures) {
            try {
                // Only one message should be sent.
                final PublishedText publishedText = new PublishedText(publishedTextInputs.get(0));

                requestTextDecryption(
                        activity,
                        displayFailures,
                        djo,
                        editTextEncrypt,
                        dataEncryptionKey,
                        accessToken,
                        Collections.singletonList(publishedText),
                        loadingDialog,
                        callback
                );
            }
            catch (Base256Exception e) {
                loadingDialog.dismissDialog();

                String s = "Input message is malformed.";
                AlertDialogs.invalidInputDialog(activity, s, true);

                callback.onReturn(false);
            }
            catch (PublishedTextException e) {
                loadingDialog.dismissDialog();

                String invalidInputErrMsg = null;

                if (e.getMessage().contains(PublishedText.EMPTY_PUB_TEXT_EXP)) {
                    invalidInputErrMsg = "Enter a non-empty message to decrypt.";
                }
                else if ( e.getMessage().contains(PublishedText.PUBLISHED_TEXT_TOO_SHORT_EXP) ||
                          e.getMessage().contains(PublishedText.MESSAGE_BYTES_TOO_SHORT_EXP) ) {
                    invalidInputErrMsg = "Input message is too short.";
                }
                else if (e.getMessage().contains(PublishedText.BAD_START_TAG_EXP)) {
                    invalidInputErrMsg = "Input message has an invalid prefix.";
                }
                else if (e.getMessage().contains(PublishedText.BAD_END_TAG_EXP)) {
                    invalidInputErrMsg = "Input message has an invalid suffix.";
                }
                else if (e.getMessage().contains(PublishedText.INVALID_GROUP_ID)) {
                    invalidInputErrMsg = "Input message has an invalid group ID.";
                }
                else if (e.getMessage()
                        .contains(PublishedText.GROUP_INACTIVE_AND_ACCESS_DENIED_EXP)) {
                    invalidInputErrMsg = GROUP_INACTIVE_AND_ACCESS_DENIED_MSG;
                }
                else if (e.getMessage().contains(PublishedText.GROUP_INACTIVE_EXP)) {
                    invalidInputErrMsg = GROUP_INACTIVE_MSG;
                }
                else if (e.getMessage().contains(PublishedText.GROUP_ACCESS_DENIED_EXP)) {
                    invalidInputErrMsg = GROUP_ACCESS_DENIED_MSG;
                }

                if (invalidInputErrMsg == null) {
                    // The LoadingDialog is dismissed above.
                    handleGenericDecryptFailed(activity, null, callback);
                }
                else {
                    AlertDialogs.invalidInputDialog(activity, invalidInputErrMsg, true);

                    callback.onReturn(false);
                }
            }
        }
        else {
            List<PublishedText> publishedTexts = new ArrayList<>();
            List<String> groupIdsBase64ToRequest = new ArrayList<>();

            for (String pti : publishedTextInputs) {
                try {
                    publishedTexts.add(new PublishedText(pti));
                }
                catch (UnknownGroupIdException e) {
                    try {
                        groupIdsBase64ToRequest.add(e.getGroupIdBase64());
                    }
                    catch (Base256Exception e2) {
                        // Fail silently.
                    }
                }
                catch (Base256Exception | PublishedTextException e) {
                    // Fail silently.
                }
            }

            if (!groupIdsBase64ToRequest.isEmpty()) {
                throw new UnknownGroupIdException(
                        "Cannot construct PublishedText object(s) with unknown group ID(s).",
                        groupIdsBase64ToRequest
                );
            }

            if (publishedTexts.isEmpty()) {
                callback.onReturn(false);
            }
            else {
                requestTextDecryption(
                        activity,
                        displayFailures,
                        djo,
                        editTextEncrypt,
                        dataEncryptionKey,
                        accessToken,
                        publishedTexts,
                        loadingDialog,
                        callback
                );
            }
        }
    }

    /**
     * Send a request to the server to decrypt a list of published text strings.
     *
     * @param activity              The calling activity.
     * @param displayFailures       Set to true for manual decryption.
     * @param decryptedJsonObject   The object to update upon successful decryption, if the request is automatic.
     * @param editTextEncrypt       The {@link EditText} to output the decrypted message to, if the request is manual.
     * @param dataEncryptionKey     The key used decrypt the encrypted group key(s).
     * @param accessToken           The most recent access token used for server requests.
     * @param publishedTexts        All {@link PublishedText} objects to decrypt.
     * @param loadingDialog         The LoadingDialog on display during the user's manual decrypt request.
     * @param callback              The BooleanCallback supplied to the decrypt method.
     */
    private static void requestTextDecryption(@NonNull BaseActivity activity,
                                              final boolean displayFailures,
                                              @Nullable final DecryptedJsonObject decryptedJsonObject,
                                              final EditText editTextEncrypt,
                                              final byte[] dataEncryptionKey,
                                              final String accessToken,
                                              final List<PublishedText> publishedTexts,
                                              final LoadingDialog loadingDialog,
                                              final BooleanCallback callback) {

        List<TextDecryptRequestObject> textDecryptRequestObjects = new ArrayList<>();
        for (PublishedText pt : publishedTexts) {
            textDecryptRequestObjects.add(
                    new TextDecryptRequestObject(
                            pt.getGroup().getNumber(),
                            Base64.toBase64String(pt.getIv()),
                            Base64.toBase64String(pt.getTimeOutCipherText())
                    )
            );
        }

        TextDecryptRequest textDecryptRequest = new TextDecryptRequest(accessToken, textDecryptRequestObjects);

        HttpOperations.post(
                HttpOperations.URI_TEXT_DECRYPT,
                textDecryptRequest,
                activity,
                response -> {
                    final TextDecryptResponse textDecryptResponse = GeneralUtils.fromJson(
                            response,
                            TextDecryptResponse.class
                    );

                    if (displayFailures) {
                        // The server should return only one message response.
                        if (textDecryptResponse.getTextDecryptResponseObjects().size() != 1) {
                            handleGenericDecryptFailed(activity, loadingDialog, callback);
                            return;
                        }

                        TextDecryptResponseObject tdro = textDecryptResponse.getTextDecryptResponseObjects().get(0);

                        switch (tdro.getMessageStatus()) {
                            case Vars.ERR_403_PERM_INACTIVE_AND_DENIED:
                                loadingDialog.dismissDialog();
                                AlertDialogs.genericConfirmationDialog(
                                        activity,
                                        HttpOperations.ERROR_DECRYPT,
                                        GROUP_INACTIVE_AND_ACCESS_DENIED_MSG
                                );
                                callback.onReturn(false);
                                return;
                            case Vars.ERR_403_PERM_INACTIVE:
                                loadingDialog.dismissDialog();
                                AlertDialogs.genericConfirmationDialog(
                                        activity,
                                        HttpOperations.ERROR_DECRYPT,
                                        GROUP_INACTIVE_MSG
                                );
                                callback.onReturn(false);
                                return;
                            case Vars.ERR_403_PERM_DENIED:
                                loadingDialog.dismissDialog();
                                AlertDialogs.genericConfirmationDialog(
                                        activity,
                                        HttpOperations.ERROR_DECRYPT,
                                        GROUP_ACCESS_DENIED_MSG
                                );
                                callback.onReturn(false);
                                return;
                            case Vars.ERR_400_BAD_PUBLISHED_MSG_STR:
                                loadingDialog.dismissDialog();
                                AlertDialogs.genericConfirmationDialog(
                                        activity,
                                        HttpOperations.ERROR_DECRYPT,
                                        BAD_PUBLISHED_MSG_MSG
                                );
                                callback.onReturn(false);
                                return;
                            case Vars.ERR_400_TIME_OUT_EXPIRED_STATUS:
                                loadingDialog.dismissDialog();
                                AlertDialogs.genericConfirmationDialog(
                                        activity,
                                        HttpOperations.ERROR_DECRYPT,
                                        TIME_OUT_EXPIRED_MSG
                                );
                                callback.onReturn(false);
                                return;
                            case Vars.SUCCESS_200:
                                final PublishedText pt = publishedTexts.get(0);

                                // Only check all TextDecryptResponseObject values are assigned to for this case since
                                // we intend to use the server response.
                                if ( tdro.getGroupNumber() != pt.getGroup().getNumber() ||
                                     !Arrays.equals(Base64.decode(tdro.getMessageIvString()), pt.getIv()) ||
                                     !Arrays.equals(Base64.decode(tdro.getEncMsg()), pt.getTimeOutCipherText()) ) {

                                    handleGenericDecryptFailed(activity, loadingDialog, callback);
                                    return;
                                }

                                try {
                                    decryptRawCipherText(pt, tdro, dataEncryptionKey);
                                }
                                catch (InvalidCipherTextException | NullPointerException e) {
                                    handleGenericDecryptFailed(activity, loadingDialog, callback);
                                    return;
                                }

                                editTextEncrypt.setText(new String(pt.getPlainText(), StandardCharsets.UTF_8));

                                callback.onReturn(true);

                                loadingDialog.dismissDialog();
                                return;
                            default:
                                handleGenericDecryptFailed(activity, loadingDialog, callback);
                        }
                    }
                    else {
                        // Create a temporary list which can have elements removed without affecting the original list.
                        List<TextDecryptResponseObject> tempTextDecryptResponseObjects =
                                new ArrayList<>(textDecryptResponse.getTextDecryptResponseObjects());

                        // Assume that the list of published texts is not smaller than the list of response objects,
                        // since some encrypted messages may fail to decrypt (currently these lists are the same size
                        // since the server also sends back failed decryptions with a failed message status).
                        for (PublishedText pt : publishedTexts) {
                            TextDecryptResponseObject tdroProcessed = null;

                            for (TextDecryptResponseObject tdro : tempTextDecryptResponseObjects) {
                                // Assume that there are no duplicate published texts (there may be duplicate plain
                                // texts) so no published text should have its members overwritten.
                                if ( !tdro.getMessageStatus().equals(Vars.SUCCESS_200) ||
                                     tdro.getGroupNumber() != pt.getGroup().getNumber() ||
                                     !Arrays.equals(Base64.decode(tdro.getMessageIvString()), pt.getIv()) ||
                                     !Arrays.equals(Base64.decode(tdro.getEncMsg()), pt.getTimeOutCipherText()) ) {

                                    continue;
                                }

                                // The response object corresponding the the published text has been found.

                                try {
                                    decryptRawCipherText(pt, tdro, dataEncryptionKey);

                                    decryptedJsonObject.add(
                                            pt.getPublishedText(),
                                            new String(pt.getPlainText(), StandardCharsets.UTF_8)
                                    );
                                }
                                catch (InvalidCipherTextException | NullPointerException e) {
                                    // Fail silently.
                                }

                                tdroProcessed = tdro;
                                break;
                            }

                            if (tdroProcessed != null) {
                                // Reduce the number of loop iterations for the next published text.
                                tempTextDecryptResponseObjects.remove(tdroProcessed);
                            }
                        }

                        callback.onReturn(decryptedJsonObject.hasPlainText());
                    }
                },
                error -> {
                    callback.onReturn(false);

                    if (displayFailures) {
                        loadingDialog.dismissDialog();

                        HttpOperations.handleStandardRequestOnErrorResponse(error, activity, false);
                    }
                }
        );
    }

    private static void decryptRawCipherText(@NonNull PublishedText publishedText,
                                             @NonNull TextDecryptResponseObject textDecryptResponseObject,
                                             @NonNull byte[] dataEncryptionKey)
            throws InvalidCipherTextException, NullPointerException {

        publishedText.decryptRawCipherText(
                textDecryptResponseObject.getTimeOut(),
                Base64.decode(textDecryptResponseObject.getDecMsg()),
                Base64.decode(textDecryptResponseObject.getEncryptedGroupKeyString()),
                Base64.decode(textDecryptResponseObject.getKeyIvString()),
                dataEncryptionKey
        );
    }

    private static void handleGenericDecryptFailed(@NonNull BaseActivity activity,
                                                   LoadingDialog loadingDialog,
                                                   @NonNull BooleanCallback callback) {

        handleGenericDecryptFailed(activity, true, loadingDialog, callback);
    }

    private static void handleGenericDecryptFailed(@NonNull BaseActivity activity,
                                                   boolean displayFailures,
                                                   LoadingDialog loadingDialog,
                                                   @NonNull BooleanCallback callback) {

        callback.onReturn(false);

        if (displayFailures) {
            LoadingDialog.safeDismiss(loadingDialog);

            AlertDialogs.internalErrorDialog(activity, false, GENERIC_DECRYPT_FAILED_MSG);
        }
    }
}
