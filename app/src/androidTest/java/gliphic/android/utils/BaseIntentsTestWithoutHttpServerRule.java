package gliphic.android.utils;

import gliphic.android.display.abstract_views.BaseActivity;
import gliphic.android.display.main.MainActivity;

import androidx.test.espresso.intent.Intents;

import static org.junit.Assert.fail;

public class BaseIntentsTestWithoutHttpServerRule<A extends BaseActivity> extends BaseIntentsTestRule<A> {

    public BaseIntentsTestWithoutHttpServerRule(Class<A> activityClass) {
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
                false,
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
}
