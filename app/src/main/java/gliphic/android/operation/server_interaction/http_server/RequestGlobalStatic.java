/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.operation.server_interaction.http_server;

import android.content.ContextWrapper;

import com.android.volley.Response;
import gliphic.android.display.libraries.AlertDialogs;
import gliphic.android.display.libraries.LoadingDialog;
import gliphic.android.display.abstract_views.BaseActivity;
import gliphic.android.display.abstract_views.BaseMainActivity;
import gliphic.android.exceptions.NoStoredObjectException;
import gliphic.android.interfaces.BaseContactCallback;
import gliphic.android.interfaces.BaseGroupCallback;
import gliphic.android.interfaces.ContactAndAccessTokenCallback;
import gliphic.android.interfaces.ContactCallback;
import gliphic.android.interfaces.ContactListAndStatusCallback;
import gliphic.android.interfaces.GroupAndAccessTokenCallback;
import gliphic.android.interfaces.GroupCallback;
import gliphic.android.interfaces.GroupListCallback;
import gliphic.android.interfaces.AccessTokenCallback;
import gliphic.android.interfaces.GroupShareListAndStatusCallback;
import gliphic.android.operation.Alerts;
import gliphic.android.operation.Contact;
import gliphic.android.exceptions.ContactException;
import gliphic.android.exceptions.ForcedServerRequestException;
import gliphic.android.operation.Group;
import gliphic.android.exceptions.GroupException;
import gliphic.android.exceptions.NullStaticVariableException;
import gliphic.android.operation.misc.ContactGroupAssociation;
import gliphic.android.operation.misc.Log;
import gliphic.android.operation.server_interaction.xmpp_server.ConnectionService;
import gliphic.android.operation.storage_handlers.ForcedDialogs;
import gliphic.android.operation.storage_handlers.SharedPreferencesHandler;

import org.bouncycastle.util.encoders.DecoderException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import libraries.GeneralUtils;
import pojo.account.AccessTokenData;
import pojo.account.GroupShare;
import pojo.load.LoadChosenContactsRequest;
import pojo.load.LoadChosenContactsResponse;
import pojo.load.LoadChosenGroupsRequest;
import pojo.load.LoadChosenGroupsResponse;
import pojo.load.LoadContactObject;
import pojo.load.LoadContactsRequest;
import pojo.load.LoadContactsResponse;
import pojo.load.LoadGroupObject;
import pojo.load.LoadGroupSharesRequest;
import pojo.load.LoadGroupSharesResponse;
import pojo.load.LoadGroupsRequest;
import pojo.load.LoadGroupsResponse;

/**
 * This class contains static methods which request resources from the server which are required when internal global
 * static variables have been reset to null (for example if Android resets the application/activity to save memory)
 * or a variable is no longer valid e.g. an (expiring) access token.
 */
public class RequestGlobalStatic {
    private static boolean requestInProgressForTargetContacts = false;
    private static boolean requestInProgressForKnownGroups = false;
    private static boolean requestInProgressForGroupShares = false;

    /**
     * Convenience method for storing a group.
     *
     * @param loadGroupObject   The group to store.
     * @return                  The stored group.
     * @throws GroupException   Thrown when the group cannot be stored.
     */
    public static Group storeGroup(@NonNull LoadGroupObject loadGroupObject) throws GroupException {
        return ContactGroupAssociation.storeGroupAndSetAssociations(loadGroupObject);
    }

    /**
     * Convenience method for storing a group and a list of chosen contacts associated with the group.
     *
     * This method is typically used during a {@link HttpOperations#URI_LOAD_CHOSEN_CONTACTS} POST request.
     *
     * @param loadGroupObject           The group to store.
     * @param loadContactObjectList     The target contacts associated with the given group.
     * @return                          The stored group.
     * @throws GroupException           Thrown when the group cannot be stored.
     *                                  Note that no exception is thrown when the target contacts cannot be stored.
     */
    public static Group storeGroupAndTargetContacts(@NonNull LoadGroupObject loadGroupObject,
                                                    @NonNull List<LoadContactObject> loadContactObjectList)
            throws GroupException {

        try {
            return ContactGroupAssociation.storeContactsAndGroupsAndSetAssociations(
                    loadContactObjectList,
                    Collections.singletonList(loadGroupObject)
            ).getGroups().get(0);
        }
        catch (GroupException | ContactException e) {
            return storeGroup(loadGroupObject);
        }
    }

    private static void setBaseContactCallback(@NonNull final BaseContactCallback callback,
                                               @Nullable final Contact contact,
                                               @Nullable final String accessToken) {

        if (callback instanceof ContactCallback) {
            ((ContactCallback) callback).onReturn(contact);
        }
        else if (callback instanceof ContactAndAccessTokenCallback) {
            ((ContactAndAccessTokenCallback) callback).onReturn(contact, accessToken);
        }
        else {
            String s = "Input callback of type BaseContactCallback is not an instance of an expected subclass.";
            throw new Error(new IOException(s));
        }
    }

    private static void setBaseGroupCallback(@NonNull final BaseGroupCallback callback,
                                             @Nullable final Group group,
                                             @Nullable final String accessToken) {

        if (callback instanceof GroupCallback) {
            ((GroupCallback) callback).onReturn(group);
        }
        else if (callback instanceof GroupAndAccessTokenCallback) {
            ((GroupAndAccessTokenCallback) callback).onReturn(group, accessToken);
        }
        else {
            String s = "Input callback of type BaseGroupCallback is not an instance of an expected subclass.";
            throw new Error(new IOException(s));
        }
    }

    /**
     * Handle setting the access token and access token expiry in SharedPreferences, requesting this information from
     * the server if necessary.
     *
     * If a null callback is given then this method can perform an additional request to the server in a background
     * thread, thus not blocking application execution until the additional request has completed.
     * The currently supported additional requests are:
     * * Sign-in time (assumed by setting a null callback).
     *
     * WARNING: When calling this method directly, the context wrapper MUST be an instance (or an instance of any
     * subclass) of either BaseActivity or ConnectionService.
     *
     * @param callback                  Returns the access token string, or null if it cannot be retrieved.
     *                                  Set this to null to perform an additional non-blocking server request.
     * @param contextWrapper            The calling activity or connection service.
     * @param loadingDialog             Set to null to not display any LoadingDialog or AlertDialog messages; if this
     *                                  is non-null then both LoadingDialog and AlertDialog messages are displayed. A
     *                                  null value is typically set when this method is called without explicit
     *                                  user-interaction in the calling method e.g. via an onResume method.
     * @param finishActivity            True if the calling activity should terminate when the dialog is dismissed,
     *                                  false otherwise.
     * @param contextWrapperIsActivity  True iff contextWrapper is an instance of BaseActivity (or an instance of
     *                                  any subclass).
     *                                  False iff contextWrapper is an instance of ConnectionService (or an instance of
     *                                  any subclass).
     */
    private static void handleRequestAccessTokenDataAndSignInTime(final @Nullable AccessTokenCallback callback,
                                                                  final @NonNull ContextWrapper contextWrapper,
                                                                  final @Nullable LoadingDialog loadingDialog,
                                                                  final boolean finishActivity,
                                                                  final boolean contextWrapperIsActivity) {
        final String accessToken;
        final long   accessTokenExpiry;
        final String refreshToken;
        try {
            accessToken       = SharedPreferencesHandler.getAccessToken(contextWrapper);
            accessTokenExpiry = SharedPreferencesHandler.getAccessTokenExpiry(contextWrapper);
            refreshToken      = SharedPreferencesHandler.getRefreshToken(contextWrapper);
        }
        catch ( NoStoredObjectException | IOException | GeneralSecurityException | DecoderException |
                NullPointerException e ) {

            if (LoadingDialog.safeDismiss(loadingDialog)) {
                AlertDialogs.internalErrorDialog((BaseActivity) contextWrapper, finishActivity, null);
            }

            safeSetCallbackOnReturn(callback, null);

            Log.e("Internal error", "Cannot read token data from shared preferences: " + e.getMessage());

            return;
        }

        // The server could return a response up to the point which the request times out,
        // so the access token must have an expiry time after this time to be usable.
        if (System.currentTimeMillis() + 6*HttpOperations.REQUEST_TIME_OUT < accessTokenExpiry) {
            safeSetCallbackOnReturn(callback, accessToken);

            if (callback == null) {
                handleRequestSignInTime((BaseMainActivity) contextWrapper, accessToken);
            }

            return;
        }

        final Response.Listener<String> successListener = response -> {
            final AccessTokenData accessTokenData = GeneralUtils.fromJson(response, AccessTokenData.class);

            try {
                // Ensure that the access token is set before the access token expiry in case setting the
                // access token expiry throws an exception.
                SharedPreferencesHandler.setAccessToken(
                        contextWrapper,
                        accessTokenData.getAccessToken()
                );
                SharedPreferencesHandler.setAccessTokenExpiry(
                        contextWrapper,
                        accessTokenData.getExpiryTime()
                );

                safeSetCallbackOnReturn(callback, accessTokenData.getAccessToken());
            }
            catch (IOException | GeneralSecurityException | NullPointerException e) {
                if (contextWrapperIsActivity && LoadingDialog.safeDismiss(loadingDialog)) {
                    AlertDialogs.internalErrorDialog(
                            (BaseActivity) contextWrapper,
                            finishActivity,
                            null
                    );
                }
                safeSetCallbackOnReturn(callback, null);

                return;
            }

            if (callback == null) {
                handleRequestSignInTime((BaseMainActivity) contextWrapper, accessTokenData.getAccessToken());
            }
        };

        final Response.ErrorListener errorListener = error -> {
            if (contextWrapperIsActivity && LoadingDialog.safeDismiss(loadingDialog)) {
                HttpOperations.handleStandardRequestOnErrorResponse(
                        error,
                        (BaseActivity) contextWrapper,
                        finishActivity
                );
            }

            safeSetCallbackOnReturn(callback, null);
        };

        if (contextWrapperIsActivity) {
            HttpOperations.post(
                    HttpOperations.URI_TOKEN_REFRESH,
                    refreshToken,
                    (BaseActivity) contextWrapper,
                    successListener,
                    errorListener
            );
        }
        else {
            HttpOperations.post(
                    HttpOperations.URI_TOKEN_REFRESH,
                    refreshToken,
                    (ConnectionService) contextWrapper,
                    successListener,
                    errorListener
            );
        }
    }

    private static void safeSetCallbackOnReturn(@Nullable AccessTokenCallback callback, @Nullable String accessToken) {
        if (callback != null) {
            callback.onReturn(accessToken);
        }
    }

    private static void handleRequestSignInTime(@NonNull BaseMainActivity activity, @NonNull String accessToken) {
        final String errMsgPrefix = "Cannot load sign-in time: ";

        HttpOperations.post(
                HttpOperations.URI_LOAD_SIGN_IN_TIME,
                accessToken,
                activity,
                response -> {
                    try {
                        final long signInTime = GeneralUtils.fromJson(response, Long.class);

                        final long storedSignInTime = SharedPreferencesHandler.getLastSignInTime(activity);

                        if (signInTime == storedSignInTime) {
                            ConnectionService.startConnectionService(activity);
                        }
                        else {
                            // Force the contact to sign out.
                            AlertDialogs.signOutDialog(
                                    activity,
                                    ForcedDialogs.ForcedSignOutAction.CONNECTION_CLOSED_CONFLICT,
                                    true
                            );
                        }
                    }
                    catch ( NullPointerException | DecoderException | NoStoredObjectException | IOException |
                            GeneralSecurityException e ) {

                        Log.e("Internal error", errMsgPrefix + e.getMessage());
                    }
                },
                error -> Log.w("HTTP request error", errMsgPrefix + "status code not 'OK'.")
        );
    }

