package gliphic.android.without_servers.encrypted_key_store_handler_tests;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import gliphic.android.display.abstract_views.BaseActivity;
import gliphic.android.display.welcome_screen.SubmitCodeActivity;
import gliphic.android.exceptions.NoStoredObjectException;
import gliphic.android.exceptions.NullStaticVariableException;
import gliphic.android.operation.Alerts;
import gliphic.android.operation.Contact;
import gliphic.android.operation.Group;
import gliphic.android.operation.TempGlobalStatics;
import gliphic.android.operation.storage_handlers.ForcedDialogs;
import gliphic.android.operation.storage_handlers.ResponseCodeAndMessage;
import gliphic.android.operation.storage_handlers.SharedPreferencesHandler;
import gliphic.android.utils.AndroidTestUtils;
import libraries.Vars;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@Ignore("Remove this annotation after refactoring the SharedPreferencesHandler class to use " +
        "EncryptedSharedPreferences (and a master key) instead of manual encryption of SharedPreferences data.")
@RunWith(Parameterized.class)
public class SharedPreferencesTestsParameterized {
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
     * processes e.g. calling any SharedPreferencesHandler methods, but other activities could also work.
     */
    @Rule
    public ActivityScenarioRule<SubmitCodeActivity> rule = new ActivityScenarioRule<>(SubmitCodeActivity.class);

    // Parametrised values for each test.
    private String objectAlias;
    private Object dummyObjectData;
    private Object objectData;
    private Object originalObject = null;

