package gliphic.android.without_servers;

import android.os.SystemClock;

import gliphic.android.R;
import gliphic.android.adapters.GroupsAdapter;
import gliphic.android.display.welcome_screen.SignInActivity;
import gliphic.android.utils.matchers.ToastMatcher;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.IsNot.not;

/**
 * This class is similar to the NullGlobalStaticTest class but tests only the GroupDetailsActivity contacts tab.
 */
@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class NullGlobalStaticsContactGroupsTest {
    @Rule
    public NullGlobalStaticsContactGroupsTestRule<SignInActivity> rule
            = new NullGlobalStaticsContactGroupsTestRule<>(SignInActivity.class);

    @Before
    public void waitForDialog() {
        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
        SystemClock.sleep(500);
    }

    /* Error contact displayed */

    @Test
    public void errorGroupVisibleInContactGroupsTabOnCreateView() {
        String s = GroupsAdapter.GENERIC_LOAD_GROUPS_FAILED_MSG;
        onView(withText(s)).inRoot(new ToastMatcher()).check(matches(isDisplayed()));

        // View(s) which are not displayed.
        onView(ViewMatchers.withId(R.id.edittext_contact_groups)).check(matches(not(isDisplayed())));
        onView(ViewMatchers.withId(R.id.textview_no_common_groups)).check(matches(not(isDisplayed())));
        // View(s) which are displayed.
        onView(ViewMatchers.withId(R.id.contact_groups_tab_error_group)).check(matches(isDisplayed()));
    }
}
