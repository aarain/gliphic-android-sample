/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.display.libraries;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.telephony.PhoneNumberUtils;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.Patterns;

import gliphic.android.display.abstract_views.BaseActivity;
import gliphic.android.display.abstract_views.BaseMainActivity;
import gliphic.android.display.main.MainActivity;
import gliphic.android.display.welcome_screen.RegisterActivity;
import gliphic.android.exceptions.InvalidUserInputException;
import gliphic.android.exceptions.NoStoredObjectException;
import gliphic.android.exceptions.NullStaticVariableException;
import gliphic.android.interfaces.BooleanCallback;
import gliphic.android.operation.Contact;
import gliphic.android.operation.misc.Log;
import gliphic.android.operation.server_interaction.xmpp_server.ConnectionService;
import gliphic.android.operation.server_interaction.http_server.HttpOperations;
import gliphic.android.operation.storage_handlers.ForcedDialogs;
import gliphic.android.operation.storage_handlers.ResponseCodeAndMessage;
import gliphic.android.operation.storage_handlers.SharedPreferencesHandler;

import org.bouncycastle.util.encoders.DecoderException;

import java.io.IOException;
import java.security.GeneralSecurityException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import libraries.GeneralUtils;
import libraries.Vars;

/**
 * AlertDialog messages to display across multiple Activities.
 */
public class AlertDialogs {
    private static final String INTERNAL_ERROR_TITLE = "Internal error";

    private static void logSharedPreferencesError(@NonNull String baseMessage, @NonNull Exception exception) {
        Log.e("AlertDialog Error", String.format("%s: %s", baseMessage, exception.getMessage()));
    }

    /**
     * Show the given AlertDialog in the activity parsed to the AlertDialog.Builder, or log an error message if the
     * AlertDialog.show() method throws an exception.
     *
     * The most likely reason an exception would be thrown is if the activity associated with the AlertDialog is not
     * running (because it is not visible) i.e. a BadTokenException is thrown.
     *
     * @param alertDialog   The AlertDialog to show.
     */
    public static void safeShowDialog(@NonNull AlertDialog alertDialog) {
        try {
            alertDialog.show();
        }
        catch (Exception e) {
            logSharedPreferencesError("Exception caught attempting to show AlertDialog", e);
        }
    }

    private static void showDialog(@NonNull AlertDialog alertDialog, boolean safeShow) {
        if (safeShow) {
            safeShowDialog(alertDialog);
        }
        else {
            alertDialog.show();
        }
    }

    /**
     * Do not throw an exception if the alert dialog fails to show but log a message and fail silently.
     *
     * @see #genericConfirmationDialog(BaseActivity, CharSequence, CharSequence, boolean)
     */
    public static void genericConfirmationDialog(@NonNull final BaseActivity activity,
                                                 @NonNull CharSequence title,
                                                 @NonNull CharSequence message) {

        genericConfirmationDialog(activity, title, message, true);
    }

    /**
     * Display an AlertDialog with only an OK confirmation button with a given title and message.
     *
     * @param activity      The calling activity.
     * @param title         The display title of the AlertDialog.
     * @param message       The display message of the AlertDialog.
     * @param safeShow      True to catch any exceptions thrown when attempting to show the dialog, false otherwise.
     */
    public static void genericConfirmationDialog(@NonNull final BaseActivity activity,
                                                 @NonNull CharSequence title,
                                                 @NonNull CharSequence message,
                                                 boolean safeShow) {

        final AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
        activity.setMAlertDialog(alertDialog);
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setButton(
                AlertDialog.BUTTON_POSITIVE,
                activity.getResources().getString(android.R.string.ok),
                (dialog, which) -> {
                    dialog.dismiss();
                    if (alertDialog.equals(activity.getMAlertDialog())) {
                        activity.setMAlertDialog(null);
                    }
                });
        alertDialog.setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);

