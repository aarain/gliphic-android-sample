package gliphic.android.with_http_server.functionality;

import android.os.SystemClock;

import gliphic.android.R;
import gliphic.android.utils.MainActivityTestRuleSetup;
import gliphic.android.with_http_server.single_fragment_activity.AddContactActivityTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.IsNot.not;

/**
 * Test how various RecyclerViews refresh content when they are modified.
 */
@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class RecyclerViewRefreshTest extends MainActivityTestRuleSetup {
    private static ViewInteraction recyclerViewInteraction;     // Define this variable for (shorter) code readability.

    private final String knownContactName = "Patient-0";

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void contactsTabRecyclerViewSortsItemsInOnStart() {
        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
        SystemClock.sleep(200);

        recyclerViewInteraction = onView(ViewMatchers.withId(R.id.recyclerview_main_tab_contacts));
        recyclerViewInteraction.check(matches(hasDescendant(withText(knownContactName))));

        onView(withId(R.id.recyclerview_main_tab_contacts))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
        onView(withId(R.id.btn_contact_details_add_remove)).perform(click());
        onView(withId(android.R.id.button1)).perform(click());
        pressBack();
        SystemClock.sleep(200);

        recyclerViewInteraction.check(matches(not(hasDescendant(withText(knownContactName)))));

        AddContactActivityTest.navigateToActivity();
        onView(withId(R.id.edittext_add_contact)).perform(replaceText(knownContactName));
        SystemClock.sleep(1000);
        onView(withId(R.id.recyclerview_add_contact))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
        onView(withId(R.id.btn_contact_details_add_remove)).perform(click());
        onView(withId(android.R.id.button1)).perform(click());
        onView(withContentDescription(R.string.action_bar_home)).perform(click());
        SystemClock.sleep(200);

        recyclerViewInteraction.check(matches(hasDescendant(withText(knownContactName))));
    }

    @Test
    public void groupContactsTabRecyclerViewSortsItemsInOnStart() {
        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
        SystemClock.sleep(200);
        onView(withId(R.id.recyclerview_main_tab_groups))
                .perform(RecyclerViewActions.actionOnItemAtPosition(1, click()));
        SystemClock.sleep(200);
        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
        SystemClock.sleep(200);

        recyclerViewInteraction = onView(ViewMatchers.withId(R.id.recyclerview_group_details_tab_target_contacts));
        recyclerViewInteraction.check(matches(hasDescendant(withText(knownContactName))));

        recyclerViewInteraction.perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
        onView(withId(R.id.btn_contact_details_add_remove)).perform(click());
        onView(withId(android.R.id.button1)).perform(click());
        pressBack();
        SystemClock.sleep(200);

        recyclerViewInteraction.check(matches(not(hasDescendant(withText(knownContactName)))));

        AddContactActivityTest.navigateToActivity();
        onView(withId(R.id.edittext_add_contact)).perform(replaceText(knownContactName));
        SystemClock.sleep(1000);
        onView(withId(R.id.recyclerview_add_contact))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
        onView(withId(R.id.btn_contact_details_add_remove)).perform(click());
        onView(withId(android.R.id.button1)).perform(click());
        pressBack();
        pressBack();
        SystemClock.sleep(200);

        recyclerViewInteraction.check(matches(hasDescendant(withText(knownContactName))));
    }
}
