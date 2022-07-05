package gliphic.android.utils;

import gliphic.android.display.welcome_screen.SignInActivity;

import org.junit.Rule;

/**
 * Extend this class for Android tests which require a setup of the main activity using calls to
 * {@link android.content.Intent}. Also see MainActivityTestRuleSetup.
 */
abstract public class MainIntentTestRuleSetup extends MainActivityBaseSetup {
    @Rule
    public BaseIntentsTestRule<SignInActivity> rule = new BaseIntentsTestRule<>(SignInActivity.class);
}