    /**
     * Return the access token and access token expiry time via the given callback.
     *
     * 1.  Attempt to retrieve the locally-stored access token.
     * 2a. If the access token is about to expire then send a HTTP request to the server to obtain the access token.
     * 2b. If the request is successful then the new access token will be stored and returned via the callback.
     *
     * This method requires a callback argument to ensure that the calling method waits for a server response.
     *
     * A non-null LoadingDialog is not dismissed unless the callback's onReturn method specifies a null value.
     *
     * @param callback          Returns the access token string, or null if it cannot be retrieved.
     * @param activity          The calling activity.
     * @param loadingDialog     Set to null to not display any LoadingDialog or AlertDialog messages; if this is
     *                          non-null then both LoadingDialog and AlertDialog messages are displayed. A null value
     *                          is typically set when this method is called without explicit user-interaction in the
     *                          calling method e.g. via an onResume method.
     * @param finishActivity    True if the calling activity should terminate when the dialog is dismissed, false
     *                          otherwise.
     */
    public static void requestAndSetAccessToken(final @NonNull AccessTokenCallback callback,
                                                final @NonNull BaseActivity activity,
                                                final @Nullable LoadingDialog loadingDialog,
                                                final boolean finishActivity) {

        handleRequestAccessTokenDataAndSignInTime(
                callback,
                activity,
                loadingDialog,
                finishActivity,
                true
        );
    }

    /**
     * Request the sign-in time from the server and display a forced sign-out dialog if the requested sign-in time does
     * not match the stored sign-in time.
     *
     * 1.  Attempt to retrieve the locally-stored access token.
     * 2a. If the access token is about to expire then send a HTTP request to the server to obtain the access token.
     * 2b. If the request is successful then the new access token will be stored and used for the next request.
     * 3.  Use the valid access token to request the sign-in time from the server; log any error causing this to fail.
     * 4.  Display a forced sign-out dialog if the requested sign-in time does not match the stored sign-in time.
     *
     * @param activity  The calling activity.
     */
    public static void requestSignInTimeAndForceSignOutOnMismatch(final @NonNull BaseMainActivity activity) {
        handleRequestAccessTokenDataAndSignInTime(
                null,
                activity,
                null,
                false,
                true
        );
    }

    /**
     * - Attempt to retrieve target contacts for any group (or a given specified group if it is non-null) from the
     *   application's local cache.
     * - If a NullStaticVariableException exception is thrown then send a HTTP request to the server to obtain the
     *   target contacts.
     * - If the request is successful then the list which holds the (group's) target contacts lists will be set and
     *   all internally stored target contacts (for the given group) will be returned from this method.
     *
     * This method requires a callback argument to ensure that the calling method waits for a server response.
     *
     * This method does not display any AlertDialog messages.
     *
     * @param callback                      The callback which should return either a list of contacts or null, as well
     *                                      as true if a request for target contacts is already in progress and false
     *                                      otherwise.
     * @param activity                      The calling activity.
     * @param forceServerRequest            Set to true to always request contacts from the server, set to false to
     *                                      only send a server request if there are no contacts available locally.
     * @param ignoreFilters                 Set to true to ignore the given filters and use default values, set to
     *                                      false to use the given CheckBoxes and the EditText search string filters.
     * @param numOfLoadedContacts           The total number of contacts already stored by the affected
     *                                      ContactsAdapter.
     * @param specifiedGroup                The specific group to request target contacts for.
     *                                      Set to null to request contacts for all groups.
     * @param isKnownContactsChecked        Set to true if the known contacts CheckBox is checked, false otherwise.
     * @param isExtendedContactsChecked     Set to true if the extended contacts CheckBox is checked, false otherwise.
     * @param searchString                  The string which the contact is filtering target contacts by.
     *                                      If this is null or an empty string no filter is applied.
     */
    public static void requestAndSetTargetContacts(final ContactListAndStatusCallback callback,
                                                   final BaseMainActivity activity,
                                                   final boolean forceServerRequest,
                                                   final boolean ignoreFilters,
                                                   final int numOfLoadedContacts,
                                                   final Group specifiedGroup,
                                                   final boolean isKnownContactsChecked,
                                                   final boolean isExtendedContactsChecked,
                                                   final String searchString) {

        // The calling activity/fragment should prevent both CheckBoxes from being unchecked.
        if (!ignoreFilters && !isKnownContactsChecked && !isExtendedContactsChecked) {
            final String s = "Both known and extended CheckBoxes cannot be unchecked (when filters are not ignored).";
            throw new Error(new InvalidParameterException(s));
        }

        try {
            if (forceServerRequest) {
                throw new ForcedServerRequestException();
            }

            // Assume that both the static target-contacts list and the non-static group-target-contacts list have not
            // been initialized if the static list is null (since the non-static list is never null).
            // Allow a NullStaticVariableException to be thrown regardless of whether the specified group is null.
            List<Contact> targetContacts = Contact.getTargetContacts();

            if (specifiedGroup == null) {
                callback.onReturn(targetContacts, false);
            }
            else {
                callback.onReturn(specifiedGroup.getGroupTargetContacts(), false);
            }
        }
        catch (ForcedServerRequestException | NullStaticVariableException e1) {
            // Prevent many target contact requests from being sent before receiving their respective server responses.
            if (requestInProgressForTargetContacts) {
                callback.onReturn(null, true);
                return;
            }

            requestInProgressForTargetContacts = true;

            RequestGlobalStatic.requestAndSetAccessToken(
                    accessToken -> {
                        if (accessToken == null) {
                            returnFromRequestAndSetTargetContacts(callback, null);
                            return;
                        }

                        Long    groupNumber;
                        boolean getKnownContacts;
                        boolean getExtendedContacts;
                        String  contactSearchString;

                        if (specifiedGroup == null) {
                            groupNumber = null;
                        }
                        else {
                            groupNumber = specifiedGroup.getNumber();
                        }

                        if (ignoreFilters) {
                            getKnownContacts    = true;
                            getExtendedContacts = false;
                            contactSearchString = null;
                        }
                        else {
                            getKnownContacts    = isKnownContactsChecked;
                            getExtendedContacts = isExtendedContactsChecked;
                            contactSearchString = searchString;
                        }

                        LoadContactsRequest loadContactsRequest = new LoadContactsRequest(
                                accessToken,
                                numOfLoadedContacts,
                                getKnownContacts,
                                getExtendedContacts,
                                contactSearchString,
                                groupNumber
                        );

                        HttpOperations.post(
                                HttpOperations.URI_LOAD_TARGET_CONTACTS,
                                loadContactsRequest,
                                activity,
                                response -> {
                                    final LoadContactsResponse lcr = GeneralUtils.fromJson(
                                            response,
                                            LoadContactsResponse.class
                                    );

                                    final List<LoadContactObject> knownContactObjects    = lcr.getKnownContacts();
                                    final List<LoadContactObject> extendedContactObjects = lcr.getExtendedContacts();

                                    Set<Long> groupNumbers = new HashSet<>();

                                    final List<LoadContactObject> targetContacts = new ArrayList<>();
                                    targetContacts.addAll(knownContactObjects);
                                    targetContacts.addAll(extendedContactObjects);

                                    for (LoadContactObject lco : targetContacts) {
                                        groupNumbers.addAll(GeneralUtils.arrayToSet(lco.getGroupNumbersNullSafe()));
                                    }

                                    if (groupNumbers.isEmpty()) {
                                        handleLoadContactsResponseAndReturn(
                                                targetContacts,
                                                null,
                                                specifiedGroup,
                                                callback
                                        );
                                        return;
                                    }

                                    LoadChosenGroupsRequest loadChosenGroupsRequest = new LoadChosenGroupsRequest(
                                            accessToken,
                                            GeneralUtils.collectionToArray(groupNumbers)
                                    );

                                    HttpOperations.post(
                                            HttpOperations.URI_LOAD_CHOSEN_GROUPS,
                                            loadChosenGroupsRequest,
                                            activity,
                                            response1 -> {
                                                LoadChosenGroupsResponse lcgr = GeneralUtils.fromJson(
                                                        response1,
                                                        LoadChosenGroupsResponse.class
                                                );

                                                handleLoadContactsResponseAndReturn(
                                                        targetContacts,
                                                        lcgr.getGroups(),
                                                        specifiedGroup,
                                                        callback
                                                );
                                            },
                                            error -> returnFromRequestAndSetTargetContacts(callback, null)
                                    );
                                },
                                error -> returnFromRequestAndSetTargetContacts(callback, null));
                    },
                    activity,
                    null,
                    false
            );
        }
    }

    private static void handleLoadContactsResponseAndReturn(@NonNull List<LoadContactObject> targetContacts,
                                                            List<LoadGroupObject> knownGroups,
                                                            Group specifiedGroup,
                                                            @NonNull ContactListAndStatusCallback callback) {
        try {
            if (knownGroups == null) {
                ContactGroupAssociation.storeContactsAndSetAssociations(targetContacts);
            }
            else {
                ContactGroupAssociation.storeContactsAndGroupsAndSetAssociations(targetContacts, knownGroups);
            }

            // Set the relevant contact list in the callback.
            if (targetContacts.isEmpty()) {
                // Ensure a NullStaticVariableException exception is not thrown.
                returnFromRequestAndSetTargetContacts(callback, new ArrayList<>());
            }
            else if (specifiedGroup == null) {
                returnFromRequestAndSetTargetContacts(callback, Contact.getTargetContacts());
            }
            else {
                returnFromRequestAndSetTargetContacts(callback, specifiedGroup.getGroupTargetContacts());
            }
        }
        catch (ContactException | GroupException | NullStaticVariableException e) {
            returnFromRequestAndSetTargetContacts(callback, null);
        }
    }

    private static void returnFromRequestAndSetTargetContacts(ContactListAndStatusCallback callback,
                                                              List<Contact> contactList) {

        requestInProgressForTargetContacts = false;

        callback.onReturn(contactList, false);
    }

    /**
     * - Attempt to retrieve the current contact from the application's local cache.
     * - If a NullStaticVariableException exception is thrown then send a HTTP request to the server to obtain the
     *   current contact.
     * - If the request is successful then the static variable which holds the current contact will be set and returned
     *   via the callback.
     *
     * This method requires a callback argument to ensure that the calling method waits for a server response.
     *
     * A non-null LoadingDialog is not dismissed unless the callback's onReturn method specifies a null value.
     *
     * @param callback                  The callback which should return either the current contact or null.
     * @param activity                  The calling activity.
     * @param loadingDialog             Set to null to not display any LoadingDialog or AlertDialog messages;
     *                                  if this is non-null then both LoadingDialog and AlertDialog messages
     *                                  are displayed. A null value is typically set when this method is called without
     *                                  explicit user-interaction in the calling method e.g. via an onResume method.
     * @param internalErrorMsg          The message to display if an internal error occurs.
     *                                  If loadingDialog is null this argument has no effect.
     * @param finishActivity            True if the calling activity should terminate when the dialog is dismissed,
     *                                  false otherwise.
     */
    public static void requestAndSetCurrentContact(@NonNull final ContactCallback callback,
                                                   @NonNull final BaseMainActivity activity,
                                                   @Nullable final LoadingDialog loadingDialog,
                                                   @Nullable final String internalErrorMsg,
                                                   final boolean finishActivity) {

        try {
            callback.onReturn(Contact.getCurrentContact());
        }
        catch (NullStaticVariableException e) {
            RequestGlobalStatic.requestAndSetAccessToken(
                    accessToken -> {
                        if (accessToken == null) {
                            callback.onReturn(null);
                            return;
                        }

                        loadCurrentContact(
                                accessToken,
                                callback,
                                activity,
                                loadingDialog,
                                internalErrorMsg,
                                finishActivity,
                                true
                        );
                    },
                    activity,
                    loadingDialog,
                    finishActivity
            );
        }
    }

