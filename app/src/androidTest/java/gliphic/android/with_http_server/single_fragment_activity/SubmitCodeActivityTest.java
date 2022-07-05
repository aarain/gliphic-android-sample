package gliphic.android.with_http_server.single_fragment_activity;

import android.content.Context;
import android.os.SystemClock;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.matcher.ViewMatchers;
import gliphic.android.R;
import gliphic.android.adapters.ImageAdapter;
import gliphic.android.display.libraries.AlertDialogs;
import gliphic.android.display.welcome_screen.SubmitCodeActivity;
import gliphic.android.operation.Contact;
import gliphic.android.operation.ObjectImage;
import gliphic.android.operation.server_interaction.http_server.HttpOperations;
import gliphic.android.operation.storage_handlers.IsStoredAndDataObject;
import gliphic.android.operation.storage_handlers.SharedPreferencesHandler;
import gliphic.android.utils.AndroidTestUtils;
import gliphic.android.utils.MainActivityBaseSetup;

import org.hamcrest.Matcher;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;

import androidx.test.filters.LargeTest;
import gliphic.android.utils.SubmitCodeActivityTestRule;
import gliphic.android.utils.matchers.CustomMatchers;
import gliphic.android.with_http_server.functionality.SignInAndOutTest;
import libraries.Vars;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static gliphic.android.utils.AndroidTestUtils.getOnViewInteractionFromId;
import static gliphic.android.utils.AndroidTestUtils.getOnViewInteractionFromString;
import static gliphic.android.utils.AndroidTestUtils.swipeUp;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.anything;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
@LargeTest
public class SubmitCodeActivityTest {
    private static final long   TEST_CONTACT_NUMBER = 0;
    private static final String VALID_EMAIL_ADDRESS = RegisterActivityTest.duplicateEmail;
    private static final String VALID_PASSWORD      = "P455w0rd";
    private static final String VALID_CONTACT_NAME  = "Test User";
    private static final ObjectImage VALID_IMAGE    = new ObjectImage(Vars.DisplayPicture.OTHER_PAINTING.get());

    private static String validActivationToken;
    private static String validRecoveryToken;
    private static String validSignInToken;

    private final ViewInteraction onViewEditTextSubmitCode = getOnViewInteractionFromId(R.id.edittext_submit_code);
    private final ViewInteraction onViewEditText2          = getOnViewInteractionFromId(R.id.submit_code_edittext_2);
    private final ViewInteraction onViewEditText3          = getOnViewInteractionFromId(R.id.submit_code_edittext_3);
    private final ViewInteraction onViewEditText4          = getOnViewInteractionFromId(R.id.submit_code_edittext_4);
    private final ViewInteraction onViewImageView          = getOnViewInteractionFromId(R.id.submit_code_image);
    private final ViewInteraction onViewButtonSubmitCode   = getOnViewInteractionFromId(R.id.btn_submit_code);

    // Parametrised values for each test.
    private CONTEXT context;
    private String validToken;
    private String successText;     // Set to null if the contact has signed in.
    private String emptyTokenText;
    private String invalidTokenMsg;

    private enum CONTEXT {ACTIVATION, RECOVERY, SIGN_IN}

    public SubmitCodeActivityTest(CONTEXT context) {
        this.context = context;

        switch (context) {
            case ACTIVATION:
                validToken = validActivationToken;
                successText = "Activation successful";
                emptyTokenText = "The activation code cannot be empty.";
                invalidTokenMsg = HttpOperations.ERROR_MSG_401_BAD_ACTIVATION_TOKEN;
                break;
            case RECOVERY:
                validToken = validRecoveryToken;
                successText = "Recovery successful";
                emptyTokenText = "The recovery code cannot be empty.";
                invalidTokenMsg = HttpOperations.ERROR_MSG_401_BAD_RECOVERY_TOKEN;
                break;
            case SIGN_IN:
                validToken = validSignInToken;
                successText = null;
                emptyTokenText = "The sign-in code cannot be empty.";
                invalidTokenMsg = HttpOperations.ERROR_MSG_403_BAD_SIGN_IN_TOKEN;
                break;
            default:
                fail();
        }
    }

    @Parameterized.Parameters
    public static List<Object[]> testCases() {
        return Arrays.asList(new Object[][] {
                {CONTEXT.ACTIVATION},
                {CONTEXT.RECOVERY},
                {CONTEXT.SIGN_IN},
        });
    }

