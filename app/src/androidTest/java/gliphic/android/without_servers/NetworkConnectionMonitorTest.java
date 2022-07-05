package gliphic.android.without_servers;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import gliphic.android.display.welcome_screen.SignInActivity;
import gliphic.android.operation.server_interaction.http_server.NetworkConnectionMonitor;
import gliphic.android.utils.AndroidTestUtils;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class NetworkConnectionMonitorTest {

    @Rule
    public ActivityScenarioRule<SignInActivity> rule = new ActivityScenarioRule<>(SignInActivity.class);

    @Test
    public void virtualDeviceHasNetworkAvailable() {
        assertThat(NetworkConnectionMonitor.isNetworkAvailable(AndroidTestUtils.getApplicationContext()), is(true));
    }

    // TODO: Write a test which checks that NetworkConnectionMonitor.isNetworkAvailable() is false.
}
