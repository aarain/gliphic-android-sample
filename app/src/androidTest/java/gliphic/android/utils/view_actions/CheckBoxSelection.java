package gliphic.android.utils.view_actions;

import android.view.View;
import android.widget.Checkable;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;

import static org.hamcrest.Matchers.isA;

/**
 * Allows a check box to be set as checked or unchecked regardless of it previous state.
 */
public class CheckBoxSelection {
    /**
     * Allows a check box to be set as checked or unchecked regardless of it previous state.
     *
     * IMPORTANT: Setting a checkbox value to true/false does not automatically add/remove items from a RecyclerView
     *            display, this must be invoked manually (e.g. by clicking the corresponding "load more" TextView).
     */
    public static ViewAction setChecked(final boolean checked) {
        return new ViewAction() {
            @Override
            public BaseMatcher<View> getConstraints() {
                return new BaseMatcher<View>() {
                    @Override
                    public boolean matches(Object object) {
                        return isA(Checkable.class).matches(object);
                    }

                    @Override
                    public void describeMismatch(Object object, Description description) {}

                    @Override
                    public void describeTo(Description description) {}
                };
            }

            @Override
            public String getDescription() {
                return null;
            }

            @Override
            public void perform(UiController uiController, View view) {
                ((Checkable) view).setChecked(checked);
            }
        };
    }
}