    @Rule
    public SubmitCodeActivityTestRule<SubmitCodeActivity> rule =
            new SubmitCodeActivityTestRule<>(SubmitCodeActivity.class);

    /*
     * Manually set up SSL (for HTTPS) since this class does not extend MainActivityBaseSetup and the
     * ActivityScenarioRule is not for the SignInActivity.
     */
    @BeforeClass
    public static void setup() throws Exception {
        HttpOperations.restSslSetup(null, null);

        // Ensure that valid tokens are verifiable by the server.
        getValidTokens();
    }

    @AfterClass
    public static void teardown() throws Exception {
        MainActivityBaseSetup.setNullRecoveryTokenId(TEST_CONTACT_NUMBER);
        MainActivityBaseSetup.setNullSignInTokenId(TEST_CONTACT_NUMBER);

        SharedPreferencesHandler.removeAllContactData(AndroidTestUtils.getApplicationContext());
    }

    @Before
    public void navigateToTab() {
        switch (context) {
            case ACTIVATION:
                // Assume that this is the default tab.

                onViewEditText2.perform(replaceText(VALID_PASSWORD));
                onViewEditText3.perform(replaceText(VALID_PASSWORD));
                onViewEditText4.perform(replaceText(VALID_CONTACT_NAME));
                changeGroupImage();
            default:
                break;
            case RECOVERY:
                getOnViewInteractionFromString(R.string.submit_code_recovery).perform(click());
                SystemClock.sleep(500);

                onViewEditText2.perform(replaceText(VALID_PASSWORD));
                onViewEditText3.perform(replaceText(VALID_PASSWORD));

                break;
            case SIGN_IN:
                getOnViewInteractionFromString(R.string.submit_code_sign_in).perform(click());
                SystemClock.sleep(500);

                onViewEditText2.perform(replaceText(VALID_EMAIL_ADDRESS));
                onViewEditText3.perform(replaceText(VALID_PASSWORD));

                break;
        }
    }

    private static void getValidTokens() throws Exception {
        validActivationToken = MainActivityBaseSetup.getValidActivationToken(
                AndroidTestUtils.CONTACT_REGISTERED_INACTIVE
        );

        validRecoveryToken = MainActivityBaseSetup.getValidRecoveryToken(TEST_CONTACT_NUMBER);

        validSignInToken = MainActivityBaseSetup.getValidSignInToken(TEST_CONTACT_NUMBER);
    }

    private void changeGroupImage() {
        getOnViewInteractionFromId(R.id.viewpager_activity_base).perform(swipeUp());
        SystemClock.sleep(100);
        onViewImageView.perform(click());
        SystemClock.sleep(200);
        onData(anything()).inAdapterView(withId(R.id.gridview_display_pictures))
                .atPosition(ImageAdapter.getItemPosition(VALID_IMAGE)).perform(click());
        SystemClock.sleep(200);
        onView(ViewMatchers.withId(R.id.btn_single_picture)).perform(click());
        SystemClock.sleep(100);
        onViewImageView.check(matches(isDisplayed()));
    }

    private static final long submitValidTokenSleep = 5000;

    private void submitValidToken(@NonNull String token) {
        submitToken(token, submitValidTokenSleep);  // Allow time for the server to respond.
    }

    private void submitToken(@NonNull String token, long sleepAfterSubmit) {
        onViewEditTextSubmitCode.perform(replaceText(token));
//        getOnViewInteraction(R.id.btn_submit_code).perform(NestedScrollTo.nestedScrollTo());
        getOnViewInteractionFromId(R.id.viewpager_activity_base).perform(swipeUp());
        SystemClock.sleep(500);
        onViewButtonSubmitCode.perform(click());

        SignInAndOutTest.assertSubmitTokenViewsNotClickable();

        SystemClock.sleep(sleepAfterSubmit);
    }

