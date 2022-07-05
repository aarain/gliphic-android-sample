package gliphic.android.with_http_server.functionality.load_more_contacts_tests;

import android.os.SystemClock;
import android.view.View;

import gliphic.android.R;
import gliphic.android.adapters.ContactsAdapter;
import gliphic.android.exceptions.ContactException;
import gliphic.android.operation.storage_handlers.SharedPreferencesHandler;
import gliphic.android.display.welcome_screen.SignInActivity;
import gliphic.android.operation.Contact;
import gliphic.android.utils.AndroidTestUtils;
import gliphic.android.utils.MainActivityBaseSetup;
import gliphic.android.utils.matchers.ToastMatcher;
import gliphic.android.utils.view_actions.NestedScrollTo;
import gliphic.android.with_http_server.LoadMoreObjectsTestInvalidRefreshTokenRule;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import gliphic.android.utils.view_actions.SetViewVisibility;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withSubstring;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static gliphic.android.utils.AndroidTestUtils.getOnViewInteractionFromId;
import static gliphic.android.utils.AndroidTestUtils.swipeUp;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class LoadMoreContactsWithBadAccessAndRefreshTokenTest {

    @Rule
    public LoadMoreObjectsTestInvalidRefreshTokenRule<SignInActivity> rule =
            new LoadMoreObjectsTestInvalidRefreshTokenRule<>(SignInActivity.class);

    // A contact number and name which are expected to not be loaded at the beginning of the test but loaded during it.
    private static final long loadedContactNumber = 99;
    private static final String loadedContactName = "99conName";

    public static void swipeToAllContactsTab() {
        onView(ViewMatchers.withText(R.string.main_contacts)).perform(click());
        SystemClock.sleep(1000);    // Give the swipe time to change tab focus and find R.id.*
    }

    public static void swipeToGroupContactsTab(Integer expectedGroupPosition) {
        if (expectedGroupPosition == null) {
            expectedGroupPosition = 1;
        }

        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
        SystemClock.sleep(1000);    // Give the swipe time to change tab focus and find R.id.*
        onView(withId(R.id.recyclerview_main_tab_groups))
                .perform(RecyclerViewActions.actionOnItemAtPosition(expectedGroupPosition, click()));
        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
        SystemClock.sleep(1000);    // Give the swipe time to change tab focus and find R.id.*
    }

    public static void loadMoreAllContacts() {
        loadMoreItems(R.id.more_contacts_progress_bar);
    }

    public static void loadMoreGroupContacts() {
        loadMoreItems(R.id.more_group_contacts_progress_bar);
    }

    private static void loadMoreItems(int progressBarId) {
        onView(withId(progressBarId)).perform(SetViewVisibility.setViewVisibility(View.VISIBLE));
        onView(withId(progressBarId)).perform(NestedScrollTo.nestedScrollTo());
        getOnViewInteractionFromId(R.id.viewpager_activity_base).perform(swipeUp());
        // Any (failure) toast message should still be on the screen after 1000 milliseconds.
        SystemClock.sleep(1000);
    }

    /**
     * Check that the a given contact object is available internally via its contact number.
     */
    public static void checkContactIsInInternalList() throws ContactException {
        Contact.getContactFromNumber(loadedContactNumber);
    }

    /**
     * Check that the a given contact object is not available internally via its contact number.
     */
    public static void checkContactNotInInternalList() {
        try {
            Contact.getContactFromNumber(loadedContactNumber);
            String s = "Contact number should not be already loaded: " + loadedContactNumber;
            throw new RuntimeException(s);
        }
        catch (ContactException e) {
            // This contact should not yet be loaded, so ignore these exceptions.
        }
    }

    /**
     * Check that a RecyclerView displays an expected string within a contact name, after loading more contacts via a
     * server request.
     *
     * @param lowerViewId   Any view id which is below the last item in the RecyclerView and within the
     *                      NestedScrollView.
     */
    public static void checkContactIsDisplayed(int lowerViewId) {
        onView(withId(lowerViewId)).perform(SetViewVisibility.setViewVisibility(View.VISIBLE));
        onView(withId(lowerViewId)).perform(NestedScrollTo.nestedScrollTo());

        SystemClock.sleep(200);

        onView(withSubstring(loadedContactName)).check(matches(isDisplayed()));
    }

    @After
    public void nullGlobalStatics() {
        AndroidTestUtils.clearStaticLists();
    }

    @AfterClass
    public static void resetRefreshToken() {
        try {
            final String refreshToken = MainActivityBaseSetup.getValidRefreshToken(0);

            SharedPreferencesHandler.setRefreshToken(AndroidTestUtils.getApplicationContext(), refreshToken);
        }
        catch (Throwable e) {
            fail(e.getMessage());
        }
    }

    // Valid operations.

    @Test
    public void loadAllContactsWithExpiredAccessTokenAndInvalidRefreshToken() {
        swipeToAllContactsTab();

        loadMoreAllContacts();

        String s = ContactsAdapter.GENERIC_LOAD_CONTACTS_FAILED_MSG;
        onView(withText(s)).inRoot(new ToastMatcher()).check(matches(isDisplayed()));

        // Check that a loaded contact is not in the internal list.
        try {
            Contact.getContactFromNumber(loadedContactNumber);
            throw new RuntimeException("Contact number should not be already loaded: " + loadedContactNumber);
        }
        catch (ContactException e) {
            // This contact should not yet be loaded, so ignore these exceptions.
        }
    }

    @Test
    public void loadGroupContactsWithExpiredAccessTokenAndInvalidRefreshToken() {
        swipeToGroupContactsTab(null);

        loadMoreGroupContacts();

        String s = ContactsAdapter.GENERIC_LOAD_CONTACTS_FAILED_MSG;
        onView(withText(s)).inRoot(new ToastMatcher()).check(matches(isDisplayed()));

        // Check that a loaded contact is not in the internal list.
        try {
            Contact.getContactFromNumber(loadedContactNumber);
            throw new RuntimeException("Contact number should not be already loaded: " + loadedContactNumber);
        }
        catch (ContactException e) {
            // This contact should not yet be loaded, so ignore these exceptions.
        }
    }
}
