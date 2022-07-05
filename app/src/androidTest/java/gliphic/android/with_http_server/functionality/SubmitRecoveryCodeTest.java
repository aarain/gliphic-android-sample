package gliphic.android.with_http_server.functionality;

import android.content.Context;
import android.os.SystemClock;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import gliphic.android.R;
import gliphic.android.display.libraries.AlertDialogs;
import gliphic.android.display.welcome_screen.SubmitCodeActivity;
import gliphic.android.operation.server_interaction.http_server.HttpOperations;
import gliphic.android.operation.storage_handlers.SharedPreferencesHandler;
import gliphic.android.utils.AndroidTestUtils;
import gliphic.android.with_http_server.single_fragment_activity.RegisterActivityTest;
import gliphic.android.with_http_server.single_fragment_activity.SubmitCodeActivityTest;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static gliphic.android.utils.AndroidTestUtils.getOnViewInteractionFromId;

@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class SubmitRecoveryCodeTest {
    private final ViewInteraction onViewEditTextSubmitCode       = getOnViewInteractionFromId(R.id.edittext_submit_code);
    private final ViewInteraction onViewEditTextNewPassword      = getOnViewInteractionFromId(R.id.submit_code_edittext_2);
    private final ViewInteraction onViewEditTextRepeatedPassword = getOnViewInteractionFromId(R.id.submit_code_edittext_3);
    private final ViewInteraction onViewButtonSubmitCode         = getOnViewInteractionFromId(R.id.btn_submit_code);

    @Rule
    public ActivityScenarioRule<SubmitCodeActivity> rule = new ActivityScenarioRule<>(SubmitCodeActivity.class);

    /*
     * Manually set up SSL (for HTTPS) since this class does not extend MainActivityBaseSetup and the
     * ActivityTestRule is not for the SignInActivity.
     */
    @BeforeClass
    public static void setup() throws Exception {
        HttpOperations.restSslSetup(null, null);
    }

    @Before
    public void navigateToTabAndSetDummyToken() {
        onView(withText(R.string.submit_code_recovery)).perform(click());
        SystemClock.sleep(500);

        onViewEditTextSubmitCode.perform(replaceText("dummy token"));
    }

    /**
     * See {@link SubmitCodeActivityTest#validSubmitToken()} for a valid account recovery test.
     */

    /* Invalid password */

    @Test
    public void emptyPassword() {
        onViewEditTextNewPassword.perform(replaceText(""));
        onViewButtonSubmitCode.perform(click());
        SystemClock.sleep(100);

        onView(withText(String.format(AlertDialogs.CHOSEN_PASSWORD_EMPTY_MSG, " new "))).check(matches(isDisplayed()));
    }

    @Test
    public void isNotPrintableStringPassword() {
        onViewEditTextNewPassword.perform(replaceText(AndroidTestUtils.INVALID_CHAR_STRING));
        onViewButtonSubmitCode.perform(click());
        SystemClock.sleep(100);

        onView(withText(AlertDialogs.CHOSEN_PASSWORD_NOT_PRINTABLE)).check(matches(isDisplayed()));
    }

    @Test
    public void invalidLengthPassword() {
        onViewEditTextNewPassword.perform(replaceText("a"));
        onViewButtonSubmitCode.perform(click());
        SystemClock.sleep(100);

        onView(withText(AlertDialogs.CHOSEN_PASSWORD_LENGTH_INVALID)).check(matches(isDisplayed()));
    }

    /* Invalid repeated password */

    @Test
    public void invalidRepeatedPassword() {
        onViewEditTextNewPassword.perform(replaceText(RegisterActivityTest.validPwd));
        onViewEditTextRepeatedPassword.perform(replaceText(""));
        onViewButtonSubmitCode.perform(click());
        SystemClock.sleep(100);

        onView(withText(AlertDialogs.CHOSEN_PASSWORDS_UNEQUAL_MSG)).check(matches(isDisplayed()));
    }

    /* Missing device data */

    private void handleNoStoredDeviceDataTest(boolean testDeviceKey, boolean testDeviceCode) throws Exception {
        try {
            final Context context = AndroidTestUtils.getApplicationContext();

            if (testDeviceKey) {
                SharedPreferencesHandler.removeDeviceKey(context);
            }

            if (testDeviceCode) {
                SharedPreferencesHandler.removeDeviceCode(context);
            }

            SystemClock.sleep(100);

            onViewEditTextNewPassword.perform(replaceText(RegisterActivityTest.validPwd));
            onViewEditTextRepeatedPassword.perform(replaceText(RegisterActivityTest.validPwd));
            onViewButtonSubmitCode.perform(click());
            SystemClock.sleep(100);

            final String displayMessage = "The data required to change your account password is not stored on this " +
                    "device. Ensure that you are attempting this request from the last device you signed in or " +
                    "activated your account with.";
            onView(withText(displayMessage)).check(matches(isDisplayed()));
        }
        finally {
            final Context context = AndroidTestUtils.getApplicationContext();

            if (testDeviceKey) {
                SharedPreferencesHandler.setDeviceKey(context, AndroidTestUtils.getDeviceKey());
            }

            if (testDeviceCode) {
                SharedPreferencesHandler.setDeviceCode(context, AndroidTestUtils.getDeviceCode());
            }
        }
    }

    @Test
    public void noStoredDeviceKey() throws Throwable {
        handleNoStoredDeviceDataTest(true, false);
    }

    @Test
    public void noStoredDeviceCode() throws Throwable {
        handleNoStoredDeviceDataTest(false, true);
    }
}
