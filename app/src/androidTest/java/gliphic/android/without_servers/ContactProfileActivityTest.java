package gliphic.android.without_servers;

import android.os.SystemClock;

import gliphic.android.R;
import gliphic.android.operation.Contact;
import gliphic.android.operation.storage_handlers.SharedPreferencesHandler;
import gliphic.android.utils.AndroidTestUtils;
import gliphic.android.utils.MainActivityBaseSetup;
import gliphic.android.utils.MainActivityTestRuleWithoutHttpServerSetup;
import gliphic.android.with_http_server.single_fragment_activity.RegisterActivityTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

/**
 * Test functionality unique to {@link gliphic.android.display.ContactProfileActivity}.
 */
@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class ContactProfileActivityTest extends MainActivityTestRuleWithoutHttpServerSetup {

    void navigateToContactProfileActivity() {
        // This assumes that the overflow menu options are available to click.

        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText(R.string.action_bar_contact_profile)).perform(click());
        SystemClock.sleep(500);
    }

    @Before
    public void waitForDialog() throws Throwable {
        AndroidTestUtils.waitForAction(AndroidTestUtils.WaitAction.SERVER_CONNECTION_FAILED_MSG);
    }

    @Test
    public void currentContactIsNullInOnCreate() throws Throwable {
        try {
            Contact.setNullCurrentContact();

            navigateToContactProfileActivity();
            SystemClock.sleep(5000);    // Allow time for the dialog to disappear.

            onView(withText("Server connection failed")).check(matches(isDisplayed()));
            onView(withId(android.R.id.button1)).perform(click());
        }
        finally {
            SharedPreferencesHandler.removeAllContactData(AndroidTestUtils.getApplicationContext());

            MainActivityBaseSetup.mainActivitySetupBeforeActivityLaunched();
        }
    }

    @Test
    public void viewsAreDisplayed() throws Exception {
        final long currentContactNumber = 0;
        final Contact currentContact = Contact.getContactFromNumber(currentContactNumber);

        navigateToContactProfileActivity();

        // Contact ID
        onView(withId(R.id.contact_profile_id)).check(matches(withText(currentContact.getId())));

        // Contact email
        onView(withId(R.id.contact_profile_email_address))
                .check(matches(withText(RegisterActivityTest.duplicateEmail)));
    }
}
