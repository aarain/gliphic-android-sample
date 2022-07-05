package gliphic.android.without_servers;

import gliphic.android.display.abstract_views.BaseActivity;
import gliphic.android.display.main.group_details.GroupDetailsActivity;
import gliphic.android.operation.Group;
import gliphic.android.operation.TempGlobalStatics;
import gliphic.android.utils.AndroidTestUtils;
import gliphic.android.utils.MainActivityBaseSetup;

import gliphic.android.utils.NullGlobalStaticsTestRule;
import libraries.Vars;

import static org.junit.Assert.fail;

/**
 *
 */
public class NullGlobalStaticsGroupContactsTestRule<A extends BaseActivity> extends NullGlobalStaticsTestRule<A> {

    public NullGlobalStaticsGroupContactsTestRule(Class<A> activityClass) {
        super(activityClass);
    }

    @Override
    protected void before() {
        super.before();

        try {
            TempGlobalStatics.setGroupClicked(
                    new Group(
                            1,
                            Vars.DisplayPicture.getRandom().get(),
                            "Some valid group name",
                            "Some valid group description.",
                            AndroidTestUtils.generateGroupId(1),
                            AndroidTestUtils.VALID_PERMISSIONS,
                            false,
                            false,
                            true
                    )
            );
        }
        catch (Exception e) {
            fail(e.getMessage());
        }

        MainActivityBaseSetup.mainActivitySetupAfterActivityLaunched(
                getScenario(),
                GroupDetailsActivity.class,
                false,
                true
        );
    }
}
