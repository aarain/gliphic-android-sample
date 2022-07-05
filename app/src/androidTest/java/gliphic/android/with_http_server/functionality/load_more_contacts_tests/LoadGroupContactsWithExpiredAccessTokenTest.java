package gliphic.android.with_http_server.functionality.load_more_contacts_tests;

import gliphic.android.R;
import gliphic.android.display.welcome_screen.SignInActivity;
import gliphic.android.with_http_server.LoadMoreObjectsTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class LoadGroupContactsWithExpiredAccessTokenTest {

    @Rule
    public LoadMoreObjectsTestRule<SignInActivity> rule = new LoadMoreObjectsTestRule<>(SignInActivity.class);

    @Before
    public void swipeToTab() {
        LoadMoreContactsWithBadAccessAndRefreshTokenTest.swipeToGroupContactsTab(null);
    }

    @Test
    public void loadGroupContactsWithExpiredAccessToken() throws Throwable {
        // Check that a loaded contact is not in the internal list.
        LoadMoreContactsWithBadAccessAndRefreshTokenTest.checkContactNotInInternalList();

        LoadMoreContactsWithBadAccessAndRefreshTokenTest.loadMoreGroupContacts();

        // Check that a loaded contact is in the internal list.
        LoadMoreContactsWithBadAccessAndRefreshTokenTest.checkContactIsInInternalList();

        // Check that a loaded contact is on display.
        LoadMoreContactsWithBadAccessAndRefreshTokenTest.checkContactIsDisplayed(R.id.more_group_contacts_progress_bar);
    }
}
