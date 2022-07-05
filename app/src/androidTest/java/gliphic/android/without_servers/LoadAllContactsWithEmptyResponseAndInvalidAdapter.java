package gliphic.android.without_servers;

import android.os.SystemClock;

import gliphic.android.R;
import gliphic.android.display.abstract_views.BaseActivity;
import gliphic.android.display.welcome_screen.SignInActivity;
import gliphic.android.operation.Contact;
import gliphic.android.operation.server_interaction.http_server.HttpOperations;
import gliphic.android.operation.storage_handlers.SharedPreferencesHandler;
import gliphic.android.utils.AndroidTestUtils;
import gliphic.android.utils.NullGlobalStaticsTestWithoutHttpServerRule;
import gliphic.android.with_http_server.functionality.load_more_contacts_tests.LoadMoreContactsWithBadAccessAndRefreshTokenTest;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class LoadAllContactsWithEmptyResponseAndInvalidAdapter {

    @Rule
    public TestRule<SignInActivity> rule = new TestRule<>(SignInActivity.class);

    private static class TestRule<A extends BaseActivity> extends NullGlobalStaticsTestWithoutHttpServerRule<A> {

        private TestRule(Class<A> activityClass) {
            super(activityClass);
        }

        @Override
        protected void before() {
            super.before();

            // None of the SharedPreferences data is removed, only the data held in static memory is removed.
            SharedPreferencesHandler.removeAllContactData(null);

            try {
                AndroidTestUtils.waitForAction(AndroidTestUtils.WaitAction.SERVER_CONNECTION_FAILED_MSG);
            }
            catch (Throwable e) {
                fail(e.getMessage());
            }

            try {
                // Ensure that the expiry is valid to prevent a new access token being requested
                // from the server.
                long validExpiryTime =
                        System.currentTimeMillis() + HttpOperations.REQUEST_TIME_OUT + 100000;
                validExpiryTime -= validExpiryTime % 1000;

                SharedPreferencesHandler.setAccessTokenExpiry(
                        AndroidTestUtils.getApplicationContext(),
                        validExpiryTime
                );
            }
            catch (Throwable e) {
                fail(e.getMessage());
            }

            // Initialise the known/extended contacts lists so that there are null.
            Contact.setNullKnownContacts();
            Contact.setNullExtendedContacts();

            SystemClock.sleep(200);
            LoadMoreContactsWithBadAccessAndRefreshTokenTest.swipeToAllContactsTab();
        }
    }

    @BeforeClass
    public static void delayForContactsTabTest() {
        SystemClock.sleep(5000);
    }

    @Test
    public void loadAllContactsWithEmptyResponseAndInvalidAdapter() {
        // View(s) which are not displayed.
        onView(ViewMatchers.withId(R.id.edittext_main_tab_contacts)).check(matches(not(isDisplayed())));
        onView(ViewMatchers.withId(R.id.checkbox_main_known_contacts)).check(matches(not(isDisplayed())));
        onView(ViewMatchers.withId(R.id.checkbox_main_extended_contacts)).check(matches(not(isDisplayed())));
        onView(ViewMatchers.withId(R.id.itemdivider_main_tab_contacts)).check(matches(not(isDisplayed())));
        onView(ViewMatchers.withId(R.id.textview_no_contacts)).check(matches(not(isDisplayed())));
        // View(s) which are displayed.
        onView(ViewMatchers.withId(R.id.contacts_tab_error_contact)).check(matches(isDisplayed()));
    }
}
