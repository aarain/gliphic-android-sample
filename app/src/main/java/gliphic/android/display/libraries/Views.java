/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.display.libraries;

import android.view.View;

import java.util.List;

import androidx.annotation.NonNull;

public class Views {

    /**
     * For all views in the given list, set the visual alpha and whether or not they can be clicked/focused.
     *
     * The given list of views can be a list of any objects which extend {@link View}, e.g. a list containing Buttons,
     * TextViews or CheckBoxes.
     *
     * @param views         All views to modify.
     * @param enableViews   True if all views should be enabled, false if all views should be disabled.
     */
    public static void setAlphaAndEnabled(@NonNull List<? extends View> views, boolean enableViews) {
        for (View v : views) {
            // Required for subclasses of View which are not visually modified by the call to setEnabled() e.g.
            // TextView.
            v.setAlpha(enableViews ? 1f : 0.5f);

            v.setEnabled(enableViews);
        }
    }
}
