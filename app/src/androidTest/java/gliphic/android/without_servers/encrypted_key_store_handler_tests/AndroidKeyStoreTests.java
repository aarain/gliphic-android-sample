package gliphic.android.without_servers.encrypted_key_store_handler_tests;

import android.os.SystemClock;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.security.KeyStore;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import gliphic.android.display.welcome_screen.SubmitCodeActivity;
import gliphic.android.operation.storage_handlers.SharedPreferencesHandler;
import libraries.Vars;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@Ignore("Remove this annotation after refactoring the SharedPreferencesHandler class to use " +
        "EncryptedSharedPreferences (and a master key) instead of manual encryption of SharedPreferences data.")
public class AndroidKeyStoreTests {
    private static final String APP_KEY_STORE_KEY = String.format("%sSymmetricKey", Vars.APP_NAME);

    /*
     * If the ActivityScenarioRule was set to the MainActivity some of the tests would throw a
     *     "Error : BinderProxy@45d459c0 is not valid; is your activity running?"
     * because the MainActivity automatically shows an AlertDialog after X milliseconds.
     *
     * SubmitCodeActivity was chosen because it does not run any (additional) background threads or have delayed
     * processes e.g. calling any SharedPreferencesHandler methods, but other activities could also work.
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

    // TODO: Implement functionality and write test for setting the app master key.
//    @Test
//    public void validSetAppSymmetricKey() throws Exception {
//        // Initialise the keystore.
//        KeyStore keyStore = KeyStore.getInstance(Vars.ANDROID_KEY_STORE);
//        keyStore.load(null);
//
//        // Remove the key from the keystore to ensure that the old key is not used.
//        keyStore.deleteEntry(APP_KEY_STORE_KEY);
//        assertThat(keyStore.containsAlias(APP_KEY_STORE_KEY), is(false));
//
//        AndroidKeyStoreHandler.setAppSymmetricKey(AndroidTestUtils.getApplicationContext(), true);
//
//        assertThat(keyStore.containsAlias(APP_KEY_STORE_KEY), is(true));
//    }

    /*
     * The SharedPreferencesHandler.isUserSignedIn() method is tested in SignInAndOutTest since the positive test case
     * requires the server to sign in.
     */

    @Test
    public void removeAllContactDataThrowsNullPointerExceptionNullActivity() {
        try {
            SharedPreferencesHandler.removeAllContactData(null);
            fail("NullPointerException expected.");
        }
        catch (NullPointerException e) {
            // None of the SharedPreferences data is removed, only the data held in static memory is removed.
        }
    }

    @Test
    public void removeAllContactDataThrowsNoExceptionsWithActivity() {
        rule.getScenario().onActivity(SharedPreferencesHandler::removeAllContactData);

        SystemClock.sleep(200);     // Allow time for the asynchronous SharedPreferences operations.
    }
}
