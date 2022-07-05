package gliphic.android.with_http_server.functionality.load_more_contacts_tests;

import android.os.SystemClock;

import androidx.annotation.NonNull;
import gliphic.android.R;
import gliphic.android.adapters.ContactsAdapter;
import gliphic.android.display.welcome_screen.SignInActivity;
import gliphic.android.operation.Contact;
import gliphic.android.utils.AndroidTestUtils;
import gliphic.android.utils.view_assertions.AssertRecyclerViewItemCount;
import gliphic.android.utils.MainActivityBaseSetup;
import gliphic.android.with_http_server.LoadMoreObjectsTestRule;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;

import androidx.test.filters.LargeTest;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(Parameterized.class)
@LargeTest
public class LoadMoreContactsParameterizedTest extends MainActivityBaseSetup {
    private static final String gdSS = "99conName";     // A search string which will yield 1 contact.

    // Parametrised values for each test.
    private boolean knownContactsChecked;
    private boolean extendedContactsChecked;
    private String searchString;
    private Long groupNum;

    public LoadMoreContactsParameterizedTest(@NonNull TestParams testParams) {
        this.knownContactsChecked = testParams.isKnownContactsChecked();
        this.extendedContactsChecked = testParams.isExtendedContactsChecked();
        this.searchString = testParams.getSearchString();
        this.groupNum = testParams.getGroupNumber();
    }

    /**
     * This class replaces direct calls to creating an Object[][] array when setting the test parameters.
     *
     * Without this class the test-class constructor is unable to differentiate between a null String and a null Long,
     * throwing a {@link IllegalArgumentException} exception with the message:
     *     <init> argument 4 has type java.lang.Long, got java.lang.String
     */
    private static class TestParams {
        private boolean knownContactsChecked;
        private boolean extendedContactsChecked;
        private String searchString;
        private Long groupNumber;

        public TestParams(boolean knownContactsChecked,
                          boolean extendedContactsChecked,
                          String searchString,
                          Long groupNumber) {

            this.knownContactsChecked = knownContactsChecked;
            this.extendedContactsChecked = extendedContactsChecked;
            this.searchString = searchString;
            this.groupNumber = groupNumber;
        }

        public boolean isKnownContactsChecked() {
            return knownContactsChecked;
        }

        public boolean isExtendedContactsChecked() {
            return extendedContactsChecked;
        }

        public String getSearchString() {
            return searchString;
        }

        public Long getGroupNumber() {
            return groupNumber;
        }
    }

    @Parameterized.Parameters
    public static List<TestParams> testCases() {
        return Arrays.asList(
                new TestParams(true,  true,  null,  null),
                new TestParams(true,  true,  null,  null),
                new TestParams(true,  false, null,  null),
                new TestParams(false, true,  null,  null),
//                new TestParameters(false, false, null, null),   // Both CheckBoxes cannot be unchecked.
                new TestParams(true,  true,  gdSS,        null),
                new TestParams(true,  true,  null,  1L));
    }

    @Rule
    public LoadMoreObjectsTestRule<SignInActivity> rule = new LoadMoreObjectsTestRule<>(SignInActivity.class);

    @After
    public void nullGlobalStatics() {
        new LoadMoreContactsWithBadAccessAndRefreshTokenTest().nullGlobalStatics();
    }

    @AfterClass
    public static void resetRefreshToken() {
        LoadMoreContactsWithBadAccessAndRefreshTokenTest.resetRefreshToken();
    }

    private int getExpectedNumDisplayedContacts(int staticContactsListSize) {
        if (knownContactsChecked) {
            if (searchString == null || !searchString.equals(gdSS)) {
                return staticContactsListSize + 50;     // Max num of contacts loaded from the server = 50.
            }
            else {
                return 1;
            }
        }
        else {
            if (extendedContactsChecked) {
                return 1;
            }
            else {
                return 0;
            }
        }
    }

    @Test
    public void parameterizedLoadTargetContacts() throws Exception {
        // Allow time for the main activity to load contacts and groups.
        SystemClock.sleep(3000);

        if (groupNum == null) {
            int expectedNumDisplayedContacts = getExpectedNumDisplayedContacts(Contact.getTargetContactsSize());

            LoadMoreContactsWithBadAccessAndRefreshTokenTest.swipeToAllContactsTab();

            // Assume by default that the known contacts CheckBox is checked and the extended contacts CheckBox is
            // unchecked. For some reason using the CheckBoxSelection class does not show/hide contacts in the app.
            if (extendedContactsChecked) {
                onView(withId(R.id.checkbox_main_extended_contacts)).perform(click());
            }
            if (!knownContactsChecked) {
                onView(withId(R.id.checkbox_main_known_contacts)).perform(click());
            }

            // Test that requesting contacts from the server is not affected by the number of contacts in the static
            // list(s) but instead affected by the number of contacts in the adapter.
            AndroidTestUtils.generateKnownContact().storeStatically();

            if (searchString != null) {
                onView(withId(R.id.edittext_main_tab_contacts)).perform(replaceText(searchString));
            }

            // If there are no contacts displayed due to filters being updated then a server request should be
            // initiated automatically. For this test this happens when the known contacts CheckBox is unchecked or
            // the search string is not null or the expected string.
            if (searchString == null && knownContactsChecked) {
                LoadMoreContactsWithBadAccessAndRefreshTokenTest.loadMoreAllContacts();
            }
            else {
                SystemClock.sleep(3000);    // Wait for the server response.
            }

            onView(withId(R.id.recyclerview_main_tab_contacts))
                    .check(new AssertRecyclerViewItemCount(expectedNumDisplayedContacts));
        }
        else {
            LoadMoreContactsWithBadAccessAndRefreshTokenTest.swipeToGroupContactsTab(groupNum.intValue());

            // Assume by default that the known contacts CheckBox is checked and the extended contacts CheckBox is
            // unchecked. For some reason using the CheckBoxSelection class does not show/hide contacts in the app.

            // Selecting the CheckBox triggers the adapter's filter method which resets the display to the default
            // number of items.

            if (!knownContactsChecked) {
                onView(withId(R.id.checkbox_group_known_contacts)).perform(click());
            }
            if (extendedContactsChecked) {
                onView(withId(R.id.checkbox_group_extended_contacts)).perform(click());
            }

            onView(withId(R.id.recyclerview_group_details_tab_target_contacts))
                    .check(new AssertRecyclerViewItemCount(ContactsAdapter.MAX_NUM_OF_ITEMS_APPENDED));

            LoadMoreContactsWithBadAccessAndRefreshTokenTest.loadMoreGroupContacts();

            if (searchString != null) {
                onView(withId(R.id.edittext_group_contacts)).perform(replaceText(searchString));
            }

            onView(withId(R.id.recyclerview_group_details_tab_target_contacts))
                    .check(new AssertRecyclerViewItemCount(ContactsAdapter.MAX_NUM_OF_ITEMS_APPENDED * 2));
        }
    }
}
