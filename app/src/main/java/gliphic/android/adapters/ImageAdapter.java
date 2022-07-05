/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.adapters;

import android.content.Context;

import android.view.View;
import android.view.ViewGroup;

import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import gliphic.android.R;
import gliphic.android.operation.ObjectImage;

/**
 * The adapter for a grid of pictures.
 *
 * Facilitates creating a {@link GridView} to display user's available images.
 */
public class ImageAdapter extends BaseAdapter {

    private static final Integer[] imageResourceIds = {
            R.drawable.lady_artist,
            R.drawable.lady_astronaut,
            R.drawable.lady_builder,
            R.drawable.lady_cook,
            R.drawable.lady_doctor,
            R.drawable.lady_farmer,
            R.drawable.lady_firefighter,
            R.drawable.lady_mechanic,
            R.drawable.lady_office_worker,
            R.drawable.lady_pilot,
            R.drawable.lady_royal,
            R.drawable.lady_scientist,
            R.drawable.lady_singer,
            R.drawable.lady_student,
            R.drawable.lady_technologist,
            R.drawable.man_artist,
            R.drawable.man_astronaut,
            R.drawable.man_builder,
            R.drawable.man_cook,
            R.drawable.man_doctor,
            R.drawable.man_farmer,
            R.drawable.man_firefighter,
            R.drawable.man_mechanic,
            R.drawable.man_office_worker,
            R.drawable.man_pilot,
            R.drawable.man_royal,
            R.drawable.man_scientist,
            R.drawable.man_singer,
            R.drawable.man_student,
            R.drawable.man_technologist,
            R.drawable.human_family,
            R.drawable.animal_cat,
            R.drawable.animal_dog,
            R.drawable.animal_horse,
            R.drawable.animal_dove,
            R.drawable.other_heart,
            R.drawable.human_brain,
            R.drawable.sport_badminton,
            R.drawable.sport_tennis,
            R.drawable.sport_cricket,
            R.drawable.sport_soccer,
            R.drawable.sport_rugby,
            R.drawable.sport_pool,
            R.drawable.sport_martial_arts,
            R.drawable.sport_golf,
            R.drawable.other_trophy,
            R.drawable.other_shield,
            R.drawable.other_fishing,
            R.drawable.other_airplane,
            R.drawable.other_boat,
            R.drawable.other_car,
            R.drawable.other_speech,
            R.drawable.other_envelope,
            R.drawable.other_camera,
            R.drawable.other_necktie,
            R.drawable.other_telescope,
            R.drawable.other_video_games,
            R.drawable.other_card,
            R.drawable.other_die,
            R.drawable.other_book,
            R.drawable.other_music,
            R.drawable.other_painting,
            R.drawable.other_performing_arts,
            R.drawable.other_tickets,
            R.drawable.other_dollar_notes,
            R.drawable.other_gift,
            R.drawable.other_party_popper,
            R.drawable.other_pumpkin,
            R.drawable.other_christmas_tree,
            R.drawable.other_desert_island,
            R.drawable.other_national_park,
            R.drawable.other_world_map
    };

    private final Context context;

    public ImageAdapter(@NonNull final Context context) {
        this.context = context;
    }

    public int getCount() {
        return imageResourceIds.length;
    }

    public ObjectImage getItem(int position) {
        return new ObjectImage((int) getItemId(position));
    }

    public long getItemId(int position) {
        return imageResourceIds[position];
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        final ImageView imageView;

        final int widthAndHeight = 200;
        final int padding = (int) context.getResources().getDimension(R.dimen.small_pad);

        if (convertView == null) {
            imageView = new ImageView(context);
            imageView.setLayoutParams(new GridView.LayoutParams(widthAndHeight, widthAndHeight));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(padding, padding, padding, padding);

        }
        else {
            imageView = (ImageView) convertView;
        }

        imageView.setImageResource(imageResourceIds[position]);

        return imageView;
    }

    /**
     * Get the position of the given image in the list of displayed images.
     *
     * If these images are displayed in a {@link GridView}, the item position of the image in the grid is calculated
     * first from left to right (starting at 0) and then top to bottom.
     *
     * @param objectImage   The object containing the image which is displayed in this adapter.
     * @return              The position of the image in this adapter.
     */
    public static Integer getItemPosition(@NonNull final ObjectImage objectImage) {
        final int resourceInt = objectImage.getResourceInt();

        for (int i = 0; i < imageResourceIds.length; i++) {
            if (imageResourceIds[i] == resourceInt) {
                return i;
            }
        }

        return null;
    }
}
