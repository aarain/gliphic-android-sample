package gliphic.android.with_http_server.functionality;

import android.content.Context;
import android.os.SystemClock;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.PerformException;
import androidx.test.platform.app.InstrumentationRegistry;
import gliphic.android.R;
import gliphic.android.adapters.AlertsAdapter;
import gliphic.android.display.welcome_screen.SignInActivity;
import gliphic.android.operation.Alerts;
import gliphic.android.operation.Group;
import gliphic.android.operation.server_interaction.http_server.HttpOperations;
import gliphic.android.operation.storage_handlers.IsStoredAndDataObject;
import gliphic.android.operation.storage_handlers.SharedPreferencesHandler;
import gliphic.android.utils.AndroidTestUtils;
import gliphic.android.utils.view_assertions.AssertRecyclerViewItemCount;
import gliphic.android.utils.MainActivityBaseSetup;
import gliphic.android.utils.SignInAndOutTestRule;
import gliphic.android.with_http_server.single_fragment_activity.RegisterActivityTest;

import org.bouncycastle.util.encoders.Base64;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.action.ViewActions.swipeRight;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static gliphic.android.utils.AndroidTestUtils.getOnViewInteractionFromId;
import static gliphic.android.utils.AndroidTestUtils.getOnViewInteractionFromString;
import static gliphic.android.utils.AndroidTestUtils.swipeUp;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class SignInAndOutTest {

    @Rule
    public SignInAndOutTestRule<SignInActivity> rule = new SignInAndOutTestRule<>(SignInActivity.class);

    private static final long TEST_CONTACT_NUMBER = 0;
    private static final String VALID_EMAIL = RegisterActivityTest.duplicateEmail;
    private static final String VALID_PWD = RegisterActivityTest.validPwd;

    private static String generateEmailAddress(long contactNumber) {
        String emailAddress = VALID_EMAIL;

        if (contactNumber > 0) {
            emailAddress = String.format("%d%s", contactNumber, emailAddress);
        }

        return emailAddress;
    }

    /**
     * Attempt to sign in but do nothing and throw no exception if signing in fails because the contact is already
     * signed in.
     *
     * @see #signIn()
     */
    public static void safeSignIn() throws Throwable {
        try {
            signIn();
        }
        catch (NoMatchingViewException e) {
            final String msg = e.getMessage();

            if (msg == null || !msg.contains("sign_in")) {
                throw e;
            }
        }
    }

    /**
     * Sign in with the server using the standard test contact.
     */
    public static void signIn() throws Throwable {
        signIn(TEST_CONTACT_NUMBER);
    }

    /**
     * Sign in with the server for a given contact number
     */
    public static void signIn(long contactNumber) throws Throwable {
        final String emailAddress = generateEmailAddress(contactNumber);

        onView(withId(R.id.sign_in_email)).perform(replaceText(emailAddress));
        onView(withId(R.id.sign_in_pwd)).perform(replaceText(VALID_PWD));
        onView(withId(R.id.btn_sign_in)).perform(click());

        assertSignInViewsNotClickable();

        /*
         * Allow time for the contact to sign in.
         *
         * Note that since the amount of time it takes for the contact to sign-in is variable and dependant on how long
         * it takes to regenerate the group key-encryption keypair, it is possible that this amount of time is not long
         * enough, therefore some tests may fail because of an assumed sign-in which has not yet completed.
         */
        AndroidTestUtils.waitForAction(AndroidTestUtils.WaitAction.SIGN_IN);
    }

    /**
     * Sign in with the server using the standard test contact and a sign-in code.
     */
    public static void signInWithCode(boolean navigateToSubmitCodeActivity, long contactNumber) throws Throwable {
        final String emailAddress = generateEmailAddress(contactNumber);
        final String signInCode   = MainActivityBaseSetup.getValidSignInToken(contactNumber);

        if (navigateToSubmitCodeActivity) {
            onView(withId(R.id.textview_submit_code)).perform(click());
            SystemClock.sleep(1000);

            getOnViewInteractionFromString(R.string.submit_code_sign_in).perform(click());
            SystemClock.sleep(500);
        }

        getOnViewInteractionFromId(R.id.submit_code_edittext_2).perform(replaceText(emailAddress));
        getOnViewInteractionFromId(R.id.submit_code_edittext_3).perform(replaceText(VALID_PWD));
        getOnViewInteractionFromId(R.id.edittext_submit_code).perform(replaceText(signInCode));

//        getOnViewInteraction(R.id.btn_submit_code).perform(NestedScrollTo.nestedScrollTo());
        getOnViewInteractionFromId(R.id.viewpager_activity_base).perform(swipeUp());
        SystemClock.sleep(200);
        getOnViewInteractionFromId(R.id.btn_submit_code).perform(click());

        assertSubmitTokenViewsNotClickable();

        AndroidTestUtils.waitForAction(AndroidTestUtils.WaitAction.SIGN_IN);
    }

    /**
     * Sign out with the server.
     */
    public static void signOut() {
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText(R.string.action_bar_sign_out)).perform(click());    // Click sign-out.
        onView(withId(android.R.id.button1)).perform(click());              // Confirm sign-out.

        // Allow time for the contact to sign out.
        SystemClock.sleep(500);
    }

    /**
     * Simulate signing out by removing all contact data.
     *
     * This can be done without a connection to the server or during the test lifecycle e.g. @AfterClass.
     */
    public static void removeAllContactData() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferencesHandler.removeAllContactData(context);

        // Ensure that the above is processed before proceeding to the next test class.
        SystemClock.sleep(200);
    }

    public static void assertSignInViewsNotClickable() {
        SystemClock.sleep(500);

        final List<Integer> signInViews = Arrays.asList(
                R.id.sign_in_email,
                R.id.sign_in_pwd,
                R.id.sign_in_remember_email,
                R.id.textview_register_new_account,
                R.id.textview_submit_code,
                R.id.textview_request_activation_code,
                R.id.textview_request_recovery_code,
                R.id.textview_terms_of_use,
                R.id.textview_activation_code_help,
                R.id.textview_recovery_code_help
        );

        for (int id : signInViews) {
            try {
                onView(withId(id)).perform(click());
//                onView(withId(id)).check(matches(not(isClickable())));

                fail("Expected an instance of NoMatchingViewException to be thrown.");
            }
            catch (NoMatchingViewException | PerformException e) {
                // This is the expected code path.
            }
        }
    }

    public static void assertSubmitTokenViewsNotClickable() {
        SystemClock.sleep(500);

        final List<Integer> submitTokenViews = Arrays.asList(
                R.id.edittext_submit_code,
                R.id.submit_code_edittext_2,
                R.id.submit_code_edittext_3,
                R.id.submit_code_checkbox,
                R.id.btn_submit_code
        );

        for (int id : submitTokenViews) {
            for (int i = 0; i < 3; i++) {
                try {
                    onView(AndroidTestUtils.withIndex(withId(id), i)).perform(click());

                    fail("Expected an instance of NoMatchingViewException to be thrown.");
                }
                catch (NoMatchingViewException e) {
                    // This is the expected code path.
                }
            }
        }
    }

    // Used with the test below.
    private IsStoredAndDataObject deviceKey = null;
    private IsStoredAndDataObject deviceCode = null;

    @Test
    public void signInWithAndWithoutDeviceKeyAndCode() throws Throwable {
        try {
            // Test sign-in with an initial device key/code stored.

            Context context = AndroidTestUtils.getApplicationContext();
            try {
                deviceKey = SharedPreferencesHandler.getDeviceKey(context, true);
                deviceCode = SharedPreferencesHandler.getDeviceCode(context, true);
            }
            catch (Throwable e) {
                fail(e.getMessage());
            }

            SystemClock.sleep(1000);    // Give the thread time to complete.
            signIn();

            // Test sign-in without an initial device key/code stored (they should be generated before signing in).

            signOut();

            context = AndroidTestUtils.getApplicationContext();
            try {
                SharedPreferencesHandler.removeDeviceKey(context);
                SharedPreferencesHandler.removeDeviceCode(context);
            }
            catch (Throwable e) {
                fail(e.getMessage());
            }

            SystemClock.sleep(1000);    // Give the thread time to complete.

            onView(withId(R.id.sign_in_email)).perform(replaceText(VALID_EMAIL));
            onView(withId(R.id.sign_in_pwd)).perform(replaceText(VALID_PWD));
            onView(withId(R.id.btn_sign_in)).perform(click());
            SystemClock.sleep(2000);    // Allow time for the server to respond.
            onView(withText("Sign-in code required")).check(matches(isDisplayed()));
            onView(withId(android.R.id.button2)).perform(click());
        }
        finally {
            Context context = AndroidTestUtils.getApplicationContext();
            try {
                if (deviceKey != null && deviceKey.wasStored()) {
                    SharedPreferencesHandler.setDeviceKey(context, deviceKey.getData());
                }

                if (deviceCode != null && deviceCode.wasStored()) {
                    SharedPreferencesHandler.setDeviceCode(context, deviceCode.getData());
                }
            }
            catch (Throwable e) {
                fail(e.getMessage());
            }

            SystemClock.sleep(1000);    // Give the thread time to complete.
        }
    }

    @Ignore(AndroidTestUtils.IGNORE_ANNOTATION_NO_API_KEY)
    @Test
    public void signInCodeRequiredRequestCode() throws Exception {
        try {
            handleSignInCodeRequiredTest("Sign-in code required");
            onView(withId(android.R.id.button1)).perform(click());
            SystemClock.sleep(2000);    // Allow time for the server to respond.
            onView(withText("Code request successful")).check(matches(isDisplayed()));
            onView(withId(android.R.id.button1)).perform(click());
        }
        finally {
            AndroidTestUtils.postString(
                    AndroidTestUtils.getUriPrefix() + "/account/set-sign-in-email-time/" + TEST_CONTACT_NUMBER,
                    Long.toString(0)
            );
        }
    }

    @Test
    public void signInCodeRequiredLastEmailTooRecent() throws Exception {
        final String uri = AndroidTestUtils.getUriPrefix() + "/account/set-sign-in-email-time/" + TEST_CONTACT_NUMBER;

        try {
            AndroidTestUtils.postString(uri, Long.toString(System.currentTimeMillis()));

            handleSignInCodeRequiredTest(HttpOperations.ERROR_MSG_429_SIGN_IN);
            onView(withId(android.R.id.button1)).perform(click());
        }
        finally {
            AndroidTestUtils.postString(uri, Long.toString(0));
        }
    }

    @Test
    public void signInCodeRequiredRegisteredButInactiveAccount() throws Throwable {
        final String emailAddress = AndroidTestUtils.CONTACT_REGISTERED_INACTIVE + VALID_EMAIL;

        handleSignInCodeRequiredTest(HttpOperations.ERROR_MSG_401_BAD_CREDENTIALS, emailAddress);
        onView(withId(android.R.id.button1)).perform(click());
    }

    private void handleSignInCodeRequiredTest(String expectedMessage) throws Exception {
        handleSignInCodeRequiredTest(expectedMessage, VALID_EMAIL);
    }

    private void handleSignInCodeRequiredTest(String expectedMessage, String emailAddress) throws Exception {
        try {
            AndroidTestUtils.postString(
                    AndroidTestUtils.getUriPrefix() + "/account/set-bad-device-code",
                    Long.toString(TEST_CONTACT_NUMBER)
            );

            onView(withId(R.id.sign_in_email)).perform(replaceText(emailAddress));
            onView(withId(R.id.sign_in_pwd)).perform(replaceText(VALID_PWD));
            onView(withId(R.id.btn_sign_in)).perform(click());
            SystemClock.sleep(2000);    // Allow time for the server to respond.

            onView(withText(expectedMessage)).check(matches(isDisplayed()));
        }
        finally {
            AndroidTestUtils.postString(
                    AndroidTestUtils.getUriPrefix() + "/account/set-device-code/" + TEST_CONTACT_NUMBER,
                    Base64.toBase64String(AndroidTestUtils.getDeviceCode())
            );
        }
    }

    @Test
    public void userIsNotSignedIn() {
        try {
            assertThat(SharedPreferencesHandler.isUserSignedIn(AndroidTestUtils.getApplicationContext()),
                    is(false));
        }
        catch (Throwable e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void validSignInAndOutWithExpiredAccessToken() throws Throwable {
        // Signing in sets a new access token expiry date so sign in before setting it.
        signIn();

        // Test the SharedPreferencesHandler.isUserSignedIn() method positive test case.
        try {
            assertThat(SharedPreferencesHandler.isUserSignedIn(AndroidTestUtils.getApplicationContext()),
                    is(true));
        }
        catch (Throwable e) {
            fail(e.getMessage());
        }

        MainActivityBaseSetup.setTokenDataContact0(true);

        signOut();
        // Check that regenerated data does not prevent the contact from signing in again.
        signIn();

        // Clean up the test by signing out.
        signOut();
    }

    @Test
    public void validSignInWithObsoleteGroupShares() throws Throwable {
        testModifiedDatabaseGroupShares(true);
    }

    @Test
    public void validSignInWithInvalidGroupShareKey() throws Throwable {
        testModifiedDatabaseGroupShares(false);
    }

    private void testModifiedDatabaseGroupShares(boolean insertGroupSharesWithValidEncryptedKey) throws Throwable {
        final int    PENDING_RECEIVED_GROUP_SHARES_LIMIT = 100;     // Copied directly from the server.
        final String testContactNumberString  = Long.toString(TEST_CONTACT_NUMBER);
        final String uriPrefix                = AndroidTestUtils.getUriPrefix() + "/load/";
        final String uriSuffix                = insertGroupSharesWithValidEncryptedKey ?
                "insert-pending-received-group-shares" :
                "insert-group-shares-with-invalid-encrypted-key";

        AndroidTestUtils.postString(uriPrefix + uriSuffix, testContactNumberString);

        try {
            // Ensure that the number of shares loaded is greater than the number returned by the sign-in operation
            // i.e. the additional shares must be obsolete group shares (with their encrypted group key updated).
            final int originalNumOfPendingReceivedShares = 1;
            final int numOfInsertedPendingReceivedShares = PENDING_RECEIVED_GROUP_SHARES_LIMIT + 1;
            final int totalLoadedGroupShares = originalNumOfPendingReceivedShares + numOfInsertedPendingReceivedShares;

            final int numOfLoadedShares    = insertGroupSharesWithValidEncryptedKey ?
                    totalLoadedGroupShares : originalNumOfPendingReceivedShares;
            final int numOfDisplayedShares = insertGroupSharesWithValidEncryptedKey ?
                    AlertsAdapter.MAX_NUM_OF_ITEMS_APPENDED : originalNumOfPendingReceivedShares;

            SystemClock.sleep(2000);
            signIn();
            SystemClock.sleep(10000);   // Allow time for sign-in and the main activity to load.
            assertThat(Alerts.getGroupShares().size(), is(numOfLoadedShares));

            // If the additional encrypted group keys inserted into the database are valid then assert that the
            // RecyclerView displays the maximum number of items which can be loaded at one time, else assert that
            // these failed group-shares are not displayed.
            onView(withId(R.id.viewpager_activity_base)).perform(swipeRight());
            SystemClock.sleep(2000);
            onView(withId(R.id.recyclerview_main_tab_alerts))
                    .check(new AssertRecyclerViewItemCount(numOfDisplayedShares));

            signOut();
        }
        finally {
            AndroidTestUtils.postString(uriPrefix + "delete-inserted-group-shares", testContactNumberString);
            SystemClock.sleep(2000);
        }
    }

    // Signing out does not (currently) require a refresh token.
//    @Test
//    public void invalidRefreshTokenSignOut() {
//        // Signing in sets a new access token expiry date so sign in before setting it.
//        signIn();
//
//        MainActivityBaseSetup.setTokenDataContact0(true);
//
//        try {
//            KeyAndTokenStore.refreshToken = "invalid refresh token";
//
//            signOut();
//
//            onView(withText(HttpOperations.ERROR_MSG_401_BAD_TOKEN)).check(matches(isDisplayed()));
//        }
//        finally {
//            KeyAndTokenStore.refreshToken  = MainActivityBaseSetup.validRefreshTokenContact0;
//
//            onView(withId(android.R.id.button1)).perform(click());
//            signOut();
//        }
//    }

    @Test
    public void signOutDialogDisplaysNewContactName() throws Throwable {
        signIn();

        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText(R.string.action_bar_contact_profile)).perform(click());

        onView(ViewMatchers.withId(R.id.contact_profile_name)).perform(click());
        SystemClock.sleep(500);    // The EditText needs to be in focus.
        onView(ViewMatchers.withId(R.id.contact_profile_name))
                .perform(replaceText(RegisterActivityTest.validNewName));
        onView(withId(R.id.contact_profile_name)).perform(pressImeActionButton());
        SystemClock.sleep(500);
        onView(withId(android.R.id.button1)).perform(click());
        SystemClock.sleep(2000);
        onView(withId(android.R.id.button1)).perform(click());
        pressBack();
        SystemClock.sleep(500);

        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText(R.string.action_bar_sign_out)).perform(click());
        SystemClock.sleep(500);
        String signOutMsg = "Are you sure you want to sign out %s?";
        onView(withText(String.format(signOutMsg, RegisterActivityTest.validNewName)))
                .check(matches(isDisplayed()));
        onView(withId(android.R.id.button2)).perform(click());

        // Revert the contact name and repeat the same check.
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText(R.string.action_bar_contact_profile)).perform(click());

        onView(ViewMatchers.withId(R.id.contact_profile_name)).perform(click());
        SystemClock.sleep(500);    // The EditText needs to be in focus.
        onView(ViewMatchers.withId(R.id.contact_profile_name))
                .perform(replaceText(RegisterActivityTest.validExistingName));
        onView(withId(R.id.contact_profile_name)).perform(pressImeActionButton());
        SystemClock.sleep(500);
        onView(withId(android.R.id.button1)).perform(click());
        SystemClock.sleep(2000);
        onView(withId(android.R.id.button1)).perform(click());
        pressBack();
        SystemClock.sleep(500);

        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText(R.string.action_bar_sign_out)).perform(click());
        SystemClock.sleep(500);
        onView(withText(String.format(signOutMsg, RegisterActivityTest.validExistingName)))
                .check(matches(isDisplayed()));
        onView(withId(android.R.id.button2)).perform(click());

        signOut();
    }

    @Test
    public void signOutDialogDisplaysNewGroupNameAndDescription() throws Throwable {
        // Bug fix test: Check that the group name and description change persists after signing out and in again.

        signIn();

        int expectedGroupIndex = 1;
        String newGroupName = "some valid group name";
        String newGroupDesc = "some valid group description";
        String oldGroupName = Group.getKnownGroups().get(expectedGroupIndex).getName();
        String oldGroupDesc = Group.getKnownGroups().get(expectedGroupIndex).getDescription();

        // Navigate to the group settings activity.
        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
        SystemClock.sleep(500);    // Give the swipe time to change tab focus and find R.id.*
        onView(withId(R.id.recyclerview_main_tab_groups))
                .perform(RecyclerViewActions.actionOnItemAtPosition(expectedGroupIndex, click()));

        // Change the group name.
        onView(ViewMatchers.withId(R.id.group_details_name)).perform(click());
        SystemClock.sleep(500);    // The EditText needs to be in focus.
        onView(ViewMatchers.withId(R.id.group_details_name)).perform(replaceText(newGroupName));
        onView(withId(R.id.group_details_name)).perform(pressImeActionButton());
        SystemClock.sleep(500);
        onView(withId(android.R.id.button1)).perform(click());
        SystemClock.sleep(2000);
        onView(withId(android.R.id.button1)).perform(click());

        // Change the group description.
        onView(ViewMatchers.withId(R.id.group_details_description)).perform(click());
        SystemClock.sleep(500);    // The EditText needs to be in focus.
        onView(ViewMatchers.withId(R.id.group_details_description)).perform(replaceText(newGroupDesc));
        onView(withId(R.id.group_details_description)).perform(pressImeActionButton());
        SystemClock.sleep(500);
        onView(withId(android.R.id.button1)).perform(click());
        SystemClock.sleep(2000);
        onView(withId(android.R.id.button1)).perform(click());

        pressBack();
        SystemClock.sleep(500);

        signOut();

        signIn();

        /* Revert the group name and description and repeat the same check. */

        // Navigate to the group settings activity.
        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
        SystemClock.sleep(500);    // Give the swipe time to change tab focus and find R.id.*
        onView(withId(R.id.recyclerview_main_tab_groups))
                .perform(RecyclerViewActions.actionOnItemAtPosition(expectedGroupIndex, click()));

        // Check that the new group name and description are on display.
        onView(ViewMatchers.withId(R.id.group_details_name)).check(matches(withText(newGroupName)));
        onView(ViewMatchers.withId(R.id.group_details_description)).check(matches(withText(newGroupDesc)));

        // Revert the group name.
        onView(ViewMatchers.withId(R.id.group_details_name)).perform(click());
        SystemClock.sleep(500);    // The EditText needs to be in focus.
        onView(ViewMatchers.withId(R.id.group_details_name)).perform(replaceText(oldGroupName));
        onView(withId(R.id.group_details_name)).perform(pressImeActionButton());
        SystemClock.sleep(500);
        onView(withId(android.R.id.button1)).perform(click());
        SystemClock.sleep(2000);
        onView(withId(android.R.id.button1)).perform(click());

        // Revert the group description.
        onView(ViewMatchers.withId(R.id.group_details_description)).perform(click());
        SystemClock.sleep(500);    // The EditText needs to be in focus.
        onView(ViewMatchers.withId(R.id.group_details_description)).perform(replaceText(oldGroupDesc));
        onView(withId(R.id.group_details_description)).perform(pressImeActionButton());
        SystemClock.sleep(500);
        onView(withId(android.R.id.button1)).perform(click());
        SystemClock.sleep(2000);
        onView(withId(android.R.id.button1)).perform(click());

        pressBack();
        SystemClock.sleep(500);

        signOut();
    }

    /* Invalid get auth salt
     *
     * Note that these tests also cover activation token request operations.
     */

    @Test
    public void badEmail() {
        onView(withId(R.id.sign_in_email)).perform(replaceText("unknown@test.com"));
        onView(withId(R.id.sign_in_pwd)).perform(replaceText(VALID_PWD));
        onView(withId(R.id.btn_sign_in)).perform(click());

        SystemClock.sleep(1000);

        onView(withText(HttpOperations.ERROR_MSG_401_BAD_CREDENTIALS)).check(matches(isDisplayed()));
    }

    @Test
    public void badPassword() {
        onView(withId(R.id.sign_in_email)).perform(replaceText(VALID_EMAIL));
        onView(withId(R.id.sign_in_pwd)).perform(replaceText("Incorrect pwd"));
        onView(withId(R.id.btn_sign_in)).perform(click());

        SystemClock.sleep(1000);

        onView(withText(HttpOperations.ERROR_MSG_401_BAD_CREDENTIALS)).check(matches(isDisplayed()));
    }

    /* Invalid sign in */

    @Test
    public void accountInactive() throws Exception {
        try {
            MainActivityBaseSetup.deactivateAccount(TEST_CONTACT_NUMBER);
            SystemClock.sleep(500);     // Allow time for the server to respond.

            onView(withId(R.id.sign_in_email)).perform(replaceText(VALID_EMAIL));
            onView(withId(R.id.sign_in_pwd)).perform(replaceText(VALID_PWD));
            onView(withId(R.id.btn_sign_in)).perform(click());
            SystemClock.sleep(2000);    // Allow time for the server to respond.

            onView(withText(HttpOperations.ERROR_MSG_403_ACCOUNT_INACTIVE)).check(matches(isDisplayed()));
        }
        finally {
            MainActivityBaseSetup.activateAccount(TEST_CONTACT_NUMBER);
        }
    }

    // Invalid JSONObject sent to the server is difficult to test (not tested).

    // Invalid new encryption on group keys to send to the server is difficult to test (not tested).

    // Invalid new JSONObject sent to the server is difficult to test (not tested).

    // AES/RSA-encrypted group keys are removed from the group/group-shares static lists upon sign-in completion.

    /* Invalid update crypto data */

    // Currently nothing to test upon receiving a response from a update crypto data request.
}
