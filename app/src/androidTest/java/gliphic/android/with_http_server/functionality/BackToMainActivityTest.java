package gliphic.android.with_http_server.functionality;

import android.os.SystemClock;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;

import androidx.test.filters.LargeTest;
import gliphic.android.R;
import gliphic.android.utils.MainActivityTestRuleSetup;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

/**
 * Test opening an overflow menu item and pressing back.
 */
@RunWith(Parameterized.class)
@LargeTest
public class BackToMainActivityTest extends MainActivityTestRuleSetup {
    private int overflowStringId;

    public BackToMainActivityTest(int overflowStringId) {
        this.overflowStringId = overflowStringId;
    }

    @Parameterized.Parameters
    public static List<Integer> testCases() {
        return Arrays.asList(
                R.string.action_bar_group_select,
                R.string.action_bar_group_create,
                R.string.action_bar_contact_add,
                R.string.action_bar_contact_profile,
                R.string.action_bar_report
        );
    }

    @Test
    public void pressBackToMainActivity() {
        // Use this to check that navigating back preserves the text in the previous activity (because the main
        // activity is not killed).
        final String text = "someRandomText";
        onView(withId(R.id.edittext_encrypt)).perform(replaceText(text));

        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText(overflowStringId)).perform(click());
        SystemClock.sleep(500);
        pressBack();

        SystemClock.sleep(500);
        onView(withId(R.id.edittext_encrypt)).check(matches(withText(text)));
        onView(withId(R.id.edittext_encrypt)).perform(replaceText(""));     // Cleanup
    }
}
