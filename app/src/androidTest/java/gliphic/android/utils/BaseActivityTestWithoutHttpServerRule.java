package gliphic.android.utils;

import gliphic.android.display.abstract_views.BaseActivity;
import gliphic.android.display.main.MainActivity;

import static org.junit.Assert.fail;

public class BaseActivityTestWithoutHttpServerRule<A extends BaseActivity> extends BaseActivityTestRule<A> {

    public BaseActivityTestWithoutHttpServerRule(Class<A> activityClass) {
        super(activityClass);
    }

    @Override
    protected void before() {
        setScenario(getScenarioSupplier());

        try {
            MainActivityBaseSetup.mainActivitySetupBeforeActivityLaunched();
        }
        catch (Exception e) {
            fail(e.getMessage());
        }

        MainActivityBaseSetup.mainActivitySetupAfterActivityLaunched(
                getScenario(),
                MainActivity.class,
                false,
                true
        );
    }
}
