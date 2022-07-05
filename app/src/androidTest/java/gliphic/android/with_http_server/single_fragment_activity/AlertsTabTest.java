package gliphic.android.with_http_server.single_fragment_activity;

import android.os.SystemClock;
import android.view.View;

import gliphic.android.R;
import gliphic.android.display.welcome_screen.SignInActivity;
import gliphic.android.operation.Group;
import gliphic.android.operation.server_interaction.http_server.HttpOperations;
import gliphic.android.utils.AndroidTestUtils;
import gliphic.android.utils.view_assertions.AssertRecyclerViewItemCount;
import gliphic.android.utils.BaseActivityTestRule;
import gliphic.android.utils.view_actions.RecyclerViewAction;
import gliphic.android.with_http_server.functionality.CreateAndLeaveGroupTest;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.ViewAction;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.action.ViewActions.swipeRight;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;

/**
 * Test functionality unique to AlertsTab.
 *
 * Note that the tests for loading alerts are elsewhere.
 */
@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class AlertsTabTest {
    private static final long CURRENT_CONTACT_NUMBER = 0;
    private static final String UNIQUE_DESCRIPTION = "4566a3f8as141fd3kjp841a54";
    private static final int TOTAL_DISPLAYED_ITEMS = 1; // Assume that this is the number of pending received requests.

    private static final Matcher<View> RECYCLER_VIEW = withId(R.id.recyclerview_main_tab_alerts);
    private static final Matcher<View> CHECK_BOX     = withId(R.id.checkbox_alerts);

    @Rule
    public BaseActivityTestRule<SignInActivity> rule = new BaseActivityTestRule<>(SignInActivity.class);

    @Before
    public void navigateToAlertsTab() {
        onView(withId(R.id.viewpager_activity_base)).perform(swipeRight());
        SystemClock.sleep(200);
    }

    private void clickAccept() {
        final ViewAction viewAction = RecyclerViewAction.clickChildViewWithId(R.id.alert_item_accept);

        onView(RECYCLER_VIEW).perform(RecyclerViewActions.actionOnItemAtPosition(0, viewAction));

        SystemClock.sleep(200);
    }

    private void resetDatabase() throws Exception {
        AndroidTestUtils.getRequest(
                AndroidTestUtils.getUriPrefix() +
                        "/group/decline/confirm/replace-expected-pending-received-group-share"
        );

        SystemClock.sleep(1000);    // Allow time for the database modification.
    }

    private void setDataEncryptionKey() throws Exception {
        AndroidTestUtils.setDataEncryptionKey(AndroidTestUtils.getApplicationContext(), CURRENT_CONTACT_NUMBER);
    }

    @Test
    public void filterAlertsViaCheckBox() {
        final int numPendingReceivedItems = 1;
        final int numUncheckedItems       = 50;

        onView(CHECK_BOX).check(matches(isChecked()));  // The CheckBox should initially be checked.
        onView(RECYCLER_VIEW).check(new AssertRecyclerViewItemCount(numPendingReceivedItems));

        onView(CHECK_BOX).perform(click());
        SystemClock.sleep(200);
        onView(CHECK_BOX).check(matches(not(isChecked())));
        onView(RECYCLER_VIEW).check(new AssertRecyclerViewItemCount(numUncheckedItems));

        // Assert that filter(s) are kept on tab-refocus.
        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
        SystemClock.sleep(200);     // Give the swipe time to change tab focus and find R.id.*
        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
        SystemClock.sleep(200);     // Give the swipe time to change tab focus and find R.id.*
        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
        SystemClock.sleep(1000);    // Give the swipe time to change tab focus and find R.id.*
        onView(withId(R.id.viewpager_activity_base)).perform(swipeRight());
        SystemClock.sleep(200);     // Give the swipe time to change tab focus and find R.id.*
        onView(withId(R.id.viewpager_activity_base)).perform(swipeRight());
        SystemClock.sleep(200);     // Give the swipe time to change tab focus and find R.id.*
        onView(withId(R.id.viewpager_activity_base)).perform(swipeRight());
        SystemClock.sleep(1000);    // Give the swipe time to change tab focus and find R.id.*
        onView(CHECK_BOX).check(matches(not(isChecked())));
        onView(RECYCLER_VIEW).check(new AssertRecyclerViewItemCount(numUncheckedItems));

        onView(CHECK_BOX).perform(click());
        SystemClock.sleep(200);
        onView(CHECK_BOX).check(matches(isChecked()));
        onView(RECYCLER_VIEW).check(new AssertRecyclerViewItemCount(numPendingReceivedItems));
        // Bug fix test: Check that toggling the CheckBox does not keep incrementing the number of displayed contacts
        // by 1 (when the box is checked).
        onView(CHECK_BOX).perform(click());
        SystemClock.sleep(200);
        onView(CHECK_BOX).perform(click());
        SystemClock.sleep(200);
        onView(RECYCLER_VIEW).check(new AssertRecyclerViewItemCount(numPendingReceivedItems));
    }

    // Bug fix test: Ensure that the Alerts tab RecyclerView always displays all available group-shares not just those
    // loaded from the server. This prevents the RecyclerView displaying 1 and 0 pending-received group-shares
    // alternately since the server would load 0 and 1 pending-received group-shares alternately (respectively) and
    // reset the adapter with only whatever the server returned.
    @Test
    public void recyclerViewDisplaysAllAvailableAlerts() {
        final int numPendingReceivedItems = 1;

        onView(RECYCLER_VIEW).check(new AssertRecyclerViewItemCount(numPendingReceivedItems));

        // Navigate to another activity (group details)
        onView(ViewMatchers.withText(R.string.main_groups)).perform(click());
        SystemClock.sleep(200);
        onView(withId(R.id.recyclerview_main_tab_groups))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
        SystemClock.sleep(1000);

        // Navigate back to the Alerts tab.
        onView(withContentDescription(R.string.action_bar_home)).perform(click());
        SystemClock.sleep(1000);
        onView(withId(R.id.viewpager_activity_base)).perform(swipeRight());
        SystemClock.sleep(200);
        onView(withId(R.id.viewpager_activity_base)).perform(swipeRight());
        SystemClock.sleep(200);

        onView(RECYCLER_VIEW).check(new AssertRecyclerViewItemCount(numPendingReceivedItems));
    }

    @Test
    public void acceptGroupRequest() throws Throwable {
        // BUG FIX: Avoid the error "MAC check failed" after accepting the group request and receiving the RSA keypair
        // from the server, by ensuring that the contact's encrypted private key does not fail to decrypt.
        setDataEncryptionKey();

        // Accept the request and the confirmation dialog with a valid description.

        onView(RECYCLER_VIEW).check(new AssertRecyclerViewItemCount(TOTAL_DISPLAYED_ITEMS));

        clickAccept();

        onView(withId(R.id.alertdialog_edittext)).perform(replaceText(UNIQUE_DESCRIPTION));
        SystemClock.sleep(200);
        onView(withId(android.R.id.button1)).perform(click());

        try {
            SystemClock.sleep(3000);
            onView(withText(containsString("Accept request successful"))).check(matches(isDisplayed()));

            onView(withId(android.R.id.button1)).perform(click());
            SystemClock.sleep(200);

            onView(RECYCLER_VIEW).check(new AssertRecyclerViewItemCount(TOTAL_DISPLAYED_ITEMS - 1));

            onView(ViewMatchers.withId(R.id.checkbox_alerts)).perform(click());
            SystemClock.sleep(1000);
            onView(ViewMatchers.withId(R.id.recyclerview_main_tab_alerts))
                    .check(matches(hasDescendant(withText(containsString("You joined group 255grpName")))));
        }
        finally {
            resetDatabase();
        }
    }

    @Test
    public void acceptGroupRequestGroupsLimitReachedClientSide() throws Throwable {
        // Ensure that none of the group numbers to store statically are duplicated with the existing groups stored.
        final long firstGroupNumber = 1000;

        try {
            AndroidTestUtils.createManyGroups(firstGroupNumber);

            clickAccept();

            onView(withText(CreateAndLeaveGroupTest.EXPECTED_GROUPS_LIMIT_DIALOG_TITLE)).check(matches(isDisplayed()));
        }
        finally {
            Group.setNullKnownGroups();
        }
    }

    @Test
    public void acceptGroupRequestGroupsLimitReachedServerSide() throws Throwable {
        // BUG FIX: Avoid the error "MAC check failed" after accepting the group request and receiving the RSA keypair
        // from the server, by ensuring that the contact's encrypted private key does not fail to decrypt.
        setDataEncryptionKey();

        final String currentContactNumber = Long.toString(CURRENT_CONTACT_NUMBER);
        final String urlSuffix = "/enough-to-reach-limit";

        try {
            // Ensure that the server has created enough groups for this contact to reach the limit.
            AndroidTestUtils.postString(HttpOperations.URI_CREATE_GROUP + urlSuffix, currentContactNumber);
            SystemClock.sleep(2000);

            clickAccept();

            onView(withId(R.id.alertdialog_edittext)).perform(replaceText(UNIQUE_DESCRIPTION));
            SystemClock.sleep(200);
            onView(withId(android.R.id.button1)).perform(click());
            SystemClock.sleep(2000);
            onView(withText(CreateAndLeaveGroupTest.EXPECTED_GROUPS_LIMIT_DIALOG_TITLE)).check(matches(isDisplayed()));
        }
        finally {
            // Clean up the database by deleting the newly created group.
            AndroidTestUtils.postString(HttpOperations.URI_LEAVE_GROUP + urlSuffix, currentContactNumber);
        }
    }

    @Test
    public void acceptGroupRequestInvalidEncryptedGroupKey() throws Exception {
        AndroidTestUtils.getRequest(
                AndroidTestUtils.getUriPrefix() +
                        "/group/accept/failed/set-invalid-encrypted-key"
        );

        try {
            // Accept the request and the confirmation dialog with a valid description.

            onView(RECYCLER_VIEW).check(new AssertRecyclerViewItemCount(TOTAL_DISPLAYED_ITEMS));

            clickAccept();

            onView(withId(R.id.alertdialog_edittext)).perform(replaceText(UNIQUE_DESCRIPTION));
            SystemClock.sleep(200);
            onView(withId(android.R.id.button1)).perform(click());

            SystemClock.sleep(3000);
            onView(withText(containsString("Accept request failed"))).check(matches(isDisplayed()));
        }
        finally {
            resetDatabase();
        }
    }

    @Test
    public void acceptGroupRequestCancelConfirmationDialog() {
        // Accept the request but cancel the confirmation dialog (i.e. the dialog requiring a group description).

        onView(RECYCLER_VIEW).check(new AssertRecyclerViewItemCount(TOTAL_DISPLAYED_ITEMS));

        clickAccept();

        onView(withId(android.R.id.button2)).perform(click());
        SystemClock.sleep(200);
        onView(RECYCLER_VIEW).check(new AssertRecyclerViewItemCount(TOTAL_DISPLAYED_ITEMS));
    }

    @Test
    public void acceptGroupRequestWithInvalidDescription() {
        // Accept the request and the confirmation dialog with an invalid description.

        onView(RECYCLER_VIEW).check(new AssertRecyclerViewItemCount(TOTAL_DISPLAYED_ITEMS));

        clickAccept();

        onView(withId(android.R.id.button1)).perform(click());
        SystemClock.sleep(200);

        // Other failure cases are not tested here since the Group.checkValidDescription() method is tested elsewhere.
        onView(withText(containsString(Group.DESCRIPTION_EMPTY_MSG))).check(matches(isDisplayed()));
    }
}
