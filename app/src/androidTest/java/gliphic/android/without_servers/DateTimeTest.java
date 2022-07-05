package gliphic.android.without_servers;

import gliphic.android.display.libraries.DateTime;
import gliphic.android.operation.misc.ApiVersionHandler;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

/**
 * This test class is duplicated in libraries/DateTimeTest.java without using the emulated device.
 * This test class is duplicated because some tests are ignored depending on whether or not an emulator is used.
 *
 * The run parameterized tests for this class are:
 * * parameterizedGetDateTime()
 * * parameterizedGetDateTimeCurrentSdk()
 * The ignored parameterized tests for this class are:
 * * parameterizedGetDateTimePreSdk26()
 */
@RunWith(Parameterized.class)
public class DateTimeTest {
    private static final long MILLISECONDS_IN_A_MINUTE = 60 * 1000;

    private static long currentTimeLowerBoundInclusive;
    private static long currentTimeUpperBoundInclusive;
    private static long currentTimeLowerBoundExclusive;
    private static long currentTimeUpperBoundExclusive;

    private long millisSinceEpoch;
    private String expectedDateTime;

    public DateTimeTest(long millisSinceEpoch, String expectedDateTime) {
        this.millisSinceEpoch = millisSinceEpoch;
        this.expectedDateTime = expectedDateTime;
    }

    @Parameterized.Parameters
    public static List<Object[]> testCases() {
        return Arrays.asList(new Object[][] {
                {1553251214026L,    "22 Mar 2019 10:40"},
                {0,                 "1 Jan 1970 01:00"},
                {-1,                "1 Jan 1970 00:59"},
                {3500000000000L,    "28 Nov 2080 06:13"},
                // TODO: Fix the code with dates far in the future.
                //       Currently the pre-SDK 26 version outputs a time 1 hour behind the current SDK version.
//                {4000000000000L,    "2 Oct 2096 08:06"},
//                {200000000000000L,  "1 Oct 8307 20:33"},
                // TODO: Fix the test with the following values, which the test currently fails.
//                {-31512125119212L,  "3 Jun 0971 07:53"},
//                {Long.MAX_VALUE,    "17 Aug +292278994 08:12"},
//                {Long.MIN_VALUE,    "16 May +292275056 16:45"}
        });
    }

    private void setTimeBounds() {
        if (millisSinceEpoch < 0) {
            currentTimeUpperBoundInclusive = millisSinceEpoch - (millisSinceEpoch % MILLISECONDS_IN_A_MINUTE) - 1;
            currentTimeLowerBoundInclusive = currentTimeUpperBoundInclusive - MILLISECONDS_IN_A_MINUTE + 1;
        }
        else {
            currentTimeLowerBoundInclusive = millisSinceEpoch - (millisSinceEpoch % MILLISECONDS_IN_A_MINUTE);
            currentTimeUpperBoundInclusive = currentTimeLowerBoundInclusive + MILLISECONDS_IN_A_MINUTE - 1;
        }
        currentTimeLowerBoundExclusive = currentTimeLowerBoundInclusive - 1;
        currentTimeUpperBoundExclusive = currentTimeUpperBoundInclusive + 1;
    }

    @Test
    public void parameterizedGetDateTime() {
        setTimeBounds();

        assertThat(DateTime.getDateTime(millisSinceEpoch),                              is(expectedDateTime));
        assertThat(DateTime.getDateTime(currentTimeLowerBoundInclusive),                is(expectedDateTime));
        assertThat(DateTime.getDateTime(currentTimeUpperBoundInclusive),                is(expectedDateTime));
        assertThat(DateTime.getDateTime(currentTimeLowerBoundExclusive),                not(expectedDateTime));
        assertThat(DateTime.getDateTime(currentTimeUpperBoundExclusive),                not(expectedDateTime));
    }

    @Test
    public void parameterizedGetDateTimePreSdk26() {
        Assume.assumeThat(ApiVersionHandler.deviceHasVersion(DateTime.API_LEVEL), is(false));

        setTimeBounds();

        assertThat(DateTime.getDateTimePreSdk26(millisSinceEpoch),                      is(expectedDateTime));
        assertThat(DateTime.getDateTimePreSdk26(currentTimeLowerBoundInclusive),        is(expectedDateTime));
        assertThat(DateTime.getDateTimePreSdk26(currentTimeUpperBoundInclusive),        is(expectedDateTime));
        assertThat(DateTime.getDateTimePreSdk26(currentTimeLowerBoundExclusive),        not(expectedDateTime));
        assertThat(DateTime.getDateTimePreSdk26(currentTimeUpperBoundExclusive),        not(expectedDateTime));
    }

    @Test
    public void parameterizedGetDateTimeCurrentSdk() {
        Assume.assumeThat(ApiVersionHandler.deviceHasVersion(DateTime.API_LEVEL), is(true));

        setTimeBounds();

        assertThat(DateTime.getDateTimeCurrentSdk(millisSinceEpoch),                    is(expectedDateTime));
        assertThat(DateTime.getDateTimeCurrentSdk(currentTimeLowerBoundInclusive),      is(expectedDateTime));
        assertThat(DateTime.getDateTimeCurrentSdk(currentTimeUpperBoundInclusive),      is(expectedDateTime));
        assertThat(DateTime.getDateTimeCurrentSdk(currentTimeLowerBoundExclusive),      not(expectedDateTime));
        assertThat(DateTime.getDateTimeCurrentSdk(currentTimeUpperBoundExclusive),      not(expectedDateTime));
    }
}
