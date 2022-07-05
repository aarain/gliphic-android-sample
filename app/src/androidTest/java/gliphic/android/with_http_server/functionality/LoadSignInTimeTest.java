package gliphic.android.with_http_server.functionality;

import android.content.Context;
import android.os.SystemClock;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import gliphic.android.R;
import gliphic.android.display.welcome_screen.SignInActivity;
import gliphic.android.operation.storage_handlers.SharedPreferencesHandler;
import gliphic.android.utils.AndroidTestUtils;
import gliphic.android.utils.BaseActivityTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class LoadSignInTimeTest {
    private long initialStoredSignInTime;

    private long accessTokenExpiry;
    private String refreshToken;

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

        final Context context = AndroidTestUtils.getApplicationContext();
        initialStoredSignInTime = SharedPreferencesHandler.getLastSignInTime(context);
        SharedPreferencesHandler.setLastSignInTime(context, initialStoredSignInTime - 1000);
    }

    @After
    public void afterTest() throws Exception {
        SharedPreferencesHandler.setLastSignInTime(AndroidTestUtils.getApplicationContext(), initialStoredSignInTime);
    }

    private void openContactProfile() {
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText(R.string.action_bar_contact_profile)).perform(click());
    }

    private void handleLoadSignInTime() {
        openContactProfile();
        SystemClock.sleep(500);
        onView(withText("Connection terminated")).check(matches(isDisplayed()));

        onView(withId(android.R.id.button1)).perform(click());
        SystemClock.sleep(500);
        onView(withText("SIGN IN")).check(matches(isDisplayed()));
    }

    @Test
    public void validLoadSignInTime() {
        handleLoadSignInTime();
    }

    @Test
    public void validLoadSignInTimeExpiredAccessTokenAndValidRefreshToken() throws Exception {
        SharedPreferencesHandler.setAccessTokenExpiry(
                AndroidTestUtils.getApplicationContext(),
                System.currentTimeMillis()
        );

        handleLoadSignInTime();
    }

    @Test
    public void invalidLoadSignInTimeExpiredAccessTokenAndInvalidRefreshToken() {
        try {
            final Context context = AndroidTestUtils.getApplicationContext();

            accessTokenExpiry = SharedPreferencesHandler.getAccessTokenExpiry(context);
            refreshToken      = SharedPreferencesHandler.getRefreshToken(context);

            SharedPreferencesHandler.setAccessTokenExpiry(context, System.currentTimeMillis());
            SharedPreferencesHandler.setRefreshToken(context, "invalid refresh token");

            // These methods must be called from outside the UI thread.

            openContactProfile();
            SystemClock.sleep(10000);

            // The ability for the user to choose to sign out implies that no forced sign-out message is
            // displayed.
            pressBack();
            SystemClock.sleep(200);
            SignInAndOutTest.signOut();

            testCleanUp(AndroidTestUtils.getApplicationContext());
        }
        catch (Throwable e) {
            try {
                testCleanUp(AndroidTestUtils.getApplicationContext());
            }
            catch (Throwable f) {
                // Test clean up failed.
            }

            fail(e.getMessage());
        }
    }

    private void testCleanUp(Context context) throws Exception {
        SharedPreferencesHandler.setAccessTokenExpiry(context, accessTokenExpiry);
        SharedPreferencesHandler.setRefreshToken(context, refreshToken);
    }
}
