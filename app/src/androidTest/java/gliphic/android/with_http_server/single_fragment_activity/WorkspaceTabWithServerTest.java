package gliphic.android.with_http_server.single_fragment_activity;

import android.content.Context;
import android.os.SystemClock;

import androidx.test.espresso.contrib.RecyclerViewActions;
import gliphic.android.R;
import gliphic.android.display.main.WorkspaceTab;
import gliphic.android.operation.Group;
import gliphic.android.exceptions.GroupException;
import gliphic.android.exceptions.NullStaticVariableException;
import gliphic.android.operation.server_interaction.http_server.HttpOperations;
import gliphic.android.operation.storage_handlers.SharedPreferencesHandler;
import gliphic.android.utils.AndroidTestUtils;
import gliphic.android.utils.MainActivityBaseSetup;
import gliphic.android.utils.MainActivityTestRuleSetup;
import gliphic.android.utils.view_actions.NestedScrollTo;
import gliphic.android.with_http_server.functionality.SignInAndOutTest;

import org.apache.commons.lang3.StringUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import libraries.GeneralUtils;
import libraries.GroupPermissions;
import libraries.Vars;
import pojo.misc.AccessTokenAndGroupNumber;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.CoreMatchers.is;

/**
 * Test all possible user inputs, assuming that a server is running to accept messages,
 * and no messages to and from the server are corrupted.
 *
 * AlertDialog messages which fail this criteria are not tested.
 *
 * Use a group with an ID which does not use standard characters to check that there are no internal errors or group ID
 * encoding/decoding errors.
 */
@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class WorkspaceTabWithServerTest extends MainActivityTestRuleSetup {
    private static final long TEST_CONTACT_NUMBER = 0;
    private static final long TEST_GROUP_NUMBER = 1;
    private static final String TEST_GROUP_NAME = "This group name has max length";

    public static final String PLAIN_TEXT = "test";
//    public static final String PUBLISHED_TEXT_GROUP_0 = "|~123456ᶳṅӼᐦ⡠ዾ⒜ᘞᗙsᵘŊ≺ႣѶ∣նкᶔ≑⑽ᶢŁ⊒ᘩᾕᙰůჱɈᙷѹ⋀ǘ⪙⨃~|";
//    public static final String PUBLISHED_TEXT_GROUP_0_TIMED_OUT = "|~123456ሹ⅝Ꭲ⤤ᕄʓ⦻⋼ჷ⤃ᓖⓣҭ⟙⠺ⓁᕻᏮӖφᚻᓛჃṘň⢜⣈ኬᵪ⇻օᓎᗑᾤ⨵⫔~|";
//    public static final String PUBLISHED_TEXT_GROUP_1 = "|~ÀBCD0ʯ⟁ᔼ⒫ᐊⓤኦ\u052Eӕ⦁⧇⒖⦵ឃ⥣∀ᓤ⢶ᵱụБ⦙ᘣỊᖳመᴋዬᐯʣᴹᴁḳƬᕲ⫝̸⩅~|";
//    public static final String PUBLISHED_TEXT_GROUP_1_TIMED_OUT = "|~ÀBCD0ʯκ⅂ᑟᘐՈᾪႧա⟕ᕞîᴜ⠂хɘ⟪Ⴍᘄ⇆⢽⇽⟜ԙѫṱ⅓Ŧᾚ⤨ԈԢӔᖕ⦏⪂⫝~|";
//    public static final String PUBLISHED_TEXT_GROUP_2 = "|~Σ҂ҊԯԱՖ≱ƹጨЊἸæАᾝέ→ᓙϱʨቩᾤÍ⤀ӉሻҚŤήᔚዣ≒ПԥG⧟ᖝខᗪѦრ⩅⩢~|";
    public static final String PUBLISHED_TEXT_GROUP_1 =
            "|~ÀBCD0÷ŗŰſĦěŪëęg1ĲŉĪäłÒÐīFvţLĽþÂjČġPÜŝćīøĩŊTĲũWâçyđõmţHŤŒøģľűÏĨAÏbóžĲŔūĤňËĤ~|";
    public static final String PUBLISHED_TEXT_GROUP_1_TIMED_OUT =
            "|~ÀBCD0÷ŗŰſűţōÐŢJLÊÖŬûĘ4u3ńŊěŻĭx1ĻŕtňĘÝúŕBpbđĚĩÎŴokőľŧČřķFīĘļjŬĤĒmÑøŮEũĖXfşi~|";
    public static final String PUBLISHED_TEXT_GROUP_2 =
            "|~0Az-_ÆÏÐœĤòŵĭĦääĭ7đŴXŠŒČŜnLĲĩŽ2çŻĽĻ×ĠVōŇŹłáBģęåZğŧâģêăÍŞ÷ĖŨóŻŝùmSŢIóîņŔėçĘ~|";
    private static final String CONTACT_AND_GROUP_NUMBER_PAIR =
            String.format("%d,%d", TEST_CONTACT_NUMBER, TEST_GROUP_NUMBER);

    // Values stored by the device.
    long accessTokenExpiry;
    String refreshToken;

    private String getGetPermsUri() throws IllegalAccessException, NoSuchFieldException {
        return AndroidTestUtils.getUriPrefix() + "/text/get-database-group-permissions";
    }

    private String getSetPermsUri() throws IllegalAccessException, NoSuchFieldException {
        return AndroidTestUtils.getUriPrefix() + "/text/set-database-group-permissions";
    }

    private void getAccessTokenExpiryAndRefreshToken() throws Exception {
        final Context context = AndroidTestUtils.getApplicationContext();

        accessTokenExpiry = SharedPreferencesHandler.getAccessTokenExpiry(context);
        refreshToken = SharedPreferencesHandler.getRefreshToken(context);
    }

    private void setInvalidAccessTokenExpiryAndRefreshToken() throws Exception {
        AndroidTestUtils.setAccessTokenExpiryAndRefreshToken(
                System.currentTimeMillis() - 10000,
                "invalid refresh token"
        );
    }

    private void setDataEncryptionKey() throws Exception {
        AndroidTestUtils.setDataEncryptionKey(AndroidTestUtils.getApplicationContext(), TEST_CONTACT_NUMBER);
    }

    @Before
    public void beforeTest() {
        try {
            onView(withId(R.id.recyclerview_main_tab_selected_group))
                    .check(matches(hasDescendant(withText(TEST_GROUP_NAME))));
        }
        catch (AssertionError e) {
            onView(withId(R.id.recyclerview_main_tab_selected_group))
                    .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
            SystemClock.sleep(200);     // Allow time for the group selection activity to appear on screen.
            onView(withId(R.id.recyclerview_group_selection))
                    .perform(RecyclerViewActions.actionOnItemAtPosition(1, click()));
            SystemClock.sleep(100);
            onView(withId(android.R.id.button1)).perform(click());
            SystemClock.sleep(100);
            pressBack();
            SystemClock.sleep(200);     // Allow time for the workspace tab to appear on screen.
        }
    }

    @AfterClass
    public static void afterClass() throws Exception {
        SignInAndOutTest.removeAllContactData();

        // Set the selected group to the default group.

        final String accessToken = MainActivityBaseSetup.getValidAccessToken(TEST_CONTACT_NUMBER);

        final String uri = AndroidTestUtils.getUriPrefix() + "/set/selected-group";
        final AccessTokenAndGroupNumber accessTokenAndGroupNumber = new AccessTokenAndGroupNumber(
                accessToken,
                Vars.DEFAULT_GROUP_NUMBER
        );

        AndroidTestUtils.postString(uri, GeneralUtils.toJson(accessTokenAndGroupNumber));
    }

    // Valid operations.

    @Test
    public void encryptDecrypt() throws Exception {
        // BUG FIX: Avoid the error "MAC check failed" after receiving the encrypted group key from the server,
        // by ensuring that it does not fail to decrypt.
        setDataEncryptionKey();

        onView(ViewMatchers.withId(R.id.edittext_encrypt)).perform(typeText(PLAIN_TEXT), closeSoftKeyboard());
        onView(withId(R.id.btn_encrypt)).perform(click());
        SystemClock.sleep(500);

        onView(withId(R.id.edittext_encrypt)).perform(clearText(), closeSoftKeyboard());
        onView(withId(R.id.btn_decrypt)).perform(click());
        SystemClock.sleep(500);

        onView(withId(R.id.edittext_encrypt)).check(matches(withText(PLAIN_TEXT)));
    }

    @Test
    public void decrypt() throws Exception {
        handleDecryptTest(PUBLISHED_TEXT_GROUP_1);
    }

    @Test
    public void decryptWhereNoChosenTargetContactsAreLoaded() throws Exception {
        final long groupNumber = 2;

        Group.getKnownGroups().remove(Group.getGroupFromNumber(groupNumber));

        handleDecryptTest(PUBLISHED_TEXT_GROUP_2);

        assertThat(Group.getGroupFromNumber(groupNumber).getGroupTargetContacts().size(), is(0));
    }

    @Test
    public void decryptWhereChosenTargetContactsAreLoaded() throws Exception {
        final long groupNumber = 1;

        Group.getKnownGroups().remove(Group.getGroupFromNumber(groupNumber));

        handleDecryptTest(PUBLISHED_TEXT_GROUP_1);

        assertThat(Group.getGroupFromNumber(groupNumber).getGroupTargetContacts().size(), is(50));
    }

    private void handleDecryptTest(String publishedText) throws Exception {
        // BUG FIX: Avoid the error "MAC check failed" after receiving the encrypted group key from the server,
        // by ensuring that it does not fail to decrypt.
        setDataEncryptionKey();

        onView(withId(R.id.edittext_decrypt)).perform(replaceText(publishedText));
        onView(withId(R.id.edittext_encrypt)).perform(clearText(), closeSoftKeyboard());
        onView(withId(R.id.btn_decrypt)).perform(click());

        SystemClock.sleep(500);    // Give time for the message to appear.
        onView(withId(R.id.edittext_encrypt)).check(matches(withText(PLAIN_TEXT)));
    }

    @Test
    public void largeEncryptDecrypt() throws Exception {
        // BUG FIX: Avoid the error "MAC check failed" after receiving the encrypted group key from the server,
        // by ensuring that it does not fail to decrypt.
        setDataEncryptionKey();

        final String str = "ſ"; // This character, defined in Base256.java, should represent the largest possible
                                // number of bytes a character can fill, to test that the server's text-encryption
                                // limit is not exceeded; most characters could fill this role except standard base-64
                                // characters which are too small.
        final int repeat = Integer.parseInt(AndroidTestUtils.getResourceString(R.string.encrypt_max_length));
        final String largePlainText = StringUtils.repeat(str, repeat);

        onView(ViewMatchers.withId(R.id.edittext_encrypt)).perform(replaceText(largePlainText), closeSoftKeyboard());
        SystemClock.sleep(1000);
        onView(withId(R.id.encrypt_buttons_layout)).perform(NestedScrollTo.nestedScrollTo());
        SystemClock.sleep(500);
        onView(withId(R.id.btn_encrypt)).perform(click());
        SystemClock.sleep(500);
        onView(withId(R.id.edittext_encrypt)).perform(clearText(), closeSoftKeyboard());
        // The decrypted text takes a while to display, even after the LoadingDialog is dismissed.
        waitForEditText(R.id.edittext_decrypt);

        // Scroll to near the bottom; unfortunately this stops just short if displaying the buttons.
        onView(withId(R.id.decrypt_buttons_layout)).perform(NestedScrollTo.nestedScrollTo());
        SystemClock.sleep(2000);
        // Swipe up a bit to show the buttons at the bottom of the screen.
        onView(withId(R.id.viewpager_activity_base)).perform(AndroidTestUtils.swipeUp());
        SystemClock.sleep(2000);

        onView(withId(R.id.btn_decrypt)).perform(click());
        // The encrypted text takes a while to display, even after the LoadingDialog is dismissed.
        waitForEditText(R.id.edittext_encrypt);

        onView(withId(R.id.edittext_encrypt)).check(matches(withText(largePlainText)));
    }

    /**
     * Check that an empty EditText view gets filled with some text within a fixed upper time bound.
     *
     * @param editTextId    The EditText view to check.
     */
    private static void waitForEditText(int editTextId) {
        try {
            final long loopDelay = 1000;
            final long maximumAggregateDelay = 10000;
            long aggregateDelay = 0;

            while (true) {
                onView(withId(editTextId)).check(matches(withText("")));

                aggregateDelay += loopDelay;

                if (aggregateDelay < maximumAggregateDelay) {
                    SystemClock.sleep(loopDelay);
                }
                else {
                    break;
                }
            }
        }
        catch (AssertionError e) {
            // return;
        }
    }

    // Invalid refresh token, with expired access token.

    @Test
    public void invalidRefreshTokenEncrypt() throws Exception{
        getAccessTokenExpiryAndRefreshToken();

        try {
            setInvalidAccessTokenExpiryAndRefreshToken();

            onView(withId(R.id.edittext_encrypt)).perform(replaceText(PLAIN_TEXT));
            onView(withId(R.id.btn_encrypt)).perform(click());
            SystemClock.sleep(1000);    // Allow time to receive a 401 error code.

            onView(withText(HttpOperations.ERROR_MSG_401_BAD_TOKEN)).check(matches(isDisplayed()));
        }
        finally {
            AndroidTestUtils.setAccessTokenExpiryAndRefreshToken(accessTokenExpiry, refreshToken);
        }
    }

    @Test
    public void invalidRefreshTokenDecrypt() throws Exception {
        getAccessTokenExpiryAndRefreshToken();

        try {
            setInvalidAccessTokenExpiryAndRefreshToken();

            onView(withId(R.id.edittext_decrypt)).perform(replaceText(PUBLISHED_TEXT_GROUP_1));
            onView(withId(R.id.btn_decrypt)).perform(click());
            SystemClock.sleep(1000);    // Allow time to receive a 401 error code.

            onView(withText(HttpOperations.ERROR_MSG_401_BAD_TOKEN)).check(matches(isDisplayed()));
        }
        finally {
            AndroidTestUtils.setAccessTokenExpiryAndRefreshToken(accessTokenExpiry, refreshToken);
        }
    }

    // Inactive account.

    @Test
    public void inactiveAccountEncrypt() throws Exception {
        try {
            MainActivityBaseSetup.deactivateAccount(TEST_CONTACT_NUMBER);

            onView(withId(R.id.edittext_encrypt)).perform(replaceText(PLAIN_TEXT));
            onView(withId(R.id.btn_encrypt)).perform(click());
            SystemClock.sleep(1000);    // Allow time to receive a 403 error code.

            onView(withText(HttpOperations.ERROR_MSG_403_ACCOUNT_INACTIVE)).check(matches(isDisplayed()));
        }
        finally {
            MainActivityBaseSetup.activateAccount(TEST_CONTACT_NUMBER);
        }
    }

    @Test
    public void inactiveAccountDecrypt() throws Exception {
        try {
            MainActivityBaseSetup.deactivateAccount(TEST_CONTACT_NUMBER);

            onView(withId(R.id.edittext_decrypt)).perform(replaceText(PUBLISHED_TEXT_GROUP_1));
            onView(withId(R.id.btn_decrypt)).perform(click());
            SystemClock.sleep(1000);    // Allow time to receive a 403 error code.

            onView(withText(HttpOperations.ERROR_MSG_403_ACCOUNT_INACTIVE)).check(matches(isDisplayed()));
        }
        finally {
            MainActivityBaseSetup.activateAccount(TEST_CONTACT_NUMBER);
        }
    }

    // Bad group permissions.

    @Test
    public void groupInactiveAndAccessDeniedEncrypt() throws GroupException, NullStaticVariableException {
        GroupPermissions originalGroupPerms = Group.getSelectedGroup().getPermissions();
        try {
            Group.getSelectedGroup().setPermissions(GroupPermissions.INACTIVE_DENIED);

            onView(withId(R.id.edittext_encrypt)).perform(replaceText(PLAIN_TEXT));
            onView(withId(R.id.btn_encrypt)).perform(click());
            SystemClock.sleep(2000);
            onView(withText(HttpOperations.ERROR_MSG_403_INACTIVE_AND_DENIED_SELECT)).check(matches(isDisplayed()));
        }
        finally {
            Group.getSelectedGroup().setPermissions(originalGroupPerms);
        }
    }

    @Test
    public void groupInactiveEncrypt() throws GroupException, NullStaticVariableException {
        GroupPermissions originalGroupPerms = Group.getSelectedGroup().getPermissions();
        try {
            Group.getSelectedGroup().setPermissions(GroupPermissions.INACTIVE_OWNER);

            onView(withId(R.id.edittext_encrypt)).perform(replaceText(PLAIN_TEXT));
            onView(withId(R.id.btn_encrypt)).perform(click());
            SystemClock.sleep(2000);
            onView(withText(HttpOperations.ERROR_MSG_403_INACTIVE_SELECT)).check(matches(isDisplayed()));
        }
        finally {
            Group.getSelectedGroup().setPermissions(originalGroupPerms);
        }
    }

    @Test
    public void groupAccessDeniedEncrypt() throws GroupException, NullStaticVariableException {
        GroupPermissions originalGroupPerms = Group.getSelectedGroup().getPermissions();
        try {
            Group.getSelectedGroup().setPermissions(GroupPermissions.ACTIVE_DENIED);

            onView(withId(R.id.edittext_encrypt)).perform(replaceText(PLAIN_TEXT));
            onView(withId(R.id.btn_encrypt)).perform(click());
            SystemClock.sleep(2000);
            onView(withText(HttpOperations.ERROR_MSG_403_DENIED_SELECT)).check(matches(isDisplayed()));
        }
        finally {
            Group.getSelectedGroup().setPermissions(originalGroupPerms);
        }
    }

    // Invalid decrypt, sent new access token.

    @Test
    public void Base256ExceptionDecrypt() {
        String shortNoBody = "|~01234567890123456789*01234567890123456789~|";
        onView(withId(R.id.edittext_decrypt)).perform(replaceText(shortNoBody));
        onView(withId(R.id.btn_decrypt)).perform(click());
        SystemClock.sleep(200);
        onView(withText("Input message is malformed.")).check(matches(isDisplayed()));
    }

    @Test
    public void emptyTextDecrypt() {
        onView(withId(R.id.edittext_decrypt)).perform(clearText(), closeSoftKeyboard());
        onView(withId(R.id.btn_decrypt)).perform(click());
        SystemClock.sleep(200);
        onView(withText("Enter a non-empty message to decrypt.")).check(matches(isDisplayed()));
    }

    @Test
    public void shortTextDecrypt1() {
        String shortNoBody = "~";
        onView(withId(R.id.edittext_decrypt)).perform(replaceText(shortNoBody));
        onView(withId(R.id.btn_decrypt)).perform(click());
        SystemClock.sleep(200);
        onView(withText("Input message is too short.")).check(matches(isDisplayed()));
    }

    @Test
    public void shortTextDecrypt2() throws GroupException {
        String s = Vars.START_TAG + Group.getGroupFromNumber(0).getId() + "GG" + Vars.END_TAG;
        onView(withId(R.id.edittext_decrypt)).perform(replaceText(s));
        onView(withId(R.id.btn_decrypt)).perform(click());
        SystemClock.sleep(200);
        onView(withText("Input message is too short.")).check(matches(isDisplayed()));
    }

    @Test
    public void invalidStartTagDecrypt() {
        String badStartTag = "GG" + PUBLISHED_TEXT_GROUP_1.substring(Vars.START_TAG.length());
        onView(withId(R.id.edittext_decrypt)).perform(replaceText(badStartTag));
        onView(withId(R.id.btn_decrypt)).perform(click());
        SystemClock.sleep(200);
        onView(withText("Input message has an invalid prefix.")).check(matches(isDisplayed()));
    }

    @Test
    public void invalidEndTagDecrypt() {
        String badEndTag = PUBLISHED_TEXT_GROUP_1
                .substring(0, PUBLISHED_TEXT_GROUP_1.length() - Vars.END_TAG.length()) + "GG";
        onView(withId(R.id.edittext_decrypt)).perform(replaceText(badEndTag));
        onView(withId(R.id.btn_decrypt)).perform(click());
        SystemClock.sleep(200);
        onView(withText("Input message has an invalid suffix.")).check(matches(isDisplayed()));
    }

    @Test
    public void invalidGroupIdDecrypt() {
        String badEndTag = PUBLISHED_TEXT_GROUP_1.substring(0, Vars.START_TAG.length()) +
                (char) 60 +
                PUBLISHED_TEXT_GROUP_1.substring(Vars.START_TAG.length() + 1);
        onView(withId(R.id.edittext_decrypt)).perform(replaceText(badEndTag));
        onView(withId(R.id.btn_decrypt)).perform(click());
        SystemClock.sleep(200);
        onView(withText("Input message has an invalid group ID.")).check(matches(isDisplayed()));
    }

    @Test
    public void unknownGroupDecrypt() throws Throwable {
        StringBuilder sb = new StringBuilder(PUBLISHED_TEXT_GROUP_1);
        sb.setCharAt(3, 'g');
        String publishedTextUnknownGroup = sb.toString();

        try {
            onView(withId(R.id.edittext_decrypt)).perform(replaceText(publishedTextUnknownGroup));
            onView(withId(R.id.btn_decrypt)).perform(click());
            SystemClock.sleep(2000);
            onView(withText("Input message is encrypted with an unknown group.")).check(matches(isDisplayed()));
        }
        finally {
            MainActivityBaseSetup.mainActivitySetupBeforeActivityLaunched();
        }
    }

    @Test
    public void groupInactiveAndAccessDeniedDecrypt() throws GroupException, NullStaticVariableException {
        GroupPermissions originalGroupPerms = Group.getSelectedGroup().getPermissions();
        try {
            Group.getSelectedGroup().setPermissions(GroupPermissions.INACTIVE_DENIED);
            onView(withId(R.id.edittext_encrypt)).perform(replaceText(PUBLISHED_TEXT_GROUP_1));
            onView(withId(R.id.btn_encrypt)).perform(click());
            SystemClock.sleep(2000);
            onView(withText(HttpOperations.ERROR_MSG_403_INACTIVE_AND_DENIED_SELECT)).check(matches(isDisplayed()));
        }
        finally {
            Group.getSelectedGroup().setPermissions(originalGroupPerms);
        }
    }

    @Test
    public void groupInactiveDecrypt() throws GroupException, NullStaticVariableException {
        GroupPermissions originalGroupPerms = Group.getSelectedGroup().getPermissions();
        try {
            Group.getSelectedGroup().setPermissions(GroupPermissions.INACTIVE_OWNER);
            onView(withId(R.id.edittext_encrypt)).perform(replaceText(PUBLISHED_TEXT_GROUP_1));
            onView(withId(R.id.btn_encrypt)).perform(click());
            SystemClock.sleep(2000);
            onView(withText(HttpOperations.ERROR_MSG_403_INACTIVE_SELECT)).check(matches(isDisplayed()));
        }
        finally {
            Group.getSelectedGroup().setPermissions(originalGroupPerms);
        }
    }

    @Test
    public void groupAccessDeniedDecrypt() throws GroupException, NullStaticVariableException {
        GroupPermissions originalGroupPerms = Group.getSelectedGroup().getPermissions();
        try {
            Group.getSelectedGroup().setPermissions(GroupPermissions.ACTIVE_DENIED);
            onView(withId(R.id.edittext_encrypt)).perform(replaceText(PUBLISHED_TEXT_GROUP_1));
            onView(withId(R.id.btn_encrypt)).perform(click());
            SystemClock.sleep(2000);
            onView(withText(HttpOperations.ERROR_MSG_403_DENIED_SELECT)).check(matches(isDisplayed()));
        }
        finally {
            Group.getSelectedGroup().setPermissions(originalGroupPerms);
        }
    }

    // Invalid decrypt, sent decrypt request.

    @Test
    public void statusBadPublishedMsg() throws IllegalAccessException, NoSuchFieldException {
        // Remove two characters from the middle of the string to create a bad message.
        int sliceIndex = PUBLISHED_TEXT_GROUP_1.length() - Vars.END_TAG.length() - 5;
        String s1 = PUBLISHED_TEXT_GROUP_1.substring(0, sliceIndex - 2);
        String s2 = PUBLISHED_TEXT_GROUP_1.substring(sliceIndex);
        String badCipherMsg = s1 + s2;

        onView(withId(R.id.edittext_decrypt)).perform(replaceText(badCipherMsg));
        onView(withId(R.id.btn_decrypt)).perform(click());

        // Use reflection to get the private static final string expected AlertDialog message.
        String BAD_PUBLISHED_MSG_MSG = (String) AndroidTestUtils.getPrivateObjectUsingReflection(
                new WorkspaceTab(), "BAD_PUBLISHED_MSG_MSG"
        );

        SystemClock.sleep(500);
        onView(withText(BAD_PUBLISHED_MSG_MSG)).check(matches(isDisplayed()));
    }

    @Test
    public void statusTimeOutExpired() throws IllegalAccessException, NoSuchFieldException {
        onView(withId(R.id.edittext_decrypt)).perform(replaceText(PUBLISHED_TEXT_GROUP_1_TIMED_OUT));
        onView(withId(R.id.btn_decrypt)).perform(click());

        // Use reflection to get the private static final string expected AlertDialog message.
        String TIME_OUT_EXPIRED_MSG = (String) AndroidTestUtils.getPrivateObjectUsingReflection(
                new WorkspaceTab(), "TIME_OUT_EXPIRED_MSG"
        );

        SystemClock.sleep(500);    // Give time for the message to appear.
        onView(withText(TIME_OUT_EXPIRED_MSG)).check(matches(isDisplayed()));
    }

    // Invalid decrypt with modified server responses

    @Test
    public void statusGroupInactiveAndAccessDenied() throws Exception {
        // Get the current permissions value to reset to at the end of the test.
        String sentString = CONTACT_AND_GROUP_NUMBER_PAIR;
        String receivedString = AndroidTestUtils.postString(getGetPermsUri(), sentString);

        try {
            // Set the permissions value.
            sentString = CONTACT_AND_GROUP_NUMBER_PAIR + ",100";
            AndroidTestUtils.postString(getSetPermsUri(), sentString);

            // Use reflection to get the expected AlertDialog message.
            String GROUP_INACTIVE_AND_ACCESS_DENIED_MSG =
                    (String) AndroidTestUtils.getPrivateObjectUsingReflection(
                            new WorkspaceTab(), "GROUP_INACTIVE_AND_ACCESS_DENIED_MSG"
                    );

            // Now try to use the group with bad permissions.
            onView(withId(R.id.edittext_decrypt)).perform(replaceText(PUBLISHED_TEXT_GROUP_1));
            onView(withId(R.id.btn_decrypt)).perform(click());
            SystemClock.sleep(1000);    // Give time for the message to appear.
            onView(withText(GROUP_INACTIVE_AND_ACCESS_DENIED_MSG)).check(matches(isDisplayed()));
        }
        finally {
            sentString = CONTACT_AND_GROUP_NUMBER_PAIR + "," + receivedString;
            AndroidTestUtils.postString(getSetPermsUri(), sentString);
        }
    }

    @Test
    public void statusGroupInactive() throws Exception {
        // Get the current permissions value to reset to at the end of the test.
        String sentString = CONTACT_AND_GROUP_NUMBER_PAIR;
        String receivedString = AndroidTestUtils.postString(getGetPermsUri(), sentString);

        try {
            // Set the permissions value.
            sentString = CONTACT_AND_GROUP_NUMBER_PAIR + ",101";
            AndroidTestUtils.postString(getSetPermsUri(), sentString);

            // Use reflection to get the expected AlertDialog message.
            String GROUP_INACTIVE_MSG = (String) AndroidTestUtils.getPrivateObjectUsingReflection(
                    new WorkspaceTab(), "GROUP_INACTIVE_MSG"
            );

            // Now try to use the group with bad permissions.
            onView(withId(R.id.edittext_decrypt)).perform(replaceText(PUBLISHED_TEXT_GROUP_1));
            onView(withId(R.id.btn_decrypt)).perform(click());
            SystemClock.sleep(1000);    // Give time for the message to appear.
            onView(withText(GROUP_INACTIVE_MSG)).check(matches(isDisplayed()));
        }
        finally {
            sentString = CONTACT_AND_GROUP_NUMBER_PAIR + "," + receivedString;
            AndroidTestUtils.postString(getSetPermsUri(), sentString);
        }
    }

    @Test
    public void statusGroupAccessDenied() throws Exception {
        // Get the current permissions value to reset to at the end of the test.
        String sentString = CONTACT_AND_GROUP_NUMBER_PAIR;
        String receivedString = AndroidTestUtils.postString(getGetPermsUri(), sentString);

        try {
            // Set the permissions value.
            sentString = CONTACT_AND_GROUP_NUMBER_PAIR + ",0";
            AndroidTestUtils.postString(getSetPermsUri(), sentString);

            // Use reflection to get the expected AlertDialog message.
            String GROUP_ACCESS_DENIED_MSG =
                    (String) AndroidTestUtils.getPrivateObjectUsingReflection(
                            new WorkspaceTab(), "GROUP_ACCESS_DENIED_MSG"
                    );

            // Now try to use the group with bad permissions.
            onView(withId(R.id.edittext_decrypt)).perform(replaceText(PUBLISHED_TEXT_GROUP_1));
            onView(withId(R.id.btn_decrypt)).perform(click());
            SystemClock.sleep(1000);    // Give time for the message to appear.
            onView(withText(GROUP_ACCESS_DENIED_MSG)).check(matches(isDisplayed()));
        }
        finally {
            sentString = CONTACT_AND_GROUP_NUMBER_PAIR + "," + receivedString;
            AndroidTestUtils.postString(getSetPermsUri(), sentString);
        }
    }

    // Invalid decrypt: bad group

    @Test
    public void decryptBadGroupNumber() throws Exception {
        final long nonExistentGroupNumber = 39470070;

        try {
            MainActivityBaseSetup.modifyGroupNumber(TEST_GROUP_NUMBER, nonExistentGroupNumber);

            onView(withId(R.id.edittext_decrypt)).perform(replaceText(PUBLISHED_TEXT_GROUP_1));
            onView(withId(R.id.edittext_encrypt)).perform(clearText(), closeSoftKeyboard());
            onView(withId(R.id.btn_decrypt)).perform(click());

            // Use reflection to get the expected AlertDialog message.
            String GENERIC_DECRYPT_FAILED_MSG =
                    (String) AndroidTestUtils.getPrivateObjectUsingReflection(
                            new WorkspaceTab(), "GENERIC_DECRYPT_FAILED_MSG"
                    );

            SystemClock.sleep(1000);    // Give time for the message to appear.
            onView(withText(GENERIC_DECRYPT_FAILED_MSG)).check(matches(isDisplayed()));
        }
        finally {
            MainActivityBaseSetup.modifyGroupNumber(nonExistentGroupNumber, TEST_GROUP_NUMBER);
        }
    }
}
