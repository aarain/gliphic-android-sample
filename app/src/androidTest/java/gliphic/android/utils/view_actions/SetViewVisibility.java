package gliphic.android.utils.view_actions;

import android.view.View;

import org.hamcrest.Matcher;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;

import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;

public class SetViewVisibility {

    /**
     * Set a view's visibility.
     *
     * Example usage:   onView(withId(R.id.some_view_id)).perform(setViewVisibility(View.VISIBLE));
     *
     * @param value     The visibility of the view, usually View.VISIBLE, View.GONE or View.INVISIBLE
     * @return          A ViewAction for the ViewInteraction.perform() method.
     */
    public static ViewAction setViewVisibility(final int value) {
        return new ViewAction() {

            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(View.class);
            }

            @Override
            public void perform(UiController uiController, View view) {
                view.setVisibility(value);
            }

            @Override
            public String getDescription() {
                return "Show / Hide View";
            }
        };
    }
}