    public SharedPreferencesTestsParameterized(String objectAlias) {
        this.objectAlias = objectAlias;

        if      (objectAlias.equals(SharedPrefAlias.REMEMBER_EMAIL_ADDRESS_ALIAS.get())) {
            this.dummyObjectData = true;
            this.objectData = false;
        }
        else if (objectAlias.equals(SharedPrefAlias.TERMS_OF_USE_AGREED_V1_ALIAS.get())) {
            this.dummyObjectData = true;
            this.objectData = false;
        }
        else if (objectAlias.equals(SharedPrefAlias.DEVICE_KEY.get())) {
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

    private enum SharedPrefAlias {
        // These are not removed when a contact signs out.
        REMEMBER_EMAIL_ADDRESS_ALIAS ("RememberEmailAddress"),
        TERMS_OF_USE_AGREED_V1_ALIAS ("TermsOfUseAgreedV1"),
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
        if      (objectAlias.equals(SharedPrefAlias.REMEMBER_EMAIL_ADDRESS_ALIAS.get())) {
            return SharedPreferencesHandler.isRememberEmailAddress(activity, null);
        }
        else if (objectAlias.equals(SharedPrefAlias.TERMS_OF_USE_AGREED_V1_ALIAS.get())) {
            return SharedPreferencesHandler.isTermsOfUseAgreed(activity);
        }
        else if (objectAlias.equals(SharedPrefAlias.DEVICE_KEY.get())) {
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

    private void setSharedPreferencesObject(BaseActivity activity, Object toStore)
            throws IOException, GeneralSecurityException {

        if      (objectAlias.equals(SharedPrefAlias.REMEMBER_EMAIL_ADDRESS_ALIAS.get())) {
            SharedPreferencesHandler.setRememberEmailAddress(activity, (Boolean) toStore);
        }
        else if (objectAlias.equals(SharedPrefAlias.TERMS_OF_USE_AGREED_V1_ALIAS.get())) {
            SharedPreferencesHandler.setTermsOfUseAgreed(activity, (Boolean) toStore);
        }
        else if (objectAlias.equals(SharedPrefAlias.DEVICE_KEY.get())) {
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

    private static MasterKey buildMasterKey(@NonNull Context context) throws GeneralSecurityException, IOException {
        // Attempt to use the StrongBox (Secure Element) Keymaster but continue without it if this is not possible by
        // using the standard hardware-backed Trusted Execution Environment.
        return new MasterKey.Builder(context)
                .setRequestStrongBoxBacked(true)
                .setUserAuthenticationRequired(false)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();
    }

    private static SharedPreferences getSharedPreferences(Context context)
            throws GeneralSecurityException, IOException {

        return EncryptedSharedPreferences.create(
                context,
                Vars.APP_NAME,
                buildMasterKey(context),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }

    private static void removeSharedPreferencesForAlias(BaseActivity activity,
                                                        String alias) throws GeneralSecurityException, IOException {

        SharedPreferences sharedPreferences = getSharedPreferences(activity);
        sharedPreferences.edit().remove(alias).apply();
    }

    private Object getStoredObject(BaseActivity activity) throws Exception {
        try {
            return getSharedPreferencesObject(activity);
        }
        catch (NoStoredObjectException e) {
            return null;
        }
    }

    private void testCleanup(BaseActivity activity) throws GeneralSecurityException, IOException {
        if (originalObject == null) {
            removeSharedPreferencesForAlias(activity, objectAlias);
        }
        else {
            setSharedPreferencesObject(activity, originalObject);
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

                assertThat(SharedPreferencesHandler.isUserSignedIn(activity), is(true));

                // The original values for data which will not be modified.
                SharedPreferences sharedPrefBefore = getSharedPreferences(activity);
                String originalDeviceKey     = sharedPrefBefore.getString(SharedPrefAlias.DEVICE_KEY.get(), null);
                String originalDeviceCode    = sharedPrefBefore.getString(SharedPrefAlias.DEVICE_CODE.get(), null);
                String originalTOUAgreed     =
                        sharedPrefBefore.getString(SharedPrefAlias.TERMS_OF_USE_AGREED_V1_ALIAS.get(), null);

                // Test the following two methods.
                SharedPreferencesHandler.removeLastEmailAddress(activity);
                SharedPreferencesHandler.removeAllContactData(activity);

                assertThat(SharedPreferencesHandler.isUserSignedIn(activity), is(false));

                // Test/Assert that all of the SharedPreferences data has either been removed or remains unaffected
                // depending on the alias.
                SharedPreferences sharedPrefAfter = getSharedPreferences(activity);
                for (SharedPrefAlias alias : SharedPrefAlias.values()) {
                    final String storedObjectString = sharedPrefAfter.getString(alias.get(), null);

                    switch (alias) {
                        case DEVICE_KEY:
                            assertThat(storedObjectString, is(originalDeviceKey));
                            break;
                        case DEVICE_CODE:
                            assertThat(storedObjectString, is(originalDeviceCode));
                            break;
                        case TERMS_OF_USE_AGREED_V1_ALIAS:
                            assertThat(storedObjectString, is(originalTOUAgreed));
                            break;
                        default:
                            // Note that the REMEMBER_EMAIL_ADDRESS_ALIAS is removed at the same time as removing
                            // LAST_EMAIL_ADDRESS.
                            assertThat(storedObjectString, is(nullValue()));
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

    @Test
    public void setAndGetObject() {
        rule.getScenario().onActivity(activity -> {
            try {
                try {
                    originalObject = getStoredObject(activity);

                    // Set and get two different objects to ensure that the new object is not the same as the original.

                    setSharedPreferencesObject(activity, dummyObjectData);
                    assertSharedPreferencesObject(activity, dummyObjectData);

                    setSharedPreferencesObject(activity, objectData);
                    assertSharedPreferencesObject(activity, objectData);
                }
                finally {
                    testCleanup(activity);
                }
            }
            catch (Throwable e) {
                fail(e.getMessage());
            }
        });
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
    public void getObjectNonExistentObject() {
        rule.getScenario().onActivity(activity -> {
            try {
                try {
                    originalObject = getStoredObject(activity);
                    removeSharedPreferencesForAlias(activity, objectAlias);

                    Object storedObject = getSharedPreferencesObject(activity);

                    if ( objectAlias.equals(SharedPrefAlias.REMEMBER_EMAIL_ADDRESS_ALIAS.get()) ||
                         objectAlias.equals(SharedPrefAlias.TERMS_OF_USE_AGREED_V1_ALIAS.get()) ) {

                        assertThat((Boolean) storedObject, is(false));
                    }
                    else {
                        fail(String.format(
                                "Expected an instance of %s thrown.",
                                NoStoredObjectException.class.getSimpleName()
                        ));
                    }
                }
                catch (Throwable e) {
                    assertThat(e.getClass().getCanonicalName(),
                            is(NoStoredObjectException.class.getCanonicalName()));

                    String s = "Cannot get non-existent SharedPreferences object with alias: %s";
                    assertThat(e.getMessage(), is(String.format(s, objectAlias)));
                }
                finally {
                    testCleanup(activity);
                }
            }
            catch (Throwable e) {
                fail(e.getMessage());
            }
        });
    }

    @Test
    public void getObjectNullActivity() throws Throwable {
        handleGetSetObjectNullActivity(true);
    }

    @Test
    public void setObjectNullActivity() throws Throwable {
        handleGetSetObjectNullActivity(false);
    }

    private void handleGetSetObjectNullActivity(boolean get) throws Throwable {
        try {
            if (get) {
                getSharedPreferencesObject(null);
            }
            else {
                setSharedPreferencesObject(null, objectData);
            }

            String s = "Expected an instance of %s to be thrown.";
            fail(String.format(s, NullPointerException.class.getSimpleName()));
        }
        catch (NullPointerException e) {
            String s = "Attempt to invoke virtual method 'android.content.Context " +
                    "android.content.Context.getApplicationContext()' on a null object reference";
            assertThat(e.getMessage(), is(s));
        }
    }

    @Test
    public void setObjectNullObject() {
        rule.getScenario().onActivity(activity -> {
            try {
                setSharedPreferencesObject(activity, null);

                String s = "Expected an instance of %s to be thrown.";
                fail(String.format(s, NullPointerException.class.getSimpleName()));
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
            catch (GeneralSecurityException | IOException e) {
                fail(e.getMessage());
            }
        });
    }
}
