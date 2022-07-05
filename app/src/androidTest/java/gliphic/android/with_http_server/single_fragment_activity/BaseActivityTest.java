package gliphic.android.with_http_server.single_fragment_activity;

import android.content.Context;
import android.os.SystemClock;

import gliphic.android.R;
import gliphic.android.display.welcome_screen.SignInActivity;
import gliphic.android.operation.storage_handlers.ForcedDialogs;
import gliphic.android.operation.storage_handlers.ResponseCodeAndMessage;
import gliphic.android.operation.storage_handlers.SharedPreferencesHandler;
import gliphic.android.utils.AndroidTestUtils;
import gliphic.android.utils.BaseActivityTestRule;
import gliphic.android.with_http_server.functionality.SignInAndOutTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class BaseActivityTest {

    @Rule
    public BaseActivityTestRule<SignInActivity> rule = new BaseActivityTestRule<>(SignInActivity.class);

    @Before
    public void beforeTest() throws Throwable {
        try {
            SignInAndOutTest.signIn();
        }
        catch (Throwable e) {
            SignInAndOutTest.signOut();
            SignInAndOutTest.signIn();
        }
    }

    private void openContactProfile() {
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText(R.string.action_bar_contact_profile)).perform(click());
        SystemClock.sleep(500);
    }

    @Test
    public void onStartDisplaysForcedSignOutDialog() throws Exception {
        try {
            // Modify shared preferences so that the appropriate dialog message will be displayed the next time the
            // BaseActivity.onStart() method is called.

            ForcedDialogs forcedDialogs = new ForcedDialogs();
            forcedDialogs.setForcedSignOutAction(ForcedDialogs.ForcedSignOutAction.CONNECTION_CLOSED_CONFLICT);

            SharedPreferencesHandler.setForcedDialogs(AndroidTestUtils.getApplicationContext(), forcedDialogs);

            // Assert that the appropriate dialog message is displayed.

            openContactProfile();
            onView(withText("Connection terminated")).check(matches(isDisplayed()));

            // Assert that confirming the dialog message signs-out the contact.

            onView(withId(android.R.id.button1)).perform(click());
            SystemClock.sleep(500);
            onView(withText("SIGN IN")).check(matches(isDisplayed()));
        }
        finally {
            final Context context = AndroidTestUtils.getApplicationContext();

            try {
                SharedPreferencesHandler.getForcedDialogs(context);

                // If the test fails it should take this code path since the contact has not signed out.
                SharedPreferencesHandler.setForcedDialogs(context, new ForcedDialogs());

                fail("Expected an exception to be thrown.");
            }
            catch (Throwable e) {
                // If the test passes it should take this code path since the contact has signed out.
            }
        }
    }

    @Test
    public void onStartDisplaysInternalErrorDialog() {
        try {
            final String dialogMessage = "some message.";

            // Modify shared preferences so that the appropriate dialog message will be displayed the next time the
            // BaseActivity.onStart() method is called.

            ForcedDialogs forcedDialogs = new ForcedDialogs();
            forcedDialogs.setInternalErrorMessage(dialogMessage);

            try {
                SharedPreferencesHandler.setForcedDialogs(AndroidTestUtils.getApplicationContext(), forcedDialogs);

                fail("Expected an exception to be thrown.");
            }
            catch (Throwable e) {
                // If the test passes it should take this code path since the contact has signed out.
            }

            // Assert that the appropriate dialog message is displayed.

            openContactProfile();
            onView(withText("Internal error")).check(matches(isDisplayed()));
            onView(withText(dialogMessage)).check(matches(isDisplayed()));

            // Assert that confirming the dialog message finishes the current activity (the finally block will be
            // unable to sign-out the contact unless the main activity is on display).

            onView(withId(android.R.id.button1)).perform(click());
            SystemClock.sleep(500);
        }
        finally {
            SignInAndOutTest.signOut();
        }
    }

    @Test
    public void onStartDisplaysResponseCodeDialog() {
        try {
            final int    errorCode     = 666;
            final String dialogMessage = "some message.";

            // Modify shared preferences so that the appropriate dialog message will be displayed the next time the
            // BaseActivity.onStart() method is called.

            ForcedDialogs forcedDialogs = new ForcedDialogs();
            forcedDialogs.setResponseCodeAndMessage(new ResponseCodeAndMessage(errorCode, dialogMessage));

            try {
                SharedPreferencesHandler.setForcedDialogs(AndroidTestUtils.getApplicationContext(), forcedDialogs);

                fail("Expected an exception to be thrown.");
            }
            catch (Throwable e) {
                // If the test passes it should take this code path since the contact has signed out.
            }

            // Assert that the appropriate dialog message is displayed.

            openContactProfile();
            onView(withText(containsString(Integer.toString(errorCode)))).check(matches(isDisplayed()));
            onView(withText(dialogMessage)).check(matches(isDisplayed()));

            // Assert that confirming the dialog message finishes the current activity (the finally block will be
            // unable to sign-out the contact unless the main activity is on display).

            onView(withId(android.R.id.button1)).perform(click());
            SystemClock.sleep(500);
        }
        finally {
            SignInAndOutTest.signOut();
        }
    }
}
