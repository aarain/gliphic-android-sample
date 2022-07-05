package gliphic.android.with_http_server.functionality;

import android.os.SystemClock;

import gliphic.android.R;
import gliphic.android.adapters.AlertsAdapter;
import gliphic.android.display.welcome_screen.SignInActivity;
import gliphic.android.operation.Alerts;
import gliphic.android.operation.storage_handlers.SharedPreferencesHandler;
import gliphic.android.utils.AndroidTestUtils;
import gliphic.android.utils.MainActivityBaseSetup;
import gliphic.android.utils.matchers.ToastMatcher;
import gliphic.android.with_http_server.LoadMoreObjectsTestInvalidRefreshTokenRule;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.swipeRight;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class LoadMoreAlertsTestInvalidTokensTest {

    @Rule
    public LoadMoreObjectsTestInvalidRefreshTokenRule<SignInActivity> rule =
            new LoadMoreObjectsTestInvalidRefreshTokenRule<>(SignInActivity.class);

    @After
    public void nullGlobalStatics() {
        AndroidTestUtils.clearStaticLists();
    }

    @AfterClass
    public static void resetRefreshToken() {
        try {
            final String refreshToken = MainActivityBaseSetup.getValidRefreshToken(0);

            SharedPreferencesHandler.setRefreshToken(AndroidTestUtils.getApplicationContext(), refreshToken);
        }
        catch (Throwable e) {
            fail(e.getMessage());
        }
    }

    // Valid operations.

    @Test
    public void loadAllAlertsWithExpiredAccessTokenAndInvalidRefreshToken() throws Exception {
        final int numTotalLoadedGroupShares = 1;

        SystemClock.sleep(10000);

        assertThat(Alerts.getGroupShares().size(), is(numTotalLoadedGroupShares));

        onView(withId(R.id.viewpager_activity_base)).perform(swipeRight());
        SystemClock.sleep(500);     // Give the swipe time to change tab focus and find R.id.*

        // Load more alerts.
        onView(withId(R.id.textview_load_more_alerts)).perform(click());
        // Any (failure) toast message should still be on the screen after 1500 milliseconds.
        SystemClock.sleep(1500);

        String s = AlertsAdapter.GENERIC_LOAD_ALERTS_FAILED_MSG;
        onView(withText(s)).inRoot(new ToastMatcher()).check(matches(isDisplayed()));

        // Check that no group-shares are loaded.
        assertThat(Alerts.getGroupShares().size(), is(numTotalLoadedGroupShares));
    }
}
