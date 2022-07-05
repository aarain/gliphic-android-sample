package gliphic.android.utils;

import android.app.Activity;
import android.content.Intent;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.test.core.app.ActivityScenario;
import gliphic.android.display.abstract_views.BaseActivity;
import gliphic.android.display.welcome_screen.SignInActivity;
import gliphic.android.operation.Alerts;
import gliphic.android.operation.Contact;
import gliphic.android.operation.Group;
import gliphic.android.exceptions.GroupException;
import gliphic.android.operation.server_interaction.http_server.HttpOperations;
import gliphic.android.operation.storage_handlers.AndroidKeyStoreHandler;
import gliphic.android.operation.storage_handlers.ForcedDialogs;
import gliphic.android.operation.storage_handlers.SharedPreferencesHandler;
import gliphic.android.with_http_server.single_fragment_activity.RegisterActivityTest;

import org.bouncycastle.crypto.generators.SCrypt;
import org.bouncycastle.util.encoders.Base64;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import libraries.Base256;
import libraries.BouncyCastleInterpreter;
import libraries.GroupPermissions;
import libraries.Vars;
import libraries.Vars.ContactType;
import pojo.account.GroupShare;
import pojo.load.LoadContactObject;
import pojo.load.LoadGroupObject;

import static org.junit.Assert.fail;

/**
 * Base class which is extended by other abstract classes with differing @Rule annotations but
 * consistent @BeforeClass annotations.
 */

abstract public class MainActivityBaseSetup {

    public static String activateAccount(long contactNumber) throws Exception {
        String uri = AndroidTestUtils.getUriPrefix() + "/account/activate/activate-contact";
        return AndroidTestUtils.postString(uri, Long.toString(contactNumber));
    }

    public static String deactivateAccount(long contactNumber) throws Exception {
        String uri = AndroidTestUtils.getUriPrefix() + "/account/activate/deactivate-contact";
        return AndroidTestUtils.postString(uri, Long.toString(contactNumber));
    }

    public static String setNullRecoveryTokenId(long contactNumber) throws Exception {
        String uri = AndroidTestUtils.getUriPrefix() + "/account/set-null-recovery-token-id/" + contactNumber;
        return AndroidTestUtils.getRequest(uri);
    }

    public static String setNonNullRecoveryTokenId(long contactNumber) throws Exception {
        String uri = AndroidTestUtils.getUriPrefix() + "/account/set-non-null-recovery-token-id/" + contactNumber;
        return AndroidTestUtils.getRequest(uri);
    }

    public static String setNullSignInTokenId(long contactNumber) throws Exception {
        String uri = AndroidTestUtils.getUriPrefix() + "/account/set-null-sign-in-token-id/" + contactNumber;
        return AndroidTestUtils.getRequest(uri);
    }

    public static String setNonNullSignInTokenId(long contactNumber) throws Exception {
        String uri = AndroidTestUtils.getUriPrefix() + "/account/set-non-null-sign-in-token-id/" + contactNumber;
        return AndroidTestUtils.getRequest(uri);
    }

    public static String modifyGroupNumber(long currentGroupNumber, long newGroupNumber) throws Exception {
        String uri = AndroidTestUtils.getUriPrefix() + "/group/modify-number/" + currentGroupNumber;
        return AndroidTestUtils.postString(uri, Long.toString(newGroupNumber));
    }

    public static String getValidActivationToken(long contactNumber) throws Exception {
        String uri = AndroidTestUtils.getUriPrefix() + "/account/get-valid-activation-token";
        return AndroidTestUtils.postString(uri, Long.toString(contactNumber));
    }

    public static String getValidAccessToken(long contactNumber) throws Exception {
        String uri = AndroidTestUtils.getUriPrefix() + "/account/get-valid-access-token";
        return AndroidTestUtils.postString(uri, Long.toString(contactNumber));
    }

    public static String getValidRefreshToken(long contactNumber) throws Exception {
        String uri = AndroidTestUtils.getUriPrefix() + "/account/get-valid-refresh-token";
        return AndroidTestUtils.postString(uri, Long.toString(contactNumber));
    }

