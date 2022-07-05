package gliphic.android.operation.alerts;

import gliphic.android.operation.Alerts;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;

import libraries.Vars;

import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class IsActionableGroupShareTestParameterized {
    private Vars.GroupShareStatus groupShareStatus;

    public IsActionableGroupShareTestParameterized(Vars.GroupShareStatus groupShareStatus) {
        this.groupShareStatus = groupShareStatus;
    }

    @Parameterized.Parameters
    public static List<Object[]> testCases() {
        return Arrays.asList(new Object[][] {
                {Vars.GroupShareStatus.PENDING_RECEIVED},
                {Vars.GroupShareStatus.PENDING_SENT},
                {Vars.GroupShareStatus.SUCCESS_RECEIVED},
                {Vars.GroupShareStatus.SUCCESS_SENT},
                {Vars.GroupShareStatus.FAILED_RECEIVED},
                {Vars.GroupShareStatus.FAILED_SENT},
                null
        });
    }

    @Test
    public void isActionableGroupShare() {
        final boolean isActionableGroupShare;

        try {
            isActionableGroupShare = Alerts.isActionableGroupShare(groupShareStatus);
        }
        catch (NullPointerException e) {
            if (groupShareStatus == null) {
                return;
            }
            else {
                throw e;
            }
        }

        if (groupShareStatus.equals(Vars.GroupShareStatus.PENDING_RECEIVED)) {
            assertThat(isActionableGroupShare, is(true));
        }
        else {
            assertThat(isActionableGroupShare, is(false));
        }
    }
}
