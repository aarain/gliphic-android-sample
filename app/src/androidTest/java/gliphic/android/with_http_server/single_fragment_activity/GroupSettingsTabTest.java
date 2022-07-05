package gliphic.android.with_http_server.single_fragment_activity;

import android.os.SystemClock;

import gliphic.android.R;
import gliphic.android.adapters.ImageAdapter;
import gliphic.android.display.welcome_screen.SignInActivity;
import gliphic.android.operation.Group;
import gliphic.android.exceptions.NullStaticVariableException;
import gliphic.android.operation.ObjectImage;
import gliphic.android.operation.server_interaction.http_server.HttpOperations;
import gliphic.android.utils.AndroidTestUtils;
import gliphic.android.utils.BaseActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import gliphic.android.utils.MainActivityBaseSetup;
import gliphic.android.utils.matchers.CustomMatchers;
import gliphic.android.utils.matchers.ToastMatcher;
import libraries.GroupPermissions;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.swipeDown;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.action.ViewActions.swipeRight;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withAlpha;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;

/**
 * Test functionality unique to GroupSettingsTab.
 */
@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class GroupSettingsTabTest {
    private static final long TEST_CONTACT_NUMBER = 0;
    private final int numOfUnmodifiableGroup    = 0;
    private final int numOfModifiableGroup      = 1;
    private final int numOfNoGroupContactsGroup = 2;

    private String originalGroupName;
    private String originalGroupDescription;
    private ObjectImage originalGroupImage;
    private final String newGroupName = "Some arbitrary string.";
    private final String newGroupDescription =
            "1111111110111111111011111111101111111110111111111011111111101111111110111111111011111111101111111110";
    private final ObjectImage newGroupImage = new ObjectImage(R.drawable.lady_office_worker);

    private void changeGroupImage() {
        SystemClock.sleep(200);
        onView(ViewMatchers.withId(R.id.group_details_img)).perform(click());
        SystemClock.sleep(200);
        onData(anything()).inAdapterView(withId(R.id.gridview_display_pictures)).atPosition(0).perform(click());
        SystemClock.sleep(200);
        onView(ViewMatchers.withId(R.id.btn_single_picture)).perform(click());
        SystemClock.sleep(500);
    }

    @Rule
    public BaseActivityTestRule<SignInActivity> rule = new BaseActivityTestRule<>(SignInActivity.class);

    @Before
    public void selectGroupsTab() {
        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
        SystemClock.sleep(1000);    // Give the swipe time to change tab focus and find R.id.*
    }

    private void changeNameAndImageAndDescription(String groupName, String groupDescription, ObjectImage objectImage) {
        // Change the group name.
        onView(ViewMatchers.withId(R.id.group_details_name)).perform(click());
        SystemClock.sleep(200);     // The EditText needs to be in focus.
        onView(ViewMatchers.withId(R.id.group_details_name)).perform(replaceText(groupName));
        onView(withId(R.id.group_details_name)).perform(pressImeActionButton());
        SystemClock.sleep(100);
        onView(withId(android.R.id.button1)).perform(click());
        SystemClock.sleep(2000);
        onView(withText("The group name has been updated.")).check(matches(isDisplayed()));
        onView(withId(android.R.id.button1)).perform(click());
        // Change the group description.
        onView(ViewMatchers.withId(R.id.group_details_description)).perform(click());
        SystemClock.sleep(200);     // The EditText needs to be in focus.
        onView(ViewMatchers.withId(R.id.group_details_description)).perform(replaceText(groupDescription));
        onView(withId(R.id.group_details_description)).perform(pressImeActionButton());
        SystemClock.sleep(100);
        onView(withId(android.R.id.button1)).perform(click());
        SystemClock.sleep(2000);
        onView(withText("The group description has been updated.")).check(matches(isDisplayed()));
        onView(withId(android.R.id.button1)).perform(click());
        // Change the group image.
        onView(ViewMatchers.withId(R.id.group_details_img)).perform(click());
        SystemClock.sleep(200);
        onData(anything()).inAdapterView(withId(R.id.gridview_display_pictures))
                .atPosition(ImageAdapter.getItemPosition(objectImage)).perform(click());
        SystemClock.sleep(200);
        onView(ViewMatchers.withId(R.id.btn_single_picture)).perform(click());
        SystemClock.sleep(100);
        onView(withText("Group image set.")).inRoot(new ToastMatcher()).check(matches(isDisplayed()));
        onView(withId(R.id.group_details_img)).check(matches(isDisplayed()));

        SystemClock.sleep(200);
        onView(withId(R.id.viewpager_activity_base)).perform(swipeDown());
        pressBack();
    }

    private void assertRecyclerviewHasDescendant(int recyclerviewId) {
        onView(withId(recyclerviewId)).check(matches(hasDescendant(withText(newGroupName))));
        onView(withId(recyclerviewId)).check(matches(hasDescendant(withText(newGroupDescription))));
        onView(withId(recyclerviewId))
                .check(matches(hasDescendant(CustomMatchers.withDrawable(newGroupImage.getResourceInt()))));
    }

    private void assertGroupDetails() {
        onView(ViewMatchers.withId(R.id.group_details_name)).check(matches(withText(newGroupName)));
        onView(ViewMatchers.withId(R.id.group_details_description)).check(matches(withText(newGroupDescription)));
        onView(ViewMatchers.withId(R.id.group_details_img))
                .check(matches(CustomMatchers.withDrawable(newGroupImage.getResourceInt())));
    }

    private void assertGroupMembers(Group group, boolean newData) {
        if (newData) {
            assertThat(group.getName(),                     is(newGroupName));
            assertThat(group.getDescription(),              is(newGroupDescription));
            assertThat(group.getImage().getResourceInt(),   is(newGroupImage.getResourceInt()));
        }
        else {
            assertThat(group.getName(),                     is(originalGroupName));
            assertThat(group.getDescription(),              is(originalGroupDescription));
            assertThat(group.getImage().getResourceInt(),   is(originalGroupImage.getResourceInt()));
        }
    }

    /* Modifying group name/image/description - Persistence */

    @Test
    public void groupDataChangePersistsFromMainActivity() throws NullStaticVariableException {
        // This test uses the MainActivity to launch the GroupDetailsActivity.

        // The order of groups in Group.knownGroups is expected to correlate the order of groups
        // displayed in the RecyclerView.
        int expectedGroupIndex = numOfModifiableGroup;

        onView(withId(R.id.recyclerview_main_tab_groups))
                .perform(RecyclerViewActions.actionOnItemAtPosition(expectedGroupIndex, click()));

        // Store the currently displayed group name for clean-up at the end of the test.
        final Group knownGroup = Group.getKnownGroups().get(expectedGroupIndex);
        originalGroupName        = knownGroup.getName();
        originalGroupDescription = knownGroup.getDescription();
        originalGroupImage       = knownGroup.getImage();

        try {
            changeNameAndImageAndDescription(newGroupName, newGroupDescription, newGroupImage);
            assertGroupMembers(knownGroup, true);

            // Bug fix test: Check that the calling MainActivity's list of groups has updated.
            assertRecyclerviewHasDescendant(R.id.recyclerview_main_tab_groups);

            onView(withId(R.id.recyclerview_main_tab_groups))
                    .perform(RecyclerViewActions.actionOnItemAtPosition(expectedGroupIndex, click()));
            SystemClock.sleep(200);

            assertGroupDetails();
        }
        finally {
            changeNameAndImageAndDescription(originalGroupName, originalGroupDescription, originalGroupImage);
            assertGroupMembers(knownGroup, false);
        }
    }

    @Test
    public void groupDataChangePersistsFromContactDetailsActivity() throws NullStaticVariableException {
        // This test uses the ContactDetailsActivity to launch the GroupDetailsActivity.

        final int contact1ViewIndex = 0;
        final int group1ViewIndex = 0;

        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
        SystemClock.sleep(1000);    // Give the swipe time to change tab focus and find R.id.*
        onView(withId(R.id.recyclerview_main_tab_contacts))
                .perform(RecyclerViewActions.actionOnItemAtPosition(contact1ViewIndex, click()));

        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
        SystemClock.sleep(1000);    // Give the swipe time to change tab focus and find R.id.*
        onView(withId(R.id.recyclerview_contact_details_tab_groups))
                .perform(RecyclerViewActions.actionOnItemAtPosition(group1ViewIndex, click()));

        // Store the currently displayed group description for clean-up at the end of the test.
        final Group knownGroup = Group.getKnownGroups().get(numOfModifiableGroup);
        originalGroupName        = knownGroup.getName();
        originalGroupDescription = knownGroup.getDescription();
        originalGroupImage       = knownGroup.getImage();

        try {
            changeNameAndImageAndDescription(newGroupName, newGroupDescription, newGroupImage);
            assertGroupMembers(knownGroup, true);

            // Bug fix test: Check that the calling ContactDetailsActivity's list of groups has updated.
            assertRecyclerviewHasDescendant(R.id.recyclerview_contact_details_tab_groups);

            onView(withId(R.id.recyclerview_contact_details_tab_groups))
                    .perform(RecyclerViewActions.actionOnItemAtPosition(group1ViewIndex, click()));

            assertGroupDetails();
        }
        finally {
            changeNameAndImageAndDescription(originalGroupName, originalGroupDescription, originalGroupImage);
            assertGroupMembers(knownGroup, false);
            pressBack();
        }
    }

    @Test
    public void groupDataChangeUpdatesInBackstack() throws NullStaticVariableException {
        onView(withId(R.id.recyclerview_main_tab_groups))
                .perform(RecyclerViewActions.actionOnItemAtPosition(numOfModifiableGroup, click()));

        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
        SystemClock.sleep(1000);    // Give the swipe time to change tab focus and find R.id.*
        onView(withId(R.id.recyclerview_group_details_tab_target_contacts))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());
        SystemClock.sleep(1000);    // Give the swipe time to change tab focus and find R.id.*
        onView(withId(R.id.recyclerview_contact_details_tab_groups))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

        // Store the currently displayed group description for clean-up at the end of the test.
        final Group knownGroup = Group.getKnownGroups().get(numOfModifiableGroup);
        originalGroupName        = knownGroup.getName();
        originalGroupDescription = knownGroup.getDescription();
        originalGroupImage       = knownGroup.getImage();

        try {
            changeNameAndImageAndDescription(newGroupName, newGroupDescription, newGroupImage);
            assertGroupMembers(knownGroup, true);
            pressBack();
            onView(withId(R.id.viewpager_activity_base)).perform(swipeRight());
            SystemClock.sleep(1000);    // Give the swipe time to change tab focus and find R.id.*
            assertGroupDetails();
        }
        finally {
            changeNameAndImageAndDescription(originalGroupName, originalGroupDescription, originalGroupImage);
            assertGroupMembers(knownGroup, false);
        }
    }

    /* Modifying group name - Error messages */

    @Test
    public void invalidGroupNameEmpty() {
        onView(withId(R.id.recyclerview_main_tab_groups))
                .perform(RecyclerViewActions.actionOnItemAtPosition(numOfModifiableGroup, click()));
        onView(ViewMatchers.withId(R.id.group_details_name)).perform(click());
        SystemClock.sleep(200);     // The EditText needs to be in focus.
        onView(ViewMatchers.withId(R.id.group_details_name)).perform(replaceText(""));
        onView(withId(R.id.group_details_name)).perform(pressImeActionButton());
        SystemClock.sleep(500);
        onView(withText(Group.NAME_EMPTY_MSG)).check(matches(isDisplayed()));
        // Clean-up.
        onView(withId(android.R.id.button1)).perform(click());
        pressBack();
    }

    @Test
    public void invalidGroupNameNotPrintable() {
        onView(withId(R.id.recyclerview_main_tab_groups))
                .perform(RecyclerViewActions.actionOnItemAtPosition(numOfModifiableGroup, click()));
        onView(ViewMatchers.withId(R.id.group_details_name)).perform(click());
        SystemClock.sleep(200);     // The EditText needs to be in focus.
        onView(ViewMatchers.withId(R.id.group_details_name)).perform(replaceText(AndroidTestUtils.INVALID_CHAR_STRING));
        onView(withId(R.id.group_details_name)).perform(pressImeActionButton());
        SystemClock.sleep(500);
        onView(withText(Group.NAME_NOT_PRINTABLE)).check(matches(isDisplayed()));
        // Clean-up.
        onView(withId(android.R.id.button1)).perform(click());
        pressBack();
    }

    @Test
    public void invalidGroupNameEndSpace() {
        onView(withId(R.id.recyclerview_main_tab_groups))
                .perform(RecyclerViewActions.actionOnItemAtPosition(numOfModifiableGroup, click()));
        onView(ViewMatchers.withId(R.id.group_details_name)).perform(click());
        SystemClock.sleep(200);     // The EditText needs to be in focus.
        String s = " end space ";
        onView(ViewMatchers.withId(R.id.group_details_name)).perform(replaceText(s));
        onView(withId(R.id.group_details_name)).perform(pressImeActionButton());
        SystemClock.sleep(500);
        onView(withText(Group.NAME_CONTAINS_END_SPACE)).check(matches(isDisplayed()));
        // Clean-up.
        onView(withId(android.R.id.button1)).perform(click());
        pressBack();
    }

    @Test
    public void invalidGroupNameConsecutiveChars() {
        onView(withId(R.id.recyclerview_main_tab_groups))
                .perform(RecyclerViewActions.actionOnItemAtPosition(numOfModifiableGroup, click()));
        onView(ViewMatchers.withId(R.id.group_details_name)).perform(click());
        SystemClock.sleep(200);     // The EditText needs to be in focus.
        String s = "consecutive -- chars";
        onView(ViewMatchers.withId(R.id.group_details_name)).perform(replaceText(s));
        onView(withId(R.id.group_details_name)).perform(pressImeActionButton());
        SystemClock.sleep(500);
        onView(withText(Group.NAME_CONTAINS_CONSECUTIVE_CHARS)).check(matches(isDisplayed()));
        // Clean-up.
        onView(withId(android.R.id.button1)).perform(click());
        pressBack();
    }

    @Test
    public void invalidGroupNameAdjacentMarks() {
        onView(withId(R.id.recyclerview_main_tab_groups))
                .perform(RecyclerViewActions.actionOnItemAtPosition(numOfModifiableGroup, click()));
        onView(ViewMatchers.withId(R.id.group_details_name)).perform(click());
        SystemClock.sleep(200);     // The EditText needs to be in focus.
        String s = "adjacent \"' marks";
        onView(ViewMatchers.withId(R.id.group_details_name)).perform(replaceText(s));
        onView(withId(R.id.group_details_name)).perform(pressImeActionButton());
        SystemClock.sleep(500);
        onView(withText(Group.NAME_CONTAINS_ADJACENT_MARKS)).check(matches(isDisplayed()));
        // Clean-up.
        onView(withId(android.R.id.button1)).perform(click());
        pressBack();
    }

    @Test
    public void invalidGroupNameLength() {
        onView(withId(R.id.recyclerview_main_tab_groups))
                .perform(RecyclerViewActions.actionOnItemAtPosition(numOfModifiableGroup, click()));
        onView(ViewMatchers.withId(R.id.group_details_name)).perform(click());
        SystemClock.sleep(200);     // The EditText needs to be in focus.
        String s = "a";
        onView(ViewMatchers.withId(R.id.group_details_name)).perform(replaceText(s));
        onView(withId(R.id.group_details_name)).perform(pressImeActionButton());
        SystemClock.sleep(500);
        onView(withText(Group.NAME_LENGTH_INVALID)).check(matches(isDisplayed()));
        // Clean-up.
        onView(withId(android.R.id.button1)).perform(click());
        pressBack();
    }

    @Test
    public void invalidChangeGroupNameForbiddenGroupPermissions() throws Exception {
        final long groupNumber = numOfModifiableGroup;

        try {
            MainActivityBaseSetup.setGroupPermissionsForTestContact(groupNumber, GroupPermissions.ACTIVE_DENIED);

            onView(withId(R.id.recyclerview_main_tab_groups))
                    .perform(RecyclerViewActions.actionOnItemAtPosition((int) groupNumber, click()));
            onView(ViewMatchers.withId(R.id.group_details_name)).perform(click());
            SystemClock.sleep(200);     // The EditText needs to be in focus.
            onView(ViewMatchers.withId(R.id.group_details_name)).perform(replaceText("valid group name"));
            onView(withId(R.id.group_details_name)).perform(pressImeActionButton());
            SystemClock.sleep(200);     // Wait for the confirmation dialog to appear.
            onView(withId(android.R.id.button1)).perform(click());
            SystemClock.sleep(500);
            onView(withText(HttpOperations.ERROR_MSG_403_DENIED_SET)).check(matches(isDisplayed()));
        }
        finally {
            MainActivityBaseSetup.setGroupPermissionsForTestContact(groupNumber, GroupPermissions.ACTIVE_OWNER);
        }
    }

    /* Modifying group description - Error messages */

    @Test
    public void invalidGroupDescriptionEmpty() {
        onView(withId(R.id.recyclerview_main_tab_groups))
                .perform(RecyclerViewActions.actionOnItemAtPosition(numOfModifiableGroup, click()));
        onView(ViewMatchers.withId(R.id.group_details_description)).perform(click());
        SystemClock.sleep(200);     // The EditText needs to be in focus.
        onView(ViewMatchers.withId(R.id.group_details_description)).perform(replaceText(""));
        onView(withId(R.id.group_details_description)).perform(pressImeActionButton());
        SystemClock.sleep(500);
        onView(withText(Group.DESCRIPTION_EMPTY_MSG)).check(matches(isDisplayed()));
        // Clean-up.
        onView(withId(android.R.id.button1)).perform(click());
        pressBack();
    }

    @Test
    public void invalidGroupDescriptionNotPrintable() {
        onView(withId(R.id.recyclerview_main_tab_groups))
                .perform(RecyclerViewActions.actionOnItemAtPosition(numOfModifiableGroup, click()));
        onView(ViewMatchers.withId(R.id.group_details_description)).perform(click());
        SystemClock.sleep(200);     // The EditText needs to be in focus.
        onView(ViewMatchers.withId(R.id.group_details_description))
                .perform(replaceText(AndroidTestUtils.INVALID_CHAR_STRING));
        onView(withId(R.id.group_details_description)).perform(pressImeActionButton());
        SystemClock.sleep(500);
        onView(withText(Group.DESCRIPTION_NOT_PRINTABLE)).check(matches(isDisplayed()));
        // Clean-up.
        onView(withId(android.R.id.button1)).perform(click());
        pressBack();
    }

    @Test
    public void invalidGroupDescriptionNotUnique() throws NullStaticVariableException {
        onView(withId(R.id.recyclerview_main_tab_groups))
                .perform(RecyclerViewActions.actionOnItemAtPosition(numOfModifiableGroup, click()));
        onView(ViewMatchers.withId(R.id.group_details_description)).perform(click());
        SystemClock.sleep(200);     // The EditText needs to be in focus.
        String s = Group.getKnownGroups().get(numOfUnmodifiableGroup).getDescription();
        onView(ViewMatchers.withId(R.id.group_details_description)).perform(replaceText(s));
        onView(withId(R.id.group_details_description)).perform(pressImeActionButton());
        SystemClock.sleep(500);
        onView(withText(Group.DESCRIPTION_NOT_UNIQUE)).check(matches(isDisplayed()));
        // Clean-up.
        onView(withId(android.R.id.button1)).perform(click());
        pressBack();
    }

    /* Modifying group image - Error messages */

    @Test
    public void invalidChangeGroupImageForbiddenGroupPermissions() throws Exception {
        final long groupNumber = numOfModifiableGroup;

        try {
            MainActivityBaseSetup.setGroupPermissionsForTestContact(groupNumber, GroupPermissions.ACTIVE_DENIED);

            onView(withId(R.id.recyclerview_main_tab_groups))
                    .perform(RecyclerViewActions.actionOnItemAtPosition((int) groupNumber, click()));
            onView(ViewMatchers.withId(R.id.group_details_img)).perform(click());
            SystemClock.sleep(200);
            onData(anything()).inAdapterView(withId(R.id.gridview_display_pictures)).atPosition(0).perform(click());
            SystemClock.sleep(200);
            onView(ViewMatchers.withId(R.id.btn_single_picture)).perform(click());
            SystemClock.sleep(500);
            onView(withText(HttpOperations.ERROR_MSG_403_DENIED_SET)).check(matches(isDisplayed()));
        }
        finally {
            MainActivityBaseSetup.setGroupPermissionsForTestContact(groupNumber, GroupPermissions.ACTIVE_OWNER);
        }
    }

    /*
     * This test uses the MainActivity to launch the GroupDetailsActivity.
     *
     * The order of groups in Group.knownGroups is expected to correlate the order of groups displayed in the
     * RecyclerView.
     */
    @Test
    public void groupNameAndImageAndDescriptionChangeInactiveAccount() throws Exception {
        try {
            MainActivityBaseSetup.deactivateAccount(TEST_CONTACT_NUMBER);

            onView(withId(R.id.recyclerview_main_tab_groups))
                    .perform(RecyclerViewActions.actionOnItemAtPosition(numOfModifiableGroup, click()));

            // Attempt to change the group name.
            onView(ViewMatchers.withId(R.id.group_details_name)).perform(click());
            SystemClock.sleep(200);     // The EditText needs to be in focus.
            onView(ViewMatchers.withId(R.id.group_details_name)).perform(replaceText("A group name."));
            onView(withId(R.id.group_details_name)).perform(pressImeActionButton());
            onView(withId(android.R.id.button1)).perform(click());
            SystemClock.sleep(2000);

            onView(withText(HttpOperations.ERROR_MSG_403_ACCOUNT_INACTIVE)).check(matches(isDisplayed()));
            onView(withId(android.R.id.button1)).perform(click());

            // Attempt to change the group description.
            onView(ViewMatchers.withId(R.id.group_details_description)).perform(click());
            SystemClock.sleep(200);     // The EditText needs to be in focus.
            onView(ViewMatchers.withId(R.id.group_details_description))
                    .perform(replaceText("A group description."));
            onView(withId(R.id.group_details_description)).perform(pressImeActionButton());
            onView(withId(android.R.id.button1)).perform(click());
            SystemClock.sleep(2000);

            onView(withText(HttpOperations.ERROR_MSG_403_ACCOUNT_INACTIVE)).check(matches(isDisplayed()));
            onView(withId(android.R.id.button1)).perform(click());

            // Attempt to change the group image.
            changeGroupImage();

            onView(withText(HttpOperations.ERROR_MSG_403_ACCOUNT_INACTIVE)).check(matches(isDisplayed()));
            onView(withId(android.R.id.button1)).perform(click());
        }
        finally {
            MainActivityBaseSetup.activateAccount(TEST_CONTACT_NUMBER);
        }
    }

    /* Check views are (not) displayed */

    @Test
    public void emptyGroupContactsTextViewDisplayed() {
        onView(withId(R.id.recyclerview_main_tab_groups))
                .perform(RecyclerViewActions.actionOnItemAtPosition(numOfNoGroupContactsGroup, click()));

        onView(withId(R.id.viewpager_activity_base)).perform(swipeLeft());

        // View(s) which are not displayed.
        onView(ViewMatchers.withId(R.id.group_contacts_tab_error_contact)).check(matches(not(isDisplayed())));
        // View(s) which are displayed.
        onView(ViewMatchers.withId(R.id.textview_no_group_contacts)).check(matches(isDisplayed()));
    }

    /* Buttons are clickable */

    @Test
    public void shareButtonExists() {
        onView(withId(R.id.recyclerview_main_tab_groups))
                .perform(RecyclerViewActions.actionOnItemAtPosition(numOfModifiableGroup, click()));

        onView(withId(R.id.btn_group_details_share)).check(matches(withAlpha(1f)));
        onView(withId(R.id.btn_group_details_share)).check(matches(isClickable()));
        onView(withId(R.id.btn_group_details_leave)).check(matches(withAlpha(1f)));
        onView(withId(R.id.btn_group_details_leave)).check(matches(isClickable()));
    }

    /* Default group tests */

    @Test
    public void defaultGroupButtonsNotClickable() {
        onView(withId(R.id.recyclerview_main_tab_groups))
                .perform(RecyclerViewActions.actionOnItemAtPosition(numOfUnmodifiableGroup, click()));

        onView(withId(R.id.btn_group_details_share)).check(matches(withAlpha(0.5f)));
        onView(withId(R.id.btn_group_details_share)).check(matches(not(isEnabled())));
        onView(withId(R.id.btn_group_details_leave)).check(matches(withAlpha(0.5f)));
        onView(withId(R.id.btn_group_details_leave)).check(matches(not(isEnabled())));
    }

    @Test
    public void cannotModifyDefaultGroupName() {
        onView(withId(R.id.recyclerview_main_tab_groups))
                .perform(RecyclerViewActions.actionOnItemAtPosition(numOfUnmodifiableGroup, click()));
        SystemClock.sleep(1000);    // Give the new activity time to acquire focus.

        String s = "Some arbitrary string.";
        onView(ViewMatchers.withId(R.id.group_details_name)).perform(click());
        SystemClock.sleep(200);
        onView(ViewMatchers.withId(R.id.group_details_name)).perform(replaceText(s), closeSoftKeyboard());
        onView(ViewMatchers.withId(R.id.group_details_description)).perform(click());

        onView(withText("Group unchanged")).check(matches(isDisplayed()));
        // Clean-up.
        onView(withId(android.R.id.button1)).perform(click());
        pressBack();
    }

    @Test
    public void cannotModifyDefaultGroupDescription() {
        onView(withId(R.id.recyclerview_main_tab_groups))
                .perform(RecyclerViewActions.actionOnItemAtPosition(numOfUnmodifiableGroup, click()));
        SystemClock.sleep(1000);    // Give the new activity time to acquire focus.

        String s = "Some arbitrary string.";
        onView(ViewMatchers.withId(R.id.group_details_description)).perform(click());
        onView(ViewMatchers.withId(R.id.group_details_description)).perform(closeSoftKeyboard());
        onView(ViewMatchers.withId(R.id.group_details_description)).perform(replaceText(s));
        onView(ViewMatchers.withId(R.id.group_details_name)).perform(click());

        onView(withText("Group unchanged")).check(matches(isDisplayed()));
        // Clean-up.
        onView(withId(android.R.id.button1)).perform(click());
        pressBack();
    }

    @Test
    public void cannotModifyDefaultGroupImage() {
        onView(withId(R.id.recyclerview_main_tab_groups))
                .perform(RecyclerViewActions.actionOnItemAtPosition(numOfUnmodifiableGroup, click()));
        SystemClock.sleep(1000);    // Give the new activity time to acquire focus.
        onView(ViewMatchers.withId(R.id.group_details_img)).perform(click());
        SystemClock.sleep(500);
        // Check that the image selection screen has not appeared.
        onView(withId(R.id.btn_group_details_select)).check(matches(withAlpha(1f)));
        onView(withId(R.id.btn_group_details_select)).check(matches(isClickable()));
    }
}
