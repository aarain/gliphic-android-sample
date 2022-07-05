package gliphic.android.utils;

import android.content.Context;
import android.content.Intent;
import android.os.StrictMode;
import android.os.SystemClock;
import android.text.SpannableString;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.platform.app.InstrumentationRegistry;
import gliphic.android.R;
import gliphic.android.display.abstract_views.BaseActivity;
import gliphic.android.display.welcome_screen.SignInActivity;
import gliphic.android.exceptions.NullStaticVariableException;
import gliphic.android.operation.Alerts;
import gliphic.android.operation.Contact;
import gliphic.android.exceptions.ContactException;
import gliphic.android.operation.Group;
import gliphic.android.exceptions.GroupException;
import gliphic.android.operation.TempGlobalStatics;
import gliphic.android.operation.misc.Log;
import gliphic.android.operation.server_interaction.xmpp_server.Connection;
import gliphic.android.operation.server_interaction.http_server.HttpOperations;
import gliphic.android.operation.server_interaction.http_server.RequestGlobalStatic;
import gliphic.android.operation.storage_handlers.AndroidKeyStoreHandler;
import gliphic.android.operation.storage_handlers.SharedPreferencesHandler;
import gliphic.android.utils.view_actions.NestedScrollTo;
import gliphic.android.with_http_server.single_fragment_activity.RegisterActivityTest;

import org.bouncycastle.util.encoders.Base64;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;

import javax.net.ssl.HttpsURLConnection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.GeneralLocation;
import androidx.test.espresso.action.GeneralSwipeAction;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Swipe;
import androidx.test.espresso.matcher.ViewMatchers;
import certificate_pinning.ExtendedSSLSocketFactory;
import certificate_pinning.KeyAndTrustStoresHandler;
import certificate_pinning.X509RestCertificate;
import libraries.BouncyCastleInterpreter;
import libraries.GeneralUtils;
import libraries.GroupPermissions;
import libraries.Vars;
import libraries.Vars.ContactType;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Static utility methods for use in android tests.
 */
public class AndroidTestUtils {

    // These values are copied directly from the test database.
    public static final long CONTACT_WITH_MANY_GROUPS = 43;
    public static final long CONTACT_WITH_ONE_KNOWN_CONTACT = 1874;
    public static final long CONTACT_REGISTERED_INACTIVE = 4333;
    public static final long CONTACT_INACTIVE = 98657;

    @Rule
    public ActivityScenarioRule<SignInActivity> rule = new ActivityScenarioRule<>(SignInActivity.class);

    /**
     * Prepare the emulated device for testing.
     *
     * This should be run if the device has recently had persistent SharedPreferences data erased e.g. via a data wipe
     * or an app uninstall.
     */
//    @Test
    public void prepareDeviceForTests() {
        // Accept the terms of use if they have not already been accepted.

        try {
            onView(withId(R.id.btn_accept_terms_of_use)).perform(NestedScrollTo.nestedScrollTo());
            SystemClock.sleep(200);
            onView(withId(R.id.btn_accept_terms_of_use)).perform(click());
        }
        catch (NoMatchingViewException e) {
            if (e.getMessage() == null || !e.getMessage().contains("btn_accept_terms_of_use")) {
                throw e;
            }
        }

        // Set the device code and device key.

        rule.getScenario().onActivity(activity -> {
            try {
                AndroidKeyStoreHandler.setAppSymmetricKey(activity, true);

                SharedPreferencesHandler.setDeviceCode(activity, getDeviceCode());
                SharedPreferencesHandler.setDeviceKey(activity, getDeviceKey());
            }
            catch (Throwable e) {
                fail(e.getMessage());
            }
        });
    }

    public static final String IGNORE_ANNOTATION_NO_API_KEY =
            "This test must be ignored if the server does not have the API Key (for checking that the email address " +
            "has a valid domain) loaded.";

    public static final String INVALID_CHAR_STRING = "char £ not printable";

    public static final GroupPermissions VALID_PERMISSIONS = GroupPermissions.ACTIVE_OWNER;

