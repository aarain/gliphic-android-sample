/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.display.libraries;

import android.app.Dialog;
import android.os.Handler;
import android.view.WindowManager;
import android.widget.TextView;

import gliphic.android.R;
import gliphic.android.display.abstract_views.BaseActivity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import gliphic.android.operation.misc.Log;

/**
 * This class allows a dialog to be shown in an activity after a specified time; the timer is started as soon as an
 * instance of this class is created.
 *
 * Delaying the time before the dialog is shown stops it from being displayed immediately and disappearing very soon
 * after, which is a situation that is likely to occur in the majority of server requests, if a reasonable internet
 * connection is assumed.
 *
 * The expected response threshold determines the mandatory delay for the dialog. It is the expected time, in
 * milliseconds, that the a HTTP response from the server is unlikely to exceed.
 */
public class LoadingDialog {
    private BaseActivity callingActivity;
    private Dialog dialog;
    private boolean hasBeenDismissed;
    private String loadingMessage;

    /**
     * @see #LoadingDialog(BaseActivity, String)
     */
    public LoadingDialog(@NonNull BaseActivity activity) {
        this(activity, activity.getResources().getString(R.string.progress_bar_default));
    }

    /**
     * Instantiate an instance of this object and display a new dialog.
     *
     * @see #showDialog()
     *
     * @param activity  The activity to display the dialog in and which will disable user interaction.
     * @param message   The message to display in the dialog.
     */
    public LoadingDialog(@NonNull BaseActivity activity, @NonNull String message) {
        this.callingActivity = activity;
        this.loadingMessage = message;

        showDialog();
    }

    /**
     * Display a new dialog using the activity previously initialized in the constructor.
     *
     * The ability for the user to interact with the given activity is also disabled.
     */
    public void showDialog() {
        // Disable user interaction with the activity immediately.
        this.callingActivity.getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        );

        AlertDialog.Builder builder = new AlertDialog.Builder(this.callingActivity);
        builder.setView(R.layout.progress_bar);
        Dialog dialog = builder.create();
        dialog.setCancelable(false);

        // Setting the text must be done in a listener to ensure that the TextView is not null.
        dialog.setOnShowListener(dialogInterface -> {
            final TextView textView = dialog.findViewById(R.id.progress_bar_textview);

            // The TextView should not be null, but if it is this listener does nothing and the default text is shown.
            if (textView != null) {
                textView.setText(this.loadingMessage);
            }
        });

        this.callingActivity.setMLoadingDialog(this);

        this.dialog = dialog;
        this.hasBeenDismissed = false;

        // Only show the dialog if the expectedResponseThreshold time has elapsed, and a response from the server has
        // not already been received.
        final int expectedResponseThreshold = 500;
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            if (!hasBeenDismissed) {
                try {
                    dialog.show();
                }
                catch (Exception e) {
                    Log.e("Dialog show", e.getMessage());
                }
            }
        }, expectedResponseThreshold);
    }

    /**
     * Dismisses the dialog and prevents it from being displayed again (for this class instance).
     *
     * The ability for the user to interact with the given activity is also enabled.
     *
     * This method should be called after a server response has been received (e.g. in both onResponse() and
     * onErrorResponse() overridden methods in Response.Listener).
     */
    public void dismissDialog() {
        hasBeenDismissed = true;

        try {
            dialog.dismiss();
        }
        catch (Exception e) {
            Log.e("Dialog dismiss", e.getMessage());
        }

        if (this.equals(callingActivity.getMLoadingDialog())) {
            callingActivity.setMLoadingDialog(null);
        }

        // Enable user interaction with the activity.
        callingActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    /**
     * This method should be called when the desire is to call the dismissDialog method on a possibly null instance of
     * this class.
     *
     * @see #dismissDialog().
     *
     * @param loadingDialog     The dialog to dismiss, which may be null.
     * @return                  True if the given dialog was dismissed, false if it was not because it is null.
     */
    public static boolean safeDismiss(@Nullable LoadingDialog loadingDialog) {
        if (loadingDialog != null) {
            loadingDialog.dismissDialog();
        }

        return loadingDialog != null;
    }
}
