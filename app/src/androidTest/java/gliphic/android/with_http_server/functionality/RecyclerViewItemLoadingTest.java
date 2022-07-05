package gliphic.android.with_http_server.functionality;

import android.os.SystemClock;
import android.view.View;

import gliphic.android.R;
import gliphic.android.adapters.AlertsAdapter;
import gliphic.android.display.welcome_screen.SignInActivity;
import gliphic.android.operation.Alerts;
import gliphic.android.operation.Contact;
import gliphic.android.operation.Group;
import gliphic.android.utils.AndroidTestUtils;
import gliphic.android.utils.view_assertions.AssertRecyclerViewItemCount;
import gliphic.android.utils.RecyclerViewItemLoadingTestRule;
import gliphic.android.utils.view_actions.NestedScrollTo;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;

import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.filters.LargeTest;
import gliphic.android.utils.view_actions.SetViewVisibility;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.action.ViewActions.swipeRight;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
@LargeTest
public class RecyclerViewItemLoadingTest {
    private static final int    NUM_OF_ITEMS_APPENDED       = AlertsAdapter.MAX_NUM_OF_ITEMS_APPENDED;
    private static final long   TEST_CONTACT_NUMBER         = 0;
    private static final String TEST_CONTACT_NUMBER_STRING  = Long.toString(TEST_CONTACT_NUMBER);

    private static String uriPrefix;

    // Parametrised values for each test.
    private CONTEXT context;
    private String insertUri;
    private String deleteUri;
    private int initialDisplayItemsSize;
    private int textViewId;
    private int recyclerViewId;
    private int progressBarId;
    private int numOfObjectsPerLoad;

    private enum CONTEXT {
        ALERTS, GROUPS, CONTACTS, CONTACT_GROUPS, GROUP_CONTACTS, GROUP_SELECTION,
        GROUP_SHARE_CONTACTS, GROUP_SHARE_GROUPS
    }

    public RecyclerViewItemLoadingTest(CONTEXT context) {
        this.context = context;

        switch (context) {
            case ALERTS:
                this.insertUri                  = uriPrefix + "insert-pending-received-group-shares";
                this.deleteUri                  = uriPrefix + "delete-inserted-group-shares";
                this.initialDisplayItemsSize    = 1;
                this.textViewId                 = R.id.textview_load_more_alerts;
                this.recyclerViewId             = R.id.recyclerview_main_tab_alerts;
                this.progressBarId              = R.id.more_alerts_progress_bar;
                this.numOfObjectsPerLoad        = 100;
                break;
            case GROUPS:
                this.insertUri                  = uriPrefix + "insert-known-groups";
                this.deleteUri                  = uriPrefix + "delete-known-groups";
                this.initialDisplayItemsSize    = 1;
                this.textViewId                 = R.id.textview_load_more_groups;
                this.recyclerViewId             = R.id.recyclerview_main_tab_groups;
                this.progressBarId              = R.id.more_groups_progress_bar;
                this.numOfObjectsPerLoad        = 50;
                break;
            case CONTACTS:
                this.insertUri                  = uriPrefix + "insert-extended-contacts";
                this.deleteUri                  = uriPrefix + "delete-extended-contacts";
                this.initialDisplayItemsSize    = 0;
                this.textViewId                 = R.id.textview_load_more_contacts;
                this.recyclerViewId             = R.id.recyclerview_main_tab_contacts;
                this.progressBarId              = R.id.more_contacts_progress_bar;
                this.numOfObjectsPerLoad        = 50;
                break;
            case CONTACT_GROUPS:
                this.insertUri                  = uriPrefix + "insert-contact-groups";
                this.deleteUri                  = uriPrefix + "delete-contact-groups";
                this.initialDisplayItemsSize    = 0;
                this.textViewId                 = R.id.textview_load_more_contact_groups;
                this.recyclerViewId             = R.id.recyclerview_contact_details_tab_groups;
                this.progressBarId              = R.id.more_contact_groups_progress_bar;
                this.numOfObjectsPerLoad        = 50;
                break;
            case GROUP_CONTACTS:
                this.insertUri                  = uriPrefix + "insert-extended-contacts";
                this.deleteUri                  = uriPrefix + "delete-extended-contacts";
                this.initialDisplayItemsSize    = 0;
                this.textViewId                 = R.id.textview_load_more_group_contacts;
                this.recyclerViewId             = R.id.recyclerview_group_details_tab_target_contacts;
                this.progressBarId              = R.id.more_group_contacts_progress_bar;
                this.numOfObjectsPerLoad        = 50;
                break;
            case GROUP_SELECTION:
                this.insertUri                  = uriPrefix + "insert-known-groups";
                this.deleteUri                  = uriPrefix + "delete-known-groups";
                this.initialDisplayItemsSize    = 1;
                this.textViewId                 = R.id.textview_load_more_groups_selection;
                this.recyclerViewId             = R.id.recyclerview_group_selection;
                this.progressBarId              = R.id.group_selection_progress_bar;
                this.numOfObjectsPerLoad        = 50;
                break;
            case GROUP_SHARE_CONTACTS:
                this.insertUri                  = uriPrefix + "insert-known-contacts";
                this.deleteUri                  = uriPrefix + "delete-known-contacts";
                this.initialDisplayItemsSize    = 1;
                this.textViewId                 = R.id.textview_group_share_load_more_items;
                this.recyclerViewId             = R.id.recyclerview_group_share;
                this.progressBarId              = R.id.group_share_progress_bar;
                this.numOfObjectsPerLoad        = 50;
                break;
            case GROUP_SHARE_GROUPS:
                this.insertUri                  = uriPrefix + "insert-known-groups";
                this.deleteUri                  = uriPrefix + "delete-known-groups";
                this.initialDisplayItemsSize    = 1;
                this.textViewId                 = R.id.textview_group_share_load_more_items;
                this.recyclerViewId             = R.id.recyclerview_group_share;
                this.progressBarId              = R.id.group_share_progress_bar;
                this.numOfObjectsPerLoad        = 50;
                break;
            default:
                fail();
        }
    }

