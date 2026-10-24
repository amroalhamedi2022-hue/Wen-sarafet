package app.wensarafet.tracker;

import java.util.Calendar;
import java.util.TimeZone;

/** A quote may be fetched once per local day when the FX form is opened.
 * No scheduled network access, and no automatic application of a quoted rate.
 */
public final class FxRateRefreshPolicy {
    private FxRateRefreshPolicy() {}

    public static boolean needsDailyRefresh(long lastSuccessfulFetch, long now, TimeZone zone) {
        if(now <= 0 || zone == null)throw new IllegalArgumentException("invalid clock");
        if(lastSuccessfulFetch<=0 || lastSuccessfulFetch>now)return true;
        Calendar last=Calendar.getInstance(zone);
        last.setTimeInMillis(lastSuccessfulFetch);
        Calendar current=Calendar.getInstance(zone);
        current.setTimeInMillis(now);
        return last.get(Calendar.YEAR)!=current.get(Calendar.YEAR)
                || last.get(Calendar.DAY_OF_YEAR)!=current.get(Calendar.DAY_OF_YEAR);
    }
}
