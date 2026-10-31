import app.wensarafet.tracker.Money;
public final class MoneyParseSafetyTest {
    private static void eq(long want,String raw) {
        Long value=Money.parseMinor(raw);
        if(value==null || value!=want)throw new AssertionError(raw+": "+value+" expected "+want);
    }
    private static void bad(String raw) {
        if(Money.parseMinor(raw)!=null)throw new AssertionError("accepted ambiguous/invalid amount: "+raw);
    }
    public static void main(String[] args) {
        eq(1000,"10");
        eq(1350,"13,50");
        eq(4920,"49.2");
        eq(117569,"1175.69");
        eq(100000,"1,000"); // This MUST NOT become 1.00.
        eq(123456,"1,234.56");
        eq(123456,"1.234,56");
        eq(100000000,"1,000,000");
        eq(100000000,"1.000.000");
        eq(234567,"٢٬٣٤٥٫٦٧");
        eq(1250,"١٢٫٥٠");
        eq(1500,"۱۵٫۰۰");
        eq(2000,"20,00");
        eq(-120000,"−1,200");
        eq(1,"0.01");
        eq(0,"0");
        bad("12.345"); // No rounding to 12.35 when the user may mean 12,345.
        bad("1000,000");
        bad("1.2.3");
        bad("1,23,4");
        bad("1,234,56");
        bad("1,");
        bad(",50");
        bad("1.000"); // Ambiguous grouping vs 3 decimals.
        bad("25.999");
        bad("1e3");
        bad("12 34");
        bad("shekel 50");
        bad("");
        bad(null);
        bad("999999999999999999999999999");
        System.out.println("MoneyParseSafetyTest PASS: exact cents, Arabic digits, grouped thousands and ambiguous input rejected.");
    }
}