    @Parameterized.Parameters
    public static List<Object[]> testCases() {
        return Arrays.asList(new Object[][] {
                {CONTEXT.ALERTS},
                {CONTEXT.GROUPS},
                {CONTEXT.CONTACTS},
                {CONTEXT.CONTACT_GROUPS},
                {CONTEXT.GROUP_CONTACTS},
                {CONTEXT.GROUP_SELECTION},
                {CONTEXT.GROUP_SHARE_CONTACTS},
                {CONTEXT.GROUP_SHARE_GROUPS},
        });
    }

    @Rule
    public RecyclerViewItemLoadingTestRule<SignInActivity> rule =
            new RecyclerViewItemLoadingTestRule<>(SignInActivity.class);

    @BeforeClass
    public static void setUriPrefix() throws Exception {
        uriPrefix = AndroidTestUtils.getUriPrefix() + "/load/";
    }

    @After
    public void nullGlobalStatics() {
        AndroidTestUtils.clearStaticLists();
    }

    private void scrollToProgressBar() {
        onView(withId(progressBarId)).perform(SetViewVisibility.setViewVisibility(View.VISIBLE));
        onView(withId(progressBarId)).perform(NestedScrollTo.nestedScrollTo());
        SystemClock.sleep(2000);
    }

    private void selectLoadMoreTextView(int textViewId) {
        onView(withId(textViewId)).perform(click());
        SystemClock.sleep(2000);
    }

    private void assertListSizeAndDisplayedItems(int listSize, int displaySize) throws Exception {
        assertThat(getInitialInternalListSize(), is(listSize));
        onView(withId(recyclerViewId)).check(new AssertRecyclerViewItemCount(displaySize));

        if (context.equals(CONTEXT.ALERTS)) {
            final int largestDisplayedCount = 9;    // This constant is defined in MainActivity.

            final String alertsTabName;

            // Assume that all displayed alerts are pending-received group-shares.
            if (displaySize > largestDisplayedCount) {
                alertsTabName = String.format("%d+", largestDisplayedCount);
            }
            else {
                alertsTabName = Integer.toString(displaySize);
            }

            onView(withText(alertsTabName)).check(matches(isDisplayed()));
        }
    }

    private int getInitialInternalListSize() throws Exception {
        switch (context) {
            case ALERTS:
                return Alerts.getGroupShares().size();
            case GROUPS:
            case GROUP_SELECTION:
                return Group.getKnownGroups().size();
            case CONTACTS:
                return Contact.getExtendedContacts().size();
            case CONTACT_GROUPS:
                return Contact.getContactFromNumber(1).getCommonGroups().size();
            case GROUP_CONTACTS:
                return Group.getGroupFromNumber(4).getGroupTargetContacts().size();
            case GROUP_SHARE_CONTACTS:
                return Contact.getKnownContacts().size();
            case GROUP_SHARE_GROUPS:
                return Group.getKnownGroups().size() - 1;   // Subtract 1 to ignore the default group.
            default:
                fail();
                return 0;
        }
    }

