package gliphic.android.without_servers;

import android.os.SystemClock;
import android.view.View;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import gliphic.android.R;
import gliphic.android.display.welcome_screen.SubmitCodeActivity;
import gliphic.android.utils.AndroidTestUtils;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.swipeDown;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static gliphic.android.utils.AndroidTestUtils.getOnViewInteractionFromId;
import static gliphic.android.utils.AndroidTestUtils.getOnViewInteractionFromString;
import static gliphic.android.utils.AndroidTestUtils.swipeUp;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class SubmitCodeActivityTest {

    private enum TabIndex {
        ACTIVATION,
        RECOVERY,
        SIGN_IN;

        // The tab indices correspond to the order in which the tabs appear, left to right.
        private static final List<TabIndex> tabIndices = Arrays.asList(ACTIVATION, RECOVERY, SIGN_IN);

        public int getIndex() {
            return tabIndices.indexOf(this);
        }
    }

    private void checkMatcherIsDisplayed(final Matcher<View> matcher, final int index) {
        handleCheckMatcherIsDisplayed(matcher, index, true);
    }

    private void checkMatcherNotDisplayed(final Matcher<View> matcher, final int index) {
        handleCheckMatcherIsDisplayed(matcher, index, false);
    }

    private void handleCheckMatcherIsDisplayed(final Matcher<View> matcher, final int index, boolean isDisplayed) {
        final ViewInteraction viewInteraction = onView(AndroidTestUtils.withIndex(matcher, index));

        if (isDisplayed) {
            viewInteraction.check(matches(isDisplayed()));
        }
        else {
            viewInteraction.check(matches(not(isDisplayed())));
        }
    }

    @Rule
    public ActivityScenarioRule<SubmitCodeActivity> rule = new ActivityScenarioRule<>(SubmitCodeActivity.class);

    @Test
    public void onCreateViewsDisplayed() {
        // Activation tab
        int tabIndex = TabIndex.ACTIVATION.getIndex();

        checkMatcherIsDisplayed(withId(R.id.submit_code_preamble), tabIndex);
        checkMatcherIsDisplayed(withId(R.id.submit_code_header_1), tabIndex);
        checkMatcherIsDisplayed(withId(R.id.edittext_submit_code), tabIndex);
        checkMatcherIsDisplayed(withId(R.id.submit_code_header_2), tabIndex);
        checkMatcherIsDisplayed(withId(R.id.submit_code_edittext_2), tabIndex);
        checkMatcherIsDisplayed(withId(R.id.submit_code_header_3), tabIndex);
        checkMatcherIsDisplayed(withId(R.id.submit_code_edittext_3), tabIndex);
        checkMatcherIsDisplayed(withId(R.id.submit_code_header_4), tabIndex);
        checkMatcherIsDisplayed(withId(R.id.submit_code_edittext_4), tabIndex);
        checkMatcherNotDisplayed(withId(R.id.submit_code_checkbox), tabIndex);

        checkMatcherIsDisplayed(withText(R.string.submit_activation_code_preamble), 0);
        checkMatcherIsDisplayed(withText(R.string.activation_code_header), 0);
        checkMatcherIsDisplayed(withText(R.string.activation_pwd), 0);
        checkMatcherIsDisplayed(withText(R.string.submit_code_pwd_repeat), tabIndex);

        getOnViewInteractionFromId(R.id.viewpager_activity_base).perform(swipeUp());
        SystemClock.sleep(100);

        checkMatcherIsDisplayed(withId(R.id.submit_code_image), tabIndex);
        checkMatcherIsDisplayed(withId(R.id.btn_submit_code), tabIndex);

        checkMatcherIsDisplayed(withText(R.string.activation_image), tabIndex);
        checkMatcherIsDisplayed(withText(R.string.submit_code_btn), tabIndex);

        // Recovery tab
        tabIndex = TabIndex.RECOVERY.getIndex();

        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
        SystemClock.sleep(500);

        checkMatcherIsDisplayed(withId(R.id.submit_code_preamble), tabIndex);
        checkMatcherIsDisplayed(withId(R.id.submit_code_header_1), tabIndex);
        checkMatcherIsDisplayed(withId(R.id.edittext_submit_code), tabIndex);
        checkMatcherIsDisplayed(withId(R.id.submit_code_header_2), tabIndex);
        checkMatcherIsDisplayed(withId(R.id.submit_code_edittext_2), tabIndex);
        checkMatcherIsDisplayed(withId(R.id.submit_code_header_3), tabIndex);
        checkMatcherIsDisplayed(withId(R.id.submit_code_edittext_3), tabIndex);
        checkMatcherNotDisplayed(withId(R.id.submit_code_header_4), tabIndex);
        checkMatcherNotDisplayed(withId(R.id.submit_code_edittext_4), tabIndex);
        checkMatcherNotDisplayed(withId(R.id.submit_code_checkbox), tabIndex);
        checkMatcherIsDisplayed(withId(R.id.btn_submit_code), tabIndex);

        checkMatcherIsDisplayed(withText(R.string.submit_recovery_code_preamble), 0);
        checkMatcherIsDisplayed(withText(R.string.recovery_code_header), 0);
        checkMatcherIsDisplayed(withText(R.string.recover_pwd), 0);
        checkMatcherIsDisplayed(withText(R.string.submit_code_pwd_repeat), tabIndex);
        checkMatcherIsDisplayed(withText(R.string.submit_code_btn), tabIndex);

        // Sign-in tab
        tabIndex = TabIndex.SIGN_IN.getIndex();

        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
        SystemClock.sleep(500);

        checkMatcherIsDisplayed(withId(R.id.submit_code_preamble), tabIndex);
        checkMatcherIsDisplayed(withId(R.id.submit_code_header_1), tabIndex);
        checkMatcherIsDisplayed(withId(R.id.edittext_submit_code), tabIndex);
        checkMatcherIsDisplayed(withId(R.id.submit_code_header_2), tabIndex);
        checkMatcherIsDisplayed(withId(R.id.submit_code_edittext_2), tabIndex);
        checkMatcherIsDisplayed(withId(R.id.submit_code_header_3), tabIndex);
        checkMatcherIsDisplayed(withId(R.id.submit_code_edittext_3), tabIndex);
        checkMatcherNotDisplayed(withId(R.id.submit_code_header_4), tabIndex);
        checkMatcherNotDisplayed(withId(R.id.submit_code_edittext_4), tabIndex);
        checkMatcherIsDisplayed(withId(R.id.submit_code_checkbox), tabIndex);
        checkMatcherIsDisplayed(withId(R.id.btn_submit_code), tabIndex);

        checkMatcherIsDisplayed(withText(R.string.submit_sign_in_code_preamble), 0);
        checkMatcherIsDisplayed(withText(R.string.sign_in_code_header), 0);
        checkMatcherIsDisplayed(withText(R.string.sign_in_email), 0);
        checkMatcherIsDisplayed(withText(R.string.sign_in_pwd), 0);
        checkMatcherIsDisplayed(withText(R.string.sign_in_btn), 0);
    }

    private void checkSubmitCodeButtonsEnabled(boolean isEnabled) {
        final Matcher<View> matcher = isEnabled ? isEnabled() : not(isEnabled());

        for (int i = 0; i < 3; i++) {
            onView(AndroidTestUtils.withIndex(withId(R.id.btn_submit_code), i)).check(matches(matcher));
        }
    }

    private void handleAllButtonsDisabledCheck() {
        AndroidTestUtils.getOnViewInteractionFromId(R.id.edittext_submit_code).perform(replaceText("some text"));
        AndroidTestUtils.getOnViewInteractionFromId(R.id.btn_submit_code).perform(click());

        checkSubmitCodeButtonsEnabled(false);

        // TODO: Test that clicking the up/back button does nothing.
//        try {
//            AndroidTestUtils.pressUp();     // Check that this does nothing.
//
//            // The views should be disabled and the loading dialog may be on screen.
//            fail("Expected an instance of NoMatchingViewException to be thrown.");
//        }
//        catch (NoMatchingViewException e) {
//            assertThat(e.getMessage(), containsString("No views in hierarchy found matching: "));
//            assertThat(e.getMessage(), containsString("value: Navigate up"));
//        }

        SystemClock.sleep(5500);
        onView(withId(android.R.id.button1)).perform(click());

        checkSubmitCodeButtonsEnabled(true);

        SystemClock.sleep(200);
        getOnViewInteractionFromId(R.id.submit_code_nestedscrollview).perform(swipeDown());
        SystemClock.sleep(200);
        AndroidTestUtils.pressUp();     // Check that this does something.
        SystemClock.sleep(1000);

        try {
            AndroidTestUtils.pressUp();

            fail("Expected an instance of RuntimeException to be thrown.");
        }
        catch (RuntimeException e) {
            final String s = "No activities found. Did you forget to launch the activity by calling getActivity() " +
                    "or startActivitySync or similar?";
            if (!s.equals(e.getMessage())) {
                throw e;
            }
        }
    }

    @Test
    public void allButtonsDisabledSubmitActivationCode() {
        final String password = "lalalalala";

        getOnViewInteractionFromString(R.string.submit_code_activation).perform(click());
        SystemClock.sleep(200);
        AndroidTestUtils.getOnViewInteractionFromId(R.id.submit_code_edittext_2).perform(replaceText(password));
        AndroidTestUtils.getOnViewInteractionFromId(R.id.submit_code_edittext_3).perform(replaceText(password));
        AndroidTestUtils.getOnViewInteractionFromId(R.id.submit_code_edittext_4).perform(replaceText("contact name"));
        getOnViewInteractionFromId(R.id.submit_code_nestedscrollview).perform(swipeUp());
        handleAllButtonsDisabledCheck();
    }

    @Test
    public void allButtonsDisabledSubmitRecoveryCode() {
        final String password = "lalalalala";

        getOnViewInteractionFromString(R.string.submit_code_recovery).perform(click());
        SystemClock.sleep(200);
        AndroidTestUtils.getOnViewInteractionFromId(R.id.submit_code_edittext_2).perform(replaceText(password));
        AndroidTestUtils.getOnViewInteractionFromId(R.id.submit_code_edittext_3).perform(replaceText(password));
        handleAllButtonsDisabledCheck();
    }

    @Test
    public void allButtonsDisabledSubmitSignInCode() {
        getOnViewInteractionFromString(R.string.submit_code_sign_in).perform(click());
        SystemClock.sleep(200);
        AndroidTestUtils.getOnViewInteractionFromId(R.id.submit_code_edittext_2)
                .perform(replaceText("test@email.com"));
        AndroidTestUtils.getOnViewInteractionFromId(R.id.submit_code_edittext_3)
                .perform(replaceText("lalalalala"));
        handleAllButtonsDisabledCheck();
    }
}
