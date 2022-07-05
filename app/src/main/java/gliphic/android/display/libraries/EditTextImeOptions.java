/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.display.libraries;

import android.app.Activity;
import android.content.Context;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;

/**
 * Handle EditText input and modify its soft keyboard.
 */
public class EditTextImeOptions {
    /**
     * Change the Enter key on the soft keyboard to show DONE instead of NEW_LINE,
     * and change/lose the focus of the EditText, hiding the keyboard if focus is lost.
     *
     * @param activity      The activity which holds the given EditText view(s).
     * @param focusedView   The (EditText) view which currently has focus.
     * @param newView       This should be one of two types:
     *                      1. null - hides the soft keyboard and clears focus.
     *                      2. EditText - switches focus to the given view.
     */
    public static void setEnterListener(@NonNull final Activity activity,
                                        @NonNull final EditText focusedView,
                                                 final EditText newView) {

        focusedView.setImeOptions(EditorInfo.IME_ACTION_DONE);

        focusedView.setOnEditorActionListener((v, actionId, event) -> {
            boolean handled = false;

            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (newView == null) {
                    InputMethodManager imm =
                            (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);

                    if (imm != null) {
                        imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                    }

                    focusedView.clearFocus();
                }
                else {
                    newView.requestFocus();
                }

                handled = true;
            }

            return handled;
        });
    }
}
