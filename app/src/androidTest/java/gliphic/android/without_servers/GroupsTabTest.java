package gliphic.android.without_servers;

import android.os.SystemClock;

import gliphic.android.R;
import gliphic.android.adapters.GroupsAdapter;
import gliphic.android.operation.Group;
import gliphic.android.utils.AndroidTestUtils;
import gliphic.android.utils.view_assertions.AssertRecyclerViewItemCount;
import gliphic.android.utils.MainActivityTestRuleWithoutHttpServerSetup;
import gliphic.android.utils.matchers.ToastMatcher;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import libraries.Vars;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.IsNot.not;

/**
 * Test functionality unique to the GroupsTab.
 */
@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class GroupsTabTest extends MainActivityTestRuleWithoutHttpServerSetup {

    @Before
    public void selectGroupsTab() throws Throwable {
        AndroidTestUtils.waitForAction(AndroidTestUtils.WaitAction.SERVER_CONNECTION_FAILED_MSG);
        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
        SystemClock.sleep(1000);    // Give the swipe time to change tab focus and find R.id.*
    }

    // TODO: Test removing/adding a group from different activities to see if this GroupsAdapter updates.
    //       Also check the number of groups in Groups.knownGroups in these tests.

    @Test
    public void noErrorGroupShown() {
        onView(ViewMatchers.withId(R.id.edittext_main_tab_groups)).check(matches(isDisplayed()));
        onView(ViewMatchers.withId(R.id.groups_tab_error_group)).check(matches(not(isDisplayed())));
    }

    @Test
    public void errorToastShown() {
        Group.setNullKnownGroups();

        // Enter another activity then return to this one to check that Toast is displayed.
        onView(withId(R.id.recyclerview_main_tab_groups))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
        SystemClock.sleep(500);
        pressBack();

        String s = GroupsAdapter.GENERIC_LOAD_GROUPS_FAILED_MSG;
        onView(withText(s)).inRoot(new ToastMatcher()).check(matches(isDisplayed()));

        onView(ViewMatchers.withId(R.id.edittext_main_tab_groups)).check(matches(isDisplayed()));
        onView(ViewMatchers.withId(R.id.groups_tab_error_group)).check(matches(not(isDisplayed())));
    }

    @Test
    public void filtersKeptOnTabRefocus() {
        // Check that the expected group name exists.
        String expectedGroupName = Vars.DEFAULT_GROUP_NAME;
        onView(withId(R.id.edittext_main_tab_groups)).perform(replaceText(expectedGroupName));
        onView(ViewMatchers.withId(R.id.recyclerview_main_tab_groups))
                .check(matches(hasDescendant(withText(expectedGroupName))));

        // Check that an unexpected group name does not exist.
        String unexpectedGroupName = "This group name has max length";
        onView(ViewMatchers.withId(R.id.recyclerview_main_tab_groups))
                .check(matches(not(hasDescendant(withText(unexpectedGroupName)))));

        // Enter another activity then return to this one to check that filters are still applied
        // (testing the group adapter's clear() and update() methods).
        onView(withId(R.id.recyclerview_main_tab_groups))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
        pressBack();

        // Check that the expected group name exists.
        onView(ViewMatchers.withId(R.id.recyclerview_main_tab_groups))
                .check(matches(hasDescendant(withText(expectedGroupName))));

        // Check that an unexpected group name does not exist.
        onView(ViewMatchers.withId(R.id.recyclerview_main_tab_groups))
                .check(matches(not(hasDescendant(withText(unexpectedGroupName)))));
    }

    @Test
    public void invalidSearchStringDoesNotChangeRecyclerViewItems() {
        final int recyclerViewId = R.id.recyclerview_main_tab_groups;
        AssertRecyclerViewItemCount arvic = new AssertRecyclerViewItemCount(true, recyclerViewId);

        onView(withId(R.id.edittext_main_tab_groups)).perform(replaceText(AndroidTestUtils.INVALID_CHAR_STRING));

        onView(withId(recyclerViewId)).check(arvic);
    }
}
