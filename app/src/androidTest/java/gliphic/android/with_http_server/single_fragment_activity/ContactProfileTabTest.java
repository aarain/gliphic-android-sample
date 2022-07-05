package gliphic.android.with_http_server.single_fragment_activity;

import android.os.SystemClock;

import gliphic.android.R;
import gliphic.android.display.welcome_screen.SignInActivity;
import gliphic.android.utils.BaseActivityTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.IsNot.not;

/**
 * Test functionality unique to ContactProfileTab.
 */
@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class ContactProfileTabTest {

    private void navigateToContactProfileTab() {
        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
        SystemClock.sleep(200);
        onView(withId(R.id.recyclerview_main_tab_contacts))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
        SystemClock.sleep(200);
    }

    private void addContact() {
        onView(withId(R.id.btn_contact_details_add_remove))
                .check(matches(withText(R.string.contact_details_add_btn)));

        onView(withId(R.id.btn_contact_details_add_remove)).perform(click());
        SystemClock.sleep(1000);
        onView(withText("Known contact added")).check(matches(isDisplayed()));

        onView(withId(android.R.id.button1)).perform(click());
        SystemClock.sleep(200);
        onView(withId(R.id.btn_contact_details_share)).check(matches(isEnabled()));
    }

    private void removeContact() {
        onView(withId(R.id.btn_contact_details_add_remove))
                .check(matches(withText(R.string.contact_details_remove_btn)));

        onView(withId(R.id.btn_contact_details_add_remove)).perform(click());
        SystemClock.sleep(1000);
        onView(withText("Known contact removed")).check(matches(isDisplayed()));

        onView(withId(android.R.id.button1)).perform(click());
        SystemClock.sleep(200);
        onView(withId(R.id.btn_contact_details_share)).check(matches(not(isEnabled())));
    }

    @Rule
    public BaseActivityTestRule<SignInActivity> rule = new BaseActivityTestRule<>(SignInActivity.class);

    @Test
    public void addThenRemoveUnknownContactAndRemoveKnownContact() throws Exception {
        final String unknownContactId =
                AddContactActivityTest.postStringContactIdFromNumber(AddContactActivityTest.unknownContactNum);

        AddContactActivityTest.navigateToActivity();

        onView(withId(R.id.btn_add_contact_by_id)).perform(click());
        onView(withId(R.id.edittext_add_contact)).perform(replaceText(unknownContactId));
        SystemClock.sleep(1200);
        onView(withId(R.id.recyclerview_add_contact))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

        // First test adding an unknown contact.
        addContact();

        // Now test removing the same contact, resetting the state of this contact.
        removeContact();
    }

    @Test
    public void removeKnownContactAndAddExtendedContact() {
        navigateToContactProfileTab();

        // First test removing a known contact.
        removeContact();

        // Now test adding an extended contact, resetting the state of this contact.
        addContact();
    }
}
