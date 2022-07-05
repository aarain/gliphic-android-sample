package gliphic.android.utils;

import org.junit.rules.ExternalResource;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import gliphic.android.display.main.MainActivity;
import gliphic.android.display.abstract_views.BaseActivity;

import static androidx.test.internal.util.Checks.checkNotNull;
import static org.junit.Assert.fail;

/**
 * Copy the format used in {@link androidx.test.ext.junit.rules.ActivityScenarioRule}.
 */
public class BaseActivityTestRule<A extends BaseActivity> extends ExternalResource {

    interface Supplier<T> {
        T get();
    }

    private final Supplier<ActivityScenario<A>> scenarioSupplier;
    private ActivityScenario<A> scenario;

    /**
     * @see ActivityScenarioRule#ActivityScenarioRule(Class)
     */
    public BaseActivityTestRule(Class<A> activityClass) {
        scenarioSupplier = () -> ActivityScenario.launch(checkNotNull(activityClass));
    }

    public ActivityScenario<A> getScenarioSupplier() {
        return scenarioSupplier.get();
    }

    /**
     * @see ActivityScenarioRule#getScenario()
     */
    public ActivityScenario<A> getScenario() {
        return checkNotNull(scenario);
    }

    public void setScenario(ActivityScenario<A> scenario) {
        this.scenario = scenario;
    }

    @Override
    protected void before() {
        scenario = scenarioSupplier.get();

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
    }

    @Override
    protected void after() {
        scenario.close();
    }
}
