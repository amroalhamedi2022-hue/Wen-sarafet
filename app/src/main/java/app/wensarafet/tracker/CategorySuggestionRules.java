package app.wensarafet.tracker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Local-only suggestions from completed, user-confirmed transactions. No automatic posting. */
public final class CategorySuggestionRules {
    private CategorySuggestionRules() {}

    public static final class Example {
        public final long categoryId, accountId, amountMinor;
        public final String description, merchant;
        public Example(long categoryId, long accountId, long amountMinor, String description, String merchant) {
            this.categoryId=categoryId; this.accountId=accountId; this.amountMinor=amountMinor;
            this.description=description; this.merchant=merchant;
        }
    }

    public static final class Suggestion {
        public final long categoryId;
        public final String reason;
        public final int matchingExamples;
        private Suggestion(long id, String why, int matches) {
            categoryId=id; reason=why; matchingExamples=matches;
        }
    }

    public static String normalize(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT).replaceAll("[\u064b-\u065f\u0670]", "")
                .replace('أ','ا').replace('إ','ا').replace('آ','ا').replace('ى','ي')
                .replace('ة','ه').replaceAll("[^\\p{L}\\p{N}]+", " ").trim()
                .replaceAll("\\s+", " ");
    }

    /** Merchant name is optional; manually entered note is a valid description. */
    public static Suggestion suggest(long accountId, long amountMinor, String description,
                                     String merchant, List<Example> history) {
        if (amountMinor <= 0 || history == null || history.isEmpty()) return null;
        String desc=normalize(description), vendor=normalize(merchant);
        Map<Long, Integer> contextScore=new HashMap<>(), contextCount=new HashMap<>();
        Map<Long, Integer> amountCount=new HashMap<>();
        int allAmountCount=0;
        for (Example old:history) {
            if (old == null || old.categoryId <= 0 || old.amountMinor <= 0) continue;
            // Amount pattern is account-specific: 2 ILS of transport on cash does not
            // classify every 2 ILS transaction on the bank account as transport.
            boolean sameAccount=old.accountId == accountId;
            boolean sameAmount=old.amountMinor == amountMinor;
            if (sameAccount && sameAmount) {
                amountCount.merge(old.categoryId, 1, Integer::sum);
                allAmountCount++;
            }
            boolean descMatch=!desc.isEmpty() && desc.length() >= 3 && desc.equals(normalize(old.description));
            boolean merchantMatch=!vendor.isEmpty() && vendor.length() >= 3 && vendor.equals(normalize(old.merchant));
            if (!descMatch && !merchantMatch) continue;
            int score = (descMatch ? 5:0)+(merchantMatch?8:0)+(sameAccount?3:0)+(sameAmount?2:0);
            contextScore.merge(old.categoryId,score,Integer::sum);
            contextCount.merge(old.categoryId,1,Integer::sum);
        }
        Suggestion contextual=topUnique(contextScore,contextCount,"بحسب المحل أو الوصف السابق",5);
        if (contextual != null) return contextual;
        // Weak evidence must be repeated and consistent; never guess on just one 2-ILS expense.
        if (allAmountCount<3) return null;
        long bestId=0; int best=0, second=0;
        for (Map.Entry<Long,Integer> entry:amountCount.entrySet()) {
            int count=entry.getValue();
            if (count>best) {second=best;best=count;bestId=entry.getKey();}
            else if (count>second) second=count;
        }
        return best >= 3 && best > second && best*4 >= allAmountCount*3
                ? new Suggestion(bestId,"غالبًا حسب المبلغ والحساب ("+best+" مرات)",best) : null;
    }

    private static Suggestion topUnique(Map<Long,Integer> scores,Map<Long,Integer> counts,String reason,int minimum) {
        long bestId=0; int best=0,runner=0;
        for (Map.Entry<Long,Integer> e:scores.entrySet()) {
            int n=e.getValue();
            if(n>best){runner=best;best=n;bestId=e.getKey();}
            else if(n>runner)runner=n;
        }
        return best>=minimum && best>runner*1.5 && bestId>0
                ? new Suggestion(bestId,reason,counts.get(bestId)):null;
    }
}
