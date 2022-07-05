package gliphic.android.utils;

import gliphic.android.display.abstract_views.BaseActivity;
import gliphic.android.display.main.MainActivity;

import static org.junit.Assert.fail;

public class BaseActivityTestWithoutMainActivityStartRule<A extends BaseActivity> extends BaseActivityTestRule<A> {

    public BaseActivityTestWithoutMainActivityStartRule(Class<A> activityClass) {
        super(activityClass);
    }

    @Override
    public void before() {
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
                true,
                false
        );
    }
}
