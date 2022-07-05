package gliphic.android.without_servers;

import gliphic.android.R;
import gliphic.android.operation.storage_handlers.SharedPreferencesHandler;
import gliphic.android.utils.AndroidTestUtils;
import gliphic.android.utils.MainActivityTestRuleWithoutHttpServerSetup;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.Espresso;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.core.IsNot.not;

/**
 * Similar to ContactProfileActivityTest but this class tests the case when the last email address information has been
 * removed from SharedPreferences.
 */
@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class ContactProfileActivityWithoutEmailTest extends MainActivityTestRuleWithoutHttpServerSetup {

    @Before
    public void beforeTest() throws Throwable {
        SharedPreferencesHandler.removeLastEmailAddress(AndroidTestUtils.getApplicationContext());

        final ContactProfileActivityTest cpat = new ContactProfileActivityTest();
        cpat.waitForDialog();
        cpat.navigateToContactProfileActivity();
    }

    @Test
    public void emailAddressIsNotDisplayed() {
        Espresso.onView(withId(R.id.contact_profile_email_address)).check(matches(not(isDisplayed())));
    }
}