    public static String getValidRecoveryToken(long contactNumber) throws Exception {
        setNonNullRecoveryTokenId(contactNumber);

        String uri = AndroidTestUtils.getUriPrefix() + "/account/get-valid-recovery-token";
        return AndroidTestUtils.postString(uri, Long.toString(contactNumber));
    }

    public static String getValidSignInToken(long contactNumber) throws Exception {
        setNonNullSignInTokenId(contactNumber);

        String uri = AndroidTestUtils.getUriPrefix() + "/account/get-valid-sign-in-token";
        return AndroidTestUtils.postString(uri, Long.toString(contactNumber));
    }

    public static String getDeviceCode(long contactNumber) throws Exception {
        String uri = AndroidTestUtils.getUriPrefix() + "/account/get-device-code";
        return AndroidTestUtils.postString(uri, Long.toString(contactNumber));
    }

    public static String setDeviceCode(long contactNumber, @NonNull byte[] deviceCode) throws Exception {
        return setDeviceCode(contactNumber, Base64.toBase64String(deviceCode));
    }

    public static String setDeviceCode(long contactNumber, @NonNull String deviceCodeString) throws Exception {
        return AndroidTestUtils.postString(
                AndroidTestUtils.getUriPrefix() + "/account/set-device-code/" + contactNumber,
                deviceCodeString
        );
    }

    public static String setGroupPermissionsForTestContact(long groupNumber,
                                                           @NonNull GroupPermissions groupPermissions)
            throws Exception {

        final LoadGroupObject loadGroupObject = new LoadGroupObject();
        loadGroupObject.setNumber(groupNumber);
        loadGroupObject.setPermissions(groupPermissions.get());

        return AndroidTestUtils.postObject(
                AndroidTestUtils.getUriPrefix() + "/group/set-permissions-for-test-contact/",
                loadGroupObject
        );
    }

    public static final String expiredActivationTokenContact0 = "eyJhbGciOiJIUzUxMiIsInppcCI6IkRFRiJ9.eNqqVsosLlayUn" +
            "LPySzIyExW0lEqLk0C8h2TSzLLEksy8_OAQomlKUAhAyArtaJAycrQ1NzEzMLQ0shcRykzsQRVIKskE2Rcmkdammmqc3lhWp5Rkp9Jq" +
            "HlZToZbjrlSLQAAAP__.sFyJpEL5jDwLw1EmxqaCIwjHwT5la8Bv8xaYo_-bMx27bepB4hiyuV6n7wcYqQ2OtF1M2-lpRyIkdXlE21V" +
            "ovw";

    public static final String expiredRecoveryTokenContact0 = "eyJhbGciOiJIUzUxMiIsInppcCI6IkRFRiJ9.eNqqVsosLlayUnLP" +
            "ySzIyExW0lEqLk0C8oNSk_PLUosqgQKJpSlAAQMgK7WiQMnK0MzAxNjUwtDIQkcpM7EEImBiYGgMEsgqyQSqLfaMNykpdSkorXLKD3A" +
            "rqirLN3OLrKzMVqoFAAAA__8.ibp1LE-DYG4ydNHzHXKkpSlSE2VAiRdV57RNNuvTwrJtLKikw7Arkp3Q5CnEztEP7WkCtv52wP3-Sm" +
            "cczA_o5w";

    public static final String expiredAccessTokenContact0 = "eyJhbGciOiJIUzUxMiIsInppcCI6IkRFRiJ9.eNqqVsosLlayUnLPyS" +
            "zIyExW0lEqLk0C8h2Tk1OBEjpKiaUpQK4BkJVaUaBkZWhqbmJmYWRgaKGjlJlYgiqQVZIJVOvkEloR718SnxnlEeke5hSfH-kSkpwf4" +
            "qdUCwAAAP__.tp2ogl0LGMi6FavPSLrrIoYFfEPJ4c4OQgeC5JvUPzB5GK2jkuMRuYn35KbA0_GIciPiqsbTu0JrfQFhmA91MA";

    /**
     * This method should only be called if the {@link #setTokenDataContact0(boolean, ActivityScenario)} method cannot
     * be called, due to the activity held by the scenario already being closed.
     */
    public static void setTokenDataContact0(final boolean isRefreshTokenValid) throws Throwable {
        ActivityScenario<?> scenario = AndroidTestUtils.getActivityScenario(SignInActivity.class);

        setTokenDataContact0(isRefreshTokenValid, scenario);

        scenario.onActivity(Activity::finish);
    }

