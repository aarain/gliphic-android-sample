package gliphic.android.without_servers.android_key_store_handler_tests;

import android.content.Context;
import android.content.SharedPreferences;

import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.DecoderException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.InvalidAlgorithmParameterException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.AEADBadTagException;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import gliphic.android.display.abstract_views.BaseActivity;
import gliphic.android.display.welcome_screen.SubmitCodeActivity;
import gliphic.android.exceptions.NoStoredObjectException;
import gliphic.android.exceptions.NullStaticVariableException;
import gliphic.android.operation.Alerts;
import gliphic.android.operation.Contact;
import gliphic.android.operation.Group;
import gliphic.android.operation.TempGlobalStatics;
import gliphic.android.operation.storage_handlers.AndroidKeyStoreHandler;
import gliphic.android.operation.storage_handlers.ForcedDialogs;
import gliphic.android.operation.storage_handlers.ResponseCodeAndMessage;
import gliphic.android.operation.storage_handlers.SharedPreferencesHandler;
import gliphic.android.utils.AndroidTestUtils;
import libraries.GeneralUtils;
import libraries.Vars;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class SharedPreferencesTestsParameterized {
    private static final byte[] INVALID_LENGTH_IV = new byte[69];
    private static final ForcedDialogs ALL_FORCED_DIALOGS = new ForcedDialogs(
            ForcedDialogs.ForcedSignOutAction.CONNECTION_CLOSED_CONFLICT,
            "Internal error display message",
            new ResponseCodeAndMessage(500, "Response code error message.")
    );

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

    // Parametrised values for each test.
    private String objectAlias;
    private String ivAlias;
    private Object dummyObjectData;
    private Object objectData;  // This can hold a byte array as a Base64-encoded string.
    private Object originalObject = null;

    public SharedPreferencesTestsParameterized(String objectAlias) {
        this.objectAlias = objectAlias;

        this.ivAlias     = objectAlias + ALIAS_IV_SUFFIX;

        // The REMEMBER_EMAIL_ADDRESS_ALIAS and TERMS_OF_USE_AGREED_V1_ALIAS aliases are tested in the
        // non-parameterized class.
        if      (objectAlias.equals(SharedPrefAlias.DEVICE_KEY.get())) {
            this.dummyObjectData = new byte[5];
            this.objectData = AndroidTestUtils.getDeviceKey();
        }
        else if (objectAlias.equals(SharedPrefAlias.DEVICE_CODE.get())) {
            this.dummyObjectData = new byte[5];
            this.objectData = AndroidTestUtils.getDeviceCode();
        }
        else if (objectAlias.equals(SharedPrefAlias.LAST_EMAIL_ADDRESS.get())) {
            this.dummyObjectData = "";
            this.objectData = "test@test.com";
        }
        else if (objectAlias.equals(SharedPrefAlias.ACCESS_TOKEN.get())) {
            this.dummyObjectData = "";
            this.objectData = "access-token";
        }
        else if (objectAlias.equals(SharedPrefAlias.ACCESS_TOKEN_EXPIRY.get())) {
            this.dummyObjectData = 0L;
            this.objectData = System.currentTimeMillis();
        }
        else if (objectAlias.equals(SharedPrefAlias.REFRESH_TOKEN.get())) {
            this.dummyObjectData = "";
            this.objectData = "refresh-token";
        }
        else if (objectAlias.equals(SharedPrefAlias.DATA_ENCRYPTION_KEY.get())) {
            this.dummyObjectData = new byte[5];

            this.objectData = new byte[] {
                    (byte)0x65, (byte)0x22, (byte)0xaf, (byte)0x66, (byte)0x5d, (byte)0xcd, (byte)0xa8, (byte)0x98,
                    (byte)0x65, (byte)0x22, (byte)0xaf, (byte)0x66, (byte)0x5d, (byte)0xcd, (byte)0xa8, (byte)0x98,
                    (byte)0x65, (byte)0x22, (byte)0xaf, (byte)0x66, (byte)0x5d, (byte)0xcd, (byte)0xa8, (byte)0x98,
                    (byte)0x65, (byte)0x22, (byte)0xaf, (byte)0x66, (byte)0x5d, (byte)0xcd, (byte)0xa8, (byte)0x98
            };
        }
        else if (objectAlias.equals(SharedPrefAlias.SIGN_IN_TIME.get())) {
            this.dummyObjectData = 0L;
            this.objectData = System.currentTimeMillis();
        }
        else if (objectAlias.equals(SharedPrefAlias.FORCED_DIALOGS.get())) {
            this.dummyObjectData = new ForcedDialogs();
            this.objectData = ALL_FORCED_DIALOGS;
        }
        else {
            throw new Error("Invalid object alias: " + objectAlias);
        }
    }

    @Parameterized.Parameters
    public static List<String> objectAlias() {
        final List<String> objectAliases = new ArrayList<>();

        for (SharedPrefAlias alias : SharedPrefAlias.values()) {
            objectAliases.add(alias.get());
        }

        return objectAliases;
    }

    private static final String NOT_BASE_64_STRING = "not b@se64 s#r|ng";
    private static final String UNENCRYPTED_STRING = Base64.toBase64String(INVALID_LENGTH_IV);
    private static final String EXPECTED_NULL_POINTER_EXCEPTION_MESSAGE = "Attempt to invoke virtual method " +
            "'javax.crypto.SecretKey java.security.KeyStore$SecretKeyEntry.getSecretKey()' on a null object reference";

    private static final String ALIAS_IV_SUFFIX = "Iv";

    private enum SharedPrefAlias {
        // These are not removed when a contact signs out.
//        REMEMBER_EMAIL_ADDRESS_ALIAS ("RememberEmailAddress"),
//        TERMS_OF_USE_AGREED_V1_ALIAS ("TermsOfUseAgreedV1"),
        DEVICE_KEY          ("DeviceKey"),
        DEVICE_CODE         ("DeviceCode"),
        // These are removed when a contact signs out.
        LAST_EMAIL_ADDRESS  ("LastEmailAddress"),
        ACCESS_TOKEN        ("AccessToken"),
        ACCESS_TOKEN_EXPIRY ("AccessTokenExpiry"),
        REFRESH_TOKEN       ("RefreshToken"),
        DATA_ENCRYPTION_KEY ("DataEncryptionKey"),
        SIGN_IN_TIME        ("LastSignInTime"),
        FORCED_DIALOGS      ("ForcedDialogs");
//        ENC_PRIVATE__KEY    ("EncPrivateKey"),
//        ENC_PUBLIC_KEY      ("EncPublicKey"),
//        SIG_PRIVATE_KEY     ("SigPrivateKey"),
//        SIG_PUBLIC_KEY      ("SigPublicKey");

        private final String alias;

        SharedPrefAlias(final String alias) {
            this.alias = alias;
        }

        public String get() {
            return alias;
        }
    }

    private Object getSharedPreferencesObject(BaseActivity activity) throws Exception {
        if      (objectAlias.equals(SharedPrefAlias.DEVICE_KEY.get())) {
            return SharedPreferencesHandler.getDeviceKey(activity, false).getData();
        }
        else if (objectAlias.equals(SharedPrefAlias.DEVICE_CODE.get())) {
            return SharedPreferencesHandler.getDeviceCode(activity, false).getData();
        }
        else if (objectAlias.equals(SharedPrefAlias.LAST_EMAIL_ADDRESS.get())) {
            return SharedPreferencesHandler.getLastEmailAddress(activity);
        }
        else if (objectAlias.equals(SharedPrefAlias.ACCESS_TOKEN.get())) {
            return SharedPreferencesHandler.getAccessToken(activity);
        }
        else if (objectAlias.equals(SharedPrefAlias.ACCESS_TOKEN_EXPIRY.get())) {
            return SharedPreferencesHandler.getAccessTokenExpiry(activity);
        }
        else if (objectAlias.equals(SharedPrefAlias.REFRESH_TOKEN.get())) {
            return SharedPreferencesHandler.getRefreshToken(activity);
        }
        else if (objectAlias.equals(SharedPrefAlias.DATA_ENCRYPTION_KEY.get())) {
            return SharedPreferencesHandler.getDataEncryptionKey(activity);
        }
        else if (objectAlias.equals(SharedPrefAlias.SIGN_IN_TIME.get())) {
            return SharedPreferencesHandler.getLastSignInTime(activity);
        }
        else if (objectAlias.equals(SharedPrefAlias.FORCED_DIALOGS.get())) {
            return SharedPreferencesHandler.getForcedDialogs(activity);
        }
        else {
            throw new Error("Invalid object alias: " + objectAlias);
        }
    }

    private void setSharedPreferencesObject(BaseActivity activity, Object toStore) throws Exception{
        if      (objectAlias.equals(SharedPrefAlias.DEVICE_KEY.get())) {
            SharedPreferencesHandler.setDeviceKey(activity, (byte[]) toStore);
        }
        else if (objectAlias.equals(SharedPrefAlias.DEVICE_CODE.get())) {
            SharedPreferencesHandler.setDeviceCode(activity, (byte[]) toStore);
        }
        else if (objectAlias.equals(SharedPrefAlias.LAST_EMAIL_ADDRESS.get())) {
            SharedPreferencesHandler.setLastEmailAddress(activity, (String) toStore, false);
        }
        else if (objectAlias.equals(SharedPrefAlias.ACCESS_TOKEN.get())) {
            SharedPreferencesHandler.setAccessToken(activity, (String) toStore);
        }
        else if (objectAlias.equals(SharedPrefAlias.ACCESS_TOKEN_EXPIRY.get())) {
            SharedPreferencesHandler.setAccessTokenExpiry(activity, (Long) toStore);
        }
        else if (objectAlias.equals(SharedPrefAlias.REFRESH_TOKEN.get())) {
            SharedPreferencesHandler.setRefreshToken(activity, (String) toStore);
        }
        else if (objectAlias.equals(SharedPrefAlias.DATA_ENCRYPTION_KEY.get())) {
            SharedPreferencesHandler.setDataEncryptionKey(activity, (byte[]) toStore);
        }
        else if (objectAlias.equals(SharedPrefAlias.SIGN_IN_TIME.get())) {
            SharedPreferencesHandler.setLastSignInTime(activity, (Long) toStore);
        }
        else if (objectAlias.equals(SharedPrefAlias.FORCED_DIALOGS.get())) {
            SharedPreferencesHandler.setForcedDialogs(activity, (ForcedDialogs) toStore);
        }
        else {
            throw new Error("Invalid object alias: " + objectAlias);
        }
    }

    private static SharedPreferences getSharedPreferences(BaseActivity activity) {
        return activity.getSharedPreferences(Vars.APP_NAME, Context.MODE_PRIVATE);
    }

    private static void removeSharedPreferencesForAlias(BaseActivity activity, String alias) {
        SharedPreferences sharedPreferences = getSharedPreferences(activity);
        sharedPreferences.edit().remove(alias).apply();
    }

    private static void setInvalidSharedPreferencesForAlias(BaseActivity activity, String alias, String toStore) {
        SharedPreferences sharedPreferences = getSharedPreferences(activity);
        sharedPreferences.edit().putString(alias, toStore).commit();
    }

    private Object getStoredObject(BaseActivity activity) throws Exception {
        // Note: the application key must be set before calling this method.
        try {
            return getSharedPreferencesObject(activity);
        }
        catch (NoStoredObjectException e) {
            return null;
        }
    }

    private void testCleanup(BaseActivity activity) {
        if (originalObject == null) {
            removeSharedPreferencesForAlias(activity, objectAlias);
        }
        else {
            try {
                setSharedPreferencesObject(activity, originalObject);
            }
            catch (Throwable e) {
                fail(e.getMessage());
            }
        }
    }

    /**
     * This method implicitly tests the removeLastEmailAddress() and removeAllContactData() methods before every test.
     *
     * Removal of persistent data is not tested:
     * @see SharedPreferencesTests#validRemoveDeviceKey()
     * @see SharedPreferencesTests#validRemoveDeviceCode()
     */
    @Before
    public void removeAllContactDataTest() throws Throwable {

        // Add dummy data to test that it is removed later.
        final Contact currentContact  = AndroidTestUtils.generateCurrentContact();
        final Contact knownContact    = AndroidTestUtils.generateKnownContact();
        final Contact extendedContact = AndroidTestUtils.generateExtendedContact();
        final Group   commonGroup     = AndroidTestUtils.generateGroup();
        currentContact.storeStatically();
        knownContact.storeStatically();
        extendedContact.storeStatically();
        commonGroup.selectGroup();
        TempGlobalStatics.setContactClicked(currentContact);
        TempGlobalStatics.setGroupClicked(commonGroup);

        rule.getScenario().onActivity(activity -> {
            try {
                AndroidKeyStoreHandler.setAppSymmetricKey(activity, true);

                // Overwrite/Insert all share preferences data so that this method can test its deletion.
                SharedPreferencesHandler.setLastEmailAddress(
                        activity,
                        "email@email.email",
                        true
                );
                SharedPreferencesHandler.setAccessToken(activity, "accesstoken");
                SharedPreferencesHandler.setAccessTokenExpiry(activity, System.currentTimeMillis());
                SharedPreferencesHandler.setRefreshToken(activity, "refreshtoken");
                SharedPreferencesHandler.setDataEncryptionKey(activity, new byte[Vars.AES_KEY_LEN]);
                SharedPreferencesHandler.setLastSignInTime(activity, System.currentTimeMillis());
                SharedPreferencesHandler.setForcedDialogs(activity, ALL_FORCED_DIALOGS);

                // The original values for data which will not be modified.

                SharedPreferences sharedPrefBefore = getSharedPreferences(activity);

                String originalDeviceKey = sharedPrefBefore.getString(
                        SharedPrefAlias.DEVICE_KEY.get(),
                        null
                );
                String originalDeviceKeyIv = sharedPrefBefore.getString(
                        SharedPrefAlias.DEVICE_KEY.get() + ALIAS_IV_SUFFIX,
                        null
                );
                String originalDeviceCode = sharedPrefBefore.getString(
                        SharedPrefAlias.DEVICE_CODE.get(),
                        null
                );
                String originalDeviceCodeIv = sharedPrefBefore.getString(
                        SharedPrefAlias.DEVICE_CODE.get() + ALIAS_IV_SUFFIX,
                        null
                );
//                String originalTOUAgreed =
//                        sharedPrefBefore.getString(SharedPrefAlias.TERMS_OF_USE_AGREED_V1_ALIAS.get(), null);

                // Test the following two methods.
                SharedPreferencesHandler.removeLastEmailAddress(activity);
                SharedPreferencesHandler.removeAllContactData(activity);

                assertThat(SharedPreferencesHandler.isUserSignedIn(activity), is(false));

                // Test/Assert that all of the SharedPreferences data has either been removed or remains unaffected
                // depending on the alias.
                SharedPreferences sharedPrefAfter = getSharedPreferences(activity);
                for (SharedPrefAlias alias : SharedPrefAlias.values()) {
                    final String storedObjectString = sharedPrefAfter.getString(
                            alias.get(),
                            null
                    );
                    final String storedObjectIvString = sharedPrefAfter.getString(
                            alias.get() + ALIAS_IV_SUFFIX,
                            null
                    );

                    switch (alias) {
                        case DEVICE_KEY:
                            assertThat(storedObjectString, is(originalDeviceKey));
                            assertThat(storedObjectIvString, is(originalDeviceKeyIv));
                            break;
                        case DEVICE_CODE:
                            assertThat(storedObjectString, is(originalDeviceCode));
                            assertThat(storedObjectIvString, is(originalDeviceCodeIv));
                            break;
                        default:
                            // Note that the REMEMBER_EMAIL_ADDRESS_ALIAS is removed at the same time as removing
                            // LAST_EMAIL_ADDRESS.
                            assertThat(storedObjectString, is(nullValue()));
                            assertThat(storedObjectIvString, is(nullValue()));
                            break;
                    }
                }
            }
            catch (Throwable e) {
                fail(e.getMessage());
            }
        });

        try { Contact.getCurrentContact(); }
        catch (NullStaticVariableException e1) {
            try { Contact.getTargetContacts(); }
            catch (NullStaticVariableException e2) {
                try { Group.getSelectedGroup(); }
                catch (NullStaticVariableException e3) {
                    try { Group.getKnownGroups(); }
                    catch (NullStaticVariableException e4) {
                        try { TempGlobalStatics.getContactClicked(); }
                        catch (NullStaticVariableException e5) {
                            try { TempGlobalStatics.getGroupClicked(); }
                            catch (NullStaticVariableException e6) {
                                try { Alerts.getGroupShares(); }
                                catch (NullStaticVariableException e7) {
                                    return;
                                }
                                fail("Expected a NullPointerException when getting group-share alerts.");
                            }
                            fail("Expected a NullPointerException when getting the current group clicked.");
                        }
                        fail("Expected a NullPointerException when getting the current contact clicked.");
                    }
                    fail("Expected a NullPointerException when getting known groups.");
                }
                fail("Expected a NullPointerException when getting the selected group.");
            }
            fail("Expected a NullPointerException when getting target contacts.");
        }
        fail("Expected a NullPointerException when getting the current contact.");
    }

    private void failExpectedException(Class<?> clazz) {
        fail(String.format("Expected an instance of %s thrown.", clazz.getSimpleName()));
    }

    private void assertSharedPreferencesObject(BaseActivity activity, Object object) throws Exception {
        if (object instanceof ForcedDialogs) {
            final ForcedDialogs storedForcedDialogs   = (ForcedDialogs) getSharedPreferencesObject(activity);
            final ForcedDialogs expectedForcedDialogs = ((ForcedDialogs) object);

            assertThat(storedForcedDialogs.getForcedSignOutAction(),
                    is(expectedForcedDialogs.getForcedSignOutAction()));

            assertThat(storedForcedDialogs.getInternalErrorMessage(),
                    is(expectedForcedDialogs.getInternalErrorMessage()));

            if (expectedForcedDialogs.getResponseCodeAndMessage() == null) {
                assertThat(storedForcedDialogs.getResponseCodeAndMessage(), is(nullValue()));
            }
            else {
                assertThat(storedForcedDialogs.getResponseCodeAndMessage().getResponseCode(),
                        is(expectedForcedDialogs.getResponseCodeAndMessage().getResponseCode()));

                assertThat(storedForcedDialogs.getResponseCodeAndMessage().getDisplayMessage(),
                        is(expectedForcedDialogs.getResponseCodeAndMessage().getDisplayMessage()));
            }
        }
        else {
            assertThat(getSharedPreferencesObject(activity), is(object));
        }
    }

    @Test
    public void setAndGetObject() {
        rule.getScenario().onActivity(activity -> {
            try {
                AndroidKeyStoreHandler.setAppSymmetricKey(activity, true);
            }
            catch (Throwable e) {
                fail(e.getMessage());
            }

            try {
                originalObject = getStoredObject(activity);

                // Set and get two different objects to ensure that the new object is not the same as the original.

                setSharedPreferencesObject(activity, dummyObjectData);
                assertSharedPreferencesObject(activity, dummyObjectData);

                setSharedPreferencesObject(activity, objectData);
                assertSharedPreferencesObject(activity, objectData);
            }
            catch (Throwable e) {
                fail(e.getMessage());
            }
            finally {
                testCleanup(activity);
            }
        });
    }

    @Test
    public void setObjectNonExistentAppKey() {
        rule.getScenario().onActivity(activity -> {
            try {
                setSharedPreferencesObject(activity, objectData);
            }
            catch (Throwable e) {
                assertThat(e.getClass().getCanonicalName(), is(NullPointerException.class.getCanonicalName()));
                assertThat(e.getMessage(), is(EXPECTED_NULL_POINTER_EXCEPTION_MESSAGE));
            }
        });
    }

    @Test
    public void getObjectNonExistentObject() {
        rule.getScenario().onActivity(activity -> {
            try {
                AndroidKeyStoreHandler.setAppSymmetricKey(activity, true);
            }
            catch (Throwable e) {
                fail(e.getMessage());
            }

            try {
                originalObject = getStoredObject(activity);
                removeSharedPreferencesForAlias(activity, objectAlias);

                getSharedPreferencesObject(activity);

                failExpectedException(NoStoredObjectException.class);
            }
            catch (Throwable e) {
                assertThat(e.getClass().getCanonicalName(),
                        is(NoStoredObjectException.class.getCanonicalName()));

                final String s = "Cannot get non-existent SharedPreferences object with alias: " + objectAlias;
                assertThat(e.getMessage(), is(s));
            }
            finally {
                testCleanup(activity);
            }
        });
    }

    @Test
    public void getObjectNullActivity(){
        handleGetSetObjectNullActivity(true);
    }

    @Test
    public void setObjectNullActivity() {
        handleGetSetObjectNullActivity(false);
    }

    private void handleGetSetObjectNullActivity(boolean get) {
        rule.getScenario().onActivity(activity -> {
            try {
                AndroidKeyStoreHandler.setAppSymmetricKey(activity, true);
            }
            catch (Throwable e) {
                fail(e.getMessage());
            }

            try {
                if (get) {
                    getSharedPreferencesObject(null);
                }
                else {
                    setSharedPreferencesObject(null, objectData);
                }

                failExpectedException(NullPointerException.class);
            }
            catch (NullPointerException e) {
                assertThat(e.getMessage(), is(SharedPreferencesTests.EXPECTED_NULL_POINTER_EXCEPTION_MESSAGE));
            }
            catch (Throwable e) {
                fail(e.getMessage());
            }
        });
    }

    @Test
    public void setObjectNullObject() {
        rule.getScenario().onActivity(activity -> {
            try {
                AndroidKeyStoreHandler.setAppSymmetricKey(activity, true);
            }
            catch (Throwable e) {
                fail(e.getMessage());
            }

            try {
                setSharedPreferencesObject(activity, null);

                failExpectedException(NullPointerException.class);
            }
            catch (NullPointerException e) {
                final String s;

                if (objectData instanceof byte[]) {
                    s = "Attempt to get length of null array";
                }
                else if (objectData instanceof Long) {
                    s = "Attempt to invoke virtual method 'long java.lang.Long.longValue()' on a null object " +
                            "reference";
                }
                else if (objectData instanceof Boolean) {
                    s = "Attempt to invoke virtual method 'boolean java.lang.Boolean.booleanValue()' on a null " +
                            "object reference";
                }
                else {
                    s = "Attempt to invoke virtual method 'java.lang.Class java.lang.Object.getClass()' on a null " +
                            "object reference";
                }

                assertThat(e.getMessage(), is(s));
            }
            catch (Throwable e) {
                fail(e.getMessage());
            }
        });
    }

    @Test
    public void getObjectNonExistentIv() {
        rule.getScenario().onActivity(activity -> {
            try {
                AndroidKeyStoreHandler.setAppSymmetricKey(activity, true);
            }
            catch (Throwable e) {
                fail(e.getMessage());
            }

            try {
                originalObject = getStoredObject(activity);
                setSharedPreferencesObject(activity, objectData);
                removeSharedPreferencesForAlias(activity, ivAlias);

                getSharedPreferencesObject(activity);

                failExpectedException(NoStoredObjectException.class);
            }
            catch (Throwable e) {
                assertThat(e.getClass().getCanonicalName(),
                        is(NoStoredObjectException.class.getCanonicalName()));

                final String s = "Cannot get non-existent SharedPreferences object with alias: " + ivAlias;
                assertThat(e.getMessage(), is(s));
            }
            finally {
                testCleanup(activity);
            }
        });
    }

    @Test
    public void getObjectObjectNotBase64() {
        rule.getScenario().onActivity(activity -> {
            try {
                AndroidKeyStoreHandler.setAppSymmetricKey(activity, true);
            }
            catch (Throwable e) {
                fail(e.getMessage());
            }

            try {
                originalObject = getStoredObject(activity);
                setSharedPreferencesObject(activity, objectData);
                setInvalidSharedPreferencesForAlias(activity, objectAlias, NOT_BASE_64_STRING);

                getSharedPreferencesObject(activity);

                failExpectedException(DecoderException.class);
            }
            catch (Throwable e) {
                assertThat(e.getClass().getCanonicalName(), is(DecoderException.class.getCanonicalName()));
                final String s = "unable to decode base64 string: invalid characters encountered in base64 data";
                assertThat(e.getMessage(), is(s));
            }
            finally {
                testCleanup(activity);
            }
        });
    }

    @Test
    public void getObjectIvNotBase64() {
        rule.getScenario().onActivity(activity -> {
            try {
                AndroidKeyStoreHandler.setAppSymmetricKey(activity, true);
            }
            catch (Throwable e) {
                fail(e.getMessage());
            }

            try {
                originalObject = getStoredObject(activity);
                setSharedPreferencesObject(activity, objectData);
                setInvalidSharedPreferencesForAlias(activity, ivAlias, NOT_BASE_64_STRING);

                getSharedPreferencesObject(activity);

                failExpectedException(DecoderException.class);
            }
            catch (Throwable e) {
                assertThat(e.getClass().getCanonicalName(), is(DecoderException.class.getCanonicalName()));
                final String s = "unable to decode base64 string: invalid characters encountered in base64 data";
                assertThat(e.getMessage(), is(s));
            }
            finally {
                testCleanup(activity);
            }
        });
    }

    @Test
    public void getObjectBadObject() {
        rule.getScenario().onActivity(activity -> {
            try {
                AndroidKeyStoreHandler.setAppSymmetricKey(activity, true);
            }
            catch (Throwable e) {
                fail(e.getMessage());
            }

            try {
                originalObject = getStoredObject(activity);
                setSharedPreferencesObject(activity, objectData);
                setInvalidSharedPreferencesForAlias(activity, objectAlias, UNENCRYPTED_STRING);

                getSharedPreferencesObject(activity);

                failExpectedException(AEADBadTagException.class);
            }
            catch (Throwable e) {
                assertThat(e.getClass().getCanonicalName(), is(AEADBadTagException.class.getCanonicalName()));
                assertThat(e.getMessage(), is(nullValue()));
            }
            finally {
                testCleanup(activity);
            }
        });
    }

    @Test
    public void getObjectBadIv() {
        rule.getScenario().onActivity(activity -> {
            try {
                AndroidKeyStoreHandler.setAppSymmetricKey(activity, true);
            }
            catch (Throwable e) {
                fail(e.getMessage());
            }

            try {
                originalObject = getStoredObject(activity);
                setSharedPreferencesObject(activity, objectData);
                setInvalidSharedPreferencesForAlias(activity, ivAlias, UNENCRYPTED_STRING);

                getSharedPreferencesObject(activity);

                failExpectedException(InvalidAlgorithmParameterException.class);
            }
            catch (Throwable e) {
                assertThat(e.getClass().getCanonicalName(),
                        is(InvalidAlgorithmParameterException.class.getCanonicalName()));
                String s = "Unsupported IV length: %d bytes. Only 12 bytes long IV supported";
                assertThat(e.getMessage(), is(String.format(s, INVALID_LENGTH_IV.length)));
            }
            finally {
                testCleanup(activity);
            }
        });
    }

    @Test
    public void getObjectIvMismatch() {
        rule.getScenario().onActivity(activity -> {
            try {
                AndroidKeyStoreHandler.setAppSymmetricKey(activity, true);
            }
            catch (Throwable e) {
                fail(e.getMessage());
            }

            try {
                originalObject = getStoredObject(activity);
                setSharedPreferencesObject(activity, objectData);
                setInvalidSharedPreferencesForAlias(
                        activity,
                        ivAlias,
                        Base64.toBase64String(GeneralUtils.generateIv()));

                getSharedPreferencesObject(activity);

                failExpectedException(AEADBadTagException.class);
            }
            catch (Throwable e) {
                assertThat(e.getClass().getCanonicalName(), is(AEADBadTagException.class.getCanonicalName()));
                assertThat(e.getMessage(), is(nullValue()));
            }
            finally {
                testCleanup(activity);
            }
        });
    }
}
