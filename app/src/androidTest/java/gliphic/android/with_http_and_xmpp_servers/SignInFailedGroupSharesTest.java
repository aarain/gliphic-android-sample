package gliphic.android.with_http_and_xmpp_servers;

import android.os.SystemClock;
import android.view.View;

import gliphic.android.R;
import gliphic.android.display.welcome_screen.SignInActivity;
import gliphic.android.operation.Alerts;
import gliphic.android.utils.AndroidTestUtils;
import gliphic.android.utils.view_assertions.AssertRecyclerViewItemCount;
import gliphic.android.utils.MainActivityBaseSetup;
import gliphic.android.utils.matchers.RecyclerViewMatcher;
import gliphic.android.utils.SignInAndOutTestRule;
import gliphic.android.utils.view_actions.NestedScrollTo;
import gliphic.android.with_http_server.functionality.SignInAndOutTest;
import gliphic.android.with_http_server.single_fragment_activity.RegisterActivityTest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import gliphic.android.utils.view_actions.SetViewVisibility;
import libraries.GeneralUtils;
import pojo.account.ContactCryptoDataUpdate;
import pojo.account.GroupShare;
import pojo.account.GroupSharesUpdate;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.swipeRight;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.StringContains.containsString;

@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class SignInFailedGroupSharesTest {

    @Rule
    public SignInAndOutTestRule<SignInActivity> rule = new SignInAndOutTestRule<>(SignInActivity.class);

    private static final int MAX_ITEM_LOAD = 50;
    private static final int MAX_EXPECTED_SHARE_TIME_FOR_INSERTED_GROUP_SHARES = 101;

    private static final long   SOURCE_CONTACT_NUMBER        = 1;
    private static final String SOURCE_CONTACT_NUMBER_STRING = Long.toString(SOURCE_CONTACT_NUMBER);

    private static final long   TARGET_CONTACT_NUMBER        = 2500;
    private static final String TARGET_CONTACT_NUMBER_STRING = Long.toString(TARGET_CONTACT_NUMBER);

    private enum SIMULATED_METHOD {UPDATE_CRYPTO_DATA, SYNCHRONIZE_PENDING_REQUESTS}

    private void navigateToAlertsTab() {
        onView(withId(R.id.viewpager_activity_base)).perform(swipeRight());
        SystemClock.sleep(200);
        onView(withId(R.id.checkbox_alerts)).perform(click());
        SystemClock.sleep(1000);
    }

    private void databaseSetUp() throws Exception {
        final String uriSuffix = "/load/insert-group-shares-as-source-with-invalid-encrypted-key";

        AndroidTestUtils.postString(AndroidTestUtils.getUriPrefix() + uriSuffix, SOURCE_CONTACT_NUMBER_STRING);
    }

    private void databaseCleanUp() throws Exception {
        AndroidTestUtils.getRequest(AndroidTestUtils.getUriPrefix() + "/load/delete-all-inserted-group-shares");
        SystemClock.sleep(2000);

        // TODO: Ensure that the target contact has their (crypto) data reverted.
        //       Note that this is currently ignored since the target contact is not important for any other tests, and
        //       reverting the data would require unnecessary copying and pasting static values on the server-side
        //       between the TestUtils method and the AppTests end-point.
    }

    private static String originalDeviceCodeString;

    @BeforeClass
    public static void beforeClass() throws Exception {
        originalDeviceCodeString = MainActivityBaseSetup.getDeviceCode(SOURCE_CONTACT_NUMBER);
        MainActivityBaseSetup.setDeviceCode(SOURCE_CONTACT_NUMBER, AndroidTestUtils.getDeviceCode());
    }

    @AfterClass
    public static void afterClass() throws Exception {
        MainActivityBaseSetup.setDeviceCode(SOURCE_CONTACT_NUMBER, originalDeviceCodeString);
    }

    @Test
    public void receivedXmppMessageFromUpdateCryptoData() throws Throwable {
        handleReceivedXmppMessageFromMethod(SIMULATED_METHOD.UPDATE_CRYPTO_DATA);
    }

    @Test
    public void receivedXmppMessageFromSynchronizePendingRequests() throws Throwable {
        handleReceivedXmppMessageFromMethod(SIMULATED_METHOD.SYNCHRONIZE_PENDING_REQUESTS);
    }

    private void signIn() throws Throwable {
        // Sign-in using a different email address so that only the group-shares in the set-up method are loaded.
        onView(withId(R.id.sign_in_email)).perform(replaceText(
                SOURCE_CONTACT_NUMBER_STRING + RegisterActivityTest.duplicateEmail
        ));
        onView(withId(R.id.sign_in_pwd)).perform(replaceText(RegisterActivityTest.validPwd));
        onView(withId(R.id.btn_sign_in)).perform(click());

        AndroidTestUtils.waitForAction(AndroidTestUtils.WaitAction.SIGN_IN);
    }

    public void handleReceivedXmppMessageFromMethod(SIMULATED_METHOD simulatedMethod) throws Throwable {
        databaseSetUp();

        try {
            // Ensure that any XMPP connection from a different contact is disconnected and a new connection with the
            // source contact number defined in this test-class is established.
            if (AndroidTestUtils.isExistingXmppConnection()) {
                signIn();
                SignInAndOutTest.signOut();
            }

            signIn();

            AndroidTestUtils.waitForAction(AndroidTestUtils.WaitAction.XMPP_AUTH);

            navigateToAlertsTab();

            // Assert pending group-shares are displayed.

            int numOfDisplayedShares = MAX_ITEM_LOAD;

            onView(withId(R.id.recyclerview_main_tab_alerts))
                    .check(new AssertRecyclerViewItemCount(numOfDisplayedShares));

            for (int i = 0; i < numOfDisplayedShares; i++) {
                onView(withId(R.id.recyclerview_main_tab_alerts))
                        .check(matches(RecyclerViewMatcher.atPosition(
                                i,
                                hasDescendant(withText(containsString("Pending:"))))
                        ));
            }

            // Simulate some other contact (not the current contact signed-in to the application) signing in and
            // updating this (source) contact which group-shares are failed.

            List<GroupShare> failedGroupShares = new ArrayList<>();
            for (GroupShare gs : Alerts.getGroupShares()) {
                if (gs.getShareTime() <= MAX_EXPECTED_SHARE_TIME_FOR_INSERTED_GROUP_SHARES) {
                    failedGroupShares.add(new GroupShare(gs.getGroup().getNumber()));
                }
            }

            switch (simulatedMethod) {
                case UPDATE_CRYPTO_DATA:
                    String getContactCryptoDataUpdateResponseJsonString = AndroidTestUtils.getRequest(
                            AndroidTestUtils.getUriPrefix() +
                                    "/account/get-ContactCryptoDataUpdate/" + TARGET_CONTACT_NUMBER_STRING
                    );

                    ContactCryptoDataUpdate contactCryptoDataUpdate = GeneralUtils.fromJson(
                            getContactCryptoDataUpdateResponseJsonString,
                            ContactCryptoDataUpdate.class
                    );

                    contactCryptoDataUpdate.setUpdatedGroupShares(failedGroupShares);

                    AndroidTestUtils.postString(
                            AndroidTestUtils.getUriPrefix() + "/account/update-crypto-data",
                            GeneralUtils.toJson(contactCryptoDataUpdate)
                    );

                    break;
                case SYNCHRONIZE_PENDING_REQUESTS:
                    String getGroupSharesUpdateResponseJsonString = AndroidTestUtils.getRequest(
                            AndroidTestUtils.getUriPrefix() +
                                    "/account/get-GroupSharesUpdate/" + TARGET_CONTACT_NUMBER_STRING
                    );

                    GroupSharesUpdate groupSharesUpdate = GeneralUtils.fromJson(
                            getGroupSharesUpdateResponseJsonString,
                            GroupSharesUpdate.class
                    );

                    groupSharesUpdate.setUpdatedGroupShares(failedGroupShares);

                    AndroidTestUtils.postString(
                            AndroidTestUtils.getUriPrefix() + "/account/synchronize-pending-requests",
                            GeneralUtils.toJson(groupSharesUpdate)
                    );

                    break;
            }

            SystemClock.sleep(5000);

            // Assert failed group-shares are displayed.

            onView(withId(R.id.recyclerview_main_tab_alerts))
                    .check(new AssertRecyclerViewItemCount(numOfDisplayedShares));

            for (int i = 0; i < numOfDisplayedShares; i++) {
                onView(withId(R.id.recyclerview_main_tab_alerts))
                        .check(matches(RecyclerViewMatcher.atPosition(
                                i,
                                hasDescendant(withText(containsString("Failed:")))
                        )));
            }

            // Bug fix test: Check that when loading additional items in a RecyclerView which has previously modified
            // displayed items but not reset the view, that duplicate items are not appended to the display.

            for (int i = 0; i < 4; i++) {
                loadMoreItems();
            }

            numOfDisplayedShares += MAX_ITEM_LOAD + 1;

            onView(withId(R.id.recyclerview_main_tab_alerts))
                    .check(new AssertRecyclerViewItemCount(numOfDisplayedShares));
        }
        finally {
            databaseCleanUp();
        }
    }

    private void loadMoreItems() {
        onView(withId(R.id.more_alerts_progress_bar)).perform(SetViewVisibility.setViewVisibility(View.VISIBLE));
        onView(withId(R.id.more_alerts_progress_bar)).perform(NestedScrollTo.nestedScrollTo());
        SystemClock.sleep(2000);
    }
}