    /**
     * Return the application context.
     *
     * This method is useful when used in conjunction with {@link SharedPreferencesHandler} methods which require a
     * {@link Context} instance.
     */
    public static Context getApplicationContext() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    /**
     * Return a new {@link ActivityScenario} by launching an instance of the given class.
     */
    public static ActivityScenario<?> getActivityScenario(@NonNull Class<?> clazz) {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), clazz);

        return ActivityScenario.launch(intent);
    }

    /**
     * Return a string from the strings.xml resource.
     *
     * @param id    The ID of the resource in the res/values/strings.xml file.
     * @return      The string represented by the given ID.
     */
    public static String getResourceString(int id) {
        return ApplicationProvider.getApplicationContext().getResources().getString(id);
    }

    /**
     * Return a view interaction when multiple views match the given R.id.
     *
     * @see #getOnViewInteraction(Matcher)
     */
    public static ViewInteraction getOnViewInteractionFromId(int id) {
        return getOnViewInteraction(withId(id));
    }

    /**
     * Return a view interaction when multiple views match the given R.string.
     *
     * @see #getOnViewInteraction(Matcher)
     */
    public static ViewInteraction getOnViewInteractionFromString(int string) {
        return getOnViewInteraction(withText(string));
    }

    /**
     * Return a view interaction when multiple views match the given matcher.
     *
     * This method is required for activities/fragments which have multiple views matching a single view ID.
     * For example, if multiple fragments extend a base fragment where only the base fragment specifies a XML layout
     * then the test would be unable to determine which extended fragment's view is being referenced, and an
     * {@link androidx.test.espresso.AmbiguousViewMatcherException} exception is thrown with a message containing:
     * "matches multiple views in the hierarchy".
     */
    private static ViewInteraction getOnViewInteraction(Matcher<View> matcher) {
        return onView(allOf(matcher, isDisplayed()));
    }

    /**
     * Press the navigate up button.
     *
     * The "up" button is the left-pointing arrow on the left side of the action bar.
     *
     * Note that the {@link Espresso#pressBack()} does not call the {@link ComponentActivity#onBackPressed()} method,
     * and back navigation is not hte same as up navigation.
     */
    public static void pressUp() {
        onView(withContentDescription(R.string.abc_action_bar_up_description)).perform(click());
    }

    /**
     * Return a matcher at the given index when multiple views match the given matcher.
     *
     * If multiple tabs exist for the given matcher, the indices are matched from left to right in ascending order.
     *
     * Note that a view will be matched if it exists in the current activity even if the view is not visible, so the
     * given index should account for this.
     *
     * Example usage - assert that the third instance of a view with ID some_id is displayed:
     *     onView(withIndex(withId(R.id.some_id), 2)).check(matches(isDisplayed()));
     *
     * @param matcher   The matcher to match.
     * @param index     The specific index of the given matcher to match.
     * @return          The single matcher corresponding to the given matcher and index.
     */
    public static Matcher<View> withIndex(final Matcher<View> matcher, final int index) {
        return new TypeSafeMatcher<View>() {
            int currentIndex = 0;

            @Override
            public void describeTo(Description description) {
                description.appendText("with index: ");
                description.appendValue(index);
                matcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                return matcher.matches(view) && currentIndex++ == index;
            }
        };
    }

    /**
     * Swipe up the screen, from bottom to top.
     *
     * @return  A ViewAction to perform.
     *          The following is an example usage such that this method's return value is defined as 'returnValue':
     *          onView(withId(R.id.viewpager_activity_base)).perform(returnValue);
     */
    public static ViewAction swipeUp() {
        return new GeneralSwipeAction(
                Swipe.FAST,
                GeneralLocation.CENTER,
                view -> {
                    float[] coordinates = GeneralLocation.BOTTOM_CENTER.calculateCoordinates(view);
//                    coordinates[1] = view.getContext().getResources().getDisplayMetrics().heightPixels;
                    coordinates[1] = 0;
                    return coordinates;
                },
                Press.FINGER
        );
    }

    public static String generateContactName(long contactNumber) {
        StringBuilder nameSb = new StringBuilder(contactNumber + "conName");
        if (nameSb.length() > Vars.CONTACT_NAME_MAX_LEN) {
            throw new IllegalArgumentException();
        }
        while (nameSb.length() < Vars.CONTACT_NAME_MAX_LEN) {
            if (nameSb.length() % 2 == 0) {
                nameSb.append("_");
            }
            else {
                nameSb.append("-");
            }
        }

        return nameSb.toString();
    }

    public static String generateContactId(long contactNumber) {
        return String.format("%dABCDEFGHIJKLMNOPQRSTUVWXY", contactNumber).substring(0, Vars.CONTACT_ID_LEN);
    }

    public static String generateGroupId(long groupNumber) {
        return String.format("%dABCDEFGHIJKLMNOPQRSTUVWXYZ", groupNumber).substring(0, Vars.GROUP_ID_LEN);
    }

    public static Contact generateCurrentContact() throws ContactException {
        final long number = 0;

        return new Contact(
                number,
                generateContactId(number),
                "Test User",
                Vars.DisplayPicture.getRandom().get(),
                ContactType.CURRENT,
                true
        );
    }

    public static Contact generateKnownContact() throws ContactException {
        final long number = 46872;

        return new Contact(
                number,
                generateContactId(number),
                String.format("knownContactName %d", number),
                Vars.DisplayPicture.getRandom().get(),
                ContactType.KNOWN,
                true
        );
    }

    public static Contact generateExtendedContact() throws ContactException {
        final long number = 66873;

        return new Contact(
                number,
                generateContactId(number),
                String.format("extendedContactName %d", number),
                Vars.DisplayPicture.getRandom().get(),
                ContactType.EXTENDED,
                true
        );
    }

    public static Group generateGroup() throws GroupException {
        return new Group(
                111245,
                Vars.DisplayPicture.getRandom().get(),
                "validName 111245",
                "validDescription 111245",
                generateGroupId(111245),
                VALID_PERMISSIONS,
                false,
                false,
                true
        );
    }

    /**
     * Create enough groups to reach the groups limit (client-side).
     *
     * @param firstGroupNumber  The first new group number to create groups for (subsequent group number are
     *                          monotonically increasing).
     * @throws GroupException   Thrown when creating a new group.
     */
    public static void createManyGroups(long firstGroupNumber) throws GroupException {
        for (long l = firstGroupNumber; l < firstGroupNumber + Vars.CONTACT_GROUPS_LIMIT; l++) {
            new Group(
                    l,
                    Vars.DisplayPicture.getRandom().get(),
                    String.format("group name %d", l),
                    String.format("group description %d", l),
                    generateGroupId(l),
                    GroupPermissions.ACTIVE_OWNER,
                    false,
                    false,
                    true
            ).storeStatically();
        }
    }

    public static Contact removeContact(long contactNumber) throws ContactException, NullStaticVariableException {
        Contact contact = Contact.getContactFromNumber(contactNumber);
        Contact.getKnownContacts().remove(contact);
        Contact.getExtendedContacts().remove(contact);
        return contact;
    }

    public static void clearStaticLists() {
        Alerts.setNullGroupShares();
        Group.setNullKnownGroups();
        Group.setNullSelectedGroup();
        Contact.setNullCurrentContact();
        Contact.setNullKnownContacts();
        Contact.setNullExtendedContacts();
        TempGlobalStatics.setNullTempGlobalStatics();
    }

    /**
     * Return the expected device key stored on the test device.
     *
     * This matches the hard-coded value in the server test database.
     */
    public static byte[] getDeviceKey() {
        return new byte[] {
                (byte)0x60, (byte)0x3a, (byte)0xa6, (byte)0x77, (byte)0xa1, (byte)0x99, (byte)0xd5, (byte)0xba,
                (byte)0x58, (byte)0x0b, (byte)0xf3, (byte)0x64, (byte)0xb2, (byte)0x55, (byte)0xa5, (byte)0x5c,
                (byte)0x50, (byte)0x3d, (byte)0x56, (byte)0x62, (byte)0xc3, (byte)0xaa, (byte)0xf5, (byte)0xbf,
                (byte)0x98, (byte)0x0c, (byte)0x93, (byte)0x89, (byte)0xd4, (byte)0xbb, (byte)0x4f, (byte)0x89
        };
    }

    /**
     * Return the expected device code stored on the test device.
     *
     * This matches the hard-coded value in the server test database.
     */
    public static byte[] getDeviceCode() {
        return new byte[] {
                (byte)0x19, (byte)0x20, (byte)0xff, (byte)0xbb, (byte)0x5a, (byte)0x9c, (byte)0x67, (byte)0x55,
                (byte)0xe5, (byte)0x91, (byte)0x36, (byte)0xaa, (byte)0xaa, (byte)0x5a, (byte)0x9a, (byte)0x78,
                (byte)0x19, (byte)0x20, (byte)0xff, (byte)0xbb, (byte)0x5a, (byte)0x9c, (byte)0x67, (byte)0x55,
                (byte)0xe5, (byte)0x91, (byte)0x36, (byte)0xaa, (byte)0xaa, (byte)0x5a, (byte)0x9a, (byte)0x78
        };
    }

    /**
     *
     * @return  True iff the device currently has a connection to the XMPP server with valid user credentials.
     */
    public static boolean isExistingXmppConnection() {
        final Connection.ConnectionState connectionState = Connection.connectionState;

        return connectionState != null && connectionState.equals(Connection.ConnectionState.AUTHENTICATED);
    }

    public enum WaitAction {SIGN_IN, XMPP_AUTH, SERVER_CONNECTION_FAILED_MSG}

    /**
     * Wait for some condition to occur up to some maximum amount of time. This condition is represented by the given
     * enum.
     *
     * This method periodically sleeps and checks the appropriate condition at the beginning of each period; if the
     * condition is valid then the method returns successfully, if not the method waits for the next period.
     * If the maximum amount of time elapses then the method throws the most recent throwable.
     *
     * This method uses an assert to check for the desired condition.

     * @param waitAction    The type of action to wait for.
     * @throws Throwable    Any throwable occurring in this method, usually as a result of an assertion.
     */
    public static void waitForAction(@NonNull WaitAction waitAction) throws Throwable {
        final long maxSleepTimeSeconds;     // the maximum number of seconds to wait before throwing an exception.
        final long sleepTimeSeconds;        // The number of seconds to wait before re-asserting.

        switch (waitAction) {
            case SIGN_IN:
                maxSleepTimeSeconds = 20;
                sleepTimeSeconds    = 1;
                break;
            case XMPP_AUTH:
                // Allow the XMPP connection time to connect and authenticate.
                maxSleepTimeSeconds = 100;
                sleepTimeSeconds    = 5;
                break;
            case SERVER_CONNECTION_FAILED_MSG:
                maxSleepTimeSeconds = 10;
                sleepTimeSeconds    = 1;
                break;
            default:
                throw new InvalidParameterException(String.format("Invalid action: %s.", waitAction.name()));
        }

        Throwable throwable = null;

        long currentSleepTimeSeconds = 0;
        while (currentSleepTimeSeconds < maxSleepTimeSeconds) {
            SystemClock.sleep(sleepTimeSeconds * 1000);
            currentSleepTimeSeconds += sleepTimeSeconds;

            try {
                switch (waitAction) {
                    case SIGN_IN:
                        onView(ViewMatchers.withText(R.string.main_workspace)).check(matches(isDisplayed()));
                        break;
                    case XMPP_AUTH:
                        assertThat(Connection.connectionState, is(Connection.ConnectionState.AUTHENTICATED));
                        break;
                    case SERVER_CONNECTION_FAILED_MSG:
                        onView(ViewMatchers.withText("Server connection failed")).check(matches(isDisplayed()));
                        onView(withId(android.R.id.button1)).perform(click());
                        SystemClock.sleep(100);
                        break;
                    default:
                        throw new Error(new IOException("The default switch case should be impossible."));
                }

                return;
            }
            catch (Throwable e) {
                throwable = e;
                // Allow the assert to keep failing until it either passes or the maximum sleep time has elapsed.
            }
        }

        throw throwable;
    }

    /**
     * Set the access token expiry and refresh token stored in SharedPreferences.
     *
     * This method is usually used to test a unauthorized server response, or to clean up a test which does so.
     *
     * @param accessTokenExpiry     The access token expiry time to set.
     * @param refreshToken          The refresh token to set.
     */
    public static void setAccessTokenExpiryAndRefreshToken(long accessTokenExpiry,
                                                           String refreshToken) throws Exception {

        final Context context = AndroidTestUtils.getApplicationContext();

        SharedPreferencesHandler.setAccessTokenExpiry(context, accessTokenExpiry);
        SharedPreferencesHandler.setRefreshToken(context, refreshToken);
        SystemClock.sleep(200);
    }

    /**
     * For a given contact, request the encryption salt from the server and use this to set the data encryption key in
     * SharedPreferences.
     *
     * @param context           The calling context.
     * @param contactNumber     The number of the contact to request the encryption salt for.
     * @throws Exception        Thrown throughout this method.
     */
    public static void setDataEncryptionKey(@NonNull Context context, long contactNumber) throws Exception {
        final String response = AndroidTestUtils.postString(
                AndroidTestUtils.getUriPrefix() + "/load/encryption-salt",
                Long.toString(contactNumber)
        );

        final byte[] newDataEncryptionKey = BouncyCastleInterpreter.scryptGenerate(
                RegisterActivityTest.validPwd.getBytes(StandardCharsets.UTF_8),
                Base64.decode(response)
        );

        SharedPreferencesHandler.setDataEncryptionKey(context, newDataEncryptionKey);

        SystemClock.sleep(200);     // Allow time for the write operation to SharedPreferences.
    }

    private static String sendHttpRequest(String url, String body, boolean post) throws Exception {
        // Overriding the default behaviour and running network operations on the main thread instead of asynchronously
        // is not recommended for the release version but is used here to block the main thread until a response is
        // received from the server. Without this, the call to HttpsURLConnection.getOutputStream() throws a
        // NetworkOnMainThreadException exception.
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());

        URL obj = new URL(url);
        HttpsURLConnection conn = (HttpsURLConnection) obj.openConnection();

        conn.setSSLSocketFactory(new ExtendedSSLSocketFactory(
                KeyAndTrustStoresHandler.initKeyAndTrustStores(Vars.ANDROID_KEY_STORE, new X509RestCertificate())
        ));
        conn.setHostnameVerifier((hostname, session) -> true);

        conn.setRequestProperty("Content-Type", "text/plain; charset=UTF-8");
        conn.setRequestProperty("Accept", "text/plain");
