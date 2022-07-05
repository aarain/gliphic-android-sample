package gliphic.android.utils.view_actions;

import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.GeneralLocation;
import androidx.test.espresso.action.GeneralSwipeAction;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Swipe;

public final class FastSwipe {
    public static ViewAction up() {
        return new GeneralSwipeAction(
                Swipe.FAST,
                GeneralLocation.BOTTOM_CENTER,
                GeneralLocation.TOP_CENTER,
                Press.FINGER
        );
    }
}