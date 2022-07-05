package gliphic.android.without_servers;

import android.os.SystemClock;

import gliphic.android.R;
import gliphic.android.operation.Contact;
import gliphic.android.utils.AndroidTestUtils;
import gliphic.android.utils.MainIntentTestRuleWithoutHttpServerSetup;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

/**
 * Test functionality unique to ContactProfileTab.
 */
@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class ContactProfileTabTest extends MainIntentTestRuleWithoutHttpServerSetup {
    private final static long displayedContactNumber = 1;

    @Before
    public void selectGroupsTab() throws Throwable {
        AndroidTestUtils.waitForAction(AndroidTestUtils.WaitAction.SERVER_CONNECTION_FAILED_MSG);
        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
        SystemClock.sleep(200);
        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
        SystemClock.sleep(200);
        onView(withId(R.id.recyclerview_main_tab_contacts))
                .perform(RecyclerViewActions.actionOnItemAtPosition((int) displayedContactNumber - 1, click()));
        SystemClock.sleep(1000);
    }

    @Test
    public void checkDisplay() throws Exception {
        onView(withId((R.id.cardview_contact_details_tab_profile))).check(matches(isDisplayed()));
        onView(withId((R.id.imageview_contact_details_tab_profile))).check(matches(isDisplayed()));
        onView(withId((R.id.btn_contact_details_add_remove))).check(matches(isDisplayed()));
        onView(withId((R.id.btn_contact_details_share))).check(matches(isDisplayed()));

        final Contact displayedContact = Contact.getContactFromNumber(displayedContactNumber);

        // Contact ID
        onView(withText(String.format(
                AndroidTestUtils.getResourceString(R.string.contact_details_id),
                displayedContact.getId()
        ))).check(matches(isDisplayed()));
        onView(withId(R.id.contact_id_contact_details_tab_profile))
                .check(matches(withText(displayedContact.getId())));

        // Contact name
        onView(withId(R.id.contact_name_contact_details_tab_profile))
                .check(matches(withText(displayedContact.getName())));
    }

    /* Contact profile updates globally */


}
