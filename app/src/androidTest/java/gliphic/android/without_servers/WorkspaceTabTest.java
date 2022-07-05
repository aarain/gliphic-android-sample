package gliphic.android.without_servers;

import android.os.SystemClock;

import gliphic.android.R;
import gliphic.android.operation.Group;
import gliphic.android.exceptions.GroupException;
import gliphic.android.exceptions.NullStaticVariableException;
import gliphic.android.utils.AndroidTestUtils;
import gliphic.android.utils.MainActivityTestRuleWithoutHttpServerSetup;
import gliphic.android.with_http_server.single_fragment_activity.WorkspaceTabWithServerTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import libraries.Vars;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

/**
 * Test encrypt and decrypt functionality which requires the server to NOT be running.
 */
@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class WorkspaceTabTest extends MainActivityTestRuleWithoutHttpServerSetup {
    private static final String expectedGroupName = Vars.DEFAULT_GROUP_NAME;

    /* Views visibility */

    @Before
    public void waitForDialog() throws Throwable {
        AndroidTestUtils.waitForAction(AndroidTestUtils.WaitAction.SERVER_CONNECTION_FAILED_MSG);
    }

    @Test
    public void selectedGroupAdapterNotClearedOnTabRefocus() throws Exception {
        // First check that the group is visible.
        SystemClock.sleep(200);
        onView(ViewMatchers.withId(R.id.recyclerview_main_tab_selected_group))
                .check(matches(hasDescendant(withText(expectedGroupName))));

        Group selectedGroup = Group.getSelectedGroup();
        try {
            openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
            onView(withText(R.string.action_bar_group_create)).perform(click());

            SystemClock.sleep(200);
            Group.setNullSelectedGroup();
            pressBack();

            SystemClock.sleep(200);
            onView(ViewMatchers.withId(R.id.recyclerview_main_tab_selected_group))
                    .check(matches(hasDescendant(withText(expectedGroupName))));
        }
        finally {
            selectedGroup.selectGroup();
        }
    }

    /* Invalid encrypt. */

    @Test
    public void emptyTextEncrypt() {
        onView(withId(R.id.edittext_encrypt)).perform(clearText(), closeSoftKeyboard());
        onView(withId(R.id.btn_encrypt)).perform(click());

        onView(withText("Enter a non-empty message to encrypt.")).check(matches(isDisplayed()));
    }

    @Test
    public void selectedGroupIsNull() throws GroupException, NullStaticVariableException {
        Group selectedGroup = Group.getSelectedGroup();
        try {
            Group.setNullSelectedGroup();

            onView(withId(R.id.edittext_encrypt)).perform(replaceText(WorkspaceTabWithServerTest.PLAIN_TEXT));
            onView(withId(R.id.btn_encrypt)).perform(click());

            SystemClock.sleep(5000);
            String s = "This error occurs if your device is not connected to the internet, " +
                    "or the remote server has become temporarily unavailable.";
            onView(withText(s)).check(matches(isDisplayed()));
            onView(withId(android.R.id.button1)).perform(click());
        }
        finally {
            selectedGroup.selectGroup();
        }
    }

    // Server unavailable

    @Test
    public void serverUnavailableTextEncrypt() {
        onView(ViewMatchers.withId(R.id.edittext_encrypt))
                .perform(replaceText(WorkspaceTabWithServerTest.PLAIN_TEXT));
        onView(withId(R.id.btn_encrypt)).perform(click());

        SystemClock.sleep(2000);
        onView(withText("Server connection failed")).check(matches(isDisplayed()));
    }

    @Test
    public void serverUnavailableTextDecrypt() {
        onView(ViewMatchers.withId(R.id.edittext_decrypt))
                .perform(replaceText(WorkspaceTabWithServerTest.PUBLISHED_TEXT_GROUP_1));
        onView(withId(R.id.btn_decrypt)).perform(click());

        SystemClock.sleep(2000);
        onView(withText("Server connection failed")).check(matches(isDisplayed()));
    }
}
