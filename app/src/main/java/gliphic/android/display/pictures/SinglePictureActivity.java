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
import android.widget.Button;
import android.widget.ImageView;

import org.bouncycastle.util.encoders.Base64;

import java.io.Serializable;

import gliphic.android.R;
import gliphic.android.adapters.ImageAdapter;
import gliphic.android.display.libraries.LoadingDialog;
import gliphic.android.display.libraries.Toasts;
import gliphic.android.exceptions.ContactException;
import gliphic.android.exceptions.GroupException;
import gliphic.android.operation.ObjectImage;
import gliphic.android.operation.misc.ContactGroupAssociation;
import gliphic.android.operation.server_interaction.http_server.HttpOperations;
import gliphic.android.operation.server_interaction.http_server.RequestGlobalStatic;
import libraries.GeneralUtils;
import pojo.load.LoadContactObject;
import pojo.load.LoadGroupObject;
import pojo.misc.AccessTokenAndString;
import pojo.set.SetGroupString;

/**
 * Display a single picture fullscreen.
 */
public class SinglePictureActivity extends DisplayPicturesActivity {
    private int imageResourceId;

    @Override
    public void onNetworkAvailable(boolean isFirstOnNetworkAvailable) {}

    @Override
    public void noNetworkOnStart() {}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_single_picture);

        setSupportActionBar(findViewById(R.id.toolbar_single_picture));

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        final ImageView imageView = findViewById(R.id.imageview_single_picture);
        final Button    button    = findViewById(R.id.btn_single_picture);

        final Bundle extras = getIntent().getExtras();

        if (extras == null) {
            return;
        }

        final int          position        = extras.getInt(DisplayPicturesActivity.IMAGE_INDEX_INTENT_NAME);
        final Serializable objectView      = extras.getSerializable(DisplayPicturesActivity.VIEW_INTENT_NAME);
        final boolean      requestSetImage = extras.getBoolean(DisplayPicturesActivity.REQUEST_SET_IMAGE);
        final long         objectNumber    = extras.getLong(DisplayPicturesActivity.OBJECT_NUMBER);

        final ImageAdapter imageAdapter = new ImageAdapter(this);
        imageResourceId = (int) imageAdapter.getItemId(position);
        imageView.setImageResource(imageResourceId);

        final ObjectImage objectImage = imageAdapter.getItem(position);
        final String imageString = Base64.toBase64String(objectImage.getImageBytes());

        button.setOnClickListener(v -> {
            if (requestSetImage) {
                final LoadingDialog loadingDialog = new LoadingDialog(SinglePictureActivity.this);

                RequestGlobalStatic.requestAndSetAccessToken(
                        accessToken -> {
                            if (accessToken == null) {
                                return;
                            }

                            if (objectView.equals(ObjectView.CONTACT_VIEW)) {
                                final AccessTokenAndString accessTokenAndString = new AccessTokenAndString(
                                        accessToken,
                                        imageString
                                );

                                HttpOperations.post(
                                        HttpOperations.URI_SET_CONTACT_IMAGE,
                                        accessTokenAndString,
                                        SinglePictureActivity.this,
                                        response -> {
                                            loadingDialog.dismissDialog();

                                            final LoadContactObject loadContactObject = GeneralUtils.fromJson(
                                                    response,
                                                    LoadContactObject.class
                                            );

                                            try {
                                                ContactGroupAssociation.storeContactAndSetAssociations(loadContactObject);

                                                Toasts.showLongToast(
                                                        SinglePictureActivity.this,
                                                        "Contact profile image set."
                                                );
                                            }
                                            catch (ContactException e) {
                                                showImageUpdatedButNotLoadedErrorToast();
                                            }

                                            finishThisAndCallingActivity();
                                        },
                                        error -> {
                                            loadingDialog.dismissDialog();

                                            HttpOperations.handleStandardRequestOnErrorResponse(
                                                    error,
                                                    SinglePictureActivity.this,
                                                    false
                                            );
                                        }
                                );
                            }
                            else if (objectView.equals(ObjectView.GROUP_VIEW)) {
                                final SetGroupString setGroupString = new SetGroupString(
                                        accessToken,
                                        objectNumber,
                                        imageString
                                );

                                HttpOperations.post(
                                        HttpOperations.URI_SET_GROUP_IMAGE,
                                        setGroupString,
                                        SinglePictureActivity.this,
                                        response -> {
                                            loadingDialog.dismissDialog();

                                            final LoadGroupObject loadGroupObject = GeneralUtils.fromJson(
                                                    response,
                                                    LoadGroupObject.class
                                            );

                                            try {
                                                ContactGroupAssociation.storeGroupAndSetAssociations(loadGroupObject);

                                                Toasts.showLongToast(
                                                        SinglePictureActivity.this,
                                                        "Group image set."
                                                );
                                            }
                                            catch (GroupException e) {
                                                showImageUpdatedButNotLoadedErrorToast();
                                            }

                                            finishThisAndCallingActivity();
                                        },
                                        error -> {
                                            loadingDialog.dismissDialog();

                                            HttpOperations.handleSetGroupDataOnErrorResponse(
                                                    error,
                                                    SinglePictureActivity.this
                                            );
                                        }
                                );
                            }
                            else {
                                Toasts.showLongToast(SinglePictureActivity.this, "Error setting the image.");
                            }
                        },
                        SinglePictureActivity.this,
                        loadingDialog,
                        false
                );
            }
            else {
                finishThisAndCallingActivity();
            }
        });
    }

    private void showImageUpdatedButNotLoadedErrorToast() {
        Toasts.showLongToast(SinglePictureActivity.this, "Image updated but not loaded.");
    }

    private void finishThisAndCallingActivity() {
        final Intent intent = new Intent();
        intent.putExtra(DisplayPicturesActivity.FINISH_ACTIVITY_INTENT_NAME, true);
        intent.putExtra(DisplayPicturesActivity.SELECTED_IMAGE_RESOURCE_ID, imageResourceId);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}
