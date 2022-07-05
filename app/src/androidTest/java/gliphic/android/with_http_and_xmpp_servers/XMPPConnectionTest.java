package gliphic.android.with_http_and_xmpp_servers;

import android.content.Intent;
import android.os.SystemClock;

import gliphic.android.R;
import gliphic.android.operation.server_interaction.xmpp_server.Connection;
import gliphic.android.operation.server_interaction.xmpp_server.ConnectionService;
import gliphic.android.utils.AndroidTestUtils;
import gliphic.android.utils.MainActivityBaseSetup;
import gliphic.android.utils.MainActivityTestRuleSetup;
import gliphic.android.with_http_server.functionality.SignInAndOutTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import libraries.Vars;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for XMPP connectivity i.e. testing how the application handles various connection states.
 */
@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class XMPPConnectionTest extends MainActivityTestRuleSetup {

    @Test
    public void connectedAndAuthenticated() throws Throwable {
        AndroidTestUtils.waitForAction(AndroidTestUtils.WaitAction.XMPP_AUTH);
    }

    @Test
    public void signOutTerminatesConnection() throws Throwable {
        AndroidTestUtils.waitForAction(AndroidTestUtils.WaitAction.XMPP_AUTH);

        SignInAndOutTest.signOut();

        assertThat(Connection.connectionState, is(Connection.ConnectionState.DISCONNECTED));

        // This test clean-up is required after a signOut() operation.
        MainActivityBaseSetup.mainActivitySetupBeforeActivityLaunched();
    }

    @Test
    public void connectionClosedOnErrorOther() throws Throwable {
        AndroidTestUtils.waitForAction(AndroidTestUtils.WaitAction.XMPP_AUTH);

        LocalBroadcastManager
                .getInstance(AndroidTestUtils.getApplicationContext())
                .sendBroadcast(new Intent(ConnectionService.ACTION_CONN_ERR_OTHER));

        SystemClock.sleep(1000);
        // This expected message string is copied from BaseMainActivity.java.
        final String msg = "To continue to use %s you will need to sign in again on this device.";
        onView(withText(String.format(msg, Vars.APP_NAME))).check(matches(isDisplayed()));

        assertThat(Connection.connectionState, is(Connection.ConnectionState.DISCONNECTED));

        // This test clean-up is required after a signOut() operation.
        MainActivityBaseSetup.mainActivitySetupBeforeActivityLaunched();
    }

    @Test
    public void overriddenConnectionDialogFromMainActivity() throws Throwable {
        handleOverriddenConnectionDialogTest();
    }

    @Test
    public void overriddenConnectionDialogFromAnotherActivity() throws Throwable {
        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
        SystemClock.sleep(500);
        onView(withId(R.id.recyclerview_main_tab_groups))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

        handleOverriddenConnectionDialogTest();
    }

    @Test
    public void overriddenConnectionDialogWithAnotherAlertDialogOnScreen() throws Throwable {
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText(R.string.action_bar_sign_out)).perform(click());

        handleOverriddenConnectionDialogTest();
    }

    private void handleOverriddenConnectionDialogTest() throws Throwable {
        AndroidTestUtils.waitForAction(AndroidTestUtils.WaitAction.XMPP_AUTH);

        LocalBroadcastManager
                .getInstance(AndroidTestUtils.getApplicationContext())
                .sendBroadcast(new Intent(ConnectionService.ACTION_CONN_ERR_CONFLICT));

        SystemClock.sleep(1000);
        // This expected message string is copied from BaseMainActivity.java.
        final String msg = "Your account has signed in on another device. To continue to use %s you will " +
                "need to sign in again on this device.";
        onView(withText(String.format(msg, Vars.APP_NAME))).check(matches(isDisplayed()));

        assertThat(Connection.connectionState, is(Connection.ConnectionState.DISCONNECTED));

        // This test clean-up is required after a signOut() operation.
        MainActivityBaseSetup.mainActivitySetupBeforeActivityLaunched();
    }
}