    public static void setTokenDataContact0(final boolean isRefreshTokenValid,
                                            final ActivityScenario<?> activityScenario) throws Throwable {

        // Any HTTP request is performed before creating the thread below.
        final String refreshToken = isRefreshTokenValid ? getValidRefreshToken(0) : "invalid refresh token";

        long inThePast = System.currentTimeMillis() - 1000;
        final long expiredTime = inThePast - (inThePast % 1000);

        activityScenario.onActivity(activity -> {
            try {
                SharedPreferencesHandler.setAccessToken(activity, expiredAccessTokenContact0);
                SharedPreferencesHandler.setAccessTokenExpiry(activity, expiredTime);
                SharedPreferencesHandler.setRefreshToken(activity, refreshToken);

                // The first call to setting the sign-in time is to ensure that the application does not assume that
                // the contact is signed-out, and thus always shows the sign-in screen, by setting any value as the
                // sign-in time.
                SharedPreferencesHandler.setLastSignInTime(activity, System.currentTimeMillis());

                // The second call to setting the sign-in time is to ensure that the forced sign-out dialog does not
                // appear, by attempting to set the sign-in time stored by the server (if not the current time is set).
                AndroidTestUtils.getSignInTime(
                        signInTime -> {
                            try {
                                SharedPreferencesHandler.setLastSignInTime(activity, signInTime);
                            }
                            catch (Throwable e) {
                                fail(e.getMessage());
                            }
                        },
                        (BaseActivity) activity
                );
            }
            catch (Throwable e) {
                fail(e.getMessage());
            }
        });
    }

    /**
     * Set the device code and device key stored on this device.
     *
     * This matches the hard-coded value in the server test database.
     */
    public static void setDefaultDeviceValues(@NonNull ActivityScenario<?> activityScenario) {
        activityScenario.onActivity(activity -> {
            try {
                AndroidKeyStoreHandler.setAppSymmetricKey(activity, true);

                SharedPreferencesHandler.setDeviceKey(activity, AndroidTestUtils.getDeviceKey());
                SharedPreferencesHandler.setDeviceCode(activity, AndroidTestUtils.getDeviceCode());
            }
            catch (Throwable e) {
                fail(e.getMessage());
            }
        });
    }

    public static void setDataEncryptionKey(final ActivityScenario<?> activityScenario) throws Throwable {
        // These byte arrays are copied directly from the server tests.
        // Note that all of this data (and more) is duplicated in the PublishedTextTest class.
        final byte[] encSalt = {
                (byte) 0x10, (byte) 0x46, (byte) 0xAB, (byte) 0xC9, (byte) 0xC3, (byte) 0xAB, (byte) 0xC9, (byte) 0xC3,
                (byte) 0xAB, (byte) 0xC9, (byte) 0xC3, (byte) 0x40, (byte) 0x45, (byte) 0xD6, (byte) 0x91, (byte) 0x40
        };

        // Use reflection to get the private static final SCrypt parameters.
        BouncyCastleInterpreter bouncyCastleInterpreter = new BouncyCastleInterpreter();
        Field field = BouncyCastleInterpreter.class.getDeclaredField("SCRYPT_N");
        field.setAccessible(true);
        final int SCRYPT_N = (int) field.get(bouncyCastleInterpreter);

        field = BouncyCastleInterpreter.class.getDeclaredField("SCRYPT_r");
        field.setAccessible(true);
        final int SCRYPT_r = (int) field.get(bouncyCastleInterpreter);

        field = BouncyCastleInterpreter.class.getDeclaredField("SCRYPT_p");
        field.setAccessible(true);
        final int SCRYPT_p = (int) field.get(bouncyCastleInterpreter);

        field = BouncyCastleInterpreter.class.getDeclaredField("SCRYPT_dkLen");
        field.setAccessible(true);
        final int SCRYPT_dkLen = (int) field.get(bouncyCastleInterpreter);

        activityScenario.onActivity(activity -> {
            try {
                byte[] dataEncryptionKey = SCrypt.generate(
                        "P455w0rd".getBytes(StandardCharsets.UTF_8),
                        encSalt,
                        SCRYPT_N,
                        SCRYPT_r,
                        SCRYPT_p,
                        SCRYPT_dkLen
                );

                SharedPreferencesHandler.setDataEncryptionKey(activity, dataEncryptionKey);
            }
            catch (Throwable e) {
                fail(e.getMessage());
            }
        });
    }

