import app.wensarafet.tracker.FinanceRules;
import app.wensarafet.tracker.TransactionSearchRules;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** CI probe: export queries from real Java production builder for Python/SQLite execution. */
public final class TransactionSearchDump {
    private static String b64(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }
    static void sample(String label,String text,String type,String currency,int limit,int offset) {
        TransactionSearchRules.Query q=TransactionSearchRules.build(text,type,currency,limit,offset);
        StringBuilder line=new StringBuilder(label).append('|').append(b64(q.sql));
        for(String arg:q.args)line.append('|').append(b64(arg));
        System.out.println(line);
    }
    public static void main(String[] unused) {
        sample("all","",null,null,4,0);
        sample("merchant","مخبز",FinanceRules.EXPENSE,"ILS",4,0);
        sample("percent","%",null,null,4,0);
        sample("amount","50.00",null,null,4,0);
        sample("page","",null,null,2,1);
        sample("incomeUSD","",FinanceRules.INCOME,"USD",4,0);
        sample("injection","%' OR 1=1 --",null,null,4,0);
    }
}
