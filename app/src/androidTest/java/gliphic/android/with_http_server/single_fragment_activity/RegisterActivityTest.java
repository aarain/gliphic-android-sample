package gliphic.android.with_http_server.single_fragment_activity;

import android.os.SystemClock;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import gliphic.android.R;
import gliphic.android.display.libraries.AlertDialogs;
import gliphic.android.display.welcome_screen.RegisterActivity;
import gliphic.android.operation.server_interaction.http_server.HttpOperations;
import gliphic.android.utils.AndroidTestUtils;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class RegisterActivityTest {
    public static final String validExistingName = "Test User";
    public static final String validNewName      = "New User";
    public static final String validPwd          = "P455w0rd";
    public static final String newEmail          = "temp-email@test.com";
    public static final String duplicateEmail    = "ashleyarain@hotmail.com";

    @Rule
    public ActivityScenarioRule<RegisterActivity> rule = new ActivityScenarioRule<>(RegisterActivity.class);

    /*
     * Manually set up SSL (for HTTPS) since this class does not extend MainActivityBaseSetup and the
     * ActivityTestRule is not for the SignInActivity.
     */
    @BeforeClass
    public static void sslSetup() throws Exception {
        HttpOperations.restSslSetup(null, null);
    }

    @Before
    public void setValidRegistrationInput() {
        onView(withId(R.id.register_email)).perform(replaceText(duplicateEmail));
    }

    @Ignore(AndroidTestUtils.IGNORE_ANNOTATION_NO_API_KEY)
    @Test
    public void validContactRegistration() throws Throwable {
        try {
            // Change the email address of the 0th contact so that the original email can be reused for a new account.
            String uri = HttpOperations.URI_REGISTER_ACCOUNT + "/set-email-for-contact-0";
            AndroidTestUtils.postString(uri, newEmail);

            onView(withId(R.id.btn_register_account)).perform(click());

            SystemClock.sleep(4000);    // Allow time for the server to respond.

            onView(withText("Account activation required")).check(matches(isDisplayed()));

            // Clean up the database by deleting the newly registered contact, if contact-registration was successful.
            uri = HttpOperations.URI_REGISTER_ACCOUNT + "/delete-newest-contact";
            AndroidTestUtils.getRequest(uri);
        }
        finally {
            // Revert the email address of the 0th contact.
            String uri = HttpOperations.URI_REGISTER_ACCOUNT + "/set-email-for-contact-0";
            AndroidTestUtils.postString(uri, duplicateEmail);
        }
    }

    /* Invalid email address */

    @Test
    public void emptyEmailAddress() {
        onView(withId(R.id.register_email)).perform(replaceText(""));
        onView(withId(R.id.btn_register_account)).perform(click());
        SystemClock.sleep(100);

        onView(withText(AlertDialogs.EMAIL_EMPTY_MSG)).check(matches(isDisplayed()));
    }

    @Test
    public void invalidLengthEmailAddress() {
        onView(withId(R.id.register_email)).perform(replaceText("r@"));
        onView(withId(R.id.btn_register_account)).perform(click());
        SystemClock.sleep(100);

        onView(withText(AlertDialogs.EMAIL_LENGTH_INVALID)).check(matches(isDisplayed()));
    }

    @Test
    public void invalidMatcherEmailAddress() {
        onView(withId(R.id.register_email)).perform(replaceText("invalid string"));
        onView(withId(R.id.btn_register_account)).perform(click());
        SystemClock.sleep(100);

        String s = "You entered an invalid email address. " +
                "Please enter a valid email address.";
        onView(withText(s)).check(matches(isDisplayed()));
    }

    /* Invalid registration requests from server responses */

    @Ignore(AndroidTestUtils.IGNORE_ANNOTATION_NO_API_KEY)
    @Test
    public void invalidEmailAddressDomain() {
        onView(withId(R.id.register_email)).perform(replaceText("email@test123.fake.dom"));
        onView(withId(R.id.btn_register_account)).perform(click());
        SystemClock.sleep(5000);

        onView(withText(AlertDialogs.EMAIL_INVALID_MSG)).check(matches(isDisplayed()));
    }

    @Test
    public void duplicateEmailAddress() {
        onView(withId(R.id.register_email)).perform(replaceText(duplicateEmail));
        onView(withId(R.id.btn_register_account)).perform(click());
        SystemClock.sleep(5000);

        String s = "The given email address is already in use. If you are the owner of this email " +
                "address and have lost your account activation code you can have another code " +
                "sent to this email address by following the instructions for an expired activation " +
                "code located on the sign-in screen.";
        onView(withText(s)).check(matches(isDisplayed()));
    }

    /* Internal server error tests */

    @Test
    public void groupsLimitReached() throws Exception {
        final String insertLatestContactNumber = "-1";
        final String urlSuffix = "/enough-to-reach-limit";

        try {
            // Ensure that the server has created enough groups for this contact to reach the limit.
            AndroidTestUtils.postString(HttpOperations.URI_CREATE_GROUP + urlSuffix, insertLatestContactNumber);
            SystemClock.sleep(2000);

            onView(withId(R.id.register_email)).perform(replaceText(newEmail));
            onView(withId(R.id.btn_register_account)).perform(click());
            SystemClock.sleep(2000);

            onView(withText(HttpOperations.ERROR_MSG_500_INVITING)).check(matches(isDisplayed()));
        }
        finally {
            // Clean up the database by deleting the newly created group.
            AndroidTestUtils.postString(HttpOperations.URI_LEAVE_GROUP + urlSuffix, insertLatestContactNumber);
        }
    }
}
