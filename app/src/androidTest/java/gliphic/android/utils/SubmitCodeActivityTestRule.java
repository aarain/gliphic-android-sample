package gliphic.android.utils;

import gliphic.android.display.main.MainActivity;
import gliphic.android.display.welcome_screen.SubmitCodeActivity;
import gliphic.android.operation.storage_handlers.SharedPreferencesHandler;

/**
 *
 */
public class SubmitCodeActivityTestRule<A extends SubmitCodeActivity> extends BaseActivityTestRule<A> {

    public SubmitCodeActivityTestRule(Class<A> activityClass) {
        super(activityClass);
    }

    @Override
    protected void before() {
        setScenario(getScenarioSupplier());

        // Ensure that no existing groups/contacts stored cause a conflict when signing in.
        SharedPreferencesHandler.removeAllContactData(AndroidTestUtils.getApplicationContext());

        MainActivityBaseSetup.mainActivitySetupAfterActivityLaunched(
                getScenario(),
                MainActivity.class,
                true,
                false
        );
    }
}