    /**
     * Attempt to retrieve the access token and current contact from the application's local cache; if either is
     * unavailable a request (or two requests) is sent to the server. If these are successfully obtained they are set
     * in the given callback, if one or both cannot be obtained they are set to null in the given callback; a null
     * access token does not imply that the current contact cannot be obtained because it may not have been requested,
     * but a null contact implies that the access token cannot be obtained.
     *
     * This method requires a callback argument to ensure that the calling method waits for a server response.
     *
     * @param callback              The callback which should return either the current contact or null, as well as the
     *                              contact's access token or null.
     * @param connectionService     the calling connection service.
     */
    public static void requestAndSetCurrentContact(@NonNull final ContactAndAccessTokenCallback callback,
                                                   @NonNull final ConnectionService connectionService) {

        RequestGlobalStatic.handleRequestAccessTokenDataAndSignInTime(
                accessToken -> {
                    if (accessToken == null) {
                        callback.onReturn(null, null);
                        return;
                    }

                    try {
                        callback.onReturn(Contact.getCurrentContact(), accessToken);
                    }
                    catch (NullStaticVariableException e) {
                        loadCurrentContact(
                                accessToken,
                                callback,
                                connectionService,
                                null,
                                null,
                                false,
                                false
                        );
                    }
                },
                connectionService,
                null,
                false,
                false
        );
    }

    /**
     * Handle requesting the current contact from the server.
     *
     * This method assumes that the access token has been successfully obtained beforehand.
     *
     * WARNING: When calling this method directly, the context wrapper MUST be an instance (or an instance of any
     * subclass) of either BaseMainActivity or ConnectionService.
     *
     * @param accessToken               The access token required to send the request to the server, and inserted into
     *                                  the callback.
     * @param callback                  An instance of either ContactCallback or ContactAndAccessTokenCallback.
     * @param contextWrapper            The calling activity or connection service.
     * @param loadingDialog             Set to null to not display any LoadingDialog or AlertDialog messages; if this
     *                                  is non-null then both LoadingDialog and AlertDialog messages are displayed. A
     *                                  null value is typically set when this method is called without explicit
     *                                  user-interaction in the calling method e.g. via an onResume method.
     * @param internalErrorMsg          The message to display if an internal error occurs.
     *                                  If loadingDialog is null this argument has no effect.
     * @param finishActivity            True if the calling activity should terminate when the dialog is dismissed,
     *                                  false otherwise.
     * @param contextWrapperIsActivity  True iff contextWrapper is an instance of BaseMainActivity (or an instance of
     *                                  any subclass).
     *                                  False iff contextWrapper is an instance of ConnectionService (or an instance of
     *                                  any subclass).
     */
    private static void loadCurrentContact(final @NonNull String accessToken,
                                           final @NonNull BaseContactCallback callback,
                                           final @NonNull ContextWrapper contextWrapper,
                                           final @Nullable LoadingDialog loadingDialog,
                                           final @Nullable String internalErrorMsg,
                                           final boolean finishActivity,
                                           final boolean contextWrapperIsActivity) {

        final Response.Listener<String> successListener = response -> {
            final LoadContactObject loadContactObject = GeneralUtils.fromJson(response, LoadContactObject.class);

            try {
                setBaseContactCallback(
                        callback,
                        ContactGroupAssociation.storeContactAndSetAssociations(loadContactObject),
                        accessToken
                );
            }
            catch (ContactException e) {
                if (contextWrapperIsActivity && LoadingDialog.safeDismiss(loadingDialog)) {
                    AlertDialogs.internalErrorDialog(
                            (BaseMainActivity) contextWrapper,
                            finishActivity,
                            internalErrorMsg
                    );
                }

                setBaseContactCallback(callback, null, accessToken);
            }
        };

        final Response.ErrorListener errorListener = error -> {
            if (contextWrapperIsActivity && LoadingDialog.safeDismiss(loadingDialog)) {
                HttpOperations.handleStandardRequestOnErrorResponse(
                        error,
                        (BaseMainActivity) contextWrapper,
                        finishActivity
                );
            }

            setBaseContactCallback(callback, null, accessToken);
        };

        if (contextWrapperIsActivity) {
            HttpOperations.post(
                    HttpOperations.URI_LOAD_CURRENT_CONTACT,
                    accessToken,
                    (BaseMainActivity) contextWrapper,
                    successListener,
                    errorListener
            );
        }
        else {
            HttpOperations.post(
                    HttpOperations.URI_LOAD_CURRENT_CONTACT,
                    accessToken,
                    (ConnectionService) contextWrapper,
                    successListener,
                    errorListener
            );
        }
    }

