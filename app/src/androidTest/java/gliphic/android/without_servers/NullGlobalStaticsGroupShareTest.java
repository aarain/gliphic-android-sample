package gliphic.android.without_servers;

import android.os.SystemClock;

import gliphic.android.R;
import gliphic.android.display.welcome_screen.SignInActivity;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.IsNot.not;

/**
 * This class is similar to the NullGlobalStaticTest class but tests only the GroupShareActivity with a given group OR
 * a given contact.
 */
@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class NullGlobalStaticsGroupShareTest {
    @Rule
    public NullGlobalStaticsGroupShareTestRule<SignInActivity> rule
            = new NullGlobalStaticsGroupShareTestRule<>(SignInActivity.class);

    @Before
    public void waitForDialog() {
        SystemClock.sleep(5500);
    }

    private void assertDisplayedMessage() {
        onView(withText("Unable to load contact details and/or group details at this time."))
                .check(matches(isDisplayed()));

        onView(withId(android.R.id.button1)).perform(click());
    }

    // TODO: Include this check. This is difficult to do because the activity closes immediately upon confirming the
    //       displayed dialog.
    private void checkViewVisibility() {
        // View(s) which are not displayed.
        onView(ViewMatchers.withId(R.id.edittext_group_share)).check(matches(not(isDisplayed())));
        onView(ViewMatchers.withId(R.id.textview_group_share_no_items)).check(matches(not(isDisplayed())));
        onView(ViewMatchers.withId(R.id.textview_group_share_load_more_items)).check(matches(not(isDisplayed())));
        // View(s) which are displayed.
        onView(ViewMatchers.withId(R.id.group_share_error_item)).check(matches(isDisplayed()));
    }

    /* Error contact/group displayed */

    @Ignore("Test manually be setting 'launchActivityType = LaunchActivityType.DISPLAY_CONTACTS;' in onCreate().")
    @Test
    public void errorContactVisibleInGroupShareActivity() {
        assertDisplayedMessage();

//        onView(ViewMatchers.withText(R.string.error_contact)).check(matches(isDisplayed()));
//
//        checkViewVisibility();
    }

    @Ignore("Test manually be setting 'launchActivityType = LaunchActivityType.DISPLAY_GROUPS;' in onCreate().")
    @Test
    public void errorGroupVisibleInGroupShareActivity() {
        assertDisplayedMessage();

//        onView(ViewMatchers.withText(R.string.error_group)).check(matches(isDisplayed()));
//
//        checkViewVisibility();
    }
}