//        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
//        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
//        conn.setRequestProperty("Request-Type", "Test clean-up");
//
//        String urlParameters = "sn=C02G8416DRJM&cn=&locale=&caller=&num=12345";

        // Add request header.
        if (post) {
            conn.setRequestMethod("POST");

            if (body != null) {
                // Send post request.
                conn.setDoOutput(true);
                DataOutputStream outputStream = new DataOutputStream(conn.getOutputStream());
                outputStream.write(body.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
                outputStream.close();
            }
        }
        else {
            conn.setRequestMethod("GET");
        }

        int responseCode = conn.getResponseCode();
        Log.e("*** HTTPS request ***", "Sending HTTP request to URL : " + url);
//        Log.e("*** Test HTTPS POST ***", "Post parameters : " + urlParameters);
        Log.e("*** HTTPS request ***", "Response Code : " + responseCode);

        if (responseCode < 400) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = bufferedReader.readLine()) != null) {
                response.append(inputLine);
            }
            bufferedReader.close();

            return response.toString();
        }
        else {
            return Integer.toString(responseCode);
        }
    }

    /**
     * Send a JSONObject in a HTTP POST message to the server without using Volley, for test purposes only.
     *
     * @param url           The URL string to send the request to.
     * @param string        The string to POST to the server.
     * @return              The response string, which may be a JSON object.
     * @throws IOException  Thrown for various read and write operations.
     */
    public static String postString(@NonNull String url, @Nullable String string) throws Exception {
        return sendHttpRequest(url, string, true);
    }

    /**
     * Send a string in a HTTP POST message to the server without using Volley, for test purposes only.
     *
     * The given object to POST is converted to a JSON string.
     *
     * @param url           The URL string to send the request to.
     * @param object        The object to POST to the server.
     * @return              The response string, which may be a JSON object.
     * @throws Exception    Thrown for various read and write operations.
     */
    public static String postObject(@NonNull String url, @Nullable Object object) throws Exception {
        return sendHttpRequest(url, object == null ? null : GeneralUtils.toJson(object), true);
    }

    /**
     * Send a JSONObject in a HTTP GET message to the server without using Volley, for test purposes only.
     *
     * @param url               The URL string to send the request to.
     * @return                  The response string, which may be a JSON object.
     * @throws IOException      Thrown for various read and write operations.
     */
    public static String getRequest(@NonNull String url) throws Exception {
        return sendHttpRequest(url, null, false);
    }

    // Convenience method to get the value of a private member variable using reflection.
    public static Object getPrivateObjectUsingReflection(Object cInstance, String varName)
            throws IllegalAccessException, NoSuchFieldException {

        Field field = cInstance.getClass().getDeclaredField(varName);
        field.setAccessible(true);
        return field.get(cInstance);
    }

    // Convenience method to set the value of a private member variable using reflection.
    public static void setPrivateObjectUsingReflection(Object cInstance, String varName, Object newVar)
            throws IllegalAccessException, NoSuchFieldException {

        Field field = cInstance.getClass().getDeclaredField(varName);
        field.setAccessible(true);
        field.set(cInstance, newVar);
    }

    /**
     * Use reflection to get the private static final strings representing the HTTP server's address and port number.
     *
     * @return  The string representing the destination URI prefix for all HTTP requests.
     */
    public static String getUriPrefix() throws IllegalAccessException, NoSuchFieldException {
        String uriServerString = (String) AndroidTestUtils.getPrivateObjectUsingReflection(
                new HttpOperations(),
                "SERVER"
        );

        String uriPortString = (String) AndroidTestUtils.getPrivateObjectUsingReflection(
                new HttpOperations(),
                "PORT"
        );

        return uriServerString + uriPortString;
    }

    /**
     * An interface allowing a long return value from a method which contains another interface.
     */
    public interface LongCallback {
        void onReturn(final long l);
    }

    /**
     * Get the sign-in time for the currently signed-in contact by requesting it from the server.
     *
     * If the contact is not signed-in or if the server is unavailable then a default value will be returned.
     *
     * The sign-in time is returned via the given callback argument (hence the return value of this method is void).
     *
     * @param callback  The callback allowing the sign-in time to be returned as a long value.
     * @param activity  The calling activity.
     */
    public static void getSignInTime(LongCallback callback, BaseActivity activity) {
        final long defaultSignInTime = System.currentTimeMillis();

        RequestGlobalStatic.requestAndSetAccessToken(
                accessToken -> {
                    if (accessToken == null) {
                        callback.onReturn(defaultSignInTime);
                        return;
                    }

                    HttpOperations.post(
                            HttpOperations.URI_LOAD_SIGN_IN_TIME,
                            accessToken,
                            activity,
                            response -> {
                                try {
                                    callback.onReturn(GeneralUtils.fromJson(response, Long.class));
                                }
                                catch (Exception e) {
                                    callback.onReturn(defaultSignInTime);
                                }
                            },
                            error -> callback.onReturn(defaultSignInTime)
                    );
                },
                activity,
                null,
                false
        );
    }

    /**
     * Click on a clickable substring within a TextView (i.e. click on a SpannableString).
     *
     * Behaviour is unknown when there are multiple substrings which equal textToClick in the TextView.
     *
     * Source: https://stackoverflow.com/questions/38314077/how-to-click-a-clickablespan-using-espresso
     *
     * @param textToClick   The substring within the TextView to click on.
     * @return              The overridden ViewAction.
     */
    public static ViewAction clickClickableSpan(final CharSequence textToClick) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return Matchers.instanceOf(TextView.class);
            }

            @Override
            public String getDescription() {
                return "clicking on a ClickableSpan";
            }

            @Override
            public void perform(UiController uiController, View view) {
                TextView textView = (TextView) view;

                // Fix: https://stackoverflow.com/questions/17882077/how-to-convert-spannedstring-to-spannable
                SpannableString spannableString = new SpannableString(textView.getText());

                if (spannableString.length() == 0) {
                    // TextView is empty, nothing to do
                    throw new NoMatchingViewException.Builder()
                            .includeViewHierarchy(true)
                            .withRootView(textView)
                            .build();
                }

                // Get the links inside the TextView and check if we find textToClick
                ClickableSpan[] spans = spannableString.getSpans(
                        0,
                        spannableString.length(),
                        ClickableSpan.class
                );
                if (spans.length > 0) {
                    ClickableSpan spanCandidate;
                    for (ClickableSpan span : spans) {
                        spanCandidate = span;
                        int start = spannableString.getSpanStart(spanCandidate);
                        int end = spannableString.getSpanEnd(spanCandidate);
                        CharSequence sequence = spannableString.subSequence(start, end);
                        if (textToClick.toString().equals(sequence.toString())) {
                            span.onClick(textView);
                            return;
                        }
                    }
                }

                // textToClick not found in TextView
                throw new NoMatchingViewException.Builder()
                        .includeViewHierarchy(true)
                        .withRootView(textView)
                        .build();
            }
        };
    }
}
