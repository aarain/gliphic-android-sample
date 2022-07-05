package gliphic.android.utils.matchers;

import android.os.IBinder;
import android.view.WindowManager;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import androidx.test.espresso.Root;

public class ToastMatcher extends TypeSafeMatcher<Root> {

    @Override
    public void describeTo(Description description) {
        description.appendText("is toast");
    }

    @Override
    public boolean matchesSafely(Root root) {
        // WindowManager.LayoutParams.TYPE_TOAST was deprecated in API level 26 but since its replacement,
        // WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, requires the Manifest.permission.SYSTEM_ALERT_WINDOW
        // permission, the deprecated value is used since this method is only used in test classes.
        if (root.getWindowLayoutParams().get().type == WindowManager.LayoutParams.TYPE_TOAST) {
            IBinder windowToken = root.getDecorView().getWindowToken();
            IBinder appToken = root.getDecorView().getApplicationWindowToken();

            // Return true if this window isn't contained by any other windows.
            // Returning false implies it has type TYPE_BASE_APPLICATION.
            return windowToken == appToken;
        }
        return false;
    }
}