    /**
     * - Attempt to retrieve all of the contact's known groups from the application's local cache.
     * - If a NullStaticVariableException exception is thrown then send a HTTP request to the server to obtain the
     *   known groups.
     * - If the request is successful then the static variable which holds the known groups list will be set and
     *   returned from this method.
     *
     * This method requires a callback argument to ensure that the calling method waits for a server response.
     *
     * A non-null LoadingDialog is not dismissed unless the callback's onReturn method specifies a null value.
     *
     * @param callback                  The callback which should return either a list of groups or null.
     * @param activity                  The calling activity.
     * @param loadingDialog             Set to null to not display any LoadingDialog or AlertDialog messages;
     *                                  if this is non-null then both LoadingDialog and AlertDialog messages
     *                                  are displayed. A null value is typically set when this method is called without
     *                                  explicit user-interaction in the calling method e.g. via an onResume method.
     * @param internalErrorMsg          The message to display if an internal error occurs.
     *                                  If loadingDialog is null this argument has no effect.
     * @param forceServerRequest        Set to true to always request contacts from the server, set to false to only
     *                                  send a server request if there are no groups available locally.
     * @param ignoreFilters             Set to true to ignore the given filters and use default values, set to false to
     *                                  use the given CheckBoxes and the EditText search string filters.
     * @param numOfLoadedGroups         The total number of groups already stored by the affected GroupsAdapter.
     * @param specifiedContact          The specific contact to request known groups for.
     *                                  Set to null to request groups for all contacts.
     * @param searchString              The string which the contact is filtering known groups by.
     *                                  If this is null or an empty string no filter is applied.
     * @param ignoreDefaultGroup        True if the default group should not be returned in the callback, false if the
     *                                  group can be returned (there is no guarantee that the group will be returned).
     * @param finishActivity            True if the calling activity should terminate when the dialog is dismissed,
     *                                  false otherwise.
     */
    public static void requestAndSetKnownGroups(final GroupListCallback callback,
                                                final BaseMainActivity activity,
                                                final LoadingDialog loadingDialog,
                                                final String internalErrorMsg,
                                                final boolean forceServerRequest,
                                                final boolean ignoreFilters,
                                                final int numOfLoadedGroups,
                                                final Contact specifiedContact,
                                                final String searchString,
                                                final boolean ignoreDefaultGroup,
                                                final boolean finishActivity) {

        try {
            if (forceServerRequest) {
                throw new ForcedServerRequestException();
            }

            // Assume that both the static groups list and the non-static common-groups list have not
            // been initialized if the static list is null (since the non-static list is never null).
            // Allow a NullStaticVariableException to be thrown regardless of whether the specified contact is null.
            List<Group> targetGroups = Group.getKnownGroups(ignoreDefaultGroup);

            if (specifiedContact != null) {
                targetGroups = specifiedContact.getCommonGroups();
            }

            setKnownGroupsRequestCallback(callback, targetGroups);
        }
        catch (ForcedServerRequestException | NullStaticVariableException e1) {
            RequestGlobalStatic.requestAndSetAccessToken(
                    accessToken -> {
                        if (accessToken == null) {
                            setKnownGroupsRequestCallback(callback, null);
                            return;
                        }

                        // Prevent many known group requests from being sent before receiving their respective server
                        // responses.
                        if (requestInProgressForKnownGroups) {
                            callback.onReturn(null, true);
                            return;
                        }

                        requestInProgressForKnownGroups = true;

                        Long   contactNumber;
                        String groupSearchString;

                        if (specifiedContact == null) {
                            contactNumber = null;
                        }
                        else {
                            contactNumber = specifiedContact.getNumber();
                        }

                        if (ignoreFilters) {
                            groupSearchString = null;
                        }
                        else {
                            groupSearchString = searchString;
                        }

                        LoadGroupsRequest loadGroupsRequest = new LoadGroupsRequest(
                                accessToken,
                                numOfLoadedGroups,
                                groupSearchString,
                                contactNumber,
                                ignoreDefaultGroup
                        );

                        HttpOperations.post(
                                HttpOperations.URI_LOAD_KNOWN_GROUPS,
                                loadGroupsRequest,
                                activity,
                                response -> {
                                    final LoadChosenContactsRequest loadChosenContactsRequest;
                                    final List<LoadGroupObject> groupObjects;
                                    try {
                                        groupObjects = GeneralUtils.fromJson(response, LoadGroupsResponse.class)
                                                .getGroups();

                                        Set<Long> contactNumbers = new HashSet<>();

                                        for (LoadGroupObject lgo : groupObjects) {
                                            contactNumbers.addAll(
                                                    GeneralUtils.arrayToSet(lgo.getTargetContactNumbersNullSafe())
                                            );
                                        }

                                        if (contactNumbers.isEmpty()) {
                                            handleLoadGroupsResponseAndReturn(
                                                    groupObjects,
                                                    null,
                                                    specifiedContact,
                                                    callback,
                                                    ignoreDefaultGroup
                                            );
                                            return;
                                        }

                                        loadChosenContactsRequest = new LoadChosenContactsRequest(
                                                accessToken,
                                                GeneralUtils.collectionToArray(contactNumbers)
                                        );
                                    }
                                    catch (IllegalArgumentException e) {
                                        if (LoadingDialog.safeDismiss(loadingDialog)) {
                                            AlertDialogs.internalErrorDialog(
                                                    activity,
                                                    finishActivity,
                                                    internalErrorMsg
                                            );
                                        }

                                        setKnownGroupsRequestCallback(callback, null);

                                        return;
                                    }

                                    HttpOperations.post(
                                            HttpOperations.URI_LOAD_CHOSEN_CONTACTS,
                                            loadChosenContactsRequest,
                                            activity,
                                            response1 -> {
                                                final LoadChosenContactsResponse lccr = GeneralUtils.fromJson(
                                                        response1,
                                                        LoadChosenContactsResponse.class
                                                );

                                                List<LoadContactObject> targetContactObjects = new ArrayList<>();

                                                targetContactObjects.addAll(lccr.getKnownContacts());
                                                targetContactObjects.addAll(lccr.getExtendedContacts());

                                                handleLoadGroupsResponseAndReturn(
                                                        groupObjects,
                                                        targetContactObjects,
                                                        specifiedContact,
                                                        callback,
                                                        ignoreDefaultGroup
                                                );
                                            },
                                            error -> {
                                                if (LoadingDialog.safeDismiss(loadingDialog)) {
                                                    HttpOperations.handleStandardRequestOnErrorResponse(
                                                            error,
                                                            activity,
                                                            finishActivity
                                                    );
                                                }

                                                setKnownGroupsRequestCallback(callback, null);
                                            }
                                    );
                                },
                                error -> {
                                    if (LoadingDialog.safeDismiss(loadingDialog)) {
                                        HttpOperations.handleStandardRequestOnErrorResponse(
                                                error,
                                                activity,
                                                finishActivity
                                        );
                                    }

                                    setKnownGroupsRequestCallback(callback, null);
                                }
                        );
                    },
                    activity,
                    loadingDialog,
                    finishActivity
            );
        }
    }

    private static void handleLoadGroupsResponseAndReturn(@NonNull List<LoadGroupObject> knownGroups,
                                                          @Nullable List<LoadContactObject> targetContacts,
                                                          @Nullable Contact specifiedContact,
                                                          @NonNull GroupListCallback callback,
                                                          boolean ignoreDefaultGroup) {
        try {
            if (targetContacts == null) {
                ContactGroupAssociation.storeGroupsAndSetAssociations(knownGroups);
            }
            else {
                ContactGroupAssociation.storeContactsAndGroupsAndSetAssociations(targetContacts, knownGroups);
            }

            // Set the relevant group list in the callback.
            List<Group> targetGroups;
            if (specifiedContact == null) {
                targetGroups = Group.getKnownGroups(ignoreDefaultGroup);
            }
            else {
                targetGroups = specifiedContact.getCommonGroups();
            }

            setKnownGroupsRequestCallback(callback, targetGroups);
        }
        catch (ContactException | GroupException | NullStaticVariableException e) {
            // A NullStaticVariableException exception cannot be caught if the default group is available.

            setKnownGroupsRequestCallback(callback, null);
        }
    }

