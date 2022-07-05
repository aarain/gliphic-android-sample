package gliphic.android.utils;

import gliphic.android.display.abstract_views.BaseActivity;
import gliphic.android.operation.server_interaction.http_server.HttpOperations;

import static org.junit.Assert.fail;

/**
 *
 */
abstract public class NullGlobalStaticsTestRule<A extends BaseActivity> extends BaseActivityTestRule<A> {

    public NullGlobalStaticsTestRule(Class<A> activityClass) {
        super(activityClass);
    }

    @Override
    protected void before() {
        setScenario(getScenarioSupplier());

        try {
            HttpOperations.restSslSetup(null, null);

            AndroidTestUtils.clearStaticLists();
        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
