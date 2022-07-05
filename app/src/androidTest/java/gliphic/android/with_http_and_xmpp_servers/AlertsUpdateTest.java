package gliphic.android.with_http_and_xmpp_servers;

import android.os.SystemClock;
import android.view.View;

import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import gliphic.android.R;
import gliphic.android.display.welcome_screen.SignInActivity;
import gliphic.android.operation.Group;
import gliphic.android.operation.server_interaction.http_server.HttpOperations;
import gliphic.android.operation.storage_handlers.SharedPreferencesHandler;
import gliphic.android.utils.AndroidTestUtils;
import gliphic.android.utils.view_assertions.AssertRecyclerViewItemCount;
import gliphic.android.utils.MainActivityBaseSetup;
import gliphic.android.utils.view_actions.CheckBoxSelection;
import gliphic.android.utils.view_actions.RecyclerViewAction;
import gliphic.android.with_http_server.functionality.SignInAndOutTest;
import gliphic.android.with_http_server.single_fragment_activity.AddContactActivityTest;

import org.bouncycastle.util.encoders.Base64;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import libraries.GeneralUtils;
import libraries.Vars;
import pojo.group.ShareGroupGetKeyRequest;
import pojo.group.ShareGroupGetKeyResponse;
import pojo.group.ShareGroupRequest;
import pojo.misc.ContactAndGroupNumberPair;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.swipeRight;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class AlertsUpdateTest {

    @Rule
    public ActivityScenarioRule<SignInActivity> rule = new ActivityScenarioRule<>(SignInActivity.class);

    private static final int TOTAL_DISPLAYED_ITEMS = 2;

    private static final long TEST_CONTACT_NUMBER = 1;
    private static final long NEW_GROUP_NUMBER    = 3;
    private static final long TEST_GROUP_NUMBER   = 1950;
    private static final String VALID_DESCRIPTION_1 = "unique valid description 1";
    private static final String VALID_DESCRIPTION_2 = "unique valid description 2";

    private static final ContactAndGroupNumberPair TEST_CONTACT_AND_GROUP_NUMBERS =
            new ContactAndGroupNumberPair(TEST_CONTACT_NUMBER, TEST_GROUP_NUMBER);

    private static final ContactAndGroupNumberPair CONTACT_REGISTERED_INACTIVE_AND_GROUP_NUMBERS =
            new ContactAndGroupNumberPair(AndroidTestUtils.CONTACT_REGISTERED_INACTIVE, TEST_GROUP_NUMBER);

    private static final ContactAndGroupNumberPair CONTACT_INACTIVE_AND_GROUP_NUMBERS =
            new ContactAndGroupNumberPair(AndroidTestUtils.CONTACT_INACTIVE, TEST_GROUP_NUMBER);

    private static final Matcher<View> RECYCLER_VIEW = withId(R.id.recyclerview_main_tab_alerts);

    private void navigateToAlertsTab(boolean clickCheckBox) {
        onView(withId(R.id.viewpager_activity_base)).perform(swipeRight());
        SystemClock.sleep(200);

        if (clickCheckBox) {
            onView(withId(R.id.checkbox_alerts)).perform(click());
            SystemClock.sleep(1000);
        }
    }

    private void databaseSetUp() throws Exception {
        // When this group-share is loaded, it should actually load two shares: one pending-sent and one
        // pending-received.
        AndroidTestUtils.postObject(
                AndroidTestUtils.getUriPrefix() + "/load/insert-same-target-and-source-group-share",
                TEST_CONTACT_AND_GROUP_NUMBERS
        );
    }

    private void databaseCleanUp() throws Exception {
        AndroidTestUtils.postObject(
                AndroidTestUtils.getUriPrefix() + "/load/delete-same-target-and-source-group-share",
                TEST_CONTACT_AND_GROUP_NUMBERS
        );
        SystemClock.sleep(2000);
    }

    private void checkNumberIsAlertsTabName(int tabName) {
        SystemClock.sleep(200);
        onView(withText(Integer.toString(tabName))).check(matches(isDisplayed()));
    }

    private void matchDisplayedTextAndAlertsTabTitle(String matchExactText, String matchContainsText) {
        final long millisecondsDelay = 100;
        for (long millisecondsWaited = 0; true; millisecondsWaited += millisecondsDelay) {
            try {
                if (matchExactText != null) {
                    onView(withText(matchExactText)).check(matches(isDisplayed()));
                }
                if (matchContainsText != null) {
                    onView(withText(containsString(matchContainsText))).check(matches(isDisplayed()));
                }

                // By this point either an exception is thrown or the previous assertion passed.

                onView(withId(android.R.id.button1)).perform(click());
                SystemClock.sleep(200);
                checkNumberIsAlertsTabName(0);

                break;
            }
            catch (AssertionError e) {
                if (millisecondsWaited > 3000) {
                    throw e;
                }
                else {
                    SystemClock.sleep(millisecondsDelay);
                }
            }
        }
    }

    private void setDataEncryptionKey() {
        rule.getScenario().onActivity(activity -> {
            try {
                AndroidTestUtils.setDataEncryptionKey(activity, TEST_CONTACT_NUMBER);
            }
            catch (Exception e) {
                fail(e.getMessage());
            }
        });
    }

    private static String originalDeviceCodeString;

    @BeforeClass
    public static void beforeClass() throws Exception {
        originalDeviceCodeString = MainActivityBaseSetup.getDeviceCode(TEST_CONTACT_NUMBER);
        MainActivityBaseSetup.setDeviceCode(TEST_CONTACT_NUMBER, AndroidTestUtils.getDeviceCode());
    }

    @AfterClass
    public static void afterClass() throws Exception {
        MainActivityBaseSetup.setDeviceCode(TEST_CONTACT_NUMBER, originalDeviceCodeString);
    }

    @Before
    public void before() {
        rule.getScenario().onActivity(SharedPreferencesHandler::removeAllContactData);
    }

    @After
    public void after() {
        SignInAndOutTest.signOut();
        SystemClock.sleep(1000);
    }

    @Test
    public void receivedGroupRequest() throws Throwable {
        final long contactNumber = 2;

        SignInAndOutTest.signIn(TEST_CONTACT_NUMBER);

        checkNumberIsAlertsTabName(0);

        navigateToAlertsTab(false);

        AndroidTestUtils.waitForAction(AndroidTestUtils.WaitAction.XMPP_AUTH);

        // Get the target contact's public key (i.e. the contact currently signed-in); the group key is not required.

        final ShareGroupGetKeyRequest shareGroupGetKeyRequest = new ShareGroupGetKeyRequest(
                MainActivityBaseSetup.getValidAccessToken(contactNumber),
                TEST_CONTACT_NUMBER,
                NEW_GROUP_NUMBER
        );

        String responseString = AndroidTestUtils.postString(
                HttpOperations.URI_GROUP_SHARE_GET_KEYS,
                GeneralUtils.toJson(shareGroupGetKeyRequest)
        );

        final ShareGroupGetKeyResponse sggkr = GeneralUtils.fromJson(
                responseString,
                ShareGroupGetKeyResponse.class
        );

        // Create and submit a group share request.

        final ShareGroupRequest shareGroupRequest = new ShareGroupRequest(
                MainActivityBaseSetup.getValidAccessToken(contactNumber),
                TEST_CONTACT_NUMBER,
                NEW_GROUP_NUMBER,
                Base64.toBase64String(new byte[Vars.PUB_ENC_AES_KEY_LEN]),  // The group key does not need to be valid.
                sggkr.getPublicKeyString()
        );

        final String expectedString = String.format(
                "(contact ID %s)",
                AddContactActivityTest.postStringContactIdFromNumber(Long.toString(contactNumber))
        );

        // Check that there is no matching pending entry before the XMPP message is received.
        onView(RECYCLER_VIEW).check(matches(not(hasDescendant(withText(containsString(expectedString))))));

        AndroidTestUtils.postString(
                HttpOperations.URI_GROUP_SHARE_SUBMIT,
                GeneralUtils.toJson(shareGroupRequest)
        );

        try {
            SystemClock.sleep(5000);

            // Check that an XMPP message has been received and the display is updated.
            onView(RECYCLER_VIEW).check(matches(hasDescendant(withText(containsString(expectedString)))));

            checkNumberIsAlertsTabName(1);
        }
        finally {
            AndroidTestUtils.postObject(
                    AndroidTestUtils.getUriPrefix() + "/group/delete-specific-group-share",
                    new ContactAndGroupNumberPair(TEST_CONTACT_NUMBER, NEW_GROUP_NUMBER)
            );
        }
    }

    @Test
    public void sentAndReceiveGroupRequestAcceptSuccessful() throws Throwable {
        // BUG FIX: Avoid the error "MAC check failed" after accepting the group request and receiving the RSA keypair
        // from the server, by ensuring that the contact's encrypted private key does not fail to decrypt.
        setDataEncryptionKey();

        final String uriGroupPrefix = AndroidTestUtils.getUriPrefix() + "/group/";
        final List<ContactAndGroupNumberPair> contactAndGroupNumberPairList = Arrays.asList(
                CONTACT_REGISTERED_INACTIVE_AND_GROUP_NUMBERS,
                CONTACT_INACTIVE_AND_GROUP_NUMBERS
        );

        try {
            databaseSetUp();

            // Setup for additional test.
            for (ContactAndGroupNumberPair contactAndGroupNumberPair : contactAndGroupNumberPairList) {
                AndroidTestUtils.postObject(
                        uriGroupPrefix + "add-invalid-contact-group",
                        contactAndGroupNumberPair
                );
            }

            SignInAndOutTest.signIn(TEST_CONTACT_NUMBER);

            checkNumberIsAlertsTabName(1);

            navigateToAlertsTab(true);

            AndroidTestUtils.waitForAction(AndroidTestUtils.WaitAction.XMPP_AUTH);

            // Assert the item count before accepting the request.
            onView(RECYCLER_VIEW).check(new AssertRecyclerViewItemCount(TOTAL_DISPLAYED_ITEMS));

            // Accept the request.
            onView(RECYCLER_VIEW).perform(RecyclerViewActions.actionOnItemAtPosition(
                    0,  // The first item should be the pending-received group-share.
                    RecyclerViewAction.clickChildViewWithId(R.id.alert_item_accept)
            ));

            // Additional test: an invalid group description displays an error message in the same AlertDialog.
            // Note that only one invalid description is tested since more comprehensive unit tests already exist.
            SystemClock.sleep(200);
            onView(withId(android.R.id.button1)).perform(click());
            SystemClock.sleep(200);
            onView(withText(containsString(Group.DESCRIPTION_EMPTY_MSG))).check(matches(isDisplayed()));

            SystemClock.sleep(200);
            onView(withId(R.id.alertdialog_edittext)).perform(replaceText(VALID_DESCRIPTION_1));
            SystemClock.sleep(200);
            onView(withId(android.R.id.button1)).perform(click());
            SystemClock.sleep(200);

            // The Alerts tab name needs to be asserted before the XMPP message is received hence the constant check.
            matchDisplayedTextAndAlertsTabTitle("Accept request successful", null);

            // Assert that the displayed items are updated.

            SystemClock.sleep(3000);
            onView(RECYCLER_VIEW).check(new AssertRecyclerViewItemCount(TOTAL_DISPLAYED_ITEMS));
            onView(RECYCLER_VIEW).check(matches(hasDescendant(withText(containsString("You joined group")))));
            onView(RECYCLER_VIEW).check(matches(hasDescendant(withText(containsString("You shared group")))));

            // Additional test: inactive target contacts should not be returned in the list of known/extended contacts
            // for the accepted group.
            onView(ViewMatchers.withText(R.string.main_groups)).perform(click());
            SystemClock.sleep(200);
            onView(withId(R.id.edittext_main_tab_groups)).perform(replaceText(Long.toString(TEST_GROUP_NUMBER)));
            SystemClock.sleep(200);
            onView(withId(R.id.recyclerview_main_tab_groups))
                    .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
            onView(ViewMatchers.withText(R.string.group_contacts)).perform(click());
            SystemClock.sleep(200);
            onView(withId(R.id.checkbox_group_extended_contacts)).perform(CheckBoxSelection.setChecked(true));
            onView(withId(R.id.textview_load_more_group_contacts)).perform(click());
            SystemClock.sleep(200);
            onView(withId(R.id.recyclerview_group_details_tab_target_contacts))
                    .check(new AssertRecyclerViewItemCount(1));
        }
        finally {
            databaseCleanUp();

            for (ContactAndGroupNumberPair contactAndGroupNumberPair : contactAndGroupNumberPairList) {
                AndroidTestUtils.postObject(
                        uriGroupPrefix + "remove-temporary-contact-group",
                        contactAndGroupNumberPair
                );
            }
        }
    }

    @Test
    public void sentAndReceiveGroupRequestAcceptFailed() throws Throwable {
        // BUG FIX: Avoid the error "MAC check failed" after accepting the group request and receiving the RSA keypair
        // from the server, by ensuring that the contact's encrypted private key does not fail to decrypt.
        setDataEncryptionKey();

        try {
            databaseSetUp();

            SignInAndOutTest.signIn(TEST_CONTACT_NUMBER);

            AndroidTestUtils.postObject(
                    AndroidTestUtils.getUriPrefix() +
                            "/load/set-invalid-encrypted-key-for-same-target-and-source-group-share",
                    TEST_CONTACT_AND_GROUP_NUMBERS
            );

            checkNumberIsAlertsTabName(1);

            navigateToAlertsTab(true);

            AndroidTestUtils.waitForAction(AndroidTestUtils.WaitAction.XMPP_AUTH);

            // Assert the item count before accepting the request.
            onView(RECYCLER_VIEW).check(new AssertRecyclerViewItemCount(TOTAL_DISPLAYED_ITEMS));

            // Accept the request.
            onView(RECYCLER_VIEW).perform(RecyclerViewActions.actionOnItemAtPosition(
                    0,  // The first item should be the pending-received group-share.
                    RecyclerViewAction.clickChildViewWithId(R.id.alert_item_accept)
            ));

            SystemClock.sleep(200);
            onView(withId(R.id.alertdialog_edittext)).perform(replaceText(VALID_DESCRIPTION_2));
            SystemClock.sleep(200);
            onView(withId(android.R.id.button1)).perform(click());
            SystemClock.sleep(200);

            // The Alerts tab name needs to be asserted before the XMPP message is received hence the constant check.
            matchDisplayedTextAndAlertsTabTitle(
                    "Accept request failed",
                    "You were unable to join group "
            );

            // Assert that the displayed items are updated.

            SystemClock.sleep(3000);

            onView(RECYCLER_VIEW)
                    .check(new AssertRecyclerViewItemCount(TOTAL_DISPLAYED_ITEMS - 1));

            final String s = " failed. This could be the result of malicious activity using your account.";
            onView(RECYCLER_VIEW).check(matches(hasDescendant(withText(containsString(s)))));
        }
        finally {
            databaseCleanUp();
        }
    }

    @Test
    public void sentAndReceiveGroupRequestDeclined() throws Throwable {
        try {
            databaseSetUp();

            SignInAndOutTest.signIn(TEST_CONTACT_NUMBER);

            checkNumberIsAlertsTabName(1);

            navigateToAlertsTab(true);

            AndroidTestUtils.waitForAction(AndroidTestUtils.WaitAction.XMPP_AUTH);

            // Decline the request.
            onView(RECYCLER_VIEW).perform(RecyclerViewActions.actionOnItemAtPosition(
                    0,  // The first item should be the pending-received group-share.
                    RecyclerViewAction.clickChildViewWithId(R.id.alert_item_decline)
            ));

            checkNumberIsAlertsTabName(0);

            SystemClock.sleep(5000);

            // Assert that the displayed items are removed.

            onView(RECYCLER_VIEW).check(new AssertRecyclerViewItemCount(TOTAL_DISPLAYED_ITEMS - 2));

            onView(withId(R.id.textview_no_alerts)).check(matches(isDisplayed()));
        }
        finally {
            databaseCleanUp();
        }
    }
}
