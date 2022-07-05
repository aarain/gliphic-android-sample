package gliphic.android.utils.matchers;

import android.content.res.Resources;
import android.view.View;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.matcher.BoundedMatcher;

import static androidx.core.util.Preconditions.checkNotNull;

public class RecyclerViewMatcher {
    private final int recyclerViewId;

    public RecyclerViewMatcher(int recyclerViewId) {
        this.recyclerViewId = recyclerViewId;
    }

    /**
     * Return a Matcher for a view within a specific RecyclerView item to check against.
     *
     * Example usage:
     *
     *     onView(new RecyclerViewMatcher(R.id.recycler_view).viewInItem(itemPosition, R.id.view_in_item))
     *         .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
     *
     * @param itemPosition  The position of the item in the RecyclerView (the first item has position 0).
     * @param viewId        The identifier of the view contained within the RecyclerView item.
     * @return              A Matcher to check against.
     */
    public Matcher<View> viewInItem(final int itemPosition, final int viewId) {
        return new TypeSafeMatcher<View>() {
            Resources resources = null;
            View childView;

            public void describeTo(Description description) {
                try {
                    String s = String.format(
                            "RecyclerView with id '%s' at position: %d",
                            resources.getResourceName(recyclerViewId),
                            itemPosition
                    );

                    description.appendText(s);
                }
                catch (NullPointerException e) {
                    description.appendText("Resources object is null.");
                }
                catch (Resources.NotFoundException e) {
                    description.appendText(String.format("RecyclerView with id '%s' not found.", recyclerViewId));
                }
            }

            public boolean matchesSafely(View view) {
                resources = view.getResources();

                if (childView == null) {
                    RecyclerView recyclerView = view.getRootView().findViewById(recyclerViewId);
                    if (recyclerView != null && recyclerView.getId() == recyclerViewId) {
                        RecyclerView.ViewHolder viewHolder =
                                recyclerView.findViewHolderForAdapterPosition(itemPosition);
                        if (viewHolder != null) {
                            childView = viewHolder.itemView;
                        }
                    }
                    else {
                        return false;
                    }
                }

                View targetView = childView.findViewById(viewId);
                return view == targetView;
            }
        };
    }

    /**
     * Assert a condition on a recycler view item at a specific position (zero-indexed).
     *
     * @param position      The item's position in the recycler view.
     * @param itemMatcher   The matcher to assert with.
     * @return              A recycler view matcher to be checked against.
     */
    public static Matcher<View> atPosition(final int position, @NonNull final Matcher<View> itemMatcher) {
        checkNotNull(itemMatcher);

        return new BoundedMatcher<View, RecyclerView>(RecyclerView.class) {
            @Override
            public void describeTo(Description description) {
                description.appendText(String.format("RecyclerView contains an item at position %d.", position));
                itemMatcher.describeTo(description);
            }

            @Override
            protected boolean matchesSafely(final RecyclerView view) {
                RecyclerView.ViewHolder viewHolder = view.findViewHolderForAdapterPosition(position);

                if (viewHolder == null) {
                    // No item at the given position exists.
                    return false;
                }

                return itemMatcher.matches(viewHolder.itemView);
            }
        };
    }
}
