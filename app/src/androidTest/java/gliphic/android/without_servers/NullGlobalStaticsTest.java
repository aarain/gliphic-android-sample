package gliphic.android.without_servers;

import android.os.SystemClock;

import gliphic.android.R;
import gliphic.android.adapters.ContactsAdapter;
import gliphic.android.adapters.GroupsAdapter;
import gliphic.android.display.welcome_screen.SignInActivity;
import gliphic.android.utils.AndroidTestUtils;
import gliphic.android.utils.NullGlobalStaticsTestWithoutHttpServerRule;
import gliphic.android.utils.matchers.ToastMatcher;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.IsNot.not;

/**
 * This class checks activities and fragments to see if an AlertDialog or error contact/group, respectively, is
 * displayed when the activity/fragment encounters a problem in its onCreate[View] method, usually caused by the global
 * statics being null and no internet connection is available request them.
 */
@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class NullGlobalStaticsTest {

    @Rule
    public NullGlobalStaticsTestWithoutHttpServerRule<SignInActivity> rule =
            new NullGlobalStaticsTestWithoutHttpServerRule<>(SignInActivity.class);

    @Before
    public void waitForDialog() throws Throwable {
        AndroidTestUtils.waitForAction(AndroidTestUtils.WaitAction.SERVER_CONNECTION_FAILED_MSG);
    }

    @After
    public void delayForContactsTabTest() {
        SystemClock.sleep(5000);
    }

    /* Error group displayed */

    @Test
    public void errorGroupVisibleInWorkspaceTabOnCreateView() {
        SystemClock.sleep(10000);
        onView(ViewMatchers.withId(R.id.workspace_tab_error_group)).check(matches(isDisplayed()));
    }

    @Test
    public void errorGroupVisibleInGroupsTabOnCreateView() {
        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());

        SystemClock.sleep(500);
        String s = GroupsAdapter.GENERIC_LOAD_GROUPS_FAILED_MSG;
        onView(withText(s)).inRoot(new ToastMatcher()).check(matches(isDisplayed()));

        // View(s) which are not displayed.
        onView(ViewMatchers.withId(R.id.edittext_main_tab_groups)).check(matches(not(isDisplayed())));
        // View(s) which are displayed.
        onView(ViewMatchers.withId(R.id.groups_tab_error_group)).check(matches(isDisplayed()));
    }

    // TODO: Write tests for when the error groups shows as a result of non-onCreateView calls e.g. onNetworkAvailable().

    /* Error contact displayed */

    @Test
    public void errorContactVisibleInContactsTabOnCreateView() {
        onView(withText(R.string.main_contacts)).perform(click());

        SystemClock.sleep(500);
        String s = ContactsAdapter.GENERIC_LOAD_CONTACTS_FAILED_MSG;
        onView(withText(s)).inRoot(new ToastMatcher()).check(matches(isDisplayed()));

        // View(s) which are not displayed.
        onView(ViewMatchers.withId(R.id.edittext_main_tab_contacts)).check(matches(not(isDisplayed())));
        onView(ViewMatchers.withId(R.id.checkbox_main_known_contacts)).check(matches(not(isDisplayed())));
        onView(ViewMatchers.withId(R.id.checkbox_main_extended_contacts)).check(matches(not(isDisplayed())));
        onView(ViewMatchers.withId(R.id.itemdivider_main_tab_contacts)).check(matches(not(isDisplayed())));
        onView(ViewMatchers.withId(R.id.textview_no_contacts)).check(matches(not(isDisplayed())));
        // View(s) which are displayed.
        onView(ViewMatchers.withId(R.id.contacts_tab_error_contact)).check(matches(isDisplayed()));
    }
}
