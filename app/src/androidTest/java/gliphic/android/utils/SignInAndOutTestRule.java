package gliphic.android.utils;

import android.os.SystemClock;

import gliphic.android.display.abstract_views.BaseActivity;
import gliphic.android.operation.storage_handlers.AndroidKeyStoreHandler;
import gliphic.android.operation.storage_handlers.SharedPreferencesHandler;

import static org.junit.Assert.fail;

public class SignInAndOutTestRule<A extends BaseActivity> extends BaseActivityTestRule<A> {

    public SignInAndOutTestRule(Class<A> activityClass) {
        super(activityClass);
    }

    @Override
    protected void before() {
        setScenario(getScenarioSupplier());

        try {
            String accessToken = MainActivityBaseSetup.getValidAccessToken(0);

            getScenario().onActivity(activity -> {
                try {
                    SharedPreferencesHandler.removeAllContactData(activity);
                    AndroidKeyStoreHandler.setAppSymmetricKey(activity, true);
                    SharedPreferencesHandler.setAccessToken(activity, accessToken);
                }
                catch (Throwable e) {
                    fail(e.getMessage());
                }
            });
        }
        catch (Throwable e) {
            fail(e.getMessage());
        }

        // Give time for the Android keystore and/or SharedPreferences to be modified.
        SystemClock.sleep(200);
    }
}