    private static void setKnownGroupsRequestCallback(@NonNull final GroupListCallback callback,
                                                      final List<Group> groupList) {

        requestInProgressForKnownGroups = false;

        callback.onReturn(groupList, false);
    }

    /**
     * @see #handleRequestAndSetSelectedGroup
     *      (BaseGroupCallback, BaseMainActivity, LoadingDialog, String, String)
     *
     * Note that the handler method does not have public access in order to prevent direct implementation of the
     * BaseGroupCallback interface.
     */
    public static void requestAndSetSelectedGroup(final GroupCallback callback,
                                                  final BaseMainActivity activity,
                                                  final LoadingDialog loadingDialog,
                                                  final String internalErrorMsg) {

        handleRequestAndSetSelectedGroup(callback, activity, loadingDialog, internalErrorMsg, null);
    }

    /**
     * @see #handleRequestAndSetSelectedGroup
     *      (BaseGroupCallback, BaseMainActivity, LoadingDialog, String, String)
     *
     * Note that the handler method does not have public access in order to prevent direct implementation of the
     * BaseGroupCallback interface.
     */
    public static void requestAndSetSelectedGroup(final GroupAndAccessTokenCallback callback,
                                                  final BaseMainActivity activity,
                                                  final LoadingDialog loadingDialog,
                                                  final String internalErrorMsg) {

        // The callback must set a non-null group and an accessToken to be successful.
        RequestGlobalStatic.requestAndSetAccessToken(
                accessToken -> {
                    if (accessToken == null) {
                        setBaseGroupCallback(callback, null, null);
                        return;
                    }

                    handleRequestAndSetSelectedGroup(
                            callback,
                            activity,
                            loadingDialog,
                            internalErrorMsg,
                            accessToken
                    );
                },
                activity,
                loadingDialog,
                false
        );
    }

    /**
     * - Attempt to retrieve the currently selected group from the application's local cache.
     * - If a NullStaticVariableException exception is thrown then send a HTTP request to the server to obtain the
     *   selected group.
     * - If the request is successful then the static variable which holds the selected group will be set and returned
     *   from this method.
     *
     * This method requires a callback argument to ensure that the calling method waits for a server response.
     *
     * A non-null LoadingDialog is not dismissed unless the callback's onReturn method specifies a null value.
     *
     * @param callback          An instance of either GroupCallback or GroupAndAccessTokenCallback.
     * @param activity          The calling activity.
     * @param loadingDialog     Set to null to not display any LoadingDialog or AlertDialog messages; if this is
     *                          non-null then both LoadingDialog and AlertDialog messages are displayed. A null value
     *                          is typically set when this method is called without  explicit user-interaction in the
     *                          calling method e.g. via an onResume method.
     * @param internalErrorMsg  The message to display if an internal error occurs.
     *                          If loadingDialog is null this argument has no effect.
     */
    private static void handleRequestAndSetSelectedGroup(final BaseGroupCallback callback,
                                                         final BaseMainActivity activity,
                                                         final LoadingDialog loadingDialog,
                                                         final String internalErrorMsg,
                                                         final String accessToken) {
        try {
            setBaseGroupCallback(callback, Group.getSelectedGroup(), accessToken);
        }
        catch (NullStaticVariableException e1) {
            if (accessToken == null) {
                RequestGlobalStatic.requestAndSetAccessToken(
                        accessToken1 -> {
                            if (accessToken1 == null) {
                                setBaseGroupCallback(callback, null, null);
                                return;
                            }

                            requestSelectedGroup(
                                    callback,
                                    activity,
                                    loadingDialog,
                                    internalErrorMsg,
                                    accessToken1
                            );
                        },
                        activity,
                        loadingDialog,
                        false
                );
            }
            else {
                requestSelectedGroup(callback, activity, loadingDialog, internalErrorMsg, accessToken);
            }
        }
    }

