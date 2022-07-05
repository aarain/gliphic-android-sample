package gliphic.android.without_servers;

import android.content.ComponentName;
import android.os.SystemClock;

import gliphic.android.R;
import gliphic.android.display.main.contact_details.ContactDetailsActivity;
import gliphic.android.utils.AndroidTestUtils;
import gliphic.android.utils.view_assertions.AssertRecyclerViewItemCount;
import gliphic.android.utils.MainIntentTestRuleWithoutHttpServerSetup;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.action.ViewActions.swipeRight;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withSubstring;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.IsNot.not;

/**
 * Test functionality unique to the ContactsTab.
 */
@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class ContactsTabTest extends MainIntentTestRuleWithoutHttpServerSetup {
    private static ViewInteraction recyclerViewInteraction;     // Define this variable for (shorter) code readability.

    private final String knownContactName    = "Patient-0";
    private final String extendedContactName = "Who dis guy?";

    private void checkDescendantHasKnownContact(boolean hasContact) {
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

    private void checkDescendantHasExtendedContact(boolean hasContact) {
        final String extendedContactType = "Extended contact - ";
        if (hasContact) {
            recyclerViewInteraction.check(matches(hasDescendant(withText(extendedContactName))));
            recyclerViewInteraction.check(matches(hasDescendant(withSubstring(extendedContactType))));
        }
        else {
            recyclerViewInteraction.check(matches(not(hasDescendant(withText(extendedContactName)))));
            recyclerViewInteraction.check(matches(not(hasDescendant(withSubstring(extendedContactType)))));
        }
    }

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @BeforeClass
    public static void beforeClass() {
        recyclerViewInteraction = onView(withId(R.id.recyclerview_main_tab_contacts));
    }

    @Before
    public void selectContactsTab() throws Throwable {
        AndroidTestUtils.waitForAction(AndroidTestUtils.WaitAction.SERVER_CONNECTION_FAILED_MSG);
        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
        SystemClock.sleep(200);
        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
        SystemClock.sleep(1000);    // Give the swipe time to change tab focus and find R.id.*
    }

    // TODO: Test removing/adding a contact from different activities to see if this ContactsAdapter updates.
    //       Also check the number of contacts in Contacts.knownContacts in these tests.

    @Test
    public void checkContactDetailsActivityLaunched() {
        onView(withId(R.id.recyclerview_main_tab_contacts))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

        intended(hasComponent(new ComponentName(getApplicationContext(), ContactDetailsActivity.class)));
    }

    @Test
    public void textFilterVisibleContacts() {
        // Check that the expected contact name exists.
        onView(withId(R.id.edittext_main_tab_contacts)).perform(replaceText(knownContactName));
        checkDescendantHasKnownContact(true);

        // Check that an unexpected contact name does not exist.
        expectedEx.expectMessage("'has descendant: with text: is \"");
        expectedEx.expectMessage(extendedContactName);
        expectedEx.expectMessage("\"' doesn't match the selected view.");
        checkDescendantHasExtendedContact(true);
    }

    @Test
    public void oneCheckBoxIsAlwaysSelected() {
        final ViewInteraction knownViewInteraction = onView(withId(R.id.checkbox_main_known_contacts));
        final ViewInteraction extendedViewInteraction = onView(withId(R.id.checkbox_main_extended_contacts));

        // Note that the custom CheckBoxSelection object should not be used since this sets the CheckBox state and
        // hence does not test that CheckBoxes cannot be both unchecked on context start-up
        knownViewInteraction.check(matches(isChecked()));
        extendedViewInteraction.check(matches(not(isChecked())));

        // Now start checking CheckBoxes.

        // Check known box is selected and extended box is not selected.
        onView(withId(R.id.checkbox_main_known_contacts)).perform(click());
        knownViewInteraction.check(matches(isChecked()));
        extendedViewInteraction.check(matches(not(isChecked())));

        // Check both boxes are selected.
        onView(withId(R.id.checkbox_main_extended_contacts)).perform(click());
        knownViewInteraction.check(matches(isChecked()));
        extendedViewInteraction.check(matches(isChecked()));

        // Check known box is not selected and extended box is selected.
        onView(withId(R.id.checkbox_main_known_contacts)).perform(click());
        knownViewInteraction.check(matches(not(isChecked())));
        extendedViewInteraction.check(matches(isChecked()));

        // Check known box is not selected and extended box is selected.
        onView(withId(R.id.checkbox_main_extended_contacts)).perform(click());
        knownViewInteraction.check(matches(not(isChecked())));
        extendedViewInteraction.check(matches(isChecked()));
    }

    @Test
    public void showKnownContacts() {
        // Check that the expected contact name does not exist.

        onView(withId(R.id.checkbox_main_extended_contacts)).perform(click());
        onView(withId(R.id.checkbox_main_known_contacts)).perform(click());

        checkDescendantHasKnownContact(false);

        // Check that the expected contact name exists.

        onView(withId(R.id.checkbox_main_known_contacts)).perform(click());

        checkDescendantHasKnownContact(true);
    }

    @Test
    public void showExtendedContacts() {
        onView(withId(R.id.checkbox_main_extended_contacts)).perform(click());
        onView(withId(R.id.checkbox_main_known_contacts)).perform(click());

        // Check that the expected contact name exists.
        checkDescendantHasExtendedContact(true);

        // Check that an unexpected contact name does not exist.
        expectedEx.expectMessage("'has descendant: with text: is \"");
        expectedEx.expectMessage(knownContactName);
        expectedEx.expectMessage("\"' doesn't match the selected view.");
        checkDescendantHasKnownContact(true);
    }

    @Test
    public void invalidSearchStringDoesNotChangeRecyclerViewItems() {
        final int recyclerViewId = R.id.recyclerview_main_tab_contacts;
        AssertRecyclerViewItemCount arvic = new AssertRecyclerViewItemCount(true, recyclerViewId);

        onView(withId(R.id.edittext_main_tab_contacts)).perform(replaceText(AndroidTestUtils.INVALID_CHAR_STRING));

        onView(withId(recyclerViewId)).check(arvic);
    }

    @Test
    public void loadMoreContactsTextIsVisible() {
        onView(withId(R.id.checkbox_main_known_contacts)).perform(click());
        // Clean-up by clicking the checkbox again.
        onView(withId(R.id.checkbox_main_known_contacts)).perform(click());
    }

    @Test
    public void filtersKeptOnTabRefocus() {
        String s = "Some arbitrary string.";

        // Apply/Reverse the filters and check that they have the expected values.
        onView(withId(R.id.checkbox_main_extended_contacts)).perform(click());
        checkDescendantHasExtendedContact(true);

        onView(withId(R.id.checkbox_main_known_contacts)).perform(click());
        checkDescendantHasKnownContact(false);

        onView(withId(R.id.edittext_main_tab_contacts)).perform(replaceText(s), closeSoftKeyboard());
        recyclerViewInteraction.check(matches(not(withText(s))));

        onView(withId(R.id.viewpager_activity_base)).perform(swipeRight());
        onView(withId(R.id.viewpager_activity_base)).perform(swipeRight());
        SystemClock.sleep(1000);    // Give the swipe time to change tab focus and find R.id.*
        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
        SystemClock.sleep(1000);    // Give the swipe time to change tab focus and find R.id.*

        // Check that the filters are still applied and undo each filter one-by-one.
        recyclerViewInteraction.check(matches(not(withText(s))));
        onView(withId(R.id.edittext_main_tab_contacts)).perform(clearText(), closeSoftKeyboard());
        checkDescendantHasExtendedContact(true);
        // Bug fix test:
        onView(withId(R.id.textview_no_contacts)).check(matches(not(isDisplayed())));

        checkDescendantHasKnownContact(false);
        checkDescendantHasExtendedContact(true);
        onView(withId(R.id.checkbox_main_known_contacts)).perform(click());
        checkDescendantHasKnownContact(true);
        onView(withId(R.id.checkbox_main_extended_contacts)).perform(click());
        checkDescendantHasExtendedContact(false);
    }
}
