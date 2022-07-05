/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.listeners;

import androidx.core.widget.NestedScrollView;

/**
 * Implement a listener which fires every time the specified item index in the given RecyclerView
 * is visible on the screen.
 */
public abstract class NestedScrollViewListener implements NestedScrollView.OnScrollChangeListener {
    /*
     * The distance from the bottom of the view which triggers the (overridden)
     * onScrollItemVisible() method. This integer must be non-negative.
     */
    private static final int LOAD_DISTANCE_FROM_BOTTOM = 0;

    public void onScrollChange(NestedScrollView view, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
        if (view.getChildAt(0) != null) {
            int viewHeight = view.getChildAt(0).getMeasuredHeight() - view.getMeasuredHeight();
            if ( scrollY + LOAD_DISTANCE_FROM_BOTTOM >= viewHeight && scrollY > oldScrollY ) {
                onScrollItemVisible();
            }
        }
    }

    public abstract void onScrollItemVisible();
}