        showDialog(alertDialog, safeShow);
    }

    /**
     * Display an AlertDialog with only an OK confirmation button with a given title and message.
     *
     * This method should only be used when the given activity does not extend {@link BaseActivity}, otherwise use
     * {@link #genericConfirmationDialog(BaseActivity, CharSequence, CharSequence, boolean)}.
     *
     * @param activity      The calling activity.
     * @param title         The display title of the AlertDialog.
     * @param message       The display message of the AlertDialog.
     */
    public static void basicGenericConfirmationDialog(@NonNull final Activity activity,
                                                      @NonNull String title,
                                                      @NonNull String message) {

        final AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setButton(
                AlertDialog.BUTTON_POSITIVE,
                activity.getResources().getString(android.R.string.ok),
                (dialog, which) ->
                        dialog.dismiss()
                );
        alertDialog.setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();
    }

    /**
     * Display an AlertDialog with a yes and no button with a given title and message.
     *
     * @param callback      Returns true if the user clicked {@link AlertDialog#BUTTON_POSITIVE},
     *                      Returns false if the user clicked {@link AlertDialog#BUTTON_NEGATIVE}.
     * @param activity      The calling activity.
     * @param title         The display title of the AlertDialog.
     * @param msg           The display message of the AlertDialog.
     */
    public static void genericYesOrNoDialog(@NonNull BooleanCallback callback,
                                            @NonNull final BaseActivity activity,
                                            @NonNull String title,
                                            @NonNull String msg) {

        final AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
        activity.setMAlertDialog(alertDialog);
        alertDialog.setTitle(title);
        alertDialog.setMessage(msg);
        alertDialog.setButton(
                AlertDialog.BUTTON_NEGATIVE,
                activity.getResources().getString(android.R.string.no),
                (dialog, which) -> {
                    dialog.dismiss();
                    if (alertDialog.equals(activity.getMAlertDialog())) {
                        activity.setMAlertDialog(null);
                    }

                    callback.onReturn(false);
                });
        alertDialog.setButton(
                AlertDialog.BUTTON_POSITIVE,
                activity.getResources().getString(android.R.string.yes),
                (dialog, which) -> {
                    dialog.dismiss();
                    if (alertDialog.equals(activity.getMAlertDialog())) {
                        activity.setMAlertDialog(null);
                    }

                    callback.onReturn(true);
                });
        alertDialog.setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);

        safeShowDialog(alertDialog);
    }

    /**
     * Display when the application notices that the current version is out-of-date and requires a critical update.
     *
     * @param activity  The calling activity.
     */
    public static void appCriticalUpdateRequired(@NonNull final BaseActivity activity) {
        genericConfirmationDialog(
                activity,
                "Update required",
                "Your version of this application is out-of-date and requires a critical update. " +
                        "If the application is already up-to-date try signing out and in again.",
                true
        );
    }

    /**
     * @see #internalErrorDialog(BaseActivity, boolean, String, boolean)
     */
    public static void internalErrorDialog(@NonNull final BaseActivity activity,
                                           final boolean finishActivity,
                                           @Nullable String displayMessage) {

        internalErrorDialog(activity, finishActivity, displayMessage, true);
    }

    /**
     * Display when an unexpected error has occurred within the app.
     *
     * Ideally, the contact should never see this.
     *
     * @param activity              The calling activity.
     * @param finishActivity        True if the calling activity should terminate when the dialog is dismissed,
     *                              false otherwise.
     * @param displayMessage        The message to display to the contact. Set to null to display a default message.
     * @param allowSetForcedDialog  True iff the forced dialog should be set in SharedPreferences (if finishActivity is
     *                              also true).
     *                              Note that the forced dialog is always removed regardless of this option when the
     *                              user clicks OK.
     */
    public static void internalErrorDialog(@NonNull final BaseActivity activity,
                                           final boolean finishActivity,
                                           @Nullable String displayMessage,
                                           boolean allowSetForcedDialog) {

        if (finishActivity && allowSetForcedDialog) {
            try {
                ForcedDialogs forcedDialogs = SharedPreferencesHandler.getForcedDialogs(activity);
                forcedDialogs.setInternalErrorMessage(displayMessage);
                SharedPreferencesHandler.setForcedDialogs(activity, forcedDialogs);
            }
            catch ( NoStoredObjectException | IOException | GeneralSecurityException | NullPointerException |
                    DecoderException e ) {

                logSharedPreferencesError(
                        "Exception caught when setting the 'forced internal error dialog message' value",
                        e
                );
            }
        }

        final AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
        activity.setMAlertDialog(alertDialog);
        alertDialog.setTitle(INTERNAL_ERROR_TITLE);
        if (displayMessage == null) {
            alertDialog.setMessage(HttpOperations.ERROR_MSG_OTHER);
        }
        else {
            alertDialog.setMessage(displayMessage);
        }
        alertDialog.setButton(
                AlertDialog.BUTTON_POSITIVE,
                activity.getResources().getString(android.R.string.ok),
                (dialog, which) -> {
                    dialog.dismiss();
                    if (alertDialog.equals(activity.getMAlertDialog())) {
                        activity.setMAlertDialog(null);
                    }

                    if (finishActivity) {
                        try {
                            ForcedDialogs forcedDialogs = SharedPreferencesHandler.getForcedDialogs(activity);
                            forcedDialogs.unsetInternalErrorMessage();
                            SharedPreferencesHandler.setForcedDialogs(activity, forcedDialogs);
                        }
                        catch ( NoStoredObjectException | IOException | GeneralSecurityException |
                                NullPointerException | DecoderException e ) {

                            logSharedPreferencesError(
                                    "Exception caught when unsetting the 'forced internal error " +
                                            "dialog message' value",
                                    e
                            );
                        }

                        activity.finish();
                    }
                });
        alertDialog.setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);

        safeShowDialog(alertDialog);
    }

    /**
     * Display when an unexpected error has occurred within the app.
     *
     * This method should only be used when the given activity does not extend {@link BaseActivity}, otherwise use
     * {@link #internalErrorDialog(BaseActivity, boolean, String, boolean)}.
     *
     * @param activity      The calling activity.
     * @param message       The display message of the AlertDialog.
     */
    public static void basicInternalErrorDialog(@NonNull final Activity activity, @NonNull String message) {
        basicGenericConfirmationDialog(activity, INTERNAL_ERROR_TITLE, message);
    }

    /**
     * @see #responseCodeDialog(BaseActivity, int, String, boolean, boolean)
     */
    public static void responseCodeDialog(@NonNull final BaseActivity activity,
                                          int responseCode,
                                          @NonNull String displayMessage,
                                          final boolean finishActivity) {

        responseCodeDialog(activity, responseCode, displayMessage, finishActivity, true);
    }

    /**
     * Display when a server response code is not in the 200 range.
     *
     * @param activity              The calling activity.
     * @param responseCode          The response code integer from the server.
     * @param displayMessage        The message to display to the contact (the display title is fixed).
     * @param finishActivity        If true the given activity is finished as soon as the OK button is pressed; set to
     *                              false to not finish the activity.
     * @param allowSetForcedDialog  True iff the forced dialog should be set in SharedPreferences (if finishActivity is
     *                              also true).
     *                              Note that the forced dialog is always removed regardless of this option when the
     *                              user clicks OK.
     */
    public static void responseCodeDialog(@NonNull final BaseActivity activity,
                                          int responseCode,
                                          @NonNull String displayMessage,
                                          final boolean finishActivity,
                                          boolean allowSetForcedDialog) {

        if (finishActivity && allowSetForcedDialog) {
            try {
                ForcedDialogs forcedDialogs = SharedPreferencesHandler.getForcedDialogs(activity);
                forcedDialogs.setResponseCodeAndMessage(new ResponseCodeAndMessage(responseCode, displayMessage));
                SharedPreferencesHandler.setForcedDialogs(activity, forcedDialogs);
            }
            catch ( NoStoredObjectException | IOException | GeneralSecurityException | NullPointerException |
                    DecoderException e ) {

                logSharedPreferencesError(
                        "Exception caught when setting the 'forced response code error dialog message' value",
                        e
                );
            }
        }

        final AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
        activity.setMAlertDialog(alertDialog);
        alertDialog.setTitle("Server response code: " + responseCode);
        alertDialog.setMessage(displayMessage);
        alertDialog.setButton(
                AlertDialog.BUTTON_POSITIVE,
                activity.getResources().getString(android.R.string.ok),
                (dialog, which) -> {
                    dialog.dismiss();
                    if (alertDialog.equals(activity.getMAlertDialog())) {
                        activity.setMAlertDialog(null);
                    }

                    if (finishActivity) {
                        try {
                            ForcedDialogs forcedDialogs = SharedPreferencesHandler.getForcedDialogs(activity);
                            forcedDialogs.unsetResponseCodeAndMessage();
                            SharedPreferencesHandler.setForcedDialogs(activity, forcedDialogs);
                        }
                        catch ( NoStoredObjectException | IOException | GeneralSecurityException |
                                NullPointerException | DecoderException e ) {

                            logSharedPreferencesError(
                                    "Exception caught when unsetting the 'forced response code error " +
                                            "dialog message' value",
                                    e
                            );
                        }

                        activity.finish();
                    }
                });
        alertDialog.setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);

        safeShowDialog(alertDialog);
    }

    /**
     * Display when the device cannot connect to the server.
     *
     * @param activity          The calling activity.
     */
    public static void serverConnectionFailedDialog(@NonNull final BaseActivity activity) {
        genericConfirmationDialog(
                activity,
                "Server connection failed",
                "This error occurs if your device is not connected to the internet, " +
                        "or the remote server has become temporarily unavailable.",
                true
        );
    }

    /**
     * Display when the contact has entered an invalid input.
     *
     * @param activity          The calling activity.
     * @param displayMessage    The message to display to the contact (the display title is fixed).
     * @param safeShow          Set to true to log a message and continue without showing an AlertDialog if any (fatal)
     *                          exception is thrown by AlertDialog.show(), set to false otherwise.
     */
    public static void invalidInputDialog(@NonNull final BaseActivity activity,
                                          @NonNull String displayMessage,
                                          boolean safeShow) {

        genericConfirmationDialog(activity, "Invalid input", displayMessage, safeShow);
    }

    /**
     * Display when the contact has entered an invalid input for the registration request.
     *
     * @param activity          The calling activity.
     * @param displayMessage    The message to display to the contact (the display title is fixed).
     * @param safeShow          Set to true to log a message and continue without showing an AlertDialog if any (fatal)
     *                          exception is thrown by AlertDialog.show(), set to false otherwise.
     */
    public static void invalidInputAccountRegistrationDialog(@NonNull final BaseActivity activity,
                                                             @NonNull String displayMessage,
                                                             boolean safeShow) {

        genericConfirmationDialog(activity, "Bad registration request", displayMessage, safeShow);
    }

    /**
     * Display when the contact has entered an invalid input for a request on the sign-in screen. This could be one of
     * the following operations:
     * * Sign-in
     * * Request an activation code.
     * * Request a recovery code.
     *
     * @param activity          The calling activity.
     * @param displayMessage    The message to display to the contact (the display title is fixed).
     */
    public static void invalidInputBadCredentialsDialog(@NonNull final BaseActivity activity,
                                                        @NonNull String displayMessage) {

        genericConfirmationDialog(activity, "Bad contact credentials", displayMessage, false);
    }

    /**
     * Display when the contact has entered an invalid input for the account recovery or password-change request.
     *
     * @param activity                          The calling activity.
     * @param changePasswordForAccountRecovery  True iff the contact is changing their password because they are
     *                                          recovering their account.
     */
    public static void noStoredDeviceKeyOrCodeDialog(@NonNull final BaseActivity activity,
                                                     boolean changePasswordForAccountRecovery) {

        final String title = changePasswordForAccountRecovery ? "Bad recovery request" : "Bad password-change request";

        final String displayMessage = "The data required to change your account password is not stored on this " +
                "device. Ensure that you are attempting this request from the last device you signed in or " +
                "activated your account with.";

        genericConfirmationDialog(activity, title, displayMessage, false);
    }

    /**
     * Display when the contact has successfully requested a code be sent to their email address.
     *
     * @param activity      The calling activity.
     * @param emailAddress  The destination email address the code was sent to, to display to the user as part of the
     *                      message dialog. If this is null, some default text will replace the email address.
     * @param codeType      The type of code requested, to display to the user as part of the message dialog.
     */
    public static void codeRequestSuccessfulDialog(@NonNull final BaseActivity activity,
                                                   @Nullable final String emailAddress,
                                                   @NonNull String codeType) {

        // Format the AlertDialog message.

        SpannableStringBuilder ssBuilder = new SpannableStringBuilder(
                String.format("A new %s code has been sent to ", codeType)
        );

        if (emailAddress == null) {
            ssBuilder.append("your email address");
        }
        else {
            int spanStart = ssBuilder.length();
            ssBuilder.append(emailAddress);
            int spanEnd = ssBuilder.length();
            ssBuilder.setSpan(new StyleSpan(Typeface.ITALIC), spanStart, spanEnd, 0);
        }

        ssBuilder.append(". ");

        genericConfirmationDialog(
                activity,
                "Code request successful",
                RegisterActivity.getCodeEmailInstructions(ssBuilder),
                true
        );
    }

    /**
     * Display when the contact successfully changes their name.
     *
     * @param activity  The calling activity.
     */
    public static void setContactNameDialog(@NonNull final BaseActivity activity) {
        genericConfirmationDialog(
                activity,
                "Contact information changed",
                "Your contact name has been updated.",
                true
        );
    }

    /**
     * Display when a target contact has been successfully added or removed.
     *
     * @param activity      The calling activity.
     * @param contactAdded  True if the contact has been added and false it it has been removed.
     * @param contactName   The name of the contact who has been added/removed.
     */
    public static void addedOrRemovedContactDialog(@NonNull final BaseActivity activity,
                                                   boolean contactAdded,
                                                   @NonNull String contactName) {

        final String title;
        final String message;
        if (contactAdded) {
            title = "Known contact added";
            message = String.format("You have successfully added\n%s.", contactName);
        }
        else {
            title = "Known contact removed";
            message = String.format("You have successfully removed\n%s.", contactName);
        }

        genericConfirmationDialog(activity, title, message, true);
    }

    /**
     * Display when the contact successfully changes the currently selected group.
     *
     * @param activity  The calling activity.
     */
    public static void selectGroupDialog(@NonNull final BaseActivity activity) {
        genericConfirmationDialog(
                activity,
                "Group selected",
                "Your selected group has been updated.",
                true
        );
    }

    /**
     * Display when the contact successfully changes their name.
     *
     * @param activity  The calling activity.
     */
    public static void setGroupNameDialog(@NonNull final BaseActivity activity) {
        genericConfirmationDialog(
                activity,
                "Group information changed",
                "The group name has been updated.",
                true
        );
    }

    /**
     * Display when the contact successfully changes their name.
     *
     * @param activity  The calling activity.
     */
    public static void setGroupDescriptionDialog(@NonNull final BaseActivity activity) {
        genericConfirmationDialog(
                activity,
                "Group information changed",
                "The group description has been updated.",
                true
        );
    }

    /**
     * Display when the contact attempts to modify or remove the default group.
     *
     * @param activity  The calling activity.
     */
    public static void cannotModifyDefaultGroupDialog(@NonNull final BaseActivity activity) {
        genericConfirmationDialog(
                activity,
                "Group unchanged",
                "The default group cannot be modified or removed.",
                false
        );
    }

    /**
     * Display when the contact attempts to create a group when they have already reached the limit for the number of
     * groups which they are allowed access to.
     *
     * @param activity  The calling activity.
     */
    public static void groupsLimitReachedDialog(@NonNull final BaseActivity activity) {
        genericConfirmationDialog(
                activity,
                "Groups limit reached",
                String.format(
                        "You cannot be a member of more than %d groups. Please leave a group before creating one.",
                        Vars.CONTACT_GROUPS_LIMIT
                ),
                true
        );
    }

    /**
     * Display when the contact has successfully submitted a request to share a group with another contact.
     *
     * @param activity      The calling activity.
     * @param contactName   The name of the (target) contact the group has been shared with.
     * @param groupName     The name of the group which has been shared.
     */
    public static void shareGroupDialog(@NonNull final BaseActivity activity,
                                        @NonNull String contactName,
                                        @NonNull String groupName) {

        final String msg = "Your request to share group %s with %s is now pending and awaiting their approval.";
//        final String msg = "You have successfully submitted a request to share group %s with %s, they must now " +
//                "accept this request to gain access to the group.";

        genericConfirmationDialog(
                activity,
                "Group sharing request submitted",
                String.format(msg, groupName, contactName),
                true
        );
    }

    /**
     * Display when the contact has chosen to sign-out.
     *
     * @see #signOutDialog(BaseActivity, ForcedDialogs.ForcedSignOutAction, boolean)
     *
     * @param activity              The calling activity.
     */
    public static void signOutDialog(@NonNull BaseActivity activity) {
        signOutDialog(activity, null, false);
    }

    /**
     * Display an AlertDialog with a title and message representing the given string.
     *
     * This method should be called when either the user manually requests to sign-out (a confirmation box will be
     * displayed allowing the user to cancel the sign-out) or an automatic sign-out occurs (a confirmation box informs
     * the user of a forced sign-out); an automatic sign-out typically occurs when the app receives a local broadcast
     * indicating an XMPP connection error.
     *
     * Upon confirming the sign-out operation, any contact-specific global data is removed from the application,
     * the the XMPP connection service is terminated, and all activities are finished before displaying the sign-in
     * activity.
     *
     * @param activity              The calling activity.
     * @param forcedSignOutAction   The type of forced sign-out action.
     * @param allowSetForcedDialog  True iff the forced dialog should be set in SharedPreferences (if finishActivity is
     *                              also true).
     *                              Note that the forced dialog is always removed regardless of this option when the
     *                              user clicks OK.
     */
    public static void signOutDialog(@NonNull BaseActivity activity,
                                     @Nullable ForcedDialogs.ForcedSignOutAction forcedSignOutAction,
                                     boolean allowSetForcedDialog) {

        final boolean isManualSignOut;
        final String alertDialogTitle;
        final String alertDialogMessage;

        if (ForcedDialogs.ForcedSignOutAction.CONNECTION_CLOSED_CONFLICT.equals(forcedSignOutAction)) {
            isManualSignOut    = false;
            alertDialogTitle   = "Connection terminated";
            alertDialogMessage = String.format(
                    "Your account has signed in on another device. " +
                            "To continue to use %s you will need to sign in again on this device.",
                    Vars.APP_NAME
            );
        }
        else if (ForcedDialogs.ForcedSignOutAction.CONNECTION_CLOSED_OTHER.equals(forcedSignOutAction)) {
            isManualSignOut    = false;
            alertDialogTitle   = "Connection terminated unexpectedly";
            alertDialogMessage = String.format(
                    "To continue to use %s you will need to sign in again on this device.",
                    Vars.APP_NAME
            );
        }
        else if (ForcedDialogs.ForcedSignOutAction.PWD_CHANGE_STORE_DATA_ERROR.equals(forcedSignOutAction)) {
            isManualSignOut    = false;
            alertDialogTitle   = "Password change successful";
            alertDialogMessage = "Your password has been successfully changed but you must sign in again.";
        }
        else {
            String messageSuffix;
            try {
                messageSuffix = " " + Contact.getCurrentContact().getName();
            }
            catch (NullStaticVariableException e) {
                messageSuffix = "";
            }

            isManualSignOut    = true;
            alertDialogTitle   = "Sign out";
            alertDialogMessage = String.format("Are you sure you want to sign out%s?", messageSuffix);
        }

        // Display the appropriate AlertDialog message depending on whether or not the sign-out was manually requested.

        final AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
        activity.setMAlertDialog(alertDialog);
        alertDialog.setTitle(alertDialogTitle);
        alertDialog.setMessage(alertDialogMessage);

        if (isManualSignOut) {
            // Optional sign-out.

            alertDialog.setButton(
                    AlertDialog.BUTTON_NEGATIVE,
                    activity.getResources().getString(android.R.string.no),
                    (dialog, which) -> {
                        dialog.dismiss();
                        if (alertDialog.equals(activity.getMAlertDialog())) {
                            activity.setMAlertDialog(null);
                        }
                    });
            alertDialog.setButton(
                    AlertDialog.BUTTON_POSITIVE,
                    activity.getResources().getString(android.R.string.yes),
                    (dialog, which) -> {
                        dialog.dismiss();
                        if (alertDialog.equals(activity.getMAlertDialog())) {
                            activity.setMAlertDialog(null);
                        }

                        ConnectionService.stopConnectionService(activity);

                        startSignInActivity(activity);
                    });
        }
        else {
            // Forced sign-out.

            ConnectionService.stopConnectionService(activity);

            // Update the stored forced dialogs object with the forced sign-out action.
            if (allowSetForcedDialog) {
                try {
                    ForcedDialogs forcedDialogs = SharedPreferencesHandler.getForcedDialogs(activity);
                    forcedDialogs.setForcedSignOutAction(forcedSignOutAction);
                    SharedPreferencesHandler.setForcedDialogs(activity, forcedDialogs);
                }
                catch ( NoStoredObjectException | IOException | GeneralSecurityException | NullPointerException |
                        DecoderException e ) {

                    logSharedPreferencesError(
                            "Exception caught when setting the 'forced sign-out' value",
                            e
                    );
                }
            }

            alertDialog.setButton(
                    AlertDialog.BUTTON_POSITIVE,
                    activity.getResources().getString(android.R.string.ok),
                    (dialog, which) -> {
                        dialog.dismiss();
                        if (alertDialog.equals(activity.getMAlertDialog())) {
                            activity.setMAlertDialog(null);
                        }

                        startSignInActivity(activity);
                    });
        }

        alertDialog.setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();
    }

    private static void startSignInActivity(@NonNull BaseActivity activity) {
        /* Erase all traces of the contact from internal memory. */

        SharedPreferencesHandler.removeAllContactData(activity);

        if (BaseMainActivity.class.isAssignableFrom(activity.getClass())) {
            ((BaseMainActivity) activity).clearBrowserState();
        }

        // Since all user-specific data has been cleared from memory, the check(s) in MainActivity should restart the
        // sign-in activity, even when the MainActivity is started here.
        Intent signInIntent = new Intent(activity, MainActivity.class);
        signInIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(signInIntent);
        activity.finish();
    }

    /* AlertDialog message generation */

    private static final String LEGAL_CHARACTERS_MSG =
            "Legal characters are uppercase and lowercase English letters, the digits 0 to 9, the space character " +
            "and the following characters:\n" + GeneralUtils.VALID_PUNCTUATION_AND_SYMBOLS;

    public static final String EMAIL_EMPTY_MSG = "You must enter an email address.";
    public static final String EMAIL_LENGTH_INVALID = String.format(
            "The email address must be between %d and %d characters (inclusive).",
            Vars.EMAIL_ADDRESS_MIN_LEN,
            Vars.EMAIL_ADDRESS_MAX_LEN
    );
    public static final String EMAIL_INVALID_MSG =
            "You entered an invalid email address. Please enter a valid email address.";

    public static final String ENTERED_PASSWORD_EMPTY_MSG =
            String.format("You must enter your %s account password.", Vars.APP_NAME);
    public static final String ENTERED_PASSWORD_NOT_PRINTABLE = String.format(
            "The password cannot contain any illegal characters. %s",
            LEGAL_CHARACTERS_MSG
    );
    public static final String ENTERED_PASSWORD_LENGTH_INVALID = String.format(
            "The password must be between %d and %d characters (inclusive).",
            Vars.CONTACT_PWD_MIN_LEN,
            Vars.CONTACT_PWD_MAX_LEN
    );

    public static final String CHOSEN_PASSWORD_EMPTY_MSG = "You must choose%sa password.";
    public static final String CHOSEN_PASSWORD_NOT_PRINTABLE = String.format(
            "Your chosen password cannot contain any illegal characters. %s",
            LEGAL_CHARACTERS_MSG
    );
    public static final String CHOSEN_PASSWORD_LENGTH_INVALID = String.format(
            "Your chosen password must be between %d and %d characters (inclusive).",
            Vars.CONTACT_PWD_MIN_LEN,
            Vars.CONTACT_PWD_MAX_LEN
    );
    public static final String CHOSEN_PASSWORDS_UNEQUAL_MSG =
            "Your chosen password and repeated password must be the same.";

    public static final String MOBILE_EMPTY_MSG = "You must enter a mobile telephone number.";
    public static final String MOBILE_LENGTH_INVALID = String.format(
            "Your mobile telephone number must be between %d and %d characters (inclusive).",
            Vars.MOBILE_NUMBER_MIN_LEN,
            Vars.MOBILE_NUMBER_MAX_LEN
    );
    public static final String MOBILE_INVALID_MSG =
            "You entered an invalid mobile telephone number. Please enter a valid mobile telephone number.";

    /**
     * Check that an email address is valid an if it is not throw an exception with a message to be used as the
     * message for an AlertDialog.
     *
     * Note that this method does not guarantee the existence of the given email address, only that it is valid.
     *
     * @param emailAddress                  The contact's email address.
     * @throws InvalidUserInputException    Thrown when the email address is invalid.
     */
    public static void checkValidEmailAddress(@NonNull String emailAddress) throws InvalidUserInputException {
        if (emailAddress.isEmpty()) {
            throw new InvalidUserInputException(EMAIL_EMPTY_MSG);
        }

        if (emailAddress.length() < Vars.EMAIL_ADDRESS_MIN_LEN || emailAddress.length() > Vars.EMAIL_ADDRESS_MAX_LEN) {
            throw new InvalidUserInputException(EMAIL_LENGTH_INVALID);
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()) {
            throw new InvalidUserInputException(EMAIL_INVALID_MSG);
        }
    }

    /**
     * Check that a given password is valid and if it is not throw an exception with a message to be used as the
     * message for an AlertDialog.
     *
     * This method is used when checking an existing password.
     *
     * @param password                      The contact's password.
     * @throws InvalidUserInputException    Thrown when the password is invalid.
     */
    public static void checkValidContactPassword(@NonNull String password) throws InvalidUserInputException {
        handleCheckValidContactPasswords(false, password, password, false);
    }

    /**
     * Check that two given passwords are valid and if they are not throw an exception with a message to be used as the
     * message for an AlertDialog.
     *
     * This method is used when creating a new password or changing a password.
     *
     * @param password1                     The contact's password.
     * @param password2                     The contact's password repeated.
     * @param passwordChange                True if the contact is changing their password, false if they are choosing
     *                                      their first password.
     * @throws InvalidUserInputException    Thrown when either password is invalid.
     */
    public static void checkValidContactPasswords(@NonNull String password1,
                                                  @NonNull String password2,
                                                  boolean passwordChange) throws InvalidUserInputException {

        handleCheckValidContactPasswords(true, password1, password2, passwordChange);
    }

    private static void handleCheckValidContactPasswords(boolean chosenPassword,
                                                         @NonNull String password1,
                                                         @NonNull String password2,
                                                         boolean passwordChange) throws InvalidUserInputException {

        if (password1.isEmpty()) {
            throw new InvalidUserInputException(
                    chosenPassword ?
                            String.format(CHOSEN_PASSWORD_EMPTY_MSG, passwordChange ? " new " : " ") :
                            ENTERED_PASSWORD_EMPTY_MSG
            );
        }

        if (!GeneralUtils.isPrintableString(password1)) {
            throw new InvalidUserInputException(
                    chosenPassword ? CHOSEN_PASSWORD_NOT_PRINTABLE : ENTERED_PASSWORD_NOT_PRINTABLE
            );
        }

        if (password1.length() < Vars.CONTACT_PWD_MIN_LEN || password1.length() > Vars.CONTACT_PWD_MAX_LEN) {
            throw new InvalidUserInputException(
                    chosenPassword ? CHOSEN_PASSWORD_LENGTH_INVALID : ENTERED_PASSWORD_LENGTH_INVALID
            );
        }

        if (!password1.equals(password2)) {
            throw new InvalidUserInputException(CHOSEN_PASSWORDS_UNEQUAL_MSG);
        }
    }

    // TODO: Remove this method if it is not going to be used.
    /**
     * Check that a given mobile telephone number is valid and if it is not throw an exception with a message to be
     * used as the message for an AlertDialog.
     *
     * @param mobileNumber                  The contact's mobile telephone number.
     * @throws InvalidUserInputException    Thrown when the mobile telephone number is invalid.
     */
    public static void checkValidMobileNumber(@NonNull String mobileNumber) throws InvalidUserInputException {
        if (mobileNumber.isEmpty()) {
            throw new InvalidUserInputException(MOBILE_EMPTY_MSG);
        }

        if (mobileNumber.length() < Vars.MOBILE_NUMBER_MIN_LEN || mobileNumber.length() > Vars.MOBILE_NUMBER_MAX_LEN) {
            throw new InvalidUserInputException(MOBILE_LENGTH_INVALID);
        }

        if (!PhoneNumberUtils.isGlobalPhoneNumber(mobileNumber)) {
            throw new InvalidUserInputException(MOBILE_INVALID_MSG);
        }
    }
}
