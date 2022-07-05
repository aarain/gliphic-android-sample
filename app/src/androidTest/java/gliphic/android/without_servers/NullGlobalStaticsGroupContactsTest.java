package gliphic.android.without_servers;

import android.os.SystemClock;

import gliphic.android.R;
import gliphic.android.display.welcome_screen.SignInActivity;

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
import static org.hamcrest.core.IsNot.not;

/**
 * This class is similar to the NullGlobalStaticTest class but tests only the GroupDetailsActivity contacts tab.
 */
@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class NullGlobalStaticsGroupContactsTest {

    @Rule
    public NullGlobalStaticsGroupContactsTestRule<SignInActivity> rule
            = new NullGlobalStaticsGroupContactsTestRule<>(SignInActivity.class);

    @Before
    public void waitForDialog() {
        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
        SystemClock.sleep(5500);
    }

    /* Error contact displayed */

    @Test
    public void errorContactVisibleInGroupContactsTabOnCreateView() {
        // View(s) which are not displayed.
        onView(ViewMatchers.withId(R.id.edittext_group_contacts)).check(matches(not(isDisplayed())));
        onView(ViewMatchers.withId(R.id.checkbox_group_known_contacts)).check(matches(not(isDisplayed())));
        onView(ViewMatchers.withId(R.id.checkbox_group_extended_contacts)).check(matches(not(isDisplayed())));
        onView(ViewMatchers.withId(R.id.itemdivider_group_contacts)).check(matches(not(isDisplayed())));
        onView(ViewMatchers.withId(R.id.textview_no_group_contacts)).check(matches(not(isDisplayed())));
        // View(s) which are displayed.
        onView(ViewMatchers.withId(R.id.group_contacts_tab_error_contact)).check(matches(isDisplayed()));
    }
}
