package gliphic.android.with_http_server.functionality;

import android.os.SystemClock;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import gliphic.android.R;
import gliphic.android.display.welcome_screen.SignInActivity;
import gliphic.android.operation.server_interaction.http_server.HttpOperations;
import gliphic.android.utils.AndroidTestUtils;
import gliphic.android.utils.MainActivityBaseSetup;
import gliphic.android.with_http_server.single_fragment_activity.RegisterActivityTest;

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
public class RequestActivationCodeTest {
           static final long   TEST_CONTACT_NUMBER         = 0;
    public static final String TEST_EMAIL                  = RegisterActivityTest.duplicateEmail;
           static final String TEST_EMAIL_BAD              = "bad@email.com";
           static final String TEST_EMAIL_TOO_RECENT       = "unactivatableInactiveContact@gmail.com";
    public static final long   TEST_CONTACT_TOO_RECENT     = 4485;

           static final String CLICKABLE_REQUEST_CODE_TEXT = "request code";

    @Rule
    public ActivityScenarioRule<SignInActivity> rule = new ActivityScenarioRule<>(SignInActivity.class);

    @Ignore(AndroidTestUtils.IGNORE_ANNOTATION_NO_API_KEY)
    @Test
    public void validActivationTokenRequest() throws Exception {
        try {
            MainActivityBaseSetup.deactivateAccount(TEST_CONTACT_NUMBER);
            SystemClock.sleep(500);     // Allow time for the server to respond.

            onView(withId(R.id.sign_in_email)).perform(replaceText(TEST_EMAIL));
            onView(withId(R.id.textview_request_activation_code)).perform(click());
            SystemClock.sleep(200);
            onView(withId(R.id.textview_activation_code_help))
                    .perform(AndroidTestUtils.clickClickableSpan(CLICKABLE_REQUEST_CODE_TEXT));
            SignInAndOutTest.assertSignInViewsNotClickable();
            SystemClock.sleep(5000);    // Allow time for the server to respond.

            onView(withText("Code request successful")).check(matches(isDisplayed()));
        }
        finally {
            MainActivityBaseSetup.activateAccount(TEST_CONTACT_NUMBER);

            AndroidTestUtils.postString(
                    AndroidTestUtils.getUriPrefix() + "/account/set-activation-email-time/" + TEST_CONTACT_NUMBER,
                    Long.toString(0)
            );
        }
    }

    @Test
    public void badEmailAddress() throws Exception {
        try {
            MainActivityBaseSetup.deactivateAccount(TEST_CONTACT_NUMBER);
            SystemClock.sleep(500);     // Allow time for the server to respond.

            onView(withId(R.id.sign_in_email)).perform(replaceText(TEST_EMAIL_BAD));
            onView(withId(R.id.textview_request_activation_code)).perform(click());
            SystemClock.sleep(200);
            onView(withId(R.id.textview_activation_code_help))
                    .perform(AndroidTestUtils.clickClickableSpan(CLICKABLE_REQUEST_CODE_TEXT));
            SignInAndOutTest.assertSignInViewsNotClickable();
            SystemClock.sleep(5000);    // Allow time for the server to respond.

            onView(withText(HttpOperations.ERROR_MSG_401_BAD_EMAIL)).check(matches(isDisplayed()));
        }
        finally {
            MainActivityBaseSetup.activateAccount(TEST_CONTACT_NUMBER);
        }
    }

    @Test
    public void accountAlreadyActive() {
        SystemClock.sleep(200);
        onView(withId(R.id.sign_in_email)).perform(replaceText(TEST_EMAIL));
        onView(withId(R.id.textview_request_activation_code)).perform(click());
        SystemClock.sleep(200);
        onView(withId(R.id.textview_activation_code_help))
                .perform(AndroidTestUtils.clickClickableSpan(CLICKABLE_REQUEST_CODE_TEXT));
        SignInAndOutTest.assertSignInViewsNotClickable();
        SystemClock.sleep(5000);    // Allow time for the server to respond.

        onView(withText(HttpOperations.ERROR_MSG_403_ACCOUNT_ACTIVE)).check(matches(isDisplayed()));
    }

    @Test
    public void lastEmailTooRecent() throws Exception {
        try {
            MainActivityBaseSetup.deactivateAccount(TEST_CONTACT_NUMBER);
            SystemClock.sleep(500);     // Allow time for the server to respond.

            SystemClock.sleep(200);
            onView(withId(R.id.sign_in_email)).perform(replaceText(TEST_EMAIL_TOO_RECENT));
            onView(withId(R.id.textview_request_activation_code)).perform(click());
            SystemClock.sleep(200);
            onView(withId(R.id.textview_activation_code_help))
                    .perform(AndroidTestUtils.clickClickableSpan(CLICKABLE_REQUEST_CODE_TEXT));
            SignInAndOutTest.assertSignInViewsNotClickable();
            SystemClock.sleep(5000);    // Allow time for the server to respond.

            onView(withText(HttpOperations.ERROR_MSG_429_ACTIVATION)).check(matches(isDisplayed()));
        }
        finally {
            MainActivityBaseSetup.activateAccount(TEST_CONTACT_NUMBER);
        }
    }
}
