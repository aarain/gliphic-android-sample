/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.display.libraries;

import android.content.Intent;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import gliphic.android.R;
import gliphic.android.display.AccountSecurityActivity;
import gliphic.android.display.AddContactActivity;
import gliphic.android.display.ContactProfileActivity;
import gliphic.android.display.CreateGroupActivity;
import gliphic.android.display.GroupSelectionActivity;
import gliphic.android.display.ReportActivity;
import gliphic.android.display.abstract_views.BaseMainActivity;

import androidx.core.app.NavUtils;

/**
 * A collection of static methods to standardise the implementation of the TabLayout widget across the application.
 */
public class TabLayoutMethods {
    /**
     * Manually set the text color (and text by necessity) for the overflow menu items on devices which have a
     * hardware menu button.
     *
     * Note that setting the color of the overflow menu icon using android:textColorSecondary also sets the text
     * color in the overflow menu, only when opened by the hardware button, so this loop is necessary to override the
     * XML value.
     *
     * MenuItem and SpannableString became available on Android API 25 platform.
     *
     * @param menu  The Menu argument from the calling onOptionsItemSelected() method.
     */
    public static void onCreateOptionsMenuContent(Menu menu) {
        for (int i = 0; i < menu.size(); i++) {
            MenuItem menuItem = menu.getItem(i);
            SpannableString s = new SpannableString(menu.getItem(i).getTitle());
            s.setSpan(new ForegroundColorSpan(Color.BLACK), 0, s.length(), 0);
            menuItem.setTitle(s);
        }
    }

    /**
     * The factored-out implementation for overriding the onOptionsItemSelectedContent() method in the calling
     * Activity. This defines the same overflow drop-down implementation for use in multiple activities.
     *
     * The {@link Intent#FLAG_ACTIVITY_REORDER_TO_FRONT} flag is used when launching an activity to ensure that only a
     * single instance of the launched activity is in the task's history stack.
     *
     * @param item      The MenuItem argument from the calling onOptionsItemSelected() method.
     * @param activity  The calling activity containing the overridden onOptionsItemSelected() method.
     * @return          A boolean where a 'true' return value should return 'true' in the calling
     *                  onOptionsItemSelected() method, and a 'false' return value should return
     *                  'super.onOptionsItemSelected(item)' in the calling method.
     */
    public static boolean onOptionsItemSelectedContent(MenuItem item, final BaseMainActivity activity) {
        final int itemId = item.getItemId();

        if (itemId == R.id.action_bar_home) {
            NavUtils.navigateUpFromSameTask(activity);
        }
        else if (itemId == R.id.action_bar_group_select) {
            Intent intent = new Intent(activity, GroupSelectionActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            activity.startActivityForResult(intent, RecyclerViewSetup.UPDATE_RECYCLER_VIEWS);
        }
        else if (itemId == R.id.action_bar_group_create) {
            startActivity(activity, CreateGroupActivity.class);
        }
        else if (itemId == R.id.action_bar_contact_add) {
            startActivity(activity, AddContactActivity.class);
        }
        else if (itemId == R.id.action_bar_contact_profile) {
            startActivity(activity, ContactProfileActivity.class);
        }
        else if (itemId == R.id.action_bar_account_security) {
            startActivity(activity, AccountSecurityActivity.class);
        }
        else if (itemId == R.id.action_bar_report) {
            startActivity(activity, ReportActivity.class);
        }
        else if (itemId == R.id.action_bar_sign_out) {
            AlertDialogs.signOutDialog(activity);
        }
        else {
            return false;
        }

        return true;
    }

    private static void startActivity(@NonNull final BaseMainActivity activity, @NonNull final Class<?> clazz) {
        Intent intent = new Intent(activity, clazz);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        activity.startActivity(intent);
    }
}
