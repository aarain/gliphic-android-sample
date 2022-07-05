package gliphic.android.with_http_server.functionality;

import android.os.SystemClock;

import gliphic.android.R;
import gliphic.android.display.welcome_screen.SignInActivity;
import gliphic.android.operation.server_interaction.http_server.HttpOperations;
import gliphic.android.utils.AndroidTestUtils;
import gliphic.android.utils.view_assertions.AssertRecyclerViewItemCount;
import gliphic.android.utils.MainActivityBaseSetup;
import gliphic.android.utils.SignInAndOutTestRule;

import org.bouncycastle.util.encoders.Base64;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import libraries.GeneralUtils;
import libraries.Vars;
import pojo.group.ShareGroupGetKeyRequest;
import pojo.group.ShareGroupGetKeyResponse;
import pojo.group.ShareGroupRequest;
import pojo.misc.ContactAndGroupNumberPair;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class IsFirstOnNetworkAvailableTest {
    private static final long TARGET_CONTACT_NUMBER = 4;
    private static final long GROUP_NUMBER = 4;
    private static String URI_GROUP_PREFIX;

    @Rule
    public SignInAndOutTestRule<SignInActivity> rule = new SignInAndOutTestRule<>(SignInActivity.class);

    private static String originalDeviceCodeString;

    @BeforeClass
    public static void beforeClass() throws Exception {
        URI_GROUP_PREFIX = AndroidTestUtils.getUriPrefix() + "/group/";

        originalDeviceCodeString = MainActivityBaseSetup.getDeviceCode(TARGET_CONTACT_NUMBER);
        MainActivityBaseSetup.setDeviceCode(TARGET_CONTACT_NUMBER, AndroidTestUtils.getDeviceCode());
    }

    @AfterClass
    public static void afterClass() throws Exception {
        MainActivityBaseSetup.setDeviceCode(TARGET_CONTACT_NUMBER, originalDeviceCodeString);
    }

    @Test
    public void alertsTab() throws Throwable {
        final String accessTokenContact0 = MainActivityBaseSetup.getValidAccessToken(0);

        SignInAndOutTest.signIn(TARGET_CONTACT_NUMBER);

        try {
            // Navigate away from the main activity to another activity.

            openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
            onView(withText(R.string.action_bar_contact_profile)).perform(click());

            // Get the public key for the target contact.

            final ShareGroupGetKeyRequest shareGroupGetKeyRequest = new ShareGroupGetKeyRequest(
                    accessTokenContact0,
                    TARGET_CONTACT_NUMBER,
                    GROUP_NUMBER
            );

            final String response = AndroidTestUtils.postString(
                    HttpOperations.URI_GROUP_SHARE_GET_KEYS,
                    GeneralUtils.toJson(shareGroupGetKeyRequest)
            );

            SystemClock.sleep(1000);

            // Submit a group-share request for the target contact and group number.

            final ShareGroupGetKeyResponse sggkr = GeneralUtils.fromJson(response, ShareGroupGetKeyResponse.class);

            AndroidTestUtils.postString(
                    HttpOperations.URI_GROUP_SHARE_SUBMIT,
                    GeneralUtils.toJson(
                            new ShareGroupRequest(
                                    accessTokenContact0,
                                    TARGET_CONTACT_NUMBER,
                                    GROUP_NUMBER,
                                    Base64.toBase64String(new byte[Vars.PUB_ENC_AES_KEY_LEN]),
                                    sggkr.getPublicKeyString()
                            )
                    )
            );

            SystemClock.sleep(1000);

            // Navigate back to the main activity to assert that the alerts tab updates.

            pressBack();
            SystemClock.sleep(200);

            onView(withId(R.id.recyclerview_main_tab_alerts)).check(new AssertRecyclerViewItemCount(1));
        }
        finally {
            final ContactAndGroupNumberPair contactAndGroupNumberPair = new ContactAndGroupNumberPair(
                    TARGET_CONTACT_NUMBER,
                    GROUP_NUMBER
            );

            AndroidTestUtils.postObject(
                    URI_GROUP_PREFIX + "delete-specific-group-share",
                    contactAndGroupNumberPair
            );

            SignInAndOutTest.signOut();
        }
    }
}
