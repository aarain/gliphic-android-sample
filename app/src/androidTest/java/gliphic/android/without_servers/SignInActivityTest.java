package gliphic.android.without_servers;

import android.os.SystemClock;

import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import gliphic.android.R;
import gliphic.android.display.libraries.AlertDialogs;
import gliphic.android.display.welcome_screen.SignInActivity;
import gliphic.android.utils.AndroidTestUtils;
import gliphic.android.utils.view_actions.CheckBoxSelection;
import gliphic.android.with_http_server.single_fragment_activity.RegisterActivityTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isNotChecked;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static gliphic.android.utils.AndroidTestUtils.getOnViewInteractionFromId;
import static gliphic.android.utils.AndroidTestUtils.getOnViewInteractionFromString;
import static gliphic.android.utils.matchers.CustomMatchers.isItalicsTypeface;
import static org.hamcrest.CoreMatchers.not;

/*
 * Note that all of these sign-in tests which cover validation of user inputs before any server interaction also cover
 * token request operations.
 */
@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class SignInActivityTest {
    private static final String validEmail = RegisterActivityTest.duplicateEmail;
    private static final String validPwd = RegisterActivityTest.validPwd;

    @Rule
    public ActivityScenarioRule<SignInActivity> rule = new ActivityScenarioRule<>(SignInActivity.class);

    @Test
    public void pressBackRegister() {
        // Use this to check that navigating back preserves the text in the previous activity (because the sign-in
        // activity is never killed).
        final String email = "someRandomText";
        onView(withId(R.id.sign_in_email)).perform(replaceText(email));

        // Test the back-to-sign-in button.
        onView(withId(R.id.textview_register_new_account)).perform(click());
        SystemClock.sleep(1000);
        getOnViewInteractionFromId(R.id.btn_register_back).perform(click());
        SystemClock.sleep(1000);
        onView(withId(R.id.sign_in_email)).check(matches(withText(email)));
    }

    @Test
    public void pressBackSubmitCode() {
        // Use this to check that navigating back preserves the text in the previous activity (because the sign-in
        // activity is never killed).
        final String email = "someRandomText";
        onView(withId(R.id.sign_in_email)).perform(replaceText(email));

        // Test the support-action-bar back button.
        onView(withId(R.id.textview_submit_code)).perform(click());
        SystemClock.sleep(1000);
        pressBack();
        SystemClock.sleep(1000);
        onView(withId(R.id.sign_in_email)).check(matches(withText(email)));
    }

    @Test
    public void showAndHideViews() {
        assertTextVisible(false, false);

        clickRequestActivationCode();
        assertTextVisible(true, false);

        clickRequestActivationCode();
        assertTextVisible(false, false);

        clickRequestRecoveryCode();
        assertTextVisible(false, true);

        clickRequestRecoveryCode();
        assertTextVisible(false, false);

        clickRequestActivationCode();
        clickRequestRecoveryCode();
        assertTextVisible(false, true);

        clickRequestActivationCode();
        assertTextVisible(true, false);
    }

    private void clickRequestActivationCode() {
        onView(withId(R.id.textview_request_activation_code)).perform(click());
        SystemClock.sleep(100);
    }

    private void clickRequestRecoveryCode() {
        onView(withId(R.id.textview_request_recovery_code)).perform(click());
        SystemClock.sleep(100);
    }

    private void assertTextVisible(boolean isActivationHelpVisible, boolean isRecoveryHelpVisible) {
        final ViewInteraction requestActivationCode = onView(withId(R.id.textview_request_activation_code));
        final ViewInteraction requestRecoveryCode   = onView(withId(R.id.textview_request_recovery_code));
        final ViewInteraction activationCodeHelp    = onView(withId(R.id.textview_activation_code_help));
        final ViewInteraction recoveryCodeHelp      = onView(withId(R.id.textview_recovery_code_help));

        if (isActivationHelpVisible) {
            requestActivationCode.check(matches(isItalicsTypeface()));
            activationCodeHelp.check(matches(isDisplayed()));
        }
        else {
            requestActivationCode.check(matches(not(isItalicsTypeface())));
            activationCodeHelp.check(matches(not(isDisplayed())));
        }

        if (isRecoveryHelpVisible) {
            requestRecoveryCode.check(matches(isItalicsTypeface()));
            recoveryCodeHelp.check(matches(isDisplayed()));
        }
        else {
            requestRecoveryCode.check(matches(not(isItalicsTypeface())));
            recoveryCodeHelp.check(matches(not(isDisplayed())));
        }
    }

    @Test
    public void signInActivityAndSubmitCodeActivityUpdateEachOther() {
        onView(withId(R.id.sign_in_remember_email)).perform(scrollTo(), CheckBoxSelection.setChecked(false));
        navigateToSubmitSignInCodeView();

        AndroidTestUtils.getOnViewInteractionFromId(R.id.submit_code_checkbox).perform(click());
        pressBack();
        SystemClock.sleep(200);
        onView(withId(R.id.sign_in_remember_email)).check(matches(isChecked()));

        onView(withId(R.id.sign_in_remember_email)).perform(click());
        navigateToSubmitSignInCodeView();
        AndroidTestUtils.getOnViewInteractionFromId(R.id.submit_code_checkbox).check(matches(isNotChecked()));

        pressBack();
        SystemClock.sleep(200);
        onView(withId(R.id.sign_in_remember_email)).check(matches(isNotChecked()));

        onView(withId(R.id.sign_in_remember_email)).perform(click());
        navigateToSubmitSignInCodeView();
        AndroidTestUtils.getOnViewInteractionFromId(R.id.submit_code_checkbox).check(matches(isChecked()));

        AndroidTestUtils.getOnViewInteractionFromId(R.id.submit_code_checkbox).perform(click());
        pressBack();
        SystemClock.sleep(200);
        onView(withId(R.id.sign_in_remember_email)).check(matches(isNotChecked()));
    }

    private void navigateToSubmitSignInCodeView() {
        onView(withId(R.id.textview_submit_code)).perform(click());
        SystemClock.sleep(200);
        getOnViewInteractionFromString(R.string.submit_code_sign_in).perform(click());
        SystemClock.sleep(200);
    }

    /* Invalid contact name */

    @Test
    public void emptyEmail() {
        onView(withId(R.id.sign_in_email)).perform(replaceText(""));
        onView(withId(R.id.sign_in_pwd)).perform(replaceText(validPwd));
        onView(withId(R.id.btn_sign_in)).perform(click());
        SystemClock.sleep(100);

        onView(withText(AlertDialogs.EMAIL_EMPTY_MSG)).check(matches(isDisplayed()));
    }

    @Test
    public void invalidEmailLength() {
        String invalidEmail = "..";
        onView(withId(R.id.sign_in_email)).perform(replaceText(invalidEmail));
        onView(withId(R.id.sign_in_pwd)).perform(replaceText(validPwd));
        onView(withId(R.id.btn_sign_in)).perform(click());
        SystemClock.sleep(100);

        onView(withText(AlertDialogs.EMAIL_LENGTH_INVALID)).check(matches(isDisplayed()));
    }

    @Test
    public void invalidEmailFormat() {
        onView(withId(R.id.sign_in_email)).perform(replaceText("\"'invalid'\" email address"));
        onView(withId(R.id.sign_in_pwd)).perform(replaceText(validPwd));
        onView(withId(R.id.btn_sign_in)).perform(click());
        SystemClock.sleep(100);

        onView(withText(AlertDialogs.EMAIL_INVALID_MSG)).check(matches(isDisplayed()));
    }

    /* Invalid password */

    @Test
    public void emptyPassword() {
        onView(withId(R.id.sign_in_email)).perform(replaceText(validEmail));
        onView(withId(R.id.sign_in_pwd)).perform(replaceText(""));
        onView(withId(R.id.btn_sign_in)).perform(click());
        SystemClock.sleep(100);

        onView(withText(AlertDialogs.ENTERED_PASSWORD_EMPTY_MSG)).check(matches(isDisplayed()));
    }

    @Test
    public void isNotPrintableStringPassword() {
        onView(withId(R.id.sign_in_email)).perform(replaceText(validEmail));
        onView(withId(R.id.sign_in_pwd)).perform(replaceText(AndroidTestUtils.INVALID_CHAR_STRING));
        onView(withId(R.id.btn_sign_in)).perform(click());
        SystemClock.sleep(100);

        onView(withText(AlertDialogs.ENTERED_PASSWORD_NOT_PRINTABLE)).check(matches(isDisplayed()));
    }

    @Test
    public void invalidLengthPassword() {
        String invalidPassword = "2short";
        onView(withId(R.id.sign_in_email)).perform(replaceText(validEmail));
        onView(withId(R.id.sign_in_pwd)).perform(replaceText(invalidPassword));
        onView(withId(R.id.btn_sign_in)).perform(click());
        SystemClock.sleep(100);

        onView(withText(AlertDialogs.ENTERED_PASSWORD_LENGTH_INVALID)).check(matches(isDisplayed()));
    }
}
