package gliphic.android.utils;

import gliphic.android.display.abstract_views.BaseActivity;
import gliphic.android.display.main.MainActivity;

/**
 *
 */
public class NullGlobalStaticsTestWithoutHttpServerRule<A extends BaseActivity> extends NullGlobalStaticsTestRule<A> {

    public NullGlobalStaticsTestWithoutHttpServerRule(Class<A> activityClass) {
        super(activityClass);
    }

    @Override
    protected void before() {
        super.before();

        MainActivityBaseSetup.mainActivitySetupAfterActivityLaunched(
                getScenario(),
                MainActivity.class,
                false,
                true
        );
    }
}
