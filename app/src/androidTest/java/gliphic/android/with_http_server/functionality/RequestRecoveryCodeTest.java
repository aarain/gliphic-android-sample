package gliphic.android.with_http_server.functionality;

import android.os.SystemClock;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.annotation.Nullable;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import gliphic.android.R;
import gliphic.android.display.welcome_screen.SignInActivity;
import gliphic.android.operation.server_interaction.http_server.HttpOperations;
import gliphic.android.operation.storage_handlers.SharedPreferencesHandler;
import gliphic.android.utils.AndroidTestUtils;
import libraries.Vars;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class RequestRecoveryCodeTest {

    @Rule
    public ActivityScenarioRule<SignInActivity> rule = new ActivityScenarioRule<>(SignInActivity.class);

    @Ignore(AndroidTestUtils.IGNORE_ANNOTATION_NO_API_KEY)
    @Test
    public void validRecoveryTokenRequest() throws Exception {
        final String emailAddress = RequestActivationCodeTest.TEST_EMAIL;

        try {
            onView(withId(R.id.sign_in_email)).perform(replaceText(emailAddress));
            onView(withId(R.id.textview_request_recovery_code)).perform(click());
            SystemClock.sleep(200);
            onView(withId(R.id.textview_recovery_code_help))
                    .perform(AndroidTestUtils.clickClickableSpan(RequestActivationCodeTest.CLICKABLE_REQUEST_CODE_TEXT));
            SignInAndOutTest.assertSignInViewsNotClickable();
            SystemClock.sleep(2000);    // Allow time for the server to respond.

            onView(withText("Code request successful")).check(matches(isDisplayed()));
        }
        finally {
            AndroidTestUtils.postString(
                    AndroidTestUtils.getUriPrefix() + "/account/set-recovery-email-time/" + emailAddress,
                    Long.toString(RequestActivationCodeTest.TEST_CONTACT_NUMBER)
            );
        }
    }

    @Test
    public void badEmail() {
        SystemClock.sleep(200);
        onView(withId(R.id.sign_in_email)).perform(replaceText(RequestActivationCodeTest.TEST_EMAIL_BAD));
        onView(withId(R.id.textview_request_recovery_code)).perform(click());
        SystemClock.sleep(200);
        onView(withId(R.id.textview_recovery_code_help))
                .perform(AndroidTestUtils.clickClickableSpan(RequestActivationCodeTest.CLICKABLE_REQUEST_CODE_TEXT));
        SignInAndOutTest.assertSignInViewsNotClickable();
        SystemClock.sleep(2000);    // Allow time for the server to respond.

        onView(withText(HttpOperations.ERROR_MSG_401_BAD_EMAIL)).check(matches(isDisplayed()));
    }

    @Test
    public void badDeviceCode() {
        handleDeviceCodeTests(new byte[Vars.DEVICE_CODE_LEN]);
    }

    // This test does not send a message to the server (so it could be moved to SignInActivityTest).
    @Test
    public void noStoredDeviceCode() {
        handleDeviceCodeTests(null);
    }

    private void handleDeviceCodeTests(@Nullable final byte[] testDeviceCode) {
        try {
            rule.getScenario().onActivity(activity -> {
                try {
                    if (testDeviceCode == null) {
                        SharedPreferencesHandler.removeDeviceCode(activity);
                    }
                    else {
                        SharedPreferencesHandler.setDeviceCode(activity, testDeviceCode);
                    }
                }
                catch (Throwable e) {
                    fail(e.getMessage());
                }
            });

            SystemClock.sleep(200);
            onView(withId(R.id.sign_in_email)).perform(replaceText(RequestActivationCodeTest.TEST_EMAIL));
            onView(withId(R.id.textview_request_recovery_code)).perform(click());
            SystemClock.sleep(200);
            onView(withId(R.id.textview_recovery_code_help))
                    .perform(AndroidTestUtils.clickClickableSpan(RequestActivationCodeTest.CLICKABLE_REQUEST_CODE_TEXT));

            // The AlertDialog appears on screen too quickly causing a NoMatchingViewException if this is not true.
            if (testDeviceCode != null) {
                SignInAndOutTest.assertSignInViewsNotClickable();
            }

            // Assume that a message is only sent to the server if the device code is non-null.
            SystemClock.sleep(testDeviceCode == null ? 200 : 2000);

            final String displayMessage;
            if (testDeviceCode == null) {
                displayMessage = "The data required to change your account password is not stored on this " +
                        "device. Ensure that you are attempting this request from the last device you signed in or " +
                        "activated your account with.";
            }
            else {
                displayMessage = HttpOperations.ERROR_MSG_401_BAD_EMAIL;
            }

            onView(withText(displayMessage)).check(matches(isDisplayed()));
        }
        finally {
            rule.getScenario().onActivity(activity -> {
                try {
                    SharedPreferencesHandler.setDeviceCode(activity, AndroidTestUtils.getDeviceCode());
                }
                catch (Throwable e) {
                    fail(e.getMessage());
                }
            });
        }
    }

    @Test
    public void lastEmailTooRecent() {
        SystemClock.sleep(200);
        onView(withId(R.id.sign_in_email)).perform(replaceText(RequestActivationCodeTest.TEST_EMAIL_TOO_RECENT));
        onView(withId(R.id.textview_request_recovery_code)).perform(click());
        SystemClock.sleep(200);
        onView(withId(R.id.textview_recovery_code_help))
                .perform(AndroidTestUtils.clickClickableSpan(RequestActivationCodeTest.CLICKABLE_REQUEST_CODE_TEXT));
        SignInAndOutTest.assertSignInViewsNotClickable();
        SystemClock.sleep(2000);    // Allow time for the server to respond.

        onView(withText(HttpOperations.ERROR_MSG_429_RECOVERY_PWD_CHANGE)).check(matches(isDisplayed()));
    }

    @Test
    public void registeredButInactiveAccount() {
        final String emailAddress =
                AndroidTestUtils.CONTACT_REGISTERED_INACTIVE + RequestActivationCodeTest.TEST_EMAIL;

        SystemClock.sleep(200);
        onView(withId(R.id.sign_in_email)).perform(replaceText(emailAddress));
        onView(withId(R.id.textview_request_recovery_code)).perform(click());
        SystemClock.sleep(200);
        onView(withId(R.id.textview_recovery_code_help))
                .perform(AndroidTestUtils.clickClickableSpan(RequestActivationCodeTest.CLICKABLE_REQUEST_CODE_TEXT));
        SignInAndOutTest.assertSignInViewsNotClickable();
        SystemClock.sleep(2000);    // Allow time for the server to respond.

        onView(withText(HttpOperations.ERROR_MSG_401_BAD_EMAIL)).check(matches(isDisplayed()));
    }
}
