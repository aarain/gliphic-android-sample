/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.display.abstract_views;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import gliphic.android.R;
import gliphic.android.display.libraries.AlertDialogs;
import gliphic.android.display.libraries.EditTextImeOptions;
import gliphic.android.display.welcome_screen.SubmitCodeActivity;
import libraries.GeneralUtils;

/**
 * This fragment should be the parent class of all other fragments which send any account code.
 *
 * This method allows child fragments to define behaviour for when the user attempts to submit a code: the message
 * that is sent to the HTTP server and how the server's response is handled.
 */

public abstract class BaseSubmitTokenFragment extends Fragment {
    private final String badCodeTitle;
    private final String emptyCodeMsg;

    public BaseSubmitTokenFragment(@NonNull String badCodeTitle, @NonNull String emptyCodeMsg) {
        this.badCodeTitle = badCodeTitle;
        this.emptyCodeMsg = emptyCodeMsg;
    }

    protected SubmitCodeActivity submitCodeActivity;
    protected EditText editTextSubmitCode;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.submit_code_tab, container, false);

        submitCodeActivity = (SubmitCodeActivity) getActivity();
        if (submitCodeActivity == null) {
            return rootView;
        }

        editTextSubmitCode              = rootView.findViewById(R.id.edittext_submit_code);
        final EditText editText2        = rootView.findViewById(R.id.submit_code_edittext_2);
        final EditText editText3        = rootView.findViewById(R.id.submit_code_edittext_3);
        final EditText editText4        = rootView.findViewById(R.id.submit_code_edittext_4);
        final CheckBox checkBox         = rootView.findViewById(R.id.submit_code_checkbox);
        final Button   submitCodeButton = rootView.findViewById(R.id.btn_submit_code);

        // This allows for easier view enabling/disabling.
        submitCodeActivity.addToAllClickableViews(Arrays.asList(
                editTextSubmitCode,
                editText2,
                editText3,
                editText4,
                checkBox,
                submitCodeButton
        ));

        // This is overridden by the extending class.
        EditTextImeOptions.setEnterListener(submitCodeActivity, editTextSubmitCode, null);

        // Required to combine the 'text' and 'textMultiLine' input types (set in XML).
        editTextSubmitCode.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editTextSubmitCode.setRawInputType(InputType.TYPE_CLASS_TEXT);

        submitCodeButton.setOnClickListener(view -> {
            // Disable all buttons.
            submitCodeActivity.deactivateClickableViews();

            // Remove all whitespaces and non-visible characters.
            final String token = GeneralUtils.removeAllWhiteSpace(editTextSubmitCode.getText().toString());
            editTextSubmitCode.setText(token);

            if (token.isEmpty()) {
                AlertDialogs.genericConfirmationDialog(submitCodeActivity, badCodeTitle, emptyCodeMsg);

                submitCodeActivity.activateClickableViews();
                return;
            }

            submitCode(submitCodeActivity, token);
        });

        return rootView;
    }

    /**
     * After the preliminary checks, send the code (token) to the server via HTTP and handle the response.
     */
    public abstract void submitCode(@NonNull SubmitCodeActivity submitCodeActivity, @NonNull String token);
}
