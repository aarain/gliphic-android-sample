package gliphic.android.without_servers;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import gliphic.android.R;
import gliphic.android.display.welcome_screen.RegisterActivity;
import gliphic.android.utils.AndroidTestUtils;
import gliphic.android.with_http_server.single_fragment_activity.RegisterActivityTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class RegisterActivityTestWithoutServers {

    @Rule
    public ActivityScenarioRule<RegisterActivity> rule = new ActivityScenarioRule<>(RegisterActivity.class);

    @Test
    public void serverUnavailableRegister() throws Throwable {
        onView(withId(R.id.register_email)).perform(replaceText(RegisterActivityTest.newEmail));
        onView(withId(R.id.btn_register_account)).perform(click());

        AndroidTestUtils.waitForAction(AndroidTestUtils.WaitAction.SERVER_CONNECTION_FAILED_MSG);
    }
}
