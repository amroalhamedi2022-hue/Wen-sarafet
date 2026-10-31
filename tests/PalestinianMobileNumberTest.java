import app.wensarafet.tracker.PalestinianMobileNumber;

public final class PalestinianMobileNumberTest {
    private static void same(String raw,String expected) {
        String actual=PalestinianMobileNumber.normalize(raw);
        if(!expected.equals(actual))throw new AssertionError(raw+" => "+actual+" expected "+expected);
        if(!actual.substring(2).equals(PalestinianMobileNumber.dialTail(raw)))
            throw new AssertionError("USSD tail must be 8 digits: "+raw);
    }
    private static void reject(String raw) {
        if(PalestinianMobileNumber.normalize(raw)!=null ||
                PalestinianMobileNumber.dialTail(raw)!=null)
            throw new AssertionError("Unsafe/foreign contact accepted: "+raw);
    }
    public static void main(String[] args) {
        same("+972590000000","0590000000");
        same("+970590000000","0590000000");
        same("00972590000000","0590000000");
        same("00970590000000","0590000000");
        same("972590000000","0590000000");
        same("970590000000","0590000000");
        same("0590000000","0590000000");
        same("590000000","0590000000");
        same("+970 59-000-0000","0590000000");
        same("+972 (59) 000 0000","0590000000");
        same("٠٥٩٠٠٠٠٠٠٠","0590000000");
        same("۰۵۹۰۰۰۰۰۰۰","0590000000");
        same("0561234567","0561234567");
        same("+972561234567","0561234567");
        same("00970561234567","0561234567");
        reject("+1 2125550123");
        reject("0581234567");
        reject("0521234567");
        reject("972");
        reject("+970");
        same("009720590000000","0590000000"); // contacts may retain the trunk zero after 00972
        reject("5900000");
        reject("05900000000");
        reject("*110*123#");
        reject("0590000000 ext 123");
        reject("0590000000;DROP");
        reject("");
        reject(null);
        System.out.println("PalestinianMobileNumberTest PASS: local/international variants, unicode, invalid and USSD injection");
    }
}
