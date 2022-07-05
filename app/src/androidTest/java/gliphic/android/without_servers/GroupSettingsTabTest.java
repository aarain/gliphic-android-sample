package gliphic.android.without_servers;

import android.os.SystemClock;

import androidx.test.espresso.contrib.RecyclerViewActions;
import gliphic.android.R;
import gliphic.android.operation.Group;
import gliphic.android.utils.AndroidTestUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import gliphic.android.utils.MainIntentTestRuleWithoutHttpServerSetup;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

/**
 * Test functionality unique to GroupSettingsTab.
 */
@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class GroupSettingsTabTest extends MainIntentTestRuleWithoutHttpServerSetup {
    private final static long displayedGroupNumber = 1;

    @Before
    public void selectTab() throws Throwable {
        AndroidTestUtils.waitForAction(AndroidTestUtils.WaitAction.SERVER_CONNECTION_FAILED_MSG);
        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
        SystemClock.sleep(200);
        onView(withId(R.id.recyclerview_main_tab_groups))
                .perform(RecyclerViewActions.actionOnItemAtPosition((int) displayedGroupNumber, click()));
        SystemClock.sleep(1000);
    }

    @Test
    public void checkDisplay() throws Exception {
        onView(withId((R.id.group_details_img))).check(matches(isDisplayed()));
        onView(withId((R.id.group_details_name))).check(matches(isDisplayed()));
        onView(withId((R.id.group_details_description))).check(matches(isDisplayed()));
        onView(withId((R.id.btn_group_details_select))).check(matches(isDisplayed()));
        onView(withId((R.id.btn_group_details_share))).check(matches(isDisplayed()));
        onView(withId((R.id.btn_group_details_leave))).check(matches(isDisplayed()));

        final Group displayedGroup = Group.getGroupFromNumber(displayedGroupNumber);

        // Group ID
        onView(withText(String.format(
                AndroidTestUtils.getResourceString(R.string.group_details_id),
                displayedGroup.getId()
        ))).check(matches(isDisplayed()));
        onView(withId(R.id.group_id_group_settings_tab_profile)).check(matches(withText(displayedGroup.getId())));

        // Group name
        onView(withText(AndroidTestUtils.getResourceString(R.string.group_details_name)))
                .check(matches(isDisplayed()));
        onView(withText(displayedGroup.getName())).check(matches(isDisplayed()));

        // Group description
        onView(withText(AndroidTestUtils.getResourceString(R.string.group_details_description)))
                .check(matches(isDisplayed()));
        onView(withText(displayedGroup.getDescription())).check(matches(isDisplayed()));
    }
}