    private void assertViews(boolean editTextViews1Cleared,
                             boolean editTextViews2Cleared,
                             boolean editTextViews3Cleared,
                             boolean editTextViews4Cleared) {

        final Matcher<View> editText1Matcher = editTextViews1Cleared ? withText("") : not(withText(""));
        final Matcher<View> editText2Matcher = editTextViews2Cleared ? withText("") : not(withText(""));
        final Matcher<View> editText3Matcher = editTextViews3Cleared ? withText("") : not(withText(""));
        final Matcher<View> editText4Matcher = editTextViews4Cleared ? withText("") : not(withText(""));

        switch (context) {
            case ACTIVATION:
                onViewEditTextSubmitCode.check(matches(editText1Matcher));
                onViewEditText2.check(matches(editText2Matcher));
                onViewEditText3.check(matches(editText3Matcher));
                onViewEditText4.check(matches(editText4Matcher));
                onViewImageView.check(matches(CustomMatchers.withDrawable(VALID_IMAGE.getResourceInt())));
            default:
                break;
            case RECOVERY:
            case SIGN_IN:
                onViewEditTextSubmitCode.check(matches(editText1Matcher));
                onViewEditText2.check(matches(editText2Matcher));
                onViewEditText3.check(matches(editText3Matcher));
                break;
        }
    }

    @Test
    public void validSubmitToken() throws Throwable {
        try {
            MainActivityBaseSetup.setDefaultDeviceValues(rule.getScenario());

            // Test that any whitespace characters are removed from the input string automatically.
            String modifiedToken = String.format("\n %s\t", validToken);

            submitValidToken(modifiedToken);

            if (successText == null) {
                AndroidTestUtils.waitForAction(AndroidTestUtils.WaitAction.SIGN_IN);
                SignInAndOutTest.signOut();
            }
            else {
                onView(withText(successText)).check(matches(isDisplayed()));

                onView(withId(android.R.id.button1)).perform(click());

                assertViews(true, true, true, true);
            }
        }
        finally {
            if (context.equals(CONTEXT.ACTIVATION)) {
                AndroidTestUtils.getRequest(String.format(
                        "%s/account/revert-registration-activation/%d",
                        AndroidTestUtils.getUriPrefix(),
                        AndroidTestUtils.CONTACT_REGISTERED_INACTIVE
                ));
            }

            // The server will set the token ID to null if the token submission was successful.
            getValidTokens();
        }
    }

    @Test
    public void validSubmitTokenWithoutDeviceKeyAndCode() throws Throwable {
        Assume.assumeThat(context, is(CONTEXT.ACTIVATION));

        IsStoredAndDataObject deviceKey = null;
        IsStoredAndDataObject deviceCode = null;

        try {
            final Context context = AndroidTestUtils.getApplicationContext();

            deviceKey = SharedPreferencesHandler.getDeviceKey(context, true);
            deviceCode = SharedPreferencesHandler.getDeviceCode(context, true);

            SharedPreferencesHandler.removeDeviceKey(context);
            SharedPreferencesHandler.removeDeviceCode(context);

            SystemClock.sleep(1000);    // Give the thread time to complete.
            validSubmitToken();
        }
        finally {
            final Context context = AndroidTestUtils.getApplicationContext();

            if (deviceKey != null && deviceKey.wasStored()) {
                SharedPreferencesHandler.setDeviceKey(context, deviceKey.getData());
            }

            if (deviceCode != null && deviceCode.wasStored()) {
                SharedPreferencesHandler.setDeviceCode(context, deviceCode.getData());
            }

            SystemClock.sleep(1000);    // Give the thread time to complete.
        }
    }

    // This test could be moved to the without_http_server package.
    @Test
    public void emptyToken() {
        String badToken = "";

        submitToken(badToken, 200);

        onView(withText(emptyTokenText)).check(matches(isDisplayed()));

        onView(withId(android.R.id.button1)).perform(click());

        assertViews(true, false, false, false);
    }

    @Test
    public void invalidToken() {
        // An expired activation token is used because it is a syntactically valid token; any token type would suffice.
        String badToken = MainActivityBaseSetup.expiredActivationTokenContact0;

        submitToken(badToken, 2000);    // Allow time for the server to respond.

        onView(withText(invalidTokenMsg)).check(matches(isDisplayed()));

        onView(withId(android.R.id.button1)).perform(click());

        assertViews(false, false, false, false);
    }

    private void submitTokenWithoutServerInvalidInput(String expectedMessage) {
        submitToken(validToken, 200);
        onView(withText(expectedMessage)).check(matches(isDisplayed()));

        onView(withId(android.R.id.button1)).perform(click());
    }

