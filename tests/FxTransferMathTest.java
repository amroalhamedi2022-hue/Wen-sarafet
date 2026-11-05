import app.wensarafet.tracker.FxTransferMath;

public final class FxTransferMathTest {
    public static void main(String[] args) {
        expect(3550,FxTransferMath.destinationMinor(1000,"3.55"),"USD 10 -> ILS 35.50");
        expect(71,FxTransferMath.destinationMinor(20,"3.55"),"round 0.20 USD");
        expect(4,FxTransferMath.destinationMinor(1,"3.55"),"round 0.01 USD");
        long[] values=FxTransferMath.balancesAfter(10000,500,2000,7100);
        expect(8000,values[0],"source deducted once");
        expect(7600,values[1],"destination credited once");
        invalid(() -> FxTransferMath.destinationMinor(100,"0"));
        invalid(() -> FxTransferMath.destinationMinor(100,"oops"));
        invalid(() -> FxTransferMath.destinationMinor(0,"3.50"));
        invalid(() -> FxTransferMath.destinationMinor(Long.MAX_VALUE,"123.2"));
        System.out.println("FxTransferMathTest: all checks passed");
    }
    static void expect(long expected,long actual,String label){if(expected!=actual)throw new AssertionError(label);}
    static void invalid(Runnable fn){try{fn.run();throw new AssertionError("accepted invalid FX");}catch(IllegalArgumentException ok){}}
}
