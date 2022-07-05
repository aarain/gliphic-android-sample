package gliphic.android.without_servers.android_key_store_handler_tests;

import android.os.SystemClock;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.security.KeyStore;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import gliphic.android.display.welcome_screen.SubmitCodeActivity;
import gliphic.android.operation.storage_handlers.AndroidKeyStoreHandler;
import gliphic.android.operation.storage_handlers.SharedPreferencesHandler;
import gliphic.android.utils.AndroidTestUtils;
import libraries.Vars;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class AndroidKeyStoreTests {
    private static final String APP_KEY_STORE_KEY = String.format("%sSymmetricKey", Vars.APP_NAME);

    /*
     * If the ActivityScenarioRule was set to the MainActivity some of the tests would throw a
     *     "Error : BinderProxy@45d459c0 is not valid; is your activity running?"
     * because the MainActivity automatically shows an AlertDialog after X milliseconds.
     *
     * SubmitCodeActivity was chosen because it does not run any (additional) background threads or have delayed
     * processes e.g. calling any AndroidKeyStoreHandler methods, but other activities could also work.
     */
    @Rule
    public ActivityScenarioRule<SubmitCodeActivity> rule = new ActivityScenarioRule<>(SubmitCodeActivity.class);

    @Before
    public void cleanup() throws Throwable {
        // Initialise the keystore.
        KeyStore keyStore = KeyStore.getInstance(Vars.ANDROID_KEY_STORE);
        keyStore.load(null);

        // Remove the key from the keystore to ensure that the old key is not used.
        keyStore.deleteEntry(APP_KEY_STORE_KEY);

        assertThat(keyStore.containsAlias(APP_KEY_STORE_KEY), is(false));
    }

    @Test
    public void validSetAppSymmetricKey() throws Exception {
        // Initialise the keystore.
        KeyStore keyStore = KeyStore.getInstance(Vars.ANDROID_KEY_STORE);
        keyStore.load(null);

        // Remove the key from the keystore to ensure that the old key is not used.
        keyStore.deleteEntry(APP_KEY_STORE_KEY);
        assertThat(keyStore.containsAlias(APP_KEY_STORE_KEY), is(false));

        AndroidKeyStoreHandler.setAppSymmetricKey(AndroidTestUtils.getApplicationContext(), true);

        assertThat(keyStore.containsAlias(APP_KEY_STORE_KEY), is(true));
    }

    /*
     * The AndroidKeyStoreHandler.isUserSignedIn() method is tested in SignInAndOutTest since the positive test case
     * requires the server to sign in.
     */

    @Test
    public void removeAllContactDataThrowsNoExceptionsNullActivity() {
        SharedPreferencesHandler.removeAllContactData(null);
        SystemClock.sleep(200);     // Allow time for the asynchronous SharedPreferences operations.
    }

    @Test
    public void removeAllContactDataThrowsNoExceptionsWithActivity() {
        rule.getScenario().onActivity(SharedPreferencesHandler::removeAllContactData);

        SystemClock.sleep(200);     // Allow time for the asynchronous SharedPreferences operations.
    }
}
