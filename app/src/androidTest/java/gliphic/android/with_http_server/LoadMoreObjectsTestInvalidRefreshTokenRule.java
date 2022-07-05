package gliphic.android.with_http_server;

import android.content.Context;
import android.os.SystemClock;

import gliphic.android.display.abstract_views.BaseActivity;
import gliphic.android.operation.storage_handlers.SharedPreferencesHandler;
import gliphic.android.utils.AndroidTestUtils;
import gliphic.android.utils.BaseActivityTestRule;
import gliphic.android.utils.MainActivityBaseSetup;
import gliphic.android.with_http_server.functionality.SignInAndOutTest;

import static org.junit.Assert.fail;

/**
 *
 */
public class LoadMoreObjectsTestInvalidRefreshTokenRule<A extends BaseActivity> extends BaseActivityTestRule<A> {

    public LoadMoreObjectsTestInvalidRefreshTokenRule(Class<A> activityClass) {
        super(activityClass);
    }

    @Override
    public void before() {
        setScenario(getScenarioSupplier());

        getScenario().onActivity(SharedPreferencesHandler::removeAllContactData);

        try {
            SignInAndOutTest.signIn();

            // Setting this expiry should imply an expired access token (even if the access token is valid).
            final Context context = AndroidTestUtils.getApplicationContext();
            SharedPreferencesHandler.setAccessTokenExpiry(context, System.currentTimeMillis());
            SharedPreferencesHandler.setRefreshToken(context, "invalid refresh token");
        }
        catch (Throwable e) {
            fail(e.getMessage());
        }

        SystemClock.sleep(200);     // Allow time for the refresh token to be set.
    }

    @Override
    public void after() {
        super.after();

        try {
            MainActivityBaseSetup.setTokenDataContact0(true);
        }
        catch (Throwable e) {
            fail(e.getMessage());
        }

        SystemClock.sleep(200);     // Allow time for the refresh token to be set.
    }
}
