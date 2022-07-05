package gliphic.android.utils.view_assertions;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.ViewAssertion;

import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class AssertRecyclerViewItemCount implements ViewAssertion {
    private Integer valueToCheck = null;

    public AssertRecyclerViewItemCount(int valueToCheck) {
        this(false, valueToCheck);
    }

    /**
     * Initialise this object with either a RecyclerView ID or a value to assert against the number of items in the
     * RecyclerView.
     *
     * When isViewId is true no assertion is made (this has to be done in a separate method call).
     * When isViewId is false an assertion is made immediately against the given value. Note that the one argument
     * constructor can also be used for the case when isViewId is false.
     *
     * Example usages:
     * * Assert the number of items currently displayed in the RecyclerView against a given value:
     *     onView(withId(recyclerViewId)).check(new AssertRecyclerViewItemCount(valueToCheck));
     *
     * * Initialise this object with the number of items currently displayed in the RecyclerView:
     *     AssertRecyclerViewItemCount arvic = new AssertRecyclerViewItemCount(true, recyclerViewId);
     *
     * * Assert the number of items currently displayed in the RecyclerView against a previous (unknown) value:
     *     onView(withId(recyclerViewId)).check(arvic);
     *
     * @param isViewId              Set to true to initialise the given (recycler) view ID with the number of items in
     *                              its view and set to false to immediately assert the given integer with the number
     *                              of items currently in the view.
     * @param viewIdOrValueToCheck  If isViewId is true then this value is the view ID of the RecyclerView and
     *                              if isViewId is false then this value is asserted against the number of items in the
     *                              RecyclerView.
     */
    public AssertRecyclerViewItemCount(boolean isViewId, int viewIdOrValueToCheck) {
        if (isViewId) {
            Espresso.onView(withId(viewIdOrValueToCheck)).check(this);
        }
        else {
            this.valueToCheck = viewIdOrValueToCheck;
        }
    }

    public Integer getValue() {
        return valueToCheck;
    }

    @Override
    public void check(View view, NoMatchingViewException noMatchingViewException) {
        if (noMatchingViewException != null) {
            throw noMatchingViewException;
        }

        RecyclerView recyclerView = (RecyclerView) view;
        RecyclerView.Adapter adapter = recyclerView.getAdapter();
        if (valueToCheck == null) {
            valueToCheck = adapter.getItemCount();
        }
        else {
            assertThat(adapter.getItemCount(), is(valueToCheck));
        }
    }
}