    /**
     * Append items to a RecyclerView such that the total number of items after each increment is not a multiple of the
     * maximum number of items the corresponding adapter can append.
     *
     * e.g. If there are already 2 items in the RecyclerView and the maximum append-increment is 50, test that the
     * RecyclerView displays 52 items after the increment and not 50.
     *
     * This test also tests clicking the context's TextView to load more items.
     */
    @Test
    public void totalItemsAfterAppendIsNotMultipleForContactsTab() throws Exception {
        final String groupsFilter   = "26";
        final String contactsFilter = "40";

        // Navigate to the correct context.
        SystemClock.sleep(3000);
        switch (context) {
            case ALERTS:
                onView(withId(R.id.viewpager_activity_base)).perform(swipeRight());
                break;
            case GROUPS:
                onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
                SystemClock.sleep(200);
                onView(withId(R.id.edittext_main_tab_groups)).perform(replaceText(groupsFilter));
                SystemClock.sleep(1000);    // Allow time for the displayed items to get filtered.
                break;
            case CONTACTS:
                onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
                SystemClock.sleep(200);
                onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
                SystemClock.sleep(200);
                onView(ViewMatchers.withId(R.id.checkbox_main_extended_contacts)).perform(click());
                SystemClock.sleep(200);
                onView(ViewMatchers.withId(R.id.checkbox_main_known_contacts)).perform(click());
                SystemClock.sleep(200);
                onView(withId(R.id.edittext_main_tab_contacts)).perform(replaceText(contactsFilter));
                SystemClock.sleep(1000);    // Allow time for the displayed items to get filtered.
                break;
            case CONTACT_GROUPS:
                onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
                SystemClock.sleep(200);
                onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
                SystemClock.sleep(200);
                onView(withId(R.id.recyclerview_main_tab_contacts))
                        .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
                SystemClock.sleep(500);
                onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
                SystemClock.sleep(200);
                onView(withId(R.id.edittext_contact_groups)).perform(replaceText(groupsFilter));
                SystemClock.sleep(1000);    // Allow time for the displayed items to get filtered.
                break;
            case GROUP_CONTACTS:
                onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
                SystemClock.sleep(200);
                onView(withId(R.id.recyclerview_main_tab_groups))
                        .perform(RecyclerViewActions.actionOnItemAtPosition(3, click()));
                SystemClock.sleep(500);
                onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
                SystemClock.sleep(200);
                onView(ViewMatchers.withId(R.id.checkbox_group_extended_contacts)).perform(click());
                SystemClock.sleep(200);
                onView(ViewMatchers.withId(R.id.checkbox_group_known_contacts)).perform(click());
                SystemClock.sleep(200);
                onView(withId(R.id.edittext_group_contacts)).perform(replaceText(contactsFilter));
                SystemClock.sleep(1000);    // Allow time for the displayed items to get filtered.
                break;
            case GROUP_SELECTION:
                openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
                SystemClock.sleep(200);
                onView(withText(R.string.action_bar_group_select)).perform(click());
                SystemClock.sleep(200);
                onView(withId(R.id.edittext_group_selection)).perform(replaceText(groupsFilter));
                SystemClock.sleep(1000);    // Allow time for the displayed items to get filtered.
                break;
            case GROUP_SHARE_CONTACTS:
                // This is required so that the load more contacts text view is visible.
                AndroidTestUtils.getRequest(uriPrefix + "modify-inserted-known-contact-names");
                SystemClock.sleep(500);

                onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
                SystemClock.sleep(200);
                onView(withId(R.id.recyclerview_main_tab_groups))
                        .perform(RecyclerViewActions.actionOnItemAtPosition(1, click()));
                SystemClock.sleep(500);
                onView(ViewMatchers.withId(R.id.btn_group_details_share)).perform(click());
                SystemClock.sleep(500);
                onView(withId(R.id.edittext_group_share)).perform(replaceText(contactsFilter));
                SystemClock.sleep(1000);    // Allow time for the displayed items to get filtered.
                break;
            case GROUP_SHARE_GROUPS:
                onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
                SystemClock.sleep(200);
                onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
                SystemClock.sleep(200);
                onView(withId(R.id.recyclerview_main_tab_contacts))
                        .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
                SystemClock.sleep(500);
                onView(ViewMatchers.withId(R.id.btn_contact_details_share)).perform(click());
                SystemClock.sleep(500);
                onView(withId(R.id.edittext_group_share)).perform(replaceText(groupsFilter));
                SystemClock.sleep(1000);    // Allow time for the displayed items to get filtered.
                break;
            default:
                fail();
                return;
        }

        final int initialInternalListSize = getInitialInternalListSize();    // This cannot be done in the constructor.

        try {
            AndroidTestUtils.postString(insertUri, TEST_CONTACT_NUMBER_STRING);
            SystemClock.sleep(5000);    // Allow time for the server to modify the database.

            // Append items so that a regular number of items are appended but a non-regular number of items are
            // displayed.

            selectLoadMoreTextView(textViewId);     // Test clicking the TextView to load more items.

            assertListSizeAndDisplayedItems(
                    initialInternalListSize + numOfObjectsPerLoad,
                    initialDisplayItemsSize + NUM_OF_ITEMS_APPENDED
            );

            // Append items to the display without loading from the server.

            if (numOfObjectsPerLoad == 100) {
                scrollToProgressBar();

                assertListSizeAndDisplayedItems(
                        initialInternalListSize + numOfObjectsPerLoad,
                        initialDisplayItemsSize + numOfObjectsPerLoad
                );
            }

            // Append a non-regular number of items if they are the last items available.

            scrollToProgressBar();

            assertListSizeAndDisplayedItems(
                    initialInternalListSize + numOfObjectsPerLoad + 1,
                    initialDisplayItemsSize + numOfObjectsPerLoad + 1
            );
        }
        finally {
            if (context.equals(CONTEXT.GROUP_SHARE_CONTACTS)) {
                AndroidTestUtils.getRequest(uriPrefix + "revert-inserted-known-contact-names");
            }

            AndroidTestUtils.postString(deleteUri, TEST_CONTACT_NUMBER_STRING);
            SystemClock.sleep(5000);    // Allow time for the server to modify the database.
        }
    }
}