    public static void mainActivitySetupAfterActivityLaunched(final ActivityScenario<?> activityScenario,
                                                              Class<?> activityToLaunch,
                                                              boolean withHTTPServer,
                                                              boolean startMainActivity) {

        activityScenario.onActivity(activity -> {
            try {
                AndroidKeyStoreHandler.setAppSymmetricKey(activity, true);
                setDefaultDeviceValues(activityScenario);
                setTokenDataContact0(withHTTPServer, activityScenario);
                setDataEncryptionKey(activityScenario);

                // Set email address.
                SharedPreferencesHandler.setRememberEmailAddress(activity, false);
                SharedPreferencesHandler.setLastEmailAddress(
                        activity,
                        RegisterActivityTest.duplicateEmail,
                        false
                );

                SharedPreferencesHandler.setTermsOfUseAgreed(activity, true);

                // Assume the contact has no forced dialogs displayed.
                SharedPreferencesHandler.setForcedDialogs(activity, new ForcedDialogs());

                // Give time for the Android keystore and/or SharedPreferences to be modified.
                SystemClock.sleep(200);

                if (startMainActivity) {
                    // Start the main activity.
                    Intent intent = new Intent(activity.getBaseContext(), activityToLaunch);
                    activity.startActivity(intent);
                    // Kill this activity.
                    activity.finish();

                    // Give time for the MainActivity to be launched.
                    SystemClock.sleep(200);
                }
            }
            catch (Throwable e) {
                fail(e.getMessage());
            }
        });
    }

