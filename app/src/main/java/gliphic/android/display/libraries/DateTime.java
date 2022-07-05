/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.display.libraries;

import android.os.Build;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.GregorianCalendar;

import androidx.annotation.RequiresApi;
import gliphic.android.operation.misc.ApiVersionHandler;

public class DateTime {
    public static final int API_LEVEL = Build.VERSION_CODES.O;  // Required for specialised date-time libraries.

    private static final String DATE_TIME_FORMAT = "d MMM yyyy HH:mm";

    /**
     * Return a human-readable date-time representation of a given long.
     *
     * WARNING: For dates far in the future (after the year 2080), the pre-SDK 26 version outputs a time 1 hour behind
     * the current SDK version. See the tests for more details. TODO: Fix this warning.
     *
     * @param millisTimeSinceEpoch  The number of milliseconds since epoch.
     *                              The most common way to obtain this is via the System.currentTimeMillis() method.
     * @return                      A formatted string displaying the date and time represented by the input.
     */
    public static String getDateTime(long millisTimeSinceEpoch) {
        if (ApiVersionHandler.deviceHasVersion(API_LEVEL)) {
            return getDateTimeCurrentSdk(millisTimeSinceEpoch);
        }
        else {
            return getDateTimePreSdk26(millisTimeSinceEpoch);
        }
    }

    public static String getDateTimePreSdk26(long millisTimeSinceEpoch) {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(millisTimeSinceEpoch);

        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_TIME_FORMAT);

        return simpleDateFormat.format(calendar.getTime());
    }

    @RequiresApi(API_LEVEL)
    public static String getDateTimeCurrentSdk(long millisTimeSinceEpoch) {
        final ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(millisTimeSinceEpoch),
                ZoneId.systemDefault()
        );

        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);

        return zonedDateTime.format(dateTimeFormatter);
    }
}
