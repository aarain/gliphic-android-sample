package gliphic.android.with_http_server.functionality;

import android.os.SystemClock;
import android.view.View;

import gliphic.android.R;
import gliphic.android.adapters.AlertsAdapter;
import gliphic.android.display.welcome_screen.SignInActivity;
import gliphic.android.operation.Alerts;
import gliphic.android.utils.AndroidTestUtils;
import gliphic.android.utils.matchers.RecyclerViewMatcher;
import gliphic.android.utils.view_assertions.AssertRecyclerViewItemCount;
import gliphic.android.utils.BaseActivityTestRule;
import gliphic.android.utils.view_actions.NestedScrollTo;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import gliphic.android.utils.view_actions.SetViewVisibility;
import libraries.Base256;
import pojo.account.GroupShare;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.swipeRight;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class LoadMoreAlertsTest {
    private static final int numSharesPerLoad = 100;
    private static final int numAlertsAppended = AlertsAdapter.MAX_NUM_OF_ITEMS_APPENDED;

    private void scrollToProgressBarShortSleep() {
        onView(withId(R.id.more_alerts_progress_bar)).perform(SetViewVisibility.setViewVisibility(View.VISIBLE));
        onView(withId(R.id.more_alerts_progress_bar)).perform(NestedScrollTo.nestedScrollTo());
        SystemClock.sleep(200);
    }

    private void scrollToProgressBarLongSleep() {
        onView(withId(R.id.more_alerts_progress_bar)).perform(SetViewVisibility.setViewVisibility(View.VISIBLE));
        onView(withId(R.id.more_alerts_progress_bar)).perform(NestedScrollTo.nestedScrollTo());
        SystemClock.sleep(2000);
    }

    private void assertListSizeAndDisplayedItems(int listSize, int displaySize) throws Exception {
        assertThat(Alerts.getGroupShares().size(), is(listSize));
        onView(withId(R.id.recyclerview_main_tab_alerts)).check(new AssertRecyclerViewItemCount(displaySize));
    }

    @Rule
    public BaseActivityTestRule<SignInActivity> rule = new BaseActivityTestRule<>(SignInActivity.class);

    @Before
    public void scrollToTab() {
        onView(withId(R.id.viewpager_activity_base)).perform(swipeRight());
        SystemClock.sleep(200);
    }

    @After
    public void nullGlobalStatics() {
        AndroidTestUtils.clearStaticLists();
    }

    @Test
    public void loadMoreAlertsViaFiltersAndScroll() throws Exception {
        final int numInitialGroupShares = Alerts.getGroupShares().size();
        final int numInitialPendingReceivedShared = 1;

        assertListSizeAndDisplayedItems(numInitialGroupShares, numInitialPendingReceivedShared);

        // Ensure that all group-share types can be loaded before sending a request to load more alerts.
        // Load more alerts by clicking the CheckBox since there were only 2 to start with.
        onView(withId(R.id.checkbox_alerts)).perform(click());
        SystemClock.sleep(1000);

        // Test that the adapter only displays the option to accept and decline pending-received request(s).

        RecyclerViewMatcher recyclerViewMatcher = new RecyclerViewMatcher(R.id.recyclerview_main_tab_alerts);

        for (int itemPosition = 0; ; itemPosition++) {
            ViewMatchers.Visibility visibility;
            if (itemPosition == 0) {
                visibility = ViewMatchers.Visibility.VISIBLE;
            }
            else {
                visibility = ViewMatchers.Visibility.GONE;
            }

            try {
                onView(recyclerViewMatcher.viewInItem(itemPosition, R.id.alert_item_accept))
                        .check(matches(withEffectiveVisibility(visibility)));
                onView(recyclerViewMatcher.viewInItem(itemPosition, R.id.alert_item_decline))
                        .check(matches(withEffectiveVisibility(visibility)));
            }
            catch (NullPointerException e) {
                // A NullPointerException occurs when there are mo more items in the RecyclerView.
                break;
            }
        }

        assertListSizeAndDisplayedItems(numInitialGroupShares + numSharesPerLoad, numAlertsAppended);

        // Load more alerts by scrolling.

        scrollToProgressBarShortSleep();

        // More alerts are appended to the display but no more are loaded from the server.
        assertListSizeAndDisplayedItems(
                numInitialGroupShares + numSharesPerLoad,
                numAlertsAppended * 2
        );

        scrollToProgressBarLongSleep();

        // More alerts are appended to the display by loaded them from the server.
        assertListSizeAndDisplayedItems(
                numInitialGroupShares + (numSharesPerLoad * 2),
                numAlertsAppended * 3
        );

        // Assert the final total number of alerts/group-shares.

        scrollToProgressBarShortSleep();
        scrollToProgressBarLongSleep();
        scrollToProgressBarShortSleep();
        scrollToProgressBarLongSleep();

        // Bug fix test: Assert that all group shares in the static list are unique and displayed.
        // Note that this relies on the uniqueness of each group in the static list.
        List<Long> allGroupShareGroupNumbers = new ArrayList<>();
        for (GroupShare gs : Alerts.getGroupShares()) {
            final long groupNumber = gs.getGroup().getNumber();
            if (allGroupShareGroupNumbers.contains(groupNumber)) {
                throw new Exception(String.format(
                        "Duplicate group number %d in static group shares list.",
                        groupNumber
                ));
            }
            allGroupShareGroupNumbers.add(groupNumber);

            onView(ViewMatchers.withId(R.id.recyclerview_main_tab_alerts))
                    .check(matches(hasDescendant(withText(containsString(
                            String.format("group ID %s", Base256.fromBase64(gs.getGroup().getIdBase64()))
                    )))));
        }

        // Assert the final values.
        final int totalNumOfItems = 302;
        assertListSizeAndDisplayedItems(totalNumOfItems, totalNumOfItems);

        // Test the CheckBox.
        onView(withId(R.id.checkbox_alerts)).perform(click());
        SystemClock.sleep(1000);
        assertListSizeAndDisplayedItems(totalNumOfItems, numInitialPendingReceivedShared);
    }

    @Test
    public void noMoreFilteredItemsToLoad() {
        AssertRecyclerViewItemCount arvic =
                new AssertRecyclerViewItemCount(true, R.id.recyclerview_main_tab_alerts);

        onView(withId(R.id.textview_load_more_alerts)).perform(click());
        SystemClock.sleep(2000);

        onView(withId(R.id.recyclerview_main_tab_alerts)).check(arvic);
    }
}
