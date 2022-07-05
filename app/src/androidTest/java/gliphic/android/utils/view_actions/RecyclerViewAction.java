package gliphic.android.utils.view_actions;

import android.view.View;

import org.hamcrest.Matcher;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;

public class RecyclerViewAction {

    /**
     * Click on a view with the given id. This method is used to click on a child view of a RecyclerView item (as
     * opposed to clicking on the item itself). This method should be used in conjunction with a method which specifies
     * the position of the item in the RecyclerView which will have its child view clicked on.
     *
     * Example usage:
     *     onView(withId(R.id.recycler_view)).perform(RecyclerViewActions.actionOnItemAtPosition(
     *             itemPositionInt,
     *             RecyclerViewAction.clickChildViewWithId(R.id.view_in_item)
     *     ));
     *
     * @param id    The id of the child view within the RecyclerView item to click on.
     * @return      A ViewAction to use with a ViewInteraction
     */
    public static ViewAction clickChildViewWithId(final int id) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return null;
            }

            @Override
            public String getDescription() {
                return "Click on a view with the given id.";
            }

            @Override
            public void perform(UiController uiController, View view) {
                View v = view.findViewById(id);
                v.performClick();
            }
        };
    }
}