    @Test
    public void badEditText2() {
        // Only test a single type of invalid input; assume that other cases are also handled.
        onViewEditText2.perform(replaceText(""));

        final String expectedMessage;

        switch (context) {
            case ACTIVATION:
                expectedMessage = String.format(AlertDialogs.CHOSEN_PASSWORD_EMPTY_MSG, " ");
                break;
            case RECOVERY:
                expectedMessage = String.format(AlertDialogs.CHOSEN_PASSWORD_EMPTY_MSG, " new ");
                break;
            case SIGN_IN:
                expectedMessage = AlertDialogs.EMAIL_EMPTY_MSG;
                break;
            default:
                fail();
                return;
        }

        submitTokenWithoutServerInvalidInput(expectedMessage);

        assertViews(false, true, false, false);
    }

    @Test
    public void badEditText3() {
        // Only test a single type of invalid input; assume that other cases are also handled.
        onViewEditText3.perform(replaceText(""));

        final String expectedMessage;

        switch (context) {
            case ACTIVATION:
            case RECOVERY:
                expectedMessage = AlertDialogs.CHOSEN_PASSWORDS_UNEQUAL_MSG;
                break;
            case SIGN_IN:
                expectedMessage = AlertDialogs.ENTERED_PASSWORD_EMPTY_MSG;
                break;
            default:
                fail();
                return;
        }

        submitTokenWithoutServerInvalidInput(expectedMessage);

        assertViews(false, false, true, false);
    }

    @Test
    public void badEditText4() {
        Assume.assumeThat(context, is(CONTEXT.ACTIVATION));

        // Only test a single type of invalid input; assume that other cases are also handled.
        onViewEditText4.perform(replaceText(""));

        final String expectedMessage = Contact.NAME_EMPTY_MSG;

        submitTokenWithoutServerInvalidInput(expectedMessage);

        assertViews(false, false, false, true);
    }

    @Test
    public void badAccountStatus() throws Exception {
        Assume.assumeThat(context, not(CONTEXT.RECOVERY));

        final String expectedMessage;

        try {
            switch (context) {
                case ACTIVATION:
                    expectedMessage = String.format(
                            HttpOperations.ERROR_MSG_403_ACCOUNT_ACTIVE_DISPLAY_EMAIL,
                            VALID_EMAIL_ADDRESS
                    );
                    // The activation token ID must be non-null to get a valid activation token for the contact.
                    MainActivityBaseSetup.deactivateAccount(TEST_CONTACT_NUMBER);
                    validToken = MainActivityBaseSetup.getValidActivationToken(TEST_CONTACT_NUMBER);
                    MainActivityBaseSetup.activateAccount(TEST_CONTACT_NUMBER);
                    break;
                case SIGN_IN:
                    expectedMessage = HttpOperations.ERROR_MSG_403_ACCOUNT_INACTIVE;
                    MainActivityBaseSetup.deactivateAccount(TEST_CONTACT_NUMBER);
                    break;
                default:
                    fail();
                    return;
            }

            submitToken(validToken, 3000);  // Allow time for the server to respond.

            onView(withText(expectedMessage)).check(matches(isDisplayed()));

            onView(withId(android.R.id.button1)).perform(click());

            assertViews(false, false, false, false);
        }
        finally {
            if (context.equals(CONTEXT.SIGN_IN)) {
                MainActivityBaseSetup.activateAccount(TEST_CONTACT_NUMBER);
            }
        }
    }

    @Test
    public void deviceCodeMismatch() throws Exception {
        Assume.assumeThat(context, is(CONTEXT.RECOVERY));

        final String testContactNumberString = Long.toString(TEST_CONTACT_NUMBER);

        String uri = AndroidTestUtils.getUriPrefix() + "/account/get-device-code";
        final String originalDeviceCode = AndroidTestUtils.postString(uri, testContactNumberString);

        try {
            uri = AndroidTestUtils.getUriPrefix() + "/account/set-bad-device-code";
            AndroidTestUtils.postString(uri, testContactNumberString);

            submitValidToken(validToken);

            onView(withText(HttpOperations.ERROR_MSG_403_DEVICE_CODE_MISMATCH)).check(matches(isDisplayed()));

            onView(withId(android.R.id.button1)).perform(click());

            assertViews(false, false, false, false);
        }
        finally {
            uri = AndroidTestUtils.getUriPrefix() + "/account/set-device-code/" + testContactNumberString;
            AndroidTestUtils.postString(uri, originalDeviceCode);
        }
    }
}
