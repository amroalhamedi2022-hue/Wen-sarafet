import app.wensarafet.tracker.FxRateRefreshPolicy;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class FxRateRefreshPolicyTest {
    static void check(boolean valid) { if(!valid) throw new AssertionError("FX daily fetch policy"); }
    static long at(TimeZone tz,int year,int month,int day,int hour,int minute) {
        GregorianCalendar c=new GregorianCalendar(tz);c.clear();c.set(year,month-1,day,hour,minute);return c.getTimeInMillis();
    }
    public static void main(String[] args) {
        TimeZone tz=TimeZone.getTimeZone("Asia/Hebron");
        long now=at(tz,2026,10,25,12,15);
        check(FxRateRefreshPolicy.needsDailyRefresh(0,now,tz));
        check(!FxRateRefreshPolicy.needsDailyRefresh(at(tz,2026,10,25,7,0),now,tz));
        check(FxRateRefreshPolicy.needsDailyRefresh(at(tz,2026,10,24,23,59),now,tz));
        check(FxRateRefreshPolicy.needsDailyRefresh(now+3600_000,now,tz));
        check(FxRateRefreshPolicy.needsDailyRefresh(at(tz,2025,10,25,7,0),now,tz));
        // Local day change even across UTC date boundaries.
        TimeZone other=TimeZone.getTimeZone("Pacific/Auckland");
        check(FxRateRefreshPolicy.needsDailyRefresh(at(other,2026,4,4,23,59),at(other,2026,4,5,0,1),other));
        System.out.println("FxRateRefreshPolicyTest passed");
    }
}