    public static void mainActivitySetupBeforeActivityLaunched() throws Exception {
        // Since BeforeClass is run before any activities are created, this method cannot contain method calls which
        // require an activity instance.

        /* ********* SSL SETUP ********* */

        HttpOperations.restSslSetup(null, null);

        /* ********* SET CONTACT INFO ********* */

        long contactNumber = 0;

        Contact.setNullCurrentContact();
        new Contact(contactNumber,
                AndroidTestUtils.generateContactId(contactNumber),
                "Test User",
                Vars.DisplayPicture.LADY_ARTIST.get(),
                ContactType.CURRENT,
                true
        ).storeStatically();

        contactNumber++;

        Contact.setNullKnownContacts();
        List<Contact> knownContacts = new ArrayList<>();
        knownContacts.add(new Contact(
                contactNumber,
                AndroidTestUtils.generateContactId(contactNumber),
                "Patient-0",
                Vars.DisplayPicture.LADY_ASTRONAUT.get(),
                ContactType.KNOWN,
                true
        ));
        contactNumber++;
        knownContacts.add(new Contact(
                contactNumber,
                AndroidTestUtils.generateContactId(contactNumber),
                "This-contact-name-has-30-chars",
                Vars.DisplayPicture.LADY_BUILDER.get(),
                ContactType.KNOWN,
                true
        ));
        contactNumber++;
        knownContacts.add(new Contact(
                contactNumber,
                AndroidTestUtils.generateContactId(contactNumber),
                "Norman Gorman",
                Vars.DisplayPicture.LADY_COOK.get(),
                ContactType.KNOWN,
                true
        ));
        contactNumber++;
        knownContacts.add(new Contact(
                contactNumber,
                AndroidTestUtils.generateContactId(contactNumber),
                "Kappa_Ross",
                Vars.DisplayPicture.LADY_DOCTOR.get(),
                ContactType.KNOWN,
                true
        ));
        contactNumber++;
        knownContacts.add(new Contact(
                contactNumber,
                AndroidTestUtils.generateContactId(contactNumber),
                "Thanks Purge SeemsGood",
                Vars.DisplayPicture.LADY_FARMER.get(),
                ContactType.KNOWN,
                true
        ));
        contactNumber++;
        knownContacts.add(new Contact(
                contactNumber,
                AndroidTestUtils.generateContactId(contactNumber),
                "NO-COMMON-GROUPS",
                Vars.DisplayPicture.LADY_FIREFIGHTER.get(),
                ContactType.KNOWN,
                true
        ));
        Contact.storeStatically(knownContacts);

        contactNumber++;

        Contact.setNullExtendedContacts();
        List<Contact> extendedContacts = new ArrayList<>();
        extendedContacts.add(new Contact(
                contactNumber,
                AndroidTestUtils.generateContactId(contactNumber),
                "Who dis guy?",
                Vars.DisplayPicture.LADY_MECHANIC.get(),
                ContactType.EXTENDED,
                true
        ));
        Contact.storeStatically(extendedContacts);

        /* ********* SET GROUP INFO ********* */

        // Note that the expected plain text group key bytes, which are set to null here, can be
        // taken from the server-side tests.
        Group.setNullSelectedGroup();
        Group.setNullKnownGroups();
        List<Group> allGroups = new ArrayList<>();
        allGroups.add(new Group(
                Vars.DEFAULT_GROUP_NUMBER,
                Vars.DisplayPicture.APP_ICON.get(),
                Vars.DEFAULT_GROUP_NAME,
                Vars.DEFAULT_GROUP_DESCRIPTION,
                Vars.DEFAULT_GROUP_ID,
                GroupPermissions.ACTIVE_MEMBER,
                false,
                true,
                true
        ));
        allGroups.add(new Group(
                1,
                Vars.DisplayPicture.ANIMAL_CAT.get(),
                "This group name has max length",
                "A maximum length description is required here to test how a long description is handled graphically.",
                "ÀBCD0÷ŗŰſ",
                GroupPermissions.ACTIVE_OWNER,
                false,
                false,
                true
        ));
        allGroups.add(new Group(
                2,
                Vars.DisplayPicture.ANIMAL_DOG.get(),
                "NO OTHER CONTACTS IN THE GROUP",
                "This group should not have any other contacts added to it, to test showing 0 contacts graphically.",
                "0Az-_ÆÏÐœ",
                GroupPermissions.ACTIVE_OWNER,
                false,
                false,
                true
        ));

        int numSelectedGroups = 0;
        for (Group g : allGroups) {
            if (g.isSelected()) {
                numSelectedGroups++;
                g.selectGroup();
            }

            g.storeStatically();
        }

        if (numSelectedGroups != 1) {
            String s = "Test setup error: Unexpected number of selected groups: " + numSelectedGroups;
            throw new GroupException(s);
        }

        /* ********* SET COMMON GROUPS AND CONTACTS ********* */

        // Do not add the default group to any contacts, and add contacts to group number 1.
        Group group1 = allGroups.get(1);

        // The number l is the contact number to add a common group for.
        for (long l = 1; l < 6; l++) {
            Contact c = Contact.getContactFromNumber(l);

            group1.addGroupContact(c);
            c.addCommonGroup(group1);
        }

        /* ********* SET ALERTS INFO ********* */

        Alerts.setNullGroupShares();

        List<GroupShare> groupShareList = new ArrayList<>();

        groupShareList.add(new GroupShare(
                Vars.GroupShareStatus.PENDING_RECEIVED,
                System.currentTimeMillis(),
                new LoadGroupObject(
                        255,
                        Base256.toBase64(AndroidTestUtils.generateGroupId(255)),
                        "255grpName",
                        null,
                         null,
                        0,
                        false,
                        false
                ),
                new LoadContactObject(
                        AndroidTestUtils.CONTACT_WITH_MANY_GROUPS,
                        AndroidTestUtils.generateContactId(AndroidTestUtils.CONTACT_WITH_MANY_GROUPS),
                        AndroidTestUtils.generateContactName(AndroidTestUtils.CONTACT_WITH_MANY_GROUPS),
                        null,
                        ContactType.KNOWN
                ),
                Base64.toBase64String(new byte[Vars.PUB_ENC_AES_KEY_LEN])
        ));

        Alerts.storeStatically(groupShareList);
    }
}
