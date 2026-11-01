package app.wensarafet.tracker;

import java.util.ArrayList;
import java.util.List;

/**
 * Parameterized local history search; no network or external indexing.
 * Page through all records without constructing thousands of Android views.
 */
public final class TransactionSearchRules {
    public static final int MAX_PAGE_SIZE = 100;
    private TransactionSearchRules() {}

    public static final class Query {
        public final String sql;
        public final String[] args;
        private Query(String statement, List<String> parameters) {
            this.sql = statement;
            this.args = parameters.toArray(new String[0]);
        }
    }

    public static Query build(String searchText, String type, String currency, int pageSize, int offset) {
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE || offset < 0)
            throw new IllegalArgumentException("عدد نتائج البحث غير صالح");
        if (type != null && !type.isEmpty() &&
                !FinanceRules.EXPENSE.equals(type) && !FinanceRules.INCOME.equals(type)
                        && !FinanceRules.TRANSFER.equals(type))
            throw new IllegalArgumentException("نوع العملية غير صحيح");
        if (currency != null && !currency.isEmpty() &&
                !"ILS".equals(currency) && !"USD".equals(currency) && !"JOD".equals(currency))
            throw new IllegalArgumentException("العملة غير مدعومة");

        String search = searchText == null ? "" : searchText.trim();
        if (search.codePointCount(0, search.length()) > 100)
            throw new IllegalArgumentException("البحث طويل جدًا");
        StringBuilder sql = new StringBuilder(
                "SELECT t.id,t.type,t.amount_minor,t.currency,t.account_id,t.destination_account_id," +
                "a.name,d.name,c.name,c.emoji,t.note,t.raw_text,t.needs_review,t.created_at," +
                "EXISTS(SELECT 1 FROM transaction_splits s WHERE s.transaction_id=t.id),t.affects_balance " +
                "FROM transactions t JOIN accounts a ON a.id=t.account_id " +
                "LEFT JOIN accounts d ON d.id=t.destination_account_id " +
                "LEFT JOIN categories c ON c.id=t.category_id WHERE 1=1");
        List<String> args = new ArrayList<>();
        if (type != null && !type.isEmpty()) {
            sql.append(" AND t.type=?");
            args.add(type);
        }
        if (currency != null && !currency.isEmpty()) {
            sql.append(" AND t.currency=?");
            args.add(currency);
        }
        if (!search.isEmpty()) {
            String pattern = literalContains(search);
            sql.append(" AND (t.note LIKE ? ESCAPE '!' OR t.raw_text LIKE ? ESCAPE '!' " +
                    "OR a.name LIKE ? ESCAPE '!' OR d.name LIKE ? ESCAPE '!' " +
                    "OR c.name LIKE ? ESCAPE '!'");
            for (int i = 0; i < 5; i++) args.add(pattern);
            Long amount = Money.parseMinor(search);
            if (amount != null && amount > 0) {
                sql.append(" OR t.amount_minor=?");
                args.add(Long.toString(amount));
            }
            sql.append(")");
        }
        sql.append(" ORDER BY t.created_at DESC,t.id DESC LIMIT ? OFFSET ?");
        args.add(Integer.toString(pageSize));
        args.add(Integer.toString(offset));
        return new Query(sql.toString(), args);
    }

    /** Quotes user wildcards so '%' and '_' are literal characters, not broad scans. */
    public static String literalContains(String text) {
        StringBuilder out = new StringBuilder("%");
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '!' || ch == '%' || ch == '_') out.append('!');
            out.append(ch);
        }
        return out.append('%').toString();
    }
}
