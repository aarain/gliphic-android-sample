package gliphic.android.utils.matchers;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.hamcrest.Description;
import org.hamcrest.Matcher;

import androidx.test.espresso.matcher.BoundedMatcher;
import gliphic.android.utils.AndroidTestUtils;

public class CustomMatchers {

    /**
     * Check if a {@link TextView} is using the {@link android.graphics.Typeface#ITALIC} typeface.
     */
    public static Matcher<View> isItalicsTypeface() {
        return new BoundedMatcher<View, TextView>(TextView.class) {
            @Override
            public boolean matchesSafely(TextView textView) {
                return textView.getTypeface().isItalic();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("check italics typeface");
            }
        };
    }

    /**
     * Check if a {@link ImageView} is using the same image as a given resource ID.
     *
     * @param resourceId    The resource ID of the expected image, e.g. from a drawable resource.
     * @return              The matcher.
     */
    public static Matcher<View> withDrawable(final int resourceId) {
        final Drawable drawable = AndroidTestUtils.getApplicationContext().getDrawable(resourceId);

        return new BoundedMatcher<View, ImageView>(ImageView.class) {
            @Override
            public boolean matchesSafely(ImageView imageView) {
                if (drawable == null) {
                    return imageView.getDrawable() == null;
                }
                else if (imageView.getDrawable() == null) {
                    return false;
                }

                if (drawable instanceof BitmapDrawable && imageView.getDrawable() instanceof BitmapDrawable) {
                    final Bitmap b1 = ((BitmapDrawable) drawable).getBitmap();
                    final Bitmap b2 = ((BitmapDrawable) imageView.getDrawable()).getBitmap();

                    return b1.sameAs(b2);
                }

                return imageView.getDrawable().getConstantState().equals(drawable.getConstantState());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("compare the image from a resource ID with a the image from the ImageView");
            }
        };
    }
}
