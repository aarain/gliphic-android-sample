package gliphic.android.utils;

import gliphic.android.display.abstract_views.BaseActivity;
import gliphic.android.display.main.MainActivity;

/**
 *
 */
public class RecyclerViewItemLoadingTestRule<A extends BaseActivity> extends NullGlobalStaticsTestRule<A> {

    public RecyclerViewItemLoadingTestRule(Class<A> activityClass) {
        super(activityClass);
    }

    @Override
    protected void before() {
        super.before();

        MainActivityBaseSetup.mainActivitySetupAfterActivityLaunched(
                getScenario(),
                MainActivity.class,
                true,
                true
        );
    }
}
