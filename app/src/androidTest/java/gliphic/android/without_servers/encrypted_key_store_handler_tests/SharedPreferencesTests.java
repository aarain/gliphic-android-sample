package gliphic.android.without_servers.encrypted_key_store_handler_tests;

import android.content.Context;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import gliphic.android.display.welcome_screen.SubmitCodeActivity;
import gliphic.android.exceptions.NoStoredObjectException;
import gliphic.android.operation.storage_handlers.IsStoredAndDataObject;
import gliphic.android.operation.storage_handlers.SharedPreferencesHandler;
import gliphic.android.utils.AndroidTestUtils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@Ignore("Remove this annotation after refactoring the SharedPreferencesHandler class to use " +
        "EncryptedSharedPreferences (and a master key) instead of manual encryption of SharedPreferences data.")
public class SharedPreferencesTests {
    private static final String EXPECTED_NULL_POINTER_EXCEPTION_MESSAGE =
            "Attempt to invoke virtual method 'android.content.Context " +
            "android.content.Context.getApplicationContext()' on a null object reference";

    /* If the ActivityScenarioRule was set to the MainActivity some of the tests would throw a
     *     "Error : BinderProxy@45d459c0 is not valid; is your activity running?"
     * because the MainActivity automatically shows an AlertDialog after X milliseconds.
     */
    @Rule
    public ActivityScenarioRule<SubmitCodeActivity> rule = new ActivityScenarioRule<>(SubmitCodeActivity.class);

    @Test
    public void isUserSignedInNullActivity() {
        try {
            SharedPreferencesHandler.isUserSignedIn(null);

            String s = "Expected an instance of %s to be thrown.";
            fail(String.format(s, NullPointerException.class.getSimpleName()));
        }
        catch (NullPointerException e) {
            assertThat(e.getMessage(), is(EXPECTED_NULL_POINTER_EXCEPTION_MESSAGE));
        }
    }

    /* Test generating a new device key/code if none exists when calling the get method. */

    private void setOriginalDeviceKey(byte[] originalDeviceKey) throws Exception {
        final Context context = AndroidTestUtils.getApplicationContext();

        if (originalDeviceKey != null) {
            SharedPreferencesHandler.setDeviceKey(context, originalDeviceKey);
        }
        IsStoredAndDataObject isStoredAndDataObject = SharedPreferencesHandler.getDeviceKey(
                context,
                true
        );
        assertThat(isStoredAndDataObject.wasStored(), is(true));
        assertThat(isStoredAndDataObject.getData(), is(originalDeviceKey));
    }

    private void setOriginalDeviceCode(byte[] originalDeviceCode) throws Exception {
        final Context context = AndroidTestUtils.getApplicationContext();

        if (originalDeviceCode != null) {
            SharedPreferencesHandler.setDeviceCode(context, originalDeviceCode);
        }
        IsStoredAndDataObject isStoredAndDataObject = SharedPreferencesHandler.getDeviceCode(
                context,
                true
        );
        assertThat(isStoredAndDataObject.wasStored(), is(true));
        assertThat(isStoredAndDataObject.getData(), is(originalDeviceCode));
    }

    @Test
    public void throwExceptionIfDeviceKeyNotExists() throws Throwable {
        final Context context = AndroidTestUtils.getApplicationContext();

        byte[] originalDeviceKey = null;
        try {
            originalDeviceKey = AndroidTestUtils.getDeviceKey();
            SharedPreferencesHandler.removeDeviceKey(context);

            // Test that a new device key is generated in the get method if one does not already exist.
            try {
                SharedPreferencesHandler.getDeviceKey(context, false);

                String s = "Expected an instance of %s to be thrown.";
                fail(String.format(s, NoStoredObjectException.class.getSimpleName()));
            }
            catch (NoStoredObjectException e) {
                final String s = "Cannot get non-existent SharedPreferences object with alias: DeviceKey";
                assertThat(e.getMessage(), is(s));
            }
        }
        finally {
            setOriginalDeviceKey(originalDeviceKey);
        }
    }

    @Test
    public void throwExceptionIfDeviceCodeNotExists() throws Throwable {
        final Context context = AndroidTestUtils.getApplicationContext();

        byte[] originalDeviceCode = null;
        try {
            originalDeviceCode = AndroidTestUtils.getDeviceCode();
            SharedPreferencesHandler.removeDeviceCode(context);

            // Test that a new device code is generated in the get method if one does not already exist.
            try {
                SharedPreferencesHandler.getDeviceCode(context, false);

                String s = "Expected an instance of %s to be thrown.";
                fail(String.format(s, NoStoredObjectException.class.getSimpleName()));
            }
            catch (NoStoredObjectException e) {
                final String s = "Cannot get non-existent SharedPreferences object with alias: DeviceCode";
                assertThat(e.getMessage(), is(s));
            }
        }
        finally {
            setOriginalDeviceCode(originalDeviceCode);
        }
    }

    @Test
    public void generateNewDeviceKeyIfNoneExists() throws Throwable {
        final Context context = AndroidTestUtils.getApplicationContext();

        byte[] originalDeviceKey = null;
        try {
            originalDeviceKey = AndroidTestUtils.getDeviceKey();
            SharedPreferencesHandler.removeDeviceKey(context);

            // Test that a new device key is generated in the get method if one does not already exist.
            IsStoredAndDataObject isStoredAndDataObject = SharedPreferencesHandler.getDeviceKey(
                    context,
                    true
            );
            assertThat(isStoredAndDataObject.wasStored(), is(false));
            assertThat(isStoredAndDataObject.getData(), not(originalDeviceKey));
        }
        finally {
            setOriginalDeviceKey(originalDeviceKey);
        }
    }

    @Test
    public void generateNewDeviceCodeIfNoneExists() throws Throwable {
        final Context context = AndroidTestUtils.getApplicationContext();

        byte[] originalDeviceCode = null;
        try {
            originalDeviceCode = AndroidTestUtils.getDeviceCode();
            SharedPreferencesHandler.removeDeviceCode(context);

            // Test that a new device code is generated in the get method if one does not already exist.
            IsStoredAndDataObject isStoredAndDataObject = SharedPreferencesHandler.getDeviceCode(
                    context,
                    true
            );
            assertThat(isStoredAndDataObject.wasStored(), is(false));
            assertThat(isStoredAndDataObject.getData(), not(originalDeviceCode));
        }
        finally {
            setOriginalDeviceCode(originalDeviceCode);
        }
    }

    /* Remove device key */

    @Test
    public void validRemoveDeviceKey() throws Throwable {
        final Context context = AndroidTestUtils.getApplicationContext();

        byte[] originalDeviceKey = null;
        try {
            originalDeviceKey = AndroidTestUtils.getDeviceKey();
            SharedPreferencesHandler.setDeviceKey(context, AndroidTestUtils.getDeviceKey());

            // Test removing the data when it already exists.
            SharedPreferencesHandler.removeDeviceKey(context);

            // Test removing the data when none exists.
            SharedPreferencesHandler.removeDeviceKey(context);
        }
        finally {
            if (originalDeviceKey != null) {
                SharedPreferencesHandler.setDeviceKey(context, originalDeviceKey);
            }
        }
    }

    @Test
    public void invalidRemoveDeviceKey() {
        try {
            final Context context = AndroidTestUtils.getApplicationContext();

            SharedPreferencesHandler.setDeviceKey(context, AndroidTestUtils.getDeviceKey());

            // Test removing the data when it already exists.
            SharedPreferencesHandler.removeDeviceKey(null);

            String s = "Expected an instance of %s to be thrown.";
            fail(String.format(s, NullPointerException.class.getSimpleName()));
        }
        catch (Throwable e) {
            assertThat(e.getClass().getCanonicalName(), is(NullPointerException.class.getCanonicalName()));
            assertThat(e.getMessage(), is(EXPECTED_NULL_POINTER_EXCEPTION_MESSAGE));
        }
    }

    /* Remove device code */

    @Test
    public void validRemoveDeviceCode() throws Throwable {
        final Context context = AndroidTestUtils.getApplicationContext();

        byte[] originalDeviceCode = null;
        try {
            originalDeviceCode = AndroidTestUtils.getDeviceCode();
            SharedPreferencesHandler.setDeviceCode(context, AndroidTestUtils.getDeviceCode());

            // Test removing the data when it already exists.
            SharedPreferencesHandler.removeDeviceCode(context);

            // Test removing the data when none exists.
            SharedPreferencesHandler.removeDeviceCode(context);
        }
        finally {
            if (originalDeviceCode != null) {
                SharedPreferencesHandler.setDeviceCode(context, originalDeviceCode);
            }
        }
    }

    @Test
    public void invalidRemoveDeviceCode() {
        try {
            final Context context = AndroidTestUtils.getApplicationContext();

            SharedPreferencesHandler.setDeviceCode(context, AndroidTestUtils.getDeviceCode());

            // Test removing the data when it already exists.
            SharedPreferencesHandler.removeDeviceCode(null);

            String s = "Expected an instance of %s to be thrown.";
            fail(String.format(s, NullPointerException.class.getSimpleName()));
        }
        catch (Throwable e) {
            assertThat(e.getClass().getCanonicalName(), is(NullPointerException.class.getCanonicalName()));
            assertThat(e.getMessage(), is(EXPECTED_NULL_POINTER_EXCEPTION_MESSAGE));
        }
    }
}
