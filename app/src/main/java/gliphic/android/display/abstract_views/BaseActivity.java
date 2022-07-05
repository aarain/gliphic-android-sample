/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.display.abstract_views;

import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import gliphic.android.display.TermsOfUseActivity;
import gliphic.android.display.libraries.AlertDialogs;
import gliphic.android.display.libraries.LoadingDialog;
import gliphic.android.display.libraries.Views;
import gliphic.android.exceptions.NoStoredObjectException;
import gliphic.android.operation.storage_handlers.ForcedDialogs;
import gliphic.android.operation.storage_handlers.SharedPreferencesHandler;

import org.bouncycastle.util.encoders.DecoderException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

/**
 * This activity extends {@link AppCompatActivity} and should be the (possibly distant) parent class of all other
 * activities. The only exception(s) are:
 * * {@link TermsOfUseActivity}
 *
 * Any method which creates an AlertDialog or LoadingDialog could be called from any activity regardless of
 * whether or not the contact has signed in, so even if an activity cannot be finished whilst there is still a dialog
 * on screen (i.e. pre-sign-in activities) they still must extend this class to ensure that if an external method calls
 * either getter/setter methods in this class the variables still exist. In short, this activity should be the direct
 * parent class of all other activities which are started before a contact signs in.
 */
public abstract class BaseActivity extends AppCompatActivity {
    private AlertDialog mAlertDialog = null;
    private LoadingDialog mLoadingDialog = null;

    public AlertDialog getMAlertDialog() {
        return mAlertDialog;
    }

    public LoadingDialog getMLoadingDialog() {
        return mLoadingDialog;
    }

    public void setMAlertDialog(AlertDialog alertDialog) {
        mAlertDialog = alertDialog;
    }

    public void setMLoadingDialog(LoadingDialog loadingDialog) {
        mLoadingDialog = loadingDialog;
    }

    /* Activate or deactivate view-clicking. */

    private boolean activeClickableViews = true;
    private List<View> allClickableViews = new ArrayList<>();

    /**
     * Add to the list of clickable views available to this activity.
     *
     * The views supplied to this method are typically have some interactive element; this excludes, for example, any
     * {@link TextView} which is clickable for copy-and-paste purposes but has no interactive element such as starting
     * a new activity.
     *
     * @param views     The views to add to the list of clickable views for the activity.
     */
    public void addToAllClickableViews(@NonNull List<View> views) {
        allClickableViews.addAll(views);
    }

    /**
     * This method is typically called when the device is waiting for a result from an asynchronous task in order to
     * prevent any further inputs.
     */
    public void deactivateClickableViews() {
        this.activeClickableViews = false;
        Views.setAlphaAndEnabled(allClickableViews, false);
    }

    /**
     * This method is typically called after an asynchronous task has received its result and after calling the
     * {@link #deactivateClickableViews()} method.
     */
    public void activateClickableViews() {
        this.activeClickableViews = true;
        Views.setAlphaAndEnabled(allClickableViews, true);
    }

    /* Handle displaying forced activities/dialogs */

    @Override
    public void onStart() {
        super.onStart();

        // Force the Terms of Use activity to start if the TOU has not been accepted on this device.
        if (!SharedPreferencesHandler.isTermsOfUseAgreed(BaseActivity.this)) {
            // Start the Terms of Use activity.
            Intent myIntent = new Intent(BaseActivity.this, TermsOfUseActivity.class);
            startActivity(myIntent);

            return;
        }

        // Show the forced sign-out dialog (when applicable) for the first activity which calls this method.
        if (mAlertDialog == null) {
            try {
                final ForcedDialogs forcedDialogs = SharedPreferencesHandler.getForcedDialogs(BaseActivity.this);

                // The order the member variables are checked is important.
                if (forcedDialogs.isForcedSignOutAction()) {
                    AlertDialogs.signOutDialog(
                            BaseActivity.this,
                            forcedDialogs.getForcedSignOutAction(),
                            false
                    );
                }
                else if (forcedDialogs.isResponseCodeAndMessage()) {
                    AlertDialogs.responseCodeDialog(
                            BaseActivity.this,
                            forcedDialogs.getResponseCodeAndMessage().getResponseCode(),
                            forcedDialogs.getResponseCodeAndMessage().getDisplayMessage(),
                            true,
                            false
                    );
                }
                else if (forcedDialogs.isInternalErrorMessage()) {
                    AlertDialogs.internalErrorDialog(
                            BaseActivity.this,
                            true,
                            forcedDialogs.getInternalErrorMessage(),
                            false
                    );
                }
            }
            catch ( NoStoredObjectException | IOException | GeneralSecurityException | DecoderException |
                    NullPointerException e ) {

                // No assumption is made that the dialog should be displayed unless the relevant action is known.
            }
        }
    }

    @Override
    protected void onStop() {
        if (getMAlertDialog() != null && getMAlertDialog().isShowing()) {
            getMAlertDialog().dismiss();
            setMAlertDialog(null);
        }

        LoadingDialog.safeDismiss(getMLoadingDialog());     // This method handles setting the dialog to null.

        super.onStop();
    }

    /* Handle pressing the back/up button. */

    @Override
    public boolean onSupportNavigateUp() {
        return handleBackOrUpPressed();
    }

    @Override
    public void onBackPressed() {
        handleBackOrUpPressed();
    }

    private boolean handleBackOrUpPressed() {
        if (activeClickableViews) {
            super.onBackPressed();
        }

        return activeClickableViews;
    }
}
