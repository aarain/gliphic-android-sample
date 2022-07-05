package gliphic.android.with_http_server.functionality;

import gliphic.android.display.ReportActivity;
import gliphic.android.display.main.MainActivity;

import gliphic.android.utils.BaseActivityTestRule;
import gliphic.android.utils.MainActivityBaseSetup;

import static org.junit.Assert.fail;

/**
 * The test rule must extend an instance of {@link gliphic.android.display.abstract_views.BaseMainActivity} but cannot
 * extend {@link MainActivity} because it will automatically finish and start the
 * {@link gliphic.android.display.welcome_screen.SignInActivity} (because the main activity setup is performed in the
 * before() method) hence the scenario would not be available for the tests.
 */
public class RequestGlobalStaticTestRule<A extends ReportActivity> extends BaseActivityTestRule<A> {

    public RequestGlobalStaticTestRule(Class<A> activityClass) {
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
                true,
                false
        );
    }
}
