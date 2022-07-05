package gliphic.android.utils;

import gliphic.android.display.main.MainActivity;
import gliphic.android.display.abstract_views.BaseActivity;

import androidx.test.espresso.intent.Intents;

import static org.junit.Assert.fail;

public class BaseIntentsTestRule<A extends BaseActivity> extends BaseActivityTestRule<A> {

    public BaseIntentsTestRule(Class<A> activityClass) {
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
                true
        );

        try {
            Intents.init();
        }
        catch (IllegalStateException e) {
            Intents.release();
            Intents.init();
        }
    }

    @Override
    protected void after() {
        Intents.release();
    }
}
