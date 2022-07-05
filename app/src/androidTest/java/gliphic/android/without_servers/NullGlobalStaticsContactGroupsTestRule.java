package gliphic.android.without_servers;

import gliphic.android.display.abstract_views.BaseActivity;
import gliphic.android.display.main.contact_details.ContactDetailsActivity;
import gliphic.android.operation.Contact;
import gliphic.android.operation.TempGlobalStatics;
import gliphic.android.utils.AndroidTestUtils;
import gliphic.android.utils.MainActivityBaseSetup;

import gliphic.android.utils.NullGlobalStaticsTestRule;
import libraries.Vars;

import static org.junit.Assert.fail;

/**
 *
 */
public class NullGlobalStaticsContactGroupsTestRule<A extends BaseActivity> extends NullGlobalStaticsTestRule<A> {

    public NullGlobalStaticsContactGroupsTestRule(Class<A> activityClass) {
        super(activityClass);
    }

    @Override
    protected void before() {
        super.before();

        try {
            final long number = 1;
            TempGlobalStatics.setContactClicked(
                    new Contact(
                            number,
                            AndroidTestUtils.generateContactId(number),
                            "Some valid contact name",
                            Vars.DisplayPicture.getRandom().get(),
                            Vars.ContactType.KNOWN,
                            true
                    )
            );
        }
        catch (Exception e) {
            fail(e.getMessage());
        }

        MainActivityBaseSetup.mainActivitySetupAfterActivityLaunched(
                getScenario(),
                ContactDetailsActivity.class,
                false,
                true
        );
    }
}
