package gliphic.android.with_http_server.functionality;

import android.os.SystemClock;

import gliphic.android.display.ReportActivity;
import gliphic.android.operation.Group;
import gliphic.android.operation.server_interaction.http_server.RequestGlobalStatic;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import gliphic.android.operation.storage_handlers.SharedPreferencesHandler;
import gliphic.android.utils.AndroidTestUtils;
import libraries.GroupPermissions;
import libraries.Vars;

import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class RequestGlobalStaticTest {

    @Rule
    public RequestGlobalStaticTestRule<ReportActivity> rule = new RequestGlobalStaticTestRule<>(ReportActivity.class);

    @After
    public void afterTest() {
        SystemClock.sleep(500);     // Allow time for the server to respond.
    }

    @AfterClass
    public static void mainActivityTeardown() {
        SharedPreferencesHandler.removeAllContactData(AndroidTestUtils.getApplicationContext());

        // Ensure that the above is processed before proceeding to the next test class.
        SystemClock.sleep(200);
    }

    @Test
    public void requestAndSetAccessTokenTest() {
        rule.getScenario().onActivity(activity -> {
            try {
                RequestGlobalStatic.requestAndSetAccessToken(
                        accessToken -> {
                            if (accessToken == null) {
                                fail("Access token is null.");
                            }
                        },
                        activity,
                        null,
                        false
                );
            }
            catch (Throwable e) {
                fail(e.getMessage());
            }
        });
    }

    @Test
    public void requestAndSetCurrentContactTest() {
        rule.getScenario().onActivity(activity -> {
            try {
                RequestGlobalStatic.requestAndSetCurrentContact(
                        currentContact -> {
                            if (currentContact == null) {
                                fail("Current contact is null.");
                            }
                        },
                        activity,
                        null,
                        null,
                        false
                );
            }
            catch (Throwable e) {
                fail(e.getMessage());
            }
        });
    }

    @Test
    public void requestAndSetAllTargetContactsTest() {
        rule.getScenario().onActivity(activity -> {
            try {
                RequestGlobalStatic.requestAndSetTargetContacts(
                        (targetContacts, targetContactsRequestInProgress) -> {
                            if (targetContacts == null || targetContactsRequestInProgress) {
                                fail("Target contacts is null or a request for target contacts is in progress.");
                            }
                        },
                        activity,
                        false,
                        true,
                        0,
                        null,
                        true,
                        true,
                        null
                );
            }
            catch (Throwable e) {
                fail(e.getMessage());
            }
        });
    }

    @Test
    public void requestAndSetGroupTargetContactsTest() {
        rule.getScenario().onActivity(activity -> {
            try {
                RequestGlobalStatic.requestAndSetTargetContacts(
                        (targetContacts, targetContactsRequestInProgress) -> {
                            if (targetContacts == null || targetContactsRequestInProgress) {
                                fail("Target contacts is null or a request for target contacts is in progress.");
                            }
                        },
                        activity,
                        false,
                        true,
                        0,
                        new Group(
                                1,
                                Vars.DisplayPicture.ANIMAL_CAT.get(),
                                "Some valid name",
                                "Some valid description.",
                                AndroidTestUtils.generateGroupId(1),
                                GroupPermissions.ACTIVE_OWNER,
                                false,
                                false,
                                false
                        ),
                        true,
                        true,
                        null
                );
            }
            catch (Throwable e) {
                fail(e.getMessage());
            }
        });
    }

    @Test
    public void requestAndSetKnownGroupsTest() {
        rule.getScenario().onActivity(activity -> {
            try {
                RequestGlobalStatic.requestAndSetKnownGroups(
                        (commonGroups, knownGroupsRequestInProgress) -> {
                            if (commonGroups == null) {
                                fail("Common groups is null.");
                            }
                        },
                        activity,
                        null,
                        null,
                        true,
                        true,
                        0,
                        null,
                        null,
                        false,
                        false
                );
            }
            catch (Throwable e) {
                fail(e.getMessage());
            }
        });
    }

    @Test
    public void requestAndSetSelectedGroupTest() {
        rule.getScenario().onActivity(activity -> {
            try {
                RequestGlobalStatic.requestAndSetSelectedGroup(
                        selectedGroup -> {
                            if (selectedGroup == null) {
                                fail("Selected group is null.");
                            }
                        },
                        activity,
                        null,
                        null
                );
            }
            catch (Throwable e) {
                fail(e.getMessage());
            }
        });
    }

    @Test
    public void requestAndSetSelectedGroupTestWithAccessToken() {
        rule.getScenario().onActivity(activity -> {
            try {
                RequestGlobalStatic.requestAndSetSelectedGroup(
                        (selectedGroup, accessToken) -> {
                            if (selectedGroup == null || accessToken == null) {
                                fail("Selected group or access token is null.");
                            }
                        },
                        activity,
                        null,
                        null
                );
            }
            catch (Throwable e) {
                fail(e.getMessage());
            }
        });
    }

    @Test
    public void requestPendingReceivedGroupSharesAlertsTest() {
        rule.getScenario().onActivity(activity -> {
            try {
                RequestGlobalStatic.requestGroupShareAlerts(
                        (groupShareList, requestInProgress) -> {
                            if (groupShareList == null) {
                                fail("Group share list is null.");
                            }
                        },
                        activity,
                        true,
                        0,
                        null,
                        null,
                        null
                );
            }
            catch (Throwable e) {
                fail(e.getMessage());
            }
        });
    }

    @Test
    public void requestNonPendingReceivedGroupSharesAlertsTest() {
        rule.getScenario().onActivity(activity -> {
            try {
                RequestGlobalStatic.requestGroupShareAlerts(
                        (groupShareList, requestInProgress) -> {
                            if (groupShareList == null) {
                                fail("Group share list is null.");
                            }
                        },
                        activity,
                        true,
                        Long.MAX_VALUE,
                        0L,
                        0L,
                        0L
                );
            }
            catch (Throwable e) {
                fail(e.getMessage());
            }
        });
    }
}
