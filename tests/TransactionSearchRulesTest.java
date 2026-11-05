import app.wensarafet.tracker.FinanceRules;
import app.wensarafet.tracker.TransactionSearchRules;

import java.util.Arrays;

public final class TransactionSearchRulesTest {
    static void check(boolean condition, String label) {
        if(!condition) throw new AssertionError(label);
    }
    static void reject(Runnable action, String label) {
        try { action.run(); } catch(IllegalArgumentException expected) { return; }
        throw new AssertionError(label);
    }
    public static void main(String[] args) {
        TransactionSearchRules.Query all=TransactionSearchRules.build("",null,null,76,0);
        check(all.args.length==2, "empty search should need only limit/offset");
        check(Arrays.equals(all.args,new String[]{"76","0"}),"empty search paging");
        check(all.sql.endsWith("ORDER BY t.created_at DESC,t.id DESC LIMIT ? OFFSET ?"),
                "stable chronological pages");
        check(!all.sql.contains(" LIKE "), "empty query must not have LIKE filters");

        TransactionSearchRules.Query match=TransactionSearchRules.build("  القدس  ",
                FinanceRules.EXPENSE,"ILS",51,120);
        check(match.sql.contains("AND t.type=? AND t.currency=?"),"type and currency filters");
        check(match.sql.contains("c.name LIKE ? ESCAPE '!'"),"category name search");
        check(match.sql.contains("t.raw_text LIKE ? ESCAPE '!'"),"original message search");
        check(match.sql.contains("a.name LIKE ? ESCAPE '!'"),"account name search");
        check(match.sql.contains("d.name LIKE ? ESCAPE '!'"),"destination name search");
        check(match.args.length==9,"five bound terms, two filters, two pagination args");
        check(match.args[0].equals("EXPENSE") && match.args[1].equals("ILS"),"filter args");
        check(match.args[2].equals("%القدس%"),"normalized bound query");
        check(match.args[7].equals("51") && match.args[8].equals("120"),"pagination args");

        TransactionSearchRules.Query amount=TransactionSearchRules.build("25.50",null,null,76,0);
        check(amount.sql.contains("OR t.amount_minor=?"),"find exact amount in smallest units");
        check(amount.args[5].equals("2550"),"amount conversion");
        check(TransactionSearchRules.literalContains("%_!").equals("%!%!_!!%"),
                "SQLite wildcard literal escaping");
        TransactionSearchRules.Query injection=TransactionSearchRules.build("%' OR 1=1 --",null,null,76,0);
        check(!injection.sql.contains("OR 1=1 --"),"never interpolate user search text");
        check(injection.args[0].contains("!%"),"literal percent escape");
        check(!injection.args[0].equals(injection.sql),"all data bound");

        reject(()->TransactionSearchRules.build("",null,null,101,0),"page limit");
        reject(()->TransactionSearchRules.build("",null,null,0,0),"zero page");
        reject(()->TransactionSearchRules.build("",null,null,76,-1),"negative offset");
        reject(()->TransactionSearchRules.build("x".repeat(101),null,null,76,0),"huge search");
        reject(()->TransactionSearchRules.build("","DEBT",null,76,0),"invalid type");
        reject(()->TransactionSearchRules.build("",null,"EUR",76,0),"invalid currency");
        System.out.println("TransactionSearchRulesTest passed: bound filters, paging, search and literal wildcard escaping");
    }
}
