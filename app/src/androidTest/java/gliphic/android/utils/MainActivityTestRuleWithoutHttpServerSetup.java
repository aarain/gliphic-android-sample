package gliphic.android.utils;

import android.os.SystemClock;

import gliphic.android.display.welcome_screen.SignInActivity;
import gliphic.android.operation.storage_handlers.SharedPreferencesHandler;

import org.junit.AfterClass;
import org.junit.Rule;

/**
 * Extend this class for Android tests which require a standard setup of the main activity.
 *
 * Typically, any test class which runs an activity which is NOT the sign-in activity likely requires this class to be
 * extended. The main indicator of which activity a test class runs is a @Rule annotation in the test class e.g.:
 *
 * * public ActivityScenarioRule<MainActivity> rule = new ActivityScenarioRule<>(MainActivity.class);
 * or
 * * public ActivityScenarioRule<SignInActivity> rule = new ActivityScenarioRule<>(SignInActivity.class);
 *
 * Note that test classes which extend this class do not need to specify this annotation as it is specified in this
 * class.
 */
abstract public class MainActivityTestRuleWithoutHttpServerSetup extends MainActivityBaseSetup {

    @Rule
    public BaseActivityTestWithoutHttpServerRule<SignInActivity> rule =
            new BaseActivityTestWithoutHttpServerRule<>(SignInActivity.class);

    @AfterClass
    public static void mainActivityTeardown() {
        SharedPreferencesHandler.removeAllContactData(AndroidTestUtils.getApplicationContext());

        // Ensure that the above is processed before proceeding to the next test class.
        SystemClock.sleep(200);
    }
}
