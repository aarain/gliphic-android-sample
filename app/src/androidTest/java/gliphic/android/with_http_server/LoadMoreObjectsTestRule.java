package gliphic.android.with_http_server;

import gliphic.android.display.abstract_views.BaseActivity;
import gliphic.android.operation.storage_handlers.SharedPreferencesHandler;
import gliphic.android.utils.BaseActivityTestRule;
import gliphic.android.with_http_server.functionality.SignInAndOutTest;

import static org.junit.Assert.fail;

/**
 *
 */
public class LoadMoreObjectsTestRule<A extends BaseActivity> extends BaseActivityTestRule<A> {

    public LoadMoreObjectsTestRule(Class<A> activityClass) {
        super(activityClass);
    }

    @Override
    public void before() {
        setScenario(getScenarioSupplier());

        getScenario().onActivity(SharedPreferencesHandler::removeAllContactData);

        try {
            SignInAndOutTest.signIn();
        }
        catch (Throwable e) {
            fail(e.getMessage());
        }
    }
}
