import app.wensarafet.tracker.BopIndicativeRate;
public class BopIndicativeRateTest {
    static void ok(boolean v){if(!v)throw new AssertionError("Bank rate check");}
    static void reject(String value){try{BopIndicativeRate.usdIlsBuyRate(value);throw new AssertionError("accepted unsafe quote");}catch(IllegalArgumentException expected){}}
    public static void main(String[]args){
        String markup="<table><tr><th>العملة</th><th>شراء</th><th>بيع</th></tr><tr><td>دولار أمريكي / شيكل</td><td>3.0100</td><td>3.0500</td></tr></table>";
        ok(BopIndicativeRate.usdIlsBuyRate(markup).equals("3.0100"));
        reject("<h1>أحدث سعر</h1>3.0100 3.0500");
        reject("دولار أمريكي / شيكل 3.0800 3.0200");
        reject("USD / ILS 123.4500 99.9900");
        System.out.println("BopIndicativeRateTest passed");
    }
}
