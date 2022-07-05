package gliphic.android.without_servers;

import android.os.SystemClock;

import gliphic.android.R;
import gliphic.android.operation.Contact;
import gliphic.android.exceptions.ContactException;
import gliphic.android.operation.Group;
import gliphic.android.exceptions.GroupException;
import gliphic.android.utils.AndroidTestUtils;
import gliphic.android.utils.view_assertions.AssertRecyclerViewItemCount;
import gliphic.android.utils.MainActivityTestRuleWithoutHttpServerSetup;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.action.ViewActions.swipeRight;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withSubstring;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.IsNot.not;

/**
 * Test functionality unique to GroupContactsTab.
 */
@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class GroupContactsTabTest extends MainActivityTestRuleWithoutHttpServerSetup {
    private static ViewInteraction recyclerViewInteraction;     // Define this variable for (shorter) code readability.

    private void checkDescendantHasKnownContact(boolean hasContact) {
        final String knownContactName = "Patient-0";
        final String knownContactType = "Known contact - ";
        if (hasContact) {
            recyclerViewInteraction.check(matches(hasDescendant(withText(knownContactName))));
            recyclerViewInteraction.check(matches(hasDescendant(withSubstring(knownContactType))));
        }
        else {
            recyclerViewInteraction.check(matches(not(hasDescendant(withText(knownContactName)))));
            recyclerViewInteraction.check(matches(not(hasDescendant(withSubstring(knownContactType)))));
        }
    }

    private void setEmptyGroupContactsListForGroup(long groupNumber) throws ContactException, GroupException {
        // By default the group contacts list for any group is null, so this method sets it to an empty ArrayList.
        Group group = Group.getGroupFromNumber(groupNumber);
        Contact contact = AndroidTestUtils.generateKnownContact();
        group.addGroupContact(contact);
        group.removeGroupContact(contact);
    }

    private void selectGroupsContactsTabFromGroupsTab() {
        onView(withId(R.id.recyclerview_main_tab_groups))
                .perform(RecyclerViewActions.actionOnItemAtPosition(1, click()));
        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
        SystemClock.sleep(1000);    // Give the swipe time to change tab focus and find R.id.*
    }

    @BeforeClass
    public static void beforeClass() {
        recyclerViewInteraction = onView(ViewMatchers.withId(R.id.recyclerview_group_details_tab_target_contacts));
    }

    @Before
    public void selectGroupsTab() throws Throwable {
        AndroidTestUtils.waitForAction(AndroidTestUtils.WaitAction.SERVER_CONNECTION_FAILED_MSG);
        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
        SystemClock.sleep(1000);    // Give the swipe time to change tab focus and find R.id.*
    }

    @Test
    public void oneCheckBoxIsAlwaysSelected() {
        selectGroupsContactsTabFromGroupsTab();

        final ViewInteraction knownViewInteraction = onView(withId(R.id.checkbox_group_known_contacts));
        final ViewInteraction extendedViewInteraction = onView(withId(R.id.checkbox_group_extended_contacts));

        // Note that the custom CheckBoxSelection object should not be used since this sets the CheckBox state and
        // hence does not test that CheckBoxes cannot be both unchecked on context start-up
        knownViewInteraction.check(matches(isChecked()));
        extendedViewInteraction.check(matches(not(isChecked())));

        // Now start checking CheckBoxes.

        // Check known box is selected and extended box is not selected.
        onView(ViewMatchers.withId(R.id.checkbox_group_known_contacts)).perform(click());
        knownViewInteraction.check(matches(isChecked()));
        extendedViewInteraction.check(matches(not(isChecked())));

        // Check both boxes are selected.
        onView(ViewMatchers.withId(R.id.checkbox_group_extended_contacts)).perform(click());
        knownViewInteraction.check(matches(isChecked()));
        extendedViewInteraction.check(matches(isChecked()));

        // Check known box is not selected and extended box is selected.
        onView(ViewMatchers.withId(R.id.checkbox_group_known_contacts)).perform(click());
        knownViewInteraction.check(matches(not(isChecked())));
        extendedViewInteraction.check(matches(isChecked()));

        // Check known box is not selected and extended box is selected.
        onView(ViewMatchers.withId(R.id.checkbox_group_extended_contacts)).perform(click());
        knownViewInteraction.check(matches(not(isChecked())));
        extendedViewInteraction.check(matches(isChecked()));
    }

    @Test
    public void contactsAreDisplayed() {
        onView(withId(R.id.recyclerview_main_tab_groups))
                .perform(RecyclerViewActions.actionOnItemAtPosition(1, click()));
        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
        SystemClock.sleep(1000);    // Give the swipe time to change tab focus and find R.id.*

        checkDescendantHasKnownContact(true);
    }

    @Test
    public void invalidSearchStringDoesNotChangeRecyclerViewItems() {
        selectGroupsContactsTabFromGroupsTab();

        final int recyclerViewId = R.id.recyclerview_group_details_tab_target_contacts;
        AssertRecyclerViewItemCount arvic = new AssertRecyclerViewItemCount(true, recyclerViewId);

        onView(withId(R.id.edittext_group_contacts)).perform(replaceText(AndroidTestUtils.INVALID_CHAR_STRING));

        onView(withId(recyclerViewId)).check(arvic);
    }

    /* No contacts are displayed */

    @Test
    public void noContactsAreDisplayed() throws ContactException, GroupException {
        final int groupNumber = 2;
        setEmptyGroupContactsListForGroup(groupNumber);

        onView(withId(R.id.recyclerview_main_tab_groups))
                .perform(RecyclerViewActions.actionOnItemAtPosition(groupNumber, click()));
        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
        SystemClock.sleep(1000);    // Give the swipe time to change tab focus and find R.id.*

        onView(ViewMatchers.withId(R.id.textview_no_group_contacts))
                .check(matches(withText(R.string.no_group_contacts)));

        checkDescendantHasKnownContact(false);
    }

    @Test
    public void noContactsAreDisplayedDefaultGroup() throws ContactException, GroupException {
        final int groupNumber = 0;
        setEmptyGroupContactsListForGroup(groupNumber);

        onView(withId(R.id.recyclerview_main_tab_groups))
                .perform(RecyclerViewActions.actionOnItemAtPosition(groupNumber, click()));
        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
        SystemClock.sleep(1000);    // Give the swipe time to change tab focus and find R.id.*

        onView(ViewMatchers.withId(R.id.textview_no_group_contacts))
                .check(matches(withText(R.string.default_group_contacts)));

        checkDescendantHasKnownContact(false);
    }

    @Test
    public void filtersKeptOnTabRefocus() {
        // Note that the extended contacts checkbox is not tested.

        onView(withId(R.id.recyclerview_main_tab_groups))
                .perform(RecyclerViewActions.actionOnItemAtPosition(1, click()));
        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
        SystemClock.sleep(1000);    // Give the swipe time to change tab focus and find R.id.*

        String s = "Some arbitrary string.";

        // Apply/Reverse the filters and check that they have the expected values.
        onView(ViewMatchers.withId(R.id.checkbox_group_extended_contacts)).perform(click());
        onView(ViewMatchers.withId(R.id.checkbox_group_known_contacts)).perform(click());
        checkDescendantHasKnownContact(false);

        onView(ViewMatchers.withId(R.id.edittext_group_contacts)).perform(replaceText(s), closeSoftKeyboard());
        recyclerViewInteraction.check(matches(not(withText(s))));

        onView(withId(R.id.viewpager_activity_base)).perform(swipeRight());
        SystemClock.sleep(1000);    // Give the swipe time to change tab focus and find R.id.*
        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
        SystemClock.sleep(1000);    // Give the swipe time to change tab focus and find R.id.*

        // Check that the filters are still applied and undo each filter one-by-one.
        recyclerViewInteraction.check(matches(not(withText(s))));
        onView(ViewMatchers.withId(R.id.edittext_group_contacts)).perform(clearText(), closeSoftKeyboard());

        checkDescendantHasKnownContact(false);
        onView(ViewMatchers.withId(R.id.checkbox_group_known_contacts)).perform(click());
        checkDescendantHasKnownContact(true);
    }
}
