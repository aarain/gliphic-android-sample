package gliphic.android.utils;

import gliphic.android.display.welcome_screen.SignInActivity;

import org.junit.Rule;

/**
 * Extend this class for Android tests which require a setup of the main activity using calls to
 * {@link android.content.Intent}. Also see MainActivityTestRuleSetup.
 */

abstract public class MainIntentTestRuleWithoutHttpServerSetup extends MainActivityBaseSetup {
    @Rule
    public BaseIntentsTestWithoutHttpServerRule<SignInActivity> rule =
            new BaseIntentsTestWithoutHttpServerRule<>(SignInActivity.class);
}
