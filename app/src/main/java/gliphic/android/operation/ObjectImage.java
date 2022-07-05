/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.operation;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import gliphic.android.R;
import libraries.Vars;

import java.util.Arrays;

/**
 * This class allows either a known resource image or (unknown) {@link Bitmap} image to be stored as the
 * {@link Contact} or {@link Group} image for a given byte array.
 *
 * This class also contains method(s) to set the image for a given {@link ImageView}.
 */

public class ObjectImage {
    private int resourceInt;
    private byte[] imageBytes;
    private Bitmap bitmap = null;

    public ObjectImage(int resourceInt) {
        this.resourceInt = resourceInt;
        this.imageBytes = ImagePreset.get(resourceInt).getBytesValue();
    }

    public ObjectImage(@NonNull final byte[] imageBytes) {
        if (Vars.DisplayPicture.isDisplayPicture(imageBytes)) {
            this.imageBytes = imageBytes;
            this.resourceInt = ImagePreset.get(imageBytes).getResourceInt();
        }
        else {
            this.bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        }
    }

    private enum ImagePreset {
        APP_ICON                (Vars.DisplayPicture.APP_ICON.get(),                R.mipmap.gliphic_icon_colored),
        LADY_ARTIST             (Vars.DisplayPicture.LADY_ARTIST.get(),             R.drawable.lady_artist),
        LADY_ASTRONAUT          (Vars.DisplayPicture.LADY_ASTRONAUT.get(),          R.drawable.lady_astronaut),
        LADY_BUILDER            (Vars.DisplayPicture.LADY_BUILDER.get(),            R.drawable.lady_builder),
        LADY_COOK               (Vars.DisplayPicture.LADY_COOK.get(),               R.drawable.lady_cook),
        LADY_DOCTOR             (Vars.DisplayPicture.LADY_DOCTOR.get(),             R.drawable.lady_doctor),
        LADY_FARMER             (Vars.DisplayPicture.LADY_FARMER.get(),             R.drawable.lady_farmer),
        LADY_FIREFIGHTER        (Vars.DisplayPicture.LADY_FIREFIGHTER.get(),        R.drawable.lady_firefighter),
        LADY_MECHANIC           (Vars.DisplayPicture.LADY_MECHANIC.get(),           R.drawable.lady_mechanic),
        LADY_OFFICE_WORKER      (Vars.DisplayPicture.LADY_OFFICE_WORKER.get(),      R.drawable.lady_office_worker),
        LADY_PILOT              (Vars.DisplayPicture.LADY_PILOT.get(),              R.drawable.lady_pilot),
        LADY_ROYAL              (Vars.DisplayPicture.LADY_ROYAL.get(),              R.drawable.lady_royal),
        LADY_SCIENTIST          (Vars.DisplayPicture.LADY_SCIENTIST.get(),          R.drawable.lady_scientist),
        LADY_SINGER             (Vars.DisplayPicture.LADY_SINGER.get(),             R.drawable.lady_singer),
        LADY_STUDENT            (Vars.DisplayPicture.LADY_STUDENT.get(),            R.drawable.lady_student),
        LADY_TECHNOLOGIST       (Vars.DisplayPicture.LADY_TECHNOLOGIST.get(),       R.drawable.lady_technologist),
        MAN_ARTIST              (Vars.DisplayPicture.MAN_ARTIST.get(),              R.drawable.man_artist),
        MAN_ASTRONAUT           (Vars.DisplayPicture.MAN_ASTRONAUT.get(),           R.drawable.man_astronaut),
        MAN_BUILDER             (Vars.DisplayPicture.MAN_BUILDER.get(),             R.drawable.man_builder),
        MAN_COOK                (Vars.DisplayPicture.MAN_COOK.get(),                R.drawable.man_cook),
        MAN_DOCTOR              (Vars.DisplayPicture.MAN_DOCTOR.get(),              R.drawable.man_doctor),
        MAN_FARMER              (Vars.DisplayPicture.MAN_FARMER.get(),              R.drawable.man_farmer),
        MAN_FIREFIGHTER         (Vars.DisplayPicture.MAN_FIREFIGHTER.get(),         R.drawable.man_firefighter),
        MAN_MECHANIC            (Vars.DisplayPicture.MAN_MECHANIC.get(),            R.drawable.man_mechanic),
        MAN_OFFICE_WORKER       (Vars.DisplayPicture.MAN_OFFICE_WORKER.get(),       R.drawable.man_office_worker),
        MAN_PILOT               (Vars.DisplayPicture.MAN_PILOT.get(),               R.drawable.man_pilot),
        MAN_ROYAL               (Vars.DisplayPicture.MAN_ROYAL.get(),               R.drawable.man_royal),
        MAN_SCIENTIST           (Vars.DisplayPicture.MAN_SCIENTIST.get(),           R.drawable.man_scientist),
        MAN_SINGER              (Vars.DisplayPicture.MAN_SINGER.get(),              R.drawable.man_singer),
        MAN_STUDENT             (Vars.DisplayPicture.MAN_STUDENT.get(),             R.drawable.man_student),
        MAN_TECHNOLOGIST        (Vars.DisplayPicture.MAN_TECHNOLOGIST.get(),        R.drawable.man_technologist),
        HUMAN_FAMILY            (Vars.DisplayPicture.HUMAN_FAMILY.get(),            R.drawable.human_family),
        HUMAN_BRAIN             (Vars.DisplayPicture.HUMAN_BRAIN.get(),             R.drawable.human_brain),
        ANIMAL_CAT              (Vars.DisplayPicture.ANIMAL_CAT.get(),              R.drawable.animal_cat),
        ANIMAL_DOG              (Vars.DisplayPicture.ANIMAL_DOG.get(),              R.drawable.animal_dog),
        ANIMAL_DOVE             (Vars.DisplayPicture.ANIMAL_DOVE.get(),             R.drawable.animal_dove),
        ANIMAL_HORSE            (Vars.DisplayPicture.ANIMAL_HORSE.get(),            R.drawable.animal_horse),
        SPORT_BADMINTON         (Vars.DisplayPicture.SPORT_BADMINTON.get(),         R.drawable.sport_badminton),
        SPORT_CRICKET           (Vars.DisplayPicture.SPORT_CRICKET.get(),           R.drawable.sport_cricket),
        SPORT_GOLF              (Vars.DisplayPicture.SPORT_GOLF.get(),              R.drawable.sport_golf),
        SPORT_MARTIAL_ARTS      (Vars.DisplayPicture.SPORT_MARTIAL_ARTS.get(),      R.drawable.sport_martial_arts),
        SPORT_POOL              (Vars.DisplayPicture.SPORT_POOL.get(),              R.drawable.sport_pool),
        SPORT_RUGBY             (Vars.DisplayPicture.SPORT_RUGBY.get(),             R.drawable.sport_rugby),
        SPORT_SOCCER            (Vars.DisplayPicture.SPORT_SOCCER.get(),            R.drawable.sport_soccer),
        SPORT_TENNIS            (Vars.DisplayPicture.SPORT_TENNIS.get(),            R.drawable.sport_tennis),
        OTHER_AIRPLANE          (Vars.DisplayPicture.OTHER_AIRPLANE.get(),          R.drawable.other_airplane),
        OTHER_BOAT              (Vars.DisplayPicture.OTHER_BOAT.get(),              R.drawable.other_boat),
        OTHER_BOOK              (Vars.DisplayPicture.OTHER_BOOK.get(),              R.drawable.other_book),
        OTHER_CAR               (Vars.DisplayPicture.OTHER_CAR.get(),               R.drawable.other_car),
        OTHER_CARD              (Vars.DisplayPicture.OTHER_CARD.get(),              R.drawable.other_card),
        OTHER_CHRISTMAS_TREE    (Vars.DisplayPicture.OTHER_CHRISTMAS_TREE.get(),    R.drawable.other_christmas_tree),
        OTHER_DESERT_ISLAND     (Vars.DisplayPicture.OTHER_DESERT_ISLAND.get(),     R.drawable.other_desert_island),
        OTHER_DIE               (Vars.DisplayPicture.OTHER_DIE.get(),               R.drawable.other_die),
        OTHER_DOLLAR_NOTES      (Vars.DisplayPicture.OTHER_DOLLAR_NOTES.get(),      R.drawable.other_dollar_notes),
        OTHER_FISHING           (Vars.DisplayPicture.OTHER_FISHING.get(),           R.drawable.other_fishing),
        OTHER_GIFT              (Vars.DisplayPicture.OTHER_GIFT.get(),              R.drawable.other_gift),
        OTHER_ENVELOPE          (Vars.DisplayPicture.OTHER_ENVELOPE.get(),          R.drawable.other_envelope),
        OTHER_CAMERA            (Vars.DisplayPicture.OTHER_CAMERA.get(),            R.drawable.other_camera),
        OTHER_MUSIC             (Vars.DisplayPicture.OTHER_MUSIC.get(),             R.drawable.other_music),
        OTHER_NATIONAL_PARK     (Vars.DisplayPicture.OTHER_NATIONAL_PARK.get(),     R.drawable.other_national_park),
        OTHER_PAINTING          (Vars.DisplayPicture.OTHER_PAINTING.get(),          R.drawable.other_painting),
        OTHER_PARTY_POPPER      (Vars.DisplayPicture.OTHER_PARTY_POPPER.get(),      R.drawable.other_party_popper),
        OTHER_PERFORMING_ARTS   (Vars.DisplayPicture.OTHER_PERFORMING_ARTS.get(),   R.drawable.other_performing_arts),
        OTHER_PUMPKIN           (Vars.DisplayPicture.OTHER_PUMPKIN.get(),           R.drawable.other_pumpkin),
        OTHER_SWORDS            (Vars.DisplayPicture.OTHER_SHIELD.get(),            R.drawable.other_shield),
        OTHER_TELESCOPE         (Vars.DisplayPicture.OTHER_TELESCOPE.get(),         R.drawable.other_telescope),
        OTHER_TICKETS           (Vars.DisplayPicture.OTHER_TICKETS.get(),           R.drawable.other_tickets),
        OTHER_VIDEO_GAMES       (Vars.DisplayPicture.OTHER_VIDEO_GAMES.get(),       R.drawable.other_video_games),
        OTHER_WORLD_MAP         (Vars.DisplayPicture.OTHER_WORLD_MAP.get(),         R.drawable.other_world_map),
        OTHER_HEART             (Vars.DisplayPicture.OTHER_HEART.get(),             R.drawable.other_heart),
        OTHER_NECKTIE           (Vars.DisplayPicture.OTHER_NECKTIE.get(),           R.drawable.other_necktie),
        OTHER_SPEECH            (Vars.DisplayPicture.OTHER_SPEECH.get(),            R.drawable.other_speech),
        OTHER_TROPHY            (Vars.DisplayPicture.OTHER_TROPHY.get(),            R.drawable.other_trophy);

        private final byte[] bytesValue;
        private final int resourceInt;

        ImagePreset(final byte[] bytesValue, int resourceInt) {
            this.bytesValue = bytesValue;
            this.resourceInt = resourceInt;
        }

        public byte[] getBytesValue() {
            return bytesValue;
        }

        public int getResourceInt() {
            return resourceInt;
        }

        public static ImagePreset get(int resourceInt) {
            for (ImagePreset imagePreset : values()) {
                if (imagePreset.getResourceInt() == resourceInt) {
                    return imagePreset;
                }
            }

            return null;
        }

        public static ImagePreset get(byte[] bytesValue) {
            for (ImagePreset imagePreset : values()) {
                if (Arrays.equals(imagePreset.getBytesValue(), bytesValue)) {
                    return imagePreset;
                }
            }

            return null;
        }
    }

    public int getResourceInt() {
        return resourceInt;
    }

    public byte[] getImageBytes() {
        return imageBytes;
    }

    public void setImageView(ImageView imageView) {
        if (bitmap == null) {
            imageView.setImageResource(resourceInt);
        }
        else {
            imageView.setImageBitmap(bitmap);
        }
    }
}
