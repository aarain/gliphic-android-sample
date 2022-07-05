package gliphic.android.with_http_server.functionality;

import android.os.SystemClock;
import android.view.View;

import gliphic.android.R;
import gliphic.android.display.welcome_screen.SignInActivity;
import gliphic.android.utils.AndroidTestUtils;
import gliphic.android.utils.SignInAndOutTestRule;
import gliphic.android.utils.view_actions.CheckBoxSelection;
import gliphic.android.with_http_server.single_fragment_activity.RegisterActivityTest;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isNotChecked;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static gliphic.android.utils.AndroidTestUtils.getOnViewInteractionFromString;

@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class RememberEmailAddressTest {
    private static final long TEST_CONTACT_NUMBER = 0;
    private static final String TEST_EMAIL_ADDRESS = RegisterActivityTest.duplicateEmail;

    @Rule
    public SignInAndOutTestRule<SignInActivity> rule = new SignInAndOutTestRule<>(SignInActivity.class);

    private void navigateToSubmitSignInCodeView() {
        onView(withId(R.id.textview_submit_code)).perform(click());
        SystemClock.sleep(200);
        getOnViewInteractionFromString(R.string.submit_code_sign_in).perform(click());
        SystemClock.sleep(200);
    }

    private void assertChecked(boolean isChecked, boolean navigateBackToSignInActivity) {
        final String expectedEmailAddress;
        final Matcher<View> isCheckedMatcher;

        if (isChecked) {
            expectedEmailAddress = TEST_EMAIL_ADDRESS;
            isCheckedMatcher = isChecked();
        }
        else {
            expectedEmailAddress = "";
            isCheckedMatcher = isNotChecked();
        }

        // Verify that the email address has been remembered/forgotten and that the CheckBox is (un)checked on the
        // sign-in screen.

        onView(ViewMatchers.withId(R.id.sign_in_email)).check(matches(withText(expectedEmailAddress)));
        onView(withId(R.id.sign_in_remember_email)).check(matches(isCheckedMatcher));

        // Verify that the email address has been remembered/forgotten and that the CheckBox is (un)checked on the
        // submit-code activity.

        navigateToSubmitSignInCodeView();

        AndroidTestUtils.getOnViewInteractionFromId(R.id.submit_code_edittext_2)
                .check(matches(ViewMatchers.withText(expectedEmailAddress)));
        AndroidTestUtils.getOnViewInteractionFromId(R.id.submit_code_checkbox).check(matches(isCheckedMatcher));

        if (navigateBackToSignInActivity) {
            pressBack();
            SystemClock.sleep(200);
        }
    }

    @Test
    public void contactProfileRememberEmailCheckBoxTest() throws Throwable {
        /* First sign-in session. */

        onView(withId(R.id.sign_in_remember_email)).perform(scrollTo(), CheckBoxSelection.setChecked(false));

        SignInAndOutTest.signIn();
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText(R.string.action_bar_contact_profile)).perform(click());

        // Verify that the CheckBox is unchecked in the contact profile.
        onView(withId(R.id.contact_profile_remember_email)).check(matches(isNotChecked()));

        // Check the CheckBox in the contact profile.
        onView(withId(R.id.contact_profile_remember_email)).perform(click());

        pressBack();
        SignInAndOutTest.signOut();

        assertChecked(true, true);

        /* Second sign-in session. */

        SignInAndOutTest.signIn();
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText(R.string.action_bar_contact_profile)).perform(click());

        // Verify that the CheckBox is checked in the contact profile.
        onView(withId(R.id.contact_profile_remember_email)).check(matches(isChecked()));

        // Uncheck the CheckBox in the contact profile.
        onView(withId(R.id.contact_profile_remember_email)).perform(click());

        pressBack();
        SignInAndOutTest.signOut();

        assertChecked(false, false);
    }

    @Test
    public void signInRememberEmailCheckBoxTest() throws Throwable {
        /* First sign-in session. */

        onView(withId(R.id.sign_in_remember_email)).perform(scrollTo(), CheckBoxSelection.setChecked(false));

        SignInAndOutTest.signIn();
        SignInAndOutTest.signOut();

        assertChecked(false, true);

        // Uncheck the CheckBox on the sign-in screen.
        onView(withId(R.id.sign_in_remember_email)).perform(click());

        /* Second sign-in session. */

        SignInAndOutTest.signIn();
        SignInAndOutTest.signOut();

        assertChecked(true, false);
    }

    @Test
    public void signInWithCodeRememberEmailCheckBoxTest() throws Throwable {
        /* First sign-in session. */

        onView(withId(R.id.sign_in_remember_email)).perform(scrollTo(), CheckBoxSelection.setChecked(false));
        navigateToSubmitSignInCodeView();

        SignInAndOutTest.signInWithCode(false, TEST_CONTACT_NUMBER);
        SignInAndOutTest.signOut();

        assertChecked(false, false);

        // Uncheck the CheckBox on the submit-code activity.
        AndroidTestUtils.getOnViewInteractionFromId(R.id.submit_code_checkbox).perform(click());

        /* Second sign-in session. */

        SignInAndOutTest.signInWithCode(false, TEST_CONTACT_NUMBER);
        SignInAndOutTest.signOut();

        assertChecked(true, false);
    }
}
