package gliphic.android.with_http_server.functionality.load_more_contacts_tests;

import gliphic.android.R;
import gliphic.android.display.welcome_screen.SignInActivity;
import gliphic.android.with_http_server.LoadMoreObjectsTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class LoadAllContactsWithExpiredAccessTokenTest {

    @Rule
    public LoadMoreObjectsTestRule<SignInActivity> rule = new LoadMoreObjectsTestRule<>(SignInActivity.class);

    @Test
    public void loadAllContactsWithExpiredAccessToken() throws Throwable {
        LoadMoreContactsWithBadAccessAndRefreshTokenTest.swipeToAllContactsTab();

        // Check that a loaded contact is not in the internal list.
        LoadMoreContactsWithBadAccessAndRefreshTokenTest.checkContactNotInInternalList();

        LoadMoreContactsWithBadAccessAndRefreshTokenTest.loadMoreAllContacts();

        // Check that a loaded contact is in the internal list.
        LoadMoreContactsWithBadAccessAndRefreshTokenTest.checkContactIsInInternalList();

        // Check that a loaded contact is on display.
        LoadMoreContactsWithBadAccessAndRefreshTokenTest.checkContactIsDisplayed(R.id.more_contacts_progress_bar);
    }
}
