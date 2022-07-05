/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.display.libraries;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import gliphic.android.adapters.AlertsAdapter;
import gliphic.android.adapters.ContactsAdapter;
import gliphic.android.adapters.GroupsAdapter;

/**
 * AlertDialog messages to display across multiple Activities.
 *
 * All of the methods in this class try to cancel any existing Toast message to prevent multiple Toast overlaps in a
 * short time span.
 *
 * The suppressed warning indicates that the {@link #showToast(Toast)} method should be called in all methods intending
 * to display a Toast.
 */
@SuppressWarnings("ShowToast")
public class Toasts {
    // A Toast is not tied to any activity so it is stored statically here.
    private static android.widget.Toast lastDisplayedToast = null;

    private static void showToast(Toast toast) {
        if (lastDisplayedToast != null) {
            lastDisplayedToast.cancel();
        }

        lastDisplayedToast = toast;

        toast.show();
    }

    /**
     * Display a {@link Toast} message with the given text for a short amount of time.
     *
     * @param context   The calling context (usually obtained via getContext() or ActivityName.this).
     * @param text      The message to display.
     */
    public static void showShortToast(@NonNull final Context context, @NonNull final String text) {
        Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);

        showToast(toast);
    }

    /**
     * Display a {@link Toast} message with the given text for a long amount of time.
     *
     * @param context   The calling context (usually obtained via getContext() or ActivityName.this).
     * @param text      The message to display.
     */
    public static void showLongToast(@NonNull final Context context, @NonNull final String text) {
        Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);

        showToast(toast);
    }

    /**
     * Display a Toast message for when the user inputs an invalid search string into an EditText.
     *
     * @param context   The calling context (usually obtained via getContext() or ActivityName.this).
     */
    public static void showInvalidSearchPatternToast(Context context) {
        showShortToast(context, "Invalid search pattern.");
    }

    /**
     * Display a Toast message for when the user inputs an invalid contact ID into an EditText.
     *
     * @param context   The calling context (usually obtained via getContext() or ActivityName.this).
     */
    public static void showInvalidSearchContactIdToast(Context context) {
        showShortToast(context, "Search input must be a valid contact ID.");
    }

    /**
     * Display a Toast message for when the user inputs an invalid contact name into an EditText.
     *
     * @param context   The calling context (usually obtained via getContext() or ActivityName.this).
     */
    public static void showInvalidSearchContactNameToast(Context context) {
        showShortToast(context, "Search input must be a valid contact name.");
    }

    /**
     * Display a Toast message for when alerts cannot be loaded from the server.
     *
     * @param context   The calling context (usually obtained via getContext() or ActivityName.this).
     */
    public static void showCannotLoadAlertsToast(Context context) {
        showShortToast(context, AlertsAdapter.GENERIC_LOAD_ALERTS_FAILED_MSG);
    }

    /**
     * Display a Toast message for when target contacts cannot be loaded from the server.
     *
     * @param context   The calling context (usually obtained via getContext() or ActivityName.this).
     */
    public static void showCannotLoadContactsToast(Context context) {
        showShortToast(context, ContactsAdapter.GENERIC_LOAD_CONTACTS_FAILED_MSG);
    }

    /**
     * Display a Toast message for when target groups cannot be loaded from the server.
     *
     * @param context   The calling context (usually obtained via getContext() or ActivityName.this).
     */
    public static void showCannotLoadGroupsToast(Context context) {
        showShortToast(context, GroupsAdapter.GENERIC_LOAD_GROUPS_FAILED_MSG);
    }

    /**
     * Display a Toast message for when the user's email address cannot be obtained.
     *
     * @param context           The calling context (usually obtained via getContext() or ActivityName.this).
     */
    public static void showCannotDisplayEmailToast(Context context) {
        showLongToast(context, "Unable to display email address.");
    }

    /**
     * Display a Toast message for when the user's email address cannot be stored in local storage.
     *
     * @param context               The calling context (usually obtained via getContext() or ActivityName.this).
     */
    public static void showCannotRememberEmailToast(Context context) {
        showCannotRememberEmailToast(context, "Unable to remember/forget email address.");
    }

    /**
     * Display a Toast message for when the user's email address cannot be stored in local storage.
     *
     * @param context               The calling context (usually obtained via getContext() or ActivityName.this).
     * @param rememberEmailAddress  Set to true if the email address was to be stored, false if it was to be removed.
     */
    public static void showCannotRememberEmailToast(Context context, boolean rememberEmailAddress) {
        showCannotRememberEmailToast(
                context,
                String.format("Unable to %s email address.", rememberEmailAddress ? "remember" : "forget")
        );
    }

    private static void showCannotRememberEmailToast(Context context, String text) {
        showLongToast(context, text);
    }
}
