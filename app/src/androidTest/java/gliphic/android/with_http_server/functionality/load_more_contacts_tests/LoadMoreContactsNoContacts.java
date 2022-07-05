package gliphic.android.with_http_server.functionality.load_more_contacts_tests;

import android.os.SystemClock;

import androidx.test.espresso.contrib.RecyclerViewActions;
import gliphic.android.R;
import gliphic.android.display.welcome_screen.SignInActivity;
import gliphic.android.utils.AndroidTestUtils;
import gliphic.android.utils.BaseActivityTestRule;
import gliphic.android.utils.MainActivityBaseSetup;
import gliphic.android.with_http_server.functionality.SignInAndOutTest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import pojo.misc.ContactAndGroupNumberPair;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.core.IsNot.not;

@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class LoadMoreContactsNoContacts {
    private static final long SOURCE_CONTACT_NUMBER = 0;
    private static final long UNKNOWN_CONTACT_NUMBER = 3999;
    private static final long GROUP_IN_COMMON = 1;
    private static final String TARGET_CONTACT_NUMBER = Long.toString(AndroidTestUtils.CONTACT_REGISTERED_INACTIVE);
    private static final ContactAndGroupNumberPair CONTACT_AND_GROUP_NUMBER_PAIR = new ContactAndGroupNumberPair(
            AndroidTestUtils.CONTACT_REGISTERED_INACTIVE,
            GROUP_IN_COMMON
    );

    private static String URI_GROUP_PREFIX;
    private static String URI_LOAD_PREFIX;

    @Rule
    public BaseActivityTestRule<SignInActivity> rule = new BaseActivityTestRule<>(SignInActivity.class);

    private static String originalDeviceCodeString;

    @BeforeClass
    public static void beforeClass() throws Exception {
        final String uriPrefix = AndroidTestUtils.getUriPrefix();
        URI_GROUP_PREFIX = uriPrefix + "/group/";
        URI_LOAD_PREFIX = uriPrefix + "/load/";

        // Required for noContactsToDisplay().
        originalDeviceCodeString = MainActivityBaseSetup.getDeviceCode(UNKNOWN_CONTACT_NUMBER);
        MainActivityBaseSetup.setDeviceCode(UNKNOWN_CONTACT_NUMBER, AndroidTestUtils.getDeviceCode());

        // Required for registeredButInactiveContactNotDisplayed().
        AndroidTestUtils.postString(
                URI_LOAD_PREFIX + "insert-chosen-known-contact/" + SOURCE_CONTACT_NUMBER,
                TARGET_CONTACT_NUMBER
        );
        AndroidTestUtils.postObject(
                URI_GROUP_PREFIX + "add-invalid-contact-group",
                CONTACT_AND_GROUP_NUMBER_PAIR
        );
    }

    @AfterClass
    public static void afterClass() throws Exception {
        // Required for noContactsToDisplay().
        MainActivityBaseSetup.setDeviceCode(UNKNOWN_CONTACT_NUMBER, originalDeviceCodeString);

        // Required for registeredButInactiveContactNotDisplayed().
        AndroidTestUtils.postString(
                URI_LOAD_PREFIX + "delete-chosen-known-contact/" + SOURCE_CONTACT_NUMBER,
                TARGET_CONTACT_NUMBER
        );
        AndroidTestUtils.postObject(
                URI_GROUP_PREFIX + "remove-temporary-contact-group/",
                CONTACT_AND_GROUP_NUMBER_PAIR
        );
    }

    @Test
    public void noContactsToDisplay() throws Throwable {
        SignInAndOutTest.signOut();
        SignInAndOutTest.signIn(UNKNOWN_CONTACT_NUMBER);

        SystemClock.sleep(200);
        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
        SystemClock.sleep(200);
        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
        SystemClock.sleep(200);

        // View(s) which are not displayed.
        onView(ViewMatchers.withId(R.id.edittext_main_tab_contacts)).check(matches(not(isDisplayed())));
        onView(ViewMatchers.withId(R.id.contacts_tab_error_contact)).check(matches(not(isDisplayed())));
        // View(s) which are displayed.
        onView(ViewMatchers.withId(R.id.textview_load_more_contacts)).check(matches(isDisplayed()));
        onView(ViewMatchers.withId(R.id.textview_no_contacts)).check(matches(isDisplayed()));

        onView(ViewMatchers.withText(R.string.no_contacts)).check(matches(isDisplayed()));
    }

    @Test
    public void registeredButInactiveContactNotDisplayed() {
        handleAssertNoContactsDisplayed(AndroidTestUtils.CONTACT_REGISTERED_INACTIVE);
    }

    @Test
    public void inactiveContactNotDisplayed() throws Exception {
        final long targetContactNumber = 1322;

        try {
            MainActivityBaseSetup.deactivateAccount(targetContactNumber);

            handleAssertNoContactsDisplayed(targetContactNumber);
        }
        finally {
            MainActivityBaseSetup.activateAccount(targetContactNumber);
        }
    }

    private void handleAssertNoContactsDisplayed(long targetContactNumber) {
        final String targetContactSearch = Long.toString(targetContactNumber);
        final int commonGroupRecyclerViewPosition = 1;

        // Assert that no matching contact is displayed.
        onView(ViewMatchers.withText(R.string.main_contacts)).perform(click());
        SystemClock.sleep(200);
        onView(withId(R.id.edittext_main_tab_contacts)).perform(replaceText(targetContactSearch));
        SystemClock.sleep(1000);
        onView(ViewMatchers.withId(R.id.recyclerview_main_tab_contacts)).check(matches(not(isDisplayed())));

        // Assert that no matching group-contact is displayed.
        onView(ViewMatchers.withText(R.string.main_groups)).perform(click());
        SystemClock.sleep(200);
        onView(withId(R.id.recyclerview_main_tab_groups))
                .perform(RecyclerViewActions.actionOnItemAtPosition(commonGroupRecyclerViewPosition, click()));
        SystemClock.sleep(200);
        onView(ViewMatchers.withText(R.string.group_contacts)).perform(click());
        SystemClock.sleep(200);
        onView(withId(R.id.edittext_group_contacts)).perform(replaceText(targetContactSearch));
        SystemClock.sleep(1000);
        onView(ViewMatchers.withId(R.id.recyclerview_group_details_tab_target_contacts))
                .check(matches(not(isDisplayed())));
    }
}
