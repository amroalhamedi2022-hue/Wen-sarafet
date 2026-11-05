package app.wensarafet.tracker;

import java.util.List;

public final class CategoryCheckpointRulesTest {
    private static void check(boolean yes,String message) {
        if(!yes)throw new AssertionError(message);
    }
    private static void rejects(Runnable fn,String reason) {
        try {
            fn.run();
            throw new AssertionError("Expected rejection: "+reason);
        }catch(IllegalArgumentException expected){}
    }
    public static void main(String[] args) {
        check(CategoryCheckpointRules.decode("").isEmpty(),"fresh category has no checkpoints");
        check(CategoryCheckpointRules.decode(null).isEmpty(),"null legacy store");
        String current=CategoryCheckpointRules.append("",1000);
        check("1000".equals(current),"initial checkpoint");
        current=CategoryCheckpointRules.append(current,2000);
        current=CategoryCheckpointRules.append(current,3000);
        List<Long> times=CategoryCheckpointRules.decode(current);
        check(times.equals(List.of(1000L,2000L,3000L)),"ordered timestamps");
        check(CategoryCheckpointRules.periodStart(times,0)==0,"oldest includes everything");
        check(CategoryCheckpointRules.periodEnd(times,0,5000)==1000,"oldest stops at boundary");
        check(CategoryCheckpointRules.periodStart(times,1)==1000,"next interval includes boundary");
        check(CategoryCheckpointRules.periodEnd(times,1,5000)==2000,"middle interval");
        check(CategoryCheckpointRules.periodStart(times,2)==2000,"third interval");
        check(CategoryCheckpointRules.periodEnd(times,2,5000)==3000,"third end");
        check(CategoryCheckpointRules.periodStart(times,3)==3000,"active interval starts at checkpoint");
        check(CategoryCheckpointRules.periodEnd(times,3,5000)==5000,"active interval stops now");
        // Values on boundary must fall in ONE and only one period.
        long[] movements={100L,999L,1000L,1500L,2000L,2999L,3000L,3999L};
        int[] totals=new int[4];
        for(long movement:movements) {
            int matches=0;
            for(int i=0;i<4;i++)if(movement>=CategoryCheckpointRules.periodStart(times,i)
                    && movement<CategoryCheckpointRules.periodEnd(times,i,5000)) {
                totals[i]++;matches++;
            }
            check(matches==1,"missing or duplicated transaction at "+movement);
        }
        check(totals[0]==2 && totals[1]==2 && totals[2]==2 && totals[3]==2,
                "partition matches without loss or duplication");
        check("1000,2000".equals(CategoryCheckpointRules.removeLast(current)),"undo last");
        check("".equals(CategoryCheckpointRules.removeLast("1000")),"undo first");
        check("".equals(CategoryCheckpointRules.removeLast("")),"empty undo safe");
        rejects(()->CategoryCheckpointRules.decode("1000,1000"),"duplicate");
        rejects(()->CategoryCheckpointRules.decode("2000,1000"),"unsorted");
        rejects(()->CategoryCheckpointRules.decode("-2"),"negative");
        rejects(()->CategoryCheckpointRules.decode("1000,"),"empty timestamp");
        rejects(()->CategoryCheckpointRules.decode("abc"),"nonnumeric");
        rejects(()->CategoryCheckpointRules.append("1000",1000),"same timestamp");
        rejects(()->CategoryCheckpointRules.append("1000",999),"older clock");
        rejects(()->CategoryCheckpointRules.append("",0),"epoch start");
        rejects(()->CategoryCheckpointRules.periodStart(times,-1),"index negative");
        rejects(()->CategoryCheckpointRules.periodEnd(times,4,0),"invalid end");
        String max="";
        for(int i=1;i<=CategoryCheckpointRules.MAX_CHECKPOINTS;i++)
            max=CategoryCheckpointRules.append(max,1000L*i);
        final String limit=max;
        rejects(()->CategoryCheckpointRules.append(limit,25000),"too many checkpoints");
        System.out.println("CategoryCheckpointRulesTest PASS: dates, archives, boundary inclusivity, undo, invalid inputs, limit");
    }
}
