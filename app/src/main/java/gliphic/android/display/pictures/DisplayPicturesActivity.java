/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.display.pictures;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.GridView;

import java.io.Serializable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import gliphic.android.R;
import gliphic.android.adapters.ImageAdapter;
import gliphic.android.display.abstract_views.BaseMainActivity;

/**
 * The contact/group pictures available to display for the current contact.
 */
public class DisplayPicturesActivity extends BaseMainActivity {
    static final String FINISH_ACTIVITY_INTENT_NAME = "finish_activity";
    static final String IMAGE_INDEX_INTENT_NAME     = "img_id";
    static final String VIEW_INTENT_NAME            = "object_view";
    static final String OBJECT_NUMBER               = "object_number";
    static final String REQUEST_SET_IMAGE           = "set_img";
    public static final String SELECTED_IMAGE_RESOURCE_ID  = "img_res_id";

    enum ObjectView {CONTACT_VIEW, GROUP_VIEW}

    /**
     * Launch an instance of this activity from a context displaying contact information.
     *
     * @param sourceActivity    The calling context which will start an instance of this activity.
     * @param requestSetImage   True iff a request should be sent to the server to set the selected image as the
     *                          contact profile image, false to not send a request.
     */
    public static void launchActivityForContact(@NonNull final Activity sourceActivity,
                                                final boolean requestSetImage) {

        launchActivity(sourceActivity, ObjectView.CONTACT_VIEW, requestSetImage, null);
    }

    /**
     * Launch an instance of this activity from a context displaying group information.
     *
     * No request will be sent to the server to set the selected image as the group image.
     *
     * @param sourceActivity    The calling context which will start an instance of this activity.
     */
    public static void launchActivityForGroup(@NonNull final Activity sourceActivity) {

        launchActivity(sourceActivity, ObjectView.GROUP_VIEW, false, null);
    }

    /**
     * Launch an instance of this activity from a context displaying group information.
     *
     * This method assumes that a request should be sent to the server to set the selected image as the group image.
     *
     * @param sourceActivity    The calling context which will start an instance of this activity.
     * @param groupNumber       The group to select the image for.
     */
    public static void launchActivityForGroup(@NonNull final Activity sourceActivity, final long groupNumber) {

        launchActivity(sourceActivity, ObjectView.GROUP_VIEW, true, groupNumber);
    }

    private static void launchActivity(@NonNull final Activity sourceActivity,
                                       @NonNull final ObjectView objectView,
                                       final boolean requestSetImage,
                                       @Nullable final Long number) {

        Intent intent = new Intent(sourceActivity, DisplayPicturesActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra(VIEW_INTENT_NAME, objectView);
        intent.putExtra(REQUEST_SET_IMAGE, requestSetImage);
        if (number != null) {
            intent.putExtra(OBJECT_NUMBER, number);
        }
        sourceActivity.startActivityForResult(intent, 1);   // The request code is ignored.
    }

    @Override
    public void onNetworkAvailable(boolean isFirstOnNetworkAvailable) {}

    @Override
    public void noNetworkOnStart() {}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_display_pictures);

        setSupportActionBar(findViewById(R.id.toolbar_display_pictures));

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        final Bundle extras = getIntent().getExtras();
        if (extras == null) {
            return;
        }

        final Serializable objectView = extras.getSerializable(VIEW_INTENT_NAME);
        if (objectView == null) {
            return;
        }

        final Long objectNumber       = extras.getLong(DisplayPicturesActivity.OBJECT_NUMBER);
        final boolean requestSetImage = extras.getBoolean(DisplayPicturesActivity.REQUEST_SET_IMAGE);

        // The activity should now be correctly initialized.

        final GridView gridview = findViewById(R.id.gridview_display_pictures);

        gridview.setAdapter(new ImageAdapter(this));

        gridview.setOnItemClickListener((adapterView, view, position, id) -> {
            final Intent intent = new Intent(getApplicationContext(), SinglePictureActivity.class);

            intent.putExtra(IMAGE_INDEX_INTENT_NAME, position);
            intent.putExtra(VIEW_INTENT_NAME, objectView);
            intent.putExtra(REQUEST_SET_IMAGE, requestSetImage);
            if (objectView.equals(ObjectView.GROUP_VIEW)) {
                intent.putExtra(OBJECT_NUMBER, objectNumber);
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

            startActivityForResult(intent, 1);  // The request code is ignored.
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            final Bundle extras = data.getExtras();

            if (extras != null && extras.getBoolean(FINISH_ACTIVITY_INTENT_NAME)) {
                final Intent intent = new Intent();
                intent.putExtra(SELECTED_IMAGE_RESOURCE_ID, extras.getInt(SELECTED_IMAGE_RESOURCE_ID));
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        }
    }
}
