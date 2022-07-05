package gliphic.android.utils.view_actions;

import static androidx.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import android.graphics.Rect;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ListView;
import android.widget.ScrollView;
import org.hamcrest.Matcher;

import androidx.core.widget.NestedScrollView;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.espresso.util.HumanReadables;
import gliphic.android.operation.misc.Log;

/**
 * Enables scrolling to the given view. View must be a descendant of a ScrollView or ListView.
 */
public final class ScrollToActionModified implements ViewAction {
    private static final String TAG = ScrollToActionModified.class.getSimpleName();

    @Override
    public Matcher<View> getConstraints() {
        return allOf(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE), ViewMatchers.isDescendantOfA(anyOf(
                ViewMatchers.isAssignableFrom(ScrollView.class),
                ViewMatchers.isAssignableFrom(HorizontalScrollView.class),
                ViewMatchers.isAssignableFrom(ListView.class),
                ViewMatchers.isAssignableFrom(NestedScrollView.class)    // Additional condition allowed.
        )));
    }
    @Override
    public void perform(UiController uiController, View view) {
        if (isDisplayingAtLeast(90).matches(view)) {
            Log.i(TAG, "View is already displayed. Returning.");
            return;
        }
        Rect rect = new Rect();
        view.getDrawingRect(rect);
        if (!view.requestRectangleOnScreen(rect, true /* immediate */)) {
            Log.w(TAG, "Scrolling to view was requested, but none of the parents scrolled.");
        }
        uiController.loopMainThreadUntilIdle();
        if (!isDisplayingAtLeast(90).matches(view)) {
            throw new PerformException.Builder()
                    .withActionDescription(this.getDescription())
                    .withViewDescription(HumanReadables.describe(view))
                    .withCause(new RuntimeException(
                            "Scrolling to view was attempted, but the view is not displayed"))
                    .build();
        }
    }
    @Override
    public String getDescription() {
        return "scroll to";
    }
}