    private static void requestSelectedGroup(@NonNull final BaseGroupCallback callback,
                                             @NonNull final BaseMainActivity activity,
                                             @Nullable final LoadingDialog loadingDialog,
                                             @Nullable final String internalErrorMsg,
                                             @NonNull final String accessToken) {

        HttpOperations.post(
                HttpOperations.URI_LOAD_SELECTED_GROUP,
                accessToken,
                activity,
                response1 -> {
                    final LoadGroupObject loadGroupObject = GeneralUtils.fromJson(response1, LoadGroupObject.class);

                    final long[] groupContacts = loadGroupObject.getTargetContactNumbersNullSafe();

                    if (groupContacts.length == 0) {
                        handleSuccessfulSelectGroupRequest(
                                callback,
                                activity,
                                loadingDialog,
                                internalErrorMsg,
                                accessToken,
                                loadGroupObject,
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
                            response2 -> {
                                final LoadChosenContactsResponse lccr = GeneralUtils.fromJson(
                                        response2,
                                        LoadChosenContactsResponse.class
                                );

                                List<LoadContactObject> targetContacts = new ArrayList<>();

                                targetContacts.addAll(lccr.getKnownContacts());
                                targetContacts.addAll(lccr.getExtendedContacts());

                                handleSuccessfulSelectGroupRequest(
                                        callback,
                                        activity,
                                        loadingDialog,
                                        internalErrorMsg,
                                        accessToken,
                                        loadGroupObject,
                                        targetContacts
                                );
                            },
                            error2 -> {
                                logError(
                                        loadGroupObject.getNumber(),
                                        String.format(
                                                "Error response from %s",
                                                HttpOperations.URI_LOAD_CHOSEN_CONTACTS
                                        )
                                );

                                handleSuccessfulSelectGroupRequest(
                                        callback,
                                        activity,
                                        loadingDialog,
                                        internalErrorMsg,
                                        accessToken,
                                        loadGroupObject,
                                        null
                                );
                            }
                    );
                },
                error1 -> {
                    if (LoadingDialog.safeDismiss(loadingDialog)) {
                        HttpOperations.handleStandardRequestOnErrorResponse(error1, activity, false);
                    }

                    setBaseGroupCallback(callback, null, accessToken);
                }
        );
    }

    private static void handleSuccessfulSelectGroupRequest(@NonNull final BaseGroupCallback callback,
                                                           @NonNull final BaseMainActivity activity,
                                                           @Nullable final LoadingDialog loadingDialog,
                                                           @Nullable final String internalErrorMsg,
                                                           @NonNull final String accessToken,
                                                           @NonNull final LoadGroupObject loadGroupObject,
                                                           @Nullable final List<LoadContactObject> lcoList) {

        try {
            Group selectedGroup;
            try {
                if (lcoList == null) {
                    selectedGroup = storeGroup(loadGroupObject);
                }
                else {
                    selectedGroup = storeGroupAndTargetContacts(loadGroupObject, lcoList);
                }
            }
            catch (GroupException e) {
                logError(
                        loadGroupObject.getNumber(),
                        "Exception message: " + e.getMessage()
                );

                throw e;
            }

            selectedGroup.selectGroup();

            setBaseGroupCallback(callback, selectedGroup, accessToken);

            LoadingDialog.safeDismiss(loadingDialog);   // This should not throw an exception.
        }
        catch (IllegalArgumentException | GroupException e) {
            if (LoadingDialog.safeDismiss(loadingDialog)) {
                AlertDialogs.internalErrorDialog(
                        activity,
                        false,
                        internalErrorMsg
                );
            }

            setBaseGroupCallback(callback, null, accessToken);
        }
    }

    private static void logError(long groupNumber, @Nullable String logMessageSuffix) {
        // Do not display an error message since the group was successfully selected but unsuccessfully loaded.
        String msg = "Successfully selected group %d but unable to load it. %s";
        Log.e("Accept group request", String.format(msg, groupNumber, logMessageSuffix));
    }

    /**
     * - Attempt to retrieve group-shares from the application's local cache.
     * - If a NullStaticVariableException exception is thrown then send a HTTP request to the server to obtain the
     *   group-shares.
     * - If the request is successful then the list which holds the group-shares will be set and all internally stored
     *   group-shares will be returned from this method.
     *
     * This method requires a callback argument to ensure that the calling method waits for a server response.
     *
     * This method does not display any AlertDialog messages.
     *
     * @param callback                              The callback which should return either a list of group-shares or
     *                                              null, as well as true if a request for group-shares is already in
     *                                              progress and false otherwise.
     * @param activity                              The calling activity.
     * @param forceServerRequest                    Set to true to always request group-shares from the server, set to
     *                                              false to only send a server request if there are no group-shares
     *                                              available locally.
     * @param numOfLoadedPendingReceivedShares      The number of pending-received group-share requests loaded by the
     *                                              adapter.
     * @param numOfLoadedPendingSentShares          The number of pending-sent group-share requests loaded by the
     *                                              adapter.
     *                                              Set this to null to not load any of these group-shares.
     * @param numOfLoadedCompletedReceivedShares    The number of success-received group-share requests loaded by the
     *                                              adapter.
     *                                              Set this to null to not load any of these group-shares.
     * @param numOfLoadedCompletedSentShares        The number of success-sent and failed-sent group-share requests
     *                                              loaded by the adapter.
     *                                              Set this to null to not load any of these group-shares.
     */
    public static void requestGroupShareAlerts(final GroupShareListAndStatusCallback callback,
                                               final BaseMainActivity activity,
                                               final boolean forceServerRequest,
                                               final long numOfLoadedPendingReceivedShares,
                                               final Long numOfLoadedPendingSentShares,
                                               final Long numOfLoadedCompletedReceivedShares,
                                               final Long numOfLoadedCompletedSentShares) {

        try {
            if (forceServerRequest) {
                throw new ForcedServerRequestException();
            }

            List<GroupShare> groupShares = Alerts.getGroupShares();

            callback.onReturn(groupShares, false);
        }
        catch (ForcedServerRequestException | NullStaticVariableException e1) {
            // Prevent many group-share requests from being sent before receiving their respective server responses.
            if (requestInProgressForGroupShares) {
                callback.onReturn(null, true);
                return;
            }

            requestInProgressForGroupShares = true;

            RequestGlobalStatic.requestAndSetAccessToken(
                    accessToken -> {
                        if (accessToken == null) {
                            returnFromRequestGroupShareAlerts(callback, null);
                            return;
                        }

                        LoadGroupSharesRequest loadGroupSharesRequest = new LoadGroupSharesRequest(
                                accessToken,
                                numOfLoadedPendingReceivedShares,
                                numOfLoadedPendingSentShares,
                                numOfLoadedCompletedReceivedShares,
                                numOfLoadedCompletedSentShares
                        );

                        HttpOperations.post(
                                HttpOperations.URI_LOAD_GROUP_SHARES,
                                loadGroupSharesRequest,
                                activity,
                                response -> {
                                    final LoadGroupSharesResponse loadGroupSharesResponse = GeneralUtils.fromJson(
                                            response,
                                            LoadGroupSharesResponse.class
                                    );

                                    Alerts.storeStatically(loadGroupSharesResponse.getGroupSharesList());

                                    try {
                                        returnFromRequestGroupShareAlerts(callback, Alerts.getGroupShares());
                                    }
                                    catch (NullStaticVariableException e) {
                                        returnFromRequestGroupShareAlerts(callback, null);
                                    }
                                },
                                error -> returnFromRequestGroupShareAlerts(callback, null));
                    },
                    activity,
                    null,
                    false
            );
        }
    }

    private static void returnFromRequestGroupShareAlerts(GroupShareListAndStatusCallback callback,
                                                          List<GroupShare> groupShareList) {

        requestInProgressForGroupShares = false;

        callback.onReturn(groupShareList, false);
    }
}
