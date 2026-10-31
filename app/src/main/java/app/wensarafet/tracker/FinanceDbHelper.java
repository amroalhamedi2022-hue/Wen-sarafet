package app.wensarafet.tracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

public final class FinanceDbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "wen_sarafet.db";
    static final int DB_VERSION = 13; // Shared with backup preflight; reject newer schema before replacement.
    private final Context appContext;

    public FinanceDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        appContext = context.getApplicationContext();
    }

    @Override public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE accounts (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, currency TEXT NOT NULL, balance_minor INTEGER NOT NULL DEFAULT 0, built_in INTEGER NOT NULL DEFAULT 1, position INTEGER NOT NULL DEFAULT 0, active INTEGER NOT NULL DEFAULT 1)");
        db.execSQL("CREATE TABLE categories (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, kind TEXT NOT NULL, emoji TEXT NOT NULL, position INTEGER NOT NULL DEFAULT 0, exclude_from_expense INTEGER NOT NULL DEFAULT 0, archived INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT NOT NULL, amount_minor INTEGER NOT NULL, currency TEXT NOT NULL, account_id INTEGER NOT NULL, destination_account_id INTEGER, category_id INTEGER, note TEXT NOT NULL DEFAULT '', raw_text TEXT, needs_review INTEGER NOT NULL DEFAULT 0, created_at INTEGER NOT NULL, source TEXT NOT NULL DEFAULT 'MANUAL', affects_balance INTEGER NOT NULL DEFAULT 1, base_affects_balance INTEGER NOT NULL DEFAULT 1, special_kind TEXT, debt_person_id INTEGER, debt_direction TEXT, destination_amount_minor INTEGER, exchange_rate TEXT, exchange_rate_source TEXT, exchange_rate_checked_at INTEGER, FOREIGN KEY(account_id) REFERENCES accounts(id), FOREIGN KEY(destination_account_id) REFERENCES accounts(id), FOREIGN KEY(category_id) REFERENCES categories(id))");
        db.execSQL("CREATE TABLE transaction_splits (id INTEGER PRIMARY KEY AUTOINCREMENT, transaction_id INTEGER NOT NULL, category_id INTEGER, amount_minor INTEGER NOT NULL, special_kind TEXT, debt_person_id INTEGER, debt_direction TEXT, " +
                "CHECK ((special_kind IS NULL AND category_id IS NOT NULL AND debt_person_id IS NULL AND debt_direction IS NULL) OR " +
                "(category_id IS NULL AND debt_person_id IS NOT NULL AND ((special_kind='DEBT' AND debt_direction='RECEIVABLE') OR (special_kind='DEBT_PAYMENT' AND debt_direction='PAYABLE')))), " +
                "FOREIGN KEY(transaction_id) REFERENCES transactions(id) ON DELETE CASCADE, FOREIGN KEY(category_id) REFERENCES categories(id), FOREIGN KEY(debt_person_id) REFERENCES debt_people(id))");
        db.execSQL("CREATE INDEX idx_tx_account ON transactions(account_id)");
        db.execSQL("CREATE INDEX idx_tx_created ON transactions(created_at)");
        createCaptureSchema(db);
        createDebtSchema(db);
        seed(db);
        createAuditSchema(db);
        createSafeUndoSchema(db);
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE accounts ADD COLUMN active INTEGER NOT NULL DEFAULT 1");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN raw_text TEXT");
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN needs_review INTEGER NOT NULL DEFAULT 0");
            createCaptureSchema(db);
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE categories ADD COLUMN exclude_from_expense INTEGER NOT NULL DEFAULT 0");
            addBrotherPaymentCategory(db);
        }
        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN affects_balance INTEGER NOT NULL DEFAULT 1");
        }
        if (oldVersion < 7) {
            reconcileExcludedExpenseBalances(db);
        }
        if (oldVersion < 8) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN base_affects_balance INTEGER NOT NULL DEFAULT 1");
            db.execSQL("UPDATE transactions SET base_affects_balance=affects_balance");
            db.execSQL("ALTER TABLE transactions ADD COLUMN special_kind TEXT");
            db.execSQL("ALTER TABLE transactions ADD COLUMN debt_person_id INTEGER");
            db.execSQL("ALTER TABLE transactions ADD COLUMN debt_direction TEXT");
            createDebtSchema(db);
        }
        if (oldVersion < 9) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN destination_amount_minor INTEGER");
            db.execSQL("ALTER TABLE transactions ADD COLUMN exchange_rate TEXT");
            db.execSQL("ALTER TABLE transactions ADD COLUMN exchange_rate_source TEXT");
            db.execSQL("ALTER TABLE transactions ADD COLUMN exchange_rate_checked_at INTEGER");
        }
        if (oldVersion < 10) {
            db.execSQL("ALTER TABLE categories ADD COLUMN archived INTEGER NOT NULL DEFAULT 0");
            createAuditSchema(db);
        }
        if (oldVersion < 11) {
            // Rebuild the child table to safely make category_id nullable. Keep original IDs and amounts.
            db.execSQL("CREATE TABLE transaction_splits_v11 (id INTEGER PRIMARY KEY AUTOINCREMENT, transaction_id INTEGER NOT NULL, category_id INTEGER, amount_minor INTEGER NOT NULL, special_kind TEXT, debt_person_id INTEGER, debt_direction TEXT, " +
                    "CHECK ((special_kind IS NULL AND category_id IS NOT NULL AND debt_person_id IS NULL AND debt_direction IS NULL) OR " +
                    "(category_id IS NULL AND debt_person_id IS NOT NULL AND ((special_kind='DEBT' AND debt_direction='RECEIVABLE') OR (special_kind='DEBT_PAYMENT' AND debt_direction='PAYABLE')))), " +
                    "FOREIGN KEY(transaction_id) REFERENCES transactions(id) ON DELETE CASCADE, FOREIGN KEY(category_id) REFERENCES categories(id), FOREIGN KEY(debt_person_id) REFERENCES debt_people(id))");
            db.execSQL("INSERT INTO transaction_splits_v11(id,transaction_id,category_id,amount_minor) SELECT id,transaction_id,category_id,amount_minor FROM transaction_splits");
            db.execSQL("DROP TABLE transaction_splits");
            db.execSQL("ALTER TABLE transaction_splits_v11 RENAME TO transaction_splits");
            createAuditSchema(db);
        }
        if (oldVersion < 12) {
            // Previous deletions intentionally have no snapshots and cannot be undone.
            createSafeUndoSchema(db);
        }
        if (oldVersion < 13) {
            // Keep all financial rows intact; only replace the local audit triggers so
            // future debt/category/detail changes are represented accurately.
            recreateAuditTriggers(db);
        }
    }

    private static void createCaptureSchema(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS capture_inbox (id INTEGER PRIMARY KEY AUTOINCREMENT, provider TEXT NOT NULL, channel TEXT NOT NULL, sender TEXT, raw_text TEXT NOT NULL, amount_minor INTEGER, currency TEXT, inferred_type TEXT, confidence INTEGER NOT NULL DEFAULT 0, counterparty TEXT, balance_minor INTEGER, status TEXT NOT NULL DEFAULT 'PENDING', fingerprint TEXT NOT NULL UNIQUE, received_at INTEGER NOT NULL, transaction_id INTEGER)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_capture_status ON capture_inbox(status,received_at)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_capture_match ON capture_inbox(provider,currency,amount_minor,received_at)");
    }

    private static void createDebtSchema(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS debt_people (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL COLLATE NOCASE UNIQUE, active INTEGER NOT NULL DEFAULT 1, created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS debts (id INTEGER PRIMARY KEY AUTOINCREMENT, person_id INTEGER NOT NULL, direction TEXT NOT NULL, currency TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, closed_at INTEGER, manual_closed INTEGER NOT NULL DEFAULT 0, note TEXT NOT NULL DEFAULT '', FOREIGN KEY(person_id) REFERENCES debt_people(id))");
        db.execSQL("CREATE TABLE IF NOT EXISTS debt_movements (id INTEGER PRIMARY KEY AUTOINCREMENT, debt_id INTEGER NOT NULL, transaction_id INTEGER, kind TEXT NOT NULL, amount_minor INTEGER NOT NULL, created_at INTEGER NOT NULL, note TEXT NOT NULL DEFAULT '', FOREIGN KEY(debt_id) REFERENCES debts(id) ON DELETE CASCADE, FOREIGN KEY(transaction_id) REFERENCES transactions(id) ON DELETE CASCADE, UNIQUE(transaction_id,debt_id,kind))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_debts_person ON debts(person_id,direction,currency,closed_at)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_debt_movements_tx ON debt_movements(transaction_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_debt_movements_debt ON debt_movements(debt_id,created_at)");
    }

    private static void createAuditSchema(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS change_history("+
                "id INTEGER PRIMARY KEY AUTOINCREMENT,table_name TEXT NOT NULL,record_id INTEGER NOT NULL,"+
                "action TEXT NOT NULL,previous TEXT,current TEXT,created_at INTEGER NOT NULL,"+
                "previous_category_id INTEGER,current_category_id INTEGER,"+
                "previous_account_id INTEGER,current_account_id INTEGER,"+
                "previous_amount_minor INTEGER,current_amount_minor INTEGER,"+
                "previous_type TEXT,current_type TEXT,undone INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_change_history_time ON change_history(created_at DESC)");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS audit_transaction_insert AFTER INSERT ON transactions BEGIN " +
                "INSERT INTO change_history(table_name,record_id,action,current,created_at) " +
                "VALUES ('transactions',NEW.id,'ADD','type='||NEW.type||', amount='||NEW.amount_minor||', account='||NEW.account_id,strftime('%s','now')*1000); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS audit_transaction_update AFTER UPDATE ON transactions " +
                "WHEN OLD.type!=NEW.type OR OLD.amount_minor!=NEW.amount_minor OR OLD.account_id!=NEW.account_id " +
                "OR COALESCE(OLD.category_id,-1)!=COALESCE(NEW.category_id,-1) " +
                "OR COALESCE(OLD.destination_account_id,-1)!=COALESCE(NEW.destination_account_id,-1) " +
                "OR OLD.note!=NEW.note OR OLD.affects_balance!=NEW.affects_balance " +
                "OR OLD.base_affects_balance!=NEW.base_affects_balance " +
                "OR COALESCE(OLD.special_kind,'')!=COALESCE(NEW.special_kind,'') " +
                "OR COALESCE(OLD.debt_person_id,-1)!=COALESCE(NEW.debt_person_id,-1) " +
                "OR COALESCE(OLD.debt_direction,'')!=COALESCE(NEW.debt_direction,'') " +
                "OR COALESCE(OLD.destination_amount_minor,-1)!=COALESCE(NEW.destination_amount_minor,-1) " +
                "OR COALESCE(OLD.exchange_rate,'')!=COALESCE(NEW.exchange_rate,'') " +
                "OR COALESCE(OLD.exchange_rate_source,'')!=COALESCE(NEW.exchange_rate_source,'') " +
                "OR COALESCE(OLD.exchange_rate_checked_at,-1)!=COALESCE(NEW.exchange_rate_checked_at,-1) BEGIN " +
                "INSERT INTO change_history(table_name,record_id,action,previous,current,created_at,"+
                "previous_category_id,current_category_id,previous_account_id,current_account_id,"+
                "previous_amount_minor,current_amount_minor,previous_type,current_type) VALUES ("+
                "'transactions',NEW.id,'EDIT',"+
                "'type='||OLD.type||', amount='||OLD.amount_minor||', category='||COALESCE(OLD.category_id,-1)||', account='||OLD.account_id||', destination='||COALESCE(OLD.destination_account_id,-1)||', affects='||OLD.affects_balance||', special='||COALESCE(OLD.special_kind,'')||', debt_person='||COALESCE(OLD.debt_person_id,-1)||', debt_direction='||COALESCE(OLD.debt_direction,'')||', destination_amount='||COALESCE(OLD.destination_amount_minor,-1)||', rate='||COALESCE(OLD.exchange_rate,'')||', rate_source='||COALESCE(OLD.exchange_rate_source,''),"+
                "'type='||NEW.type||', amount='||NEW.amount_minor||', category='||COALESCE(NEW.category_id,-1)||', account='||NEW.account_id||', destination='||COALESCE(NEW.destination_account_id,-1)||', affects='||NEW.affects_balance||', special='||COALESCE(NEW.special_kind,'')||', debt_person='||COALESCE(NEW.debt_person_id,-1)||', debt_direction='||COALESCE(NEW.debt_direction,'')||', destination_amount='||COALESCE(NEW.destination_amount_minor,-1)||', rate='||COALESCE(NEW.exchange_rate,'')||', rate_source='||COALESCE(NEW.exchange_rate_source,''),"+
                "strftime('%s','now')*1000,OLD.category_id,NEW.category_id,OLD.account_id,NEW.account_id,"+
                "OLD.amount_minor,NEW.amount_minor,OLD.type,NEW.type); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS audit_transaction_delete BEFORE DELETE ON transactions BEGIN " +
                "INSERT INTO change_history(table_name,record_id,action,previous,created_at) VALUES ("+
                "'transactions',OLD.id,'DELETE','type='||OLD.type||', amount='||OLD.amount_minor||', account='||OLD.account_id,strftime('%s','now')*1000); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS audit_split_insert AFTER INSERT ON transaction_splits BEGIN " +
                "INSERT INTO change_history(table_name,record_id,action,current,created_at) VALUES ("+
                "'transaction_splits',NEW.transaction_id,'SPLIT_ADD','category='||COALESCE(NEW.category_id,-1)||', amount='||NEW.amount_minor,strftime('%s','now')*1000); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS audit_split_delete BEFORE DELETE ON transaction_splits BEGIN " +
                "INSERT INTO change_history(table_name,record_id,action,previous,created_at) VALUES ("+
                "'transaction_splits',OLD.transaction_id,'SPLIT_REMOVE','category='||COALESCE(OLD.category_id,-1)||', amount='||OLD.amount_minor,strftime('%s','now')*1000); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS audit_category_update AFTER UPDATE ON categories " +
                "WHEN OLD.name!=NEW.name OR OLD.kind!=NEW.kind OR OLD.emoji!=NEW.emoji " +
                "OR OLD.exclude_from_expense!=NEW.exclude_from_expense OR OLD.archived!=NEW.archived BEGIN " +
                "INSERT INTO change_history(table_name,record_id,action,previous,current,created_at) VALUES ("+
                "'categories',NEW.id,'CATEGORY_EDIT',OLD.name||' / '||OLD.emoji,NEW.name||' / '||NEW.emoji,strftime('%s','now')*1000); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS audit_category_insert AFTER INSERT ON categories BEGIN " +
                "INSERT INTO change_history(table_name,record_id,action,current,created_at) VALUES ("+
                "'categories',NEW.id,'CATEGORY_ADD',NEW.name||' / '||NEW.emoji,strftime('%s','now')*1000); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS audit_category_delete BEFORE DELETE ON categories BEGIN " +
                "INSERT INTO change_history(table_name,record_id,action,previous,created_at) VALUES ("+
                "'categories',OLD.id,'CATEGORY_DELETE',OLD.name,strftime('%s','now')*1000); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS audit_debt_movement_insert AFTER INSERT ON debt_movements BEGIN " +
                "INSERT INTO change_history(table_name,record_id,action,current,created_at) VALUES ("+
                "'debt_movements',NEW.id,'DEBT_ADD','debt='||NEW.debt_id||', kind='||NEW.kind||', amount='||NEW.amount_minor,strftime('%s','now')*1000); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS audit_debt_movement_update AFTER UPDATE ON debt_movements " +
                "WHEN OLD.kind!=NEW.kind OR OLD.amount_minor!=NEW.amount_minor OR OLD.note!=NEW.note BEGIN " +
                "INSERT INTO change_history(table_name,record_id,action,previous,current,created_at) VALUES ("+
                "'debt_movements',NEW.id,'DEBT_EDIT','kind='||OLD.kind||', amount='||OLD.amount_minor,"+
                "'kind='||NEW.kind||', amount='||NEW.amount_minor,strftime('%s','now')*1000); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS audit_debt_movement_delete BEFORE DELETE ON debt_movements BEGIN " +
                "INSERT INTO change_history(table_name,record_id,action,previous,created_at) VALUES ("+
                "'debt_movements',OLD.id,'DEBT_DELETE','debt='||OLD.debt_id||', kind='||OLD.kind||', amount='||OLD.amount_minor,strftime('%s','now')*1000); END");
    }

    private static void recreateAuditTriggers(SQLiteDatabase db) {
        String[] names = {"audit_transaction_insert", "audit_transaction_update", "audit_transaction_delete",
                "audit_split_insert", "audit_split_delete", "audit_category_insert", "audit_category_update",
                "audit_category_delete", "audit_debt_movement_insert", "audit_debt_movement_update",
                "audit_debt_movement_delete"};
        for (String name : names) db.execSQL("DROP TRIGGER IF EXISTS " + name);
        createAuditSchema(db);
    }

    /** Only newly deleted, simple manual transactions get an undo snapshot.
     *  No plaintext snapshot is exported to GitHub or a network service.
     */
    private static void createSafeUndoSchema(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS deleted_transaction_snapshots(" +
                "audit_id INTEGER PRIMARY KEY,transaction_id INTEGER NOT NULL," +
                "type TEXT NOT NULL,amount_minor INTEGER NOT NULL,currency TEXT NOT NULL," +
                "account_id INTEGER NOT NULL,category_id INTEGER NOT NULL," +
                "note TEXT NOT NULL,raw_text TEXT,needs_review INTEGER NOT NULL," +
                "created_at INTEGER NOT NULL,source TEXT NOT NULL," +
                "affects_balance INTEGER NOT NULL,base_affects_balance INTEGER NOT NULL," +
                "balance_impact_minor INTEGER NOT NULL,deleted_at INTEGER NOT NULL," +
                "restored_at INTEGER,FOREIGN KEY(audit_id) REFERENCES change_history(id))");
    }

    public static final class AuditEntry {
        public long id,recordId,createdAt;
        public String table,action,before,after;
        public boolean undoable;
    }
    /** Guard shared by the history button and its execution to avoid stale/unsafe Undo. */
    private static final String SAFE_CATEGORY_UNDO_SQL =
            "AND t.source='MANUAL' AND t.needs_review=0 " +
            "AND t.destination_amount_minor IS NULL AND t.exchange_rate IS NULL " +
            "AND t.exchange_rate_source IS NULL AND t.exchange_rate_checked_at IS NULL " +
            "AND t.debt_person_id IS NULL AND t.debt_direction IS NULL " +
            "AND NOT EXISTS(SELECT 1 FROM transaction_splits sp WHERE sp.transaction_id=t.id) " +
            "AND NOT EXISTS(SELECT 1 FROM debt_movements dm WHERE dm.transaction_id=t.id) " +
            "AND NOT EXISTS(SELECT 1 FROM capture_inbox ci WHERE ci.transaction_id=t.id) " +
            "AND EXISTS(SELECT 1 FROM categories oldcat " +
            "WHERE oldcat.id=h.previous_category_id AND oldcat.kind=t.type) " +
            "AND EXISTS(SELECT 1 FROM categories newcat " +
            "WHERE newcat.id=h.current_category_id AND newcat.kind=t.type) ";

    public List<AuditEntry> getChangeHistory(int limit) {
        List<AuditEntry> result=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT h.id,h.table_name,h.record_id,h.action,h.previous,h.current,h.created_at,"+
                "CASE WHEN h.action='DELETE' AND h.table_name='transactions' AND h.undone=0 " +
                "AND EXISTS(SELECT 1 FROM deleted_transaction_snapshots ss WHERE ss.audit_id=h.id AND ss.restored_at IS NULL) " +
                "AND NOT EXISTS(SELECT 1 FROM transactions other WHERE other.id=h.record_id) THEN 1 " +
                "WHEN h.action='EDIT' AND h.undone=0 AND h.previous_category_id IS NOT NULL " +
                "AND h.current_category_id IS NOT NULL AND h.previous_category_id!=h.current_category_id " +
                "AND h.previous_type=h.current_type AND h.previous_account_id=h.current_account_id " +
                "AND h.previous_amount_minor=h.current_amount_minor " +
                "AND EXISTS(SELECT 1 FROM transactions t WHERE t.id=h.record_id " +
                "AND t.type=h.current_type AND t.category_id=h.current_category_id " +
                "AND t.account_id=h.current_account_id AND t.amount_minor=h.current_amount_minor " +
                "AND t.special_kind IS NULL AND t.destination_account_id IS NULL " +
                SAFE_CATEGORY_UNDO_SQL + ") " +
                "AND h.id=(SELECT MAX(next.id) FROM change_history next " +
                "WHERE next.table_name='transactions' AND next.record_id=h.record_id) " +
                "THEN 1 ELSE 0 END " +
                "FROM change_history h ORDER BY h.id DESC LIMIT ?",new String[]{String.valueOf(Math.min(500,Math.max(1,limit)))})) {
            while(c.moveToNext()) {
                AuditEntry a=new AuditEntry();a.id=c.getLong(0);a.table=c.getString(1);a.recordId=c.getLong(2);
                a.action=c.getString(3);a.before=c.getString(4);a.after=c.getString(5);a.createdAt=c.getLong(6);a.undoable=c.getInt(7)==1;result.add(a);
            }
        }
        return result;
    }

    /** Undo a latest manual category edit only. The edit and audit mark commit together. */
    public void undoLatestSimpleCategoryChange(long auditId) {
        SQLiteDatabase writable=getWritableDatabase();
        writable.beginTransaction();
        try {
            long txId,oldCategory,account;
            String kind;
            try(Cursor c=writable.rawQuery(
                    "SELECT h.record_id,h.previous_category_id,h.previous_type,h.previous_account_id " +
                    "FROM change_history h JOIN transactions t ON t.id=h.record_id " +
                    "WHERE h.id=? AND h.table_name='transactions' AND h.action='EDIT' AND h.undone=0 " +
                    "AND h.previous_category_id IS NOT NULL AND h.current_category_id IS NOT NULL " +
                    "AND h.previous_category_id!=h.current_category_id AND h.previous_type=h.current_type " +
                    "AND h.previous_account_id=h.current_account_id AND h.previous_amount_minor=h.current_amount_minor " +
                    "AND t.type=h.current_type AND t.category_id=h.current_category_id " +
                    "AND t.account_id=h.current_account_id AND t.amount_minor=h.current_amount_minor " +
                    "AND t.special_kind IS NULL AND t.destination_account_id IS NULL " +
                    SAFE_CATEGORY_UNDO_SQL +
                    "AND h.id=(SELECT MAX(next.id) FROM change_history next WHERE next.table_name='transactions' AND next.record_id=h.record_id)",
                    new String[]{String.valueOf(auditId)})) {
                if(!c.moveToFirst())throw new IllegalArgumentException("هذه الحركة ليست آمنة للتراجع أو تغيّرت بعدها بيانات أخرى");
                txId=c.getLong(0);oldCategory=c.getLong(1);kind=c.getString(2);account=c.getLong(3);
            }
            if(!FinanceRules.EXPENSE.equals(kind)&&!FinanceRules.INCOME.equals(kind))
                throw new IllegalArgumentException("التراجع متاح لتغيير التصنيف العادي فقط");
            // reclassifyTransaction nests an Android SQLite transaction. A failure in either
            // step rolls back the entire outer transaction, including balances and audit state.
            reclassifyTransaction(txId,kind,account,null,oldCategory,new ArrayList<>());
            ContentValues reversal=new ContentValues();reversal.put("action","UNDO");
            if(writable.update("change_history",reversal,
                    "id=(SELECT MAX(id) FROM change_history WHERE table_name='transactions' AND record_id=?) AND id>? AND action='EDIT'",
                    new String[]{String.valueOf(txId),String.valueOf(auditId)})!=1)
                throw new IllegalStateException("تعذّر توثيق حدث التراجع");
            ContentValues cv=new ContentValues();cv.put("undone",1);
            if(writable.update("change_history",cv,"id=? AND undone=0",new String[]{String.valueOf(auditId)})!=1)
                throw new IllegalStateException("تعذّر تثبيت سجل التراجع");
            writable.setTransactionSuccessful();
        } finally {
            writable.endTransaction();
        }
    }

    /** Deletion is permanent only for unused categories; referenced categories are archived. */
    public int removeCategoriesSafely(List<Long> ids) {
        if(ids==null||ids.isEmpty())return 0;
        SQLiteDatabase writable=getWritableDatabase();writable.beginTransaction();
        int changed=0;
        try {
            for(Long id:ids){
                if(id==null)continue;
                try(Cursor c=writable.rawQuery("SELECT name,exclude_from_expense FROM categories WHERE id=? AND archived=0",new String[]{String.valueOf(id)})) {
                    if(!c.moveToFirst())continue;
                    if(c.getInt(1)!=0 || CategoryNamingRules.RECEIVED_TRANSFER.equals(c.getString(0)))
                        throw new IllegalArgumentException("التصنيف المحمي لا يمكن حذفه: "+c.getString(0));
                }
                long used=0;
                try(Cursor c=writable.rawQuery("SELECT (SELECT COUNT(*) FROM transactions WHERE category_id=?) + " +
                        "(SELECT COUNT(*) FROM transaction_splits WHERE category_id=?)",new String[]{String.valueOf(id),String.valueOf(id)})) {
                    if(c.moveToFirst())used=c.getLong(0);
                }
                if(used>0){ContentValues cv=new ContentValues();cv.put("archived",1);
                    changed+=writable.update("categories",cv,"id=?",new String[]{String.valueOf(id)});
                } else changed+=writable.delete("categories","id=?",new String[]{String.valueOf(id)});
            }
            writable.setTransactionSuccessful();
        } finally { writable.endTransaction(); }
        return changed;
    }
    public void setCategoryIcon(long categoryId,String icon){
        if(icon==null||icon.trim().isEmpty()||icon.codePointCount(0,icon.length())>8)
            throw new IllegalArgumentException("الرمز غير صالح");
        ContentValues cv=new ContentValues();cv.put("emoji",icon.trim());
        if(getWritableDatabase().update("categories",cv,"id=? AND archived=0",new String[]{String.valueOf(categoryId)})!=1)
            throw new IllegalArgumentException("التصنيف غير موجود");
    }

    private void seed(SQLiteDatabase db) {
        addAccount(db, "بنك فلسطين", "ILS", 0, 1, 0);
        addAccount(db, "بنك فلسطين", "USD", 0, 1, 1);
        addAccount(db, "Jawwal Pay", "ILS", 0, 1, 2);
        addAccount(db, "PalPay", "ILS", 0, 1, 3);
        addAccount(db, "نقدي", "ILS", 0, 1, 4);
        addAccount(db, "نقدي", "USD", 0, 1, 5);
        addAccount(db, "نقدي", "JOD", 0, 1, 6);

        String[][] expenses = {
                {"🍽️", "أكل وشرب"}, {"🚕", "مواصلات"}, {"🛒", "تسوق"},
                {"🏠", "منزل وفواتير"}, {"🎓", "تعليم"}, {"🩺", "صحة"},
                {"📱", "اتصالات"}, {"🎮", "ترفيه"}, {"🎁", "هدايا"},
                {"🤲", "صدقة وتبرعات"}, {"💳", "ديون وأقساط"}, {"📦", "أخرى"}
        };
        String[][] income = {
                {"💼", "راتب"}, {"↙️", "حوالة مستلمة"}, {"↩️", "استرداد مبلغ"},
                {"🎁", "هدية"}, {"💰", "دخل آخر"}
        };
        for (int i = 0; i < expenses.length; i++) addCategory(db, expenses[i][1], FinanceRules.EXPENSE, expenses[i][0], i);
        for (int i = 0; i < income.length; i++) addCategory(db, income[i][1], FinanceRules.INCOME, income[i][0], i);
        addBrotherPaymentCategory(db);
    }

    private static void addAccount(SQLiteDatabase db, String name, String currency, long balance, int builtIn, int position) {
        ContentValues cv = new ContentValues();
        cv.put("name", name); cv.put("currency", currency); cv.put("balance_minor", balance);
        cv.put("built_in", builtIn); cv.put("position", position);
        db.insertOrThrow("accounts", null, cv);
    }

    private static void addCategory(SQLiteDatabase db, String name, String kind, String emoji, int position) {
        ContentValues cv = new ContentValues();
        cv.put("name", name); cv.put("kind", kind); cv.put("emoji", emoji); cv.put("position", position); cv.put("exclude_from_expense", 0);
        db.insertOrThrow("categories", null, cv);
    }

    private static void addBrotherPaymentCategory(SQLiteDatabase db) {
        db.execSQL("UPDATE categories SET exclude_from_expense=1 WHERE kind=? AND name='دفع لأخوي'",
                new Object[]{FinanceRules.EXPENSE});
        db.execSQL("INSERT INTO categories(name,kind,emoji,position,exclude_from_expense) " +
                "SELECT 'دفع لأخوي',?,'🤝',90,1 WHERE NOT EXISTS(" +
                "SELECT 1 FROM categories WHERE kind=? AND name='دفع لأخوي')",
                new Object[]{FinanceRules.EXPENSE, FinanceRules.EXPENSE});
    }

    private static void reconcileExcludedExpenseBalances(SQLiteDatabase db) {
        Map<Long, Long> refunds = new LinkedHashMap<>();
        String sql = "SELECT account_id,SUM(value) FROM (" +
                "SELECT t.account_id account_id,t.amount_minor value FROM transactions t " +
                "JOIN categories c ON c.id=t.category_id " +
                "WHERE t.type=? AND t.affects_balance=1 AND c.exclude_from_expense=1 " +
                "AND NOT EXISTS(SELECT 1 FROM transaction_splits x WHERE x.transaction_id=t.id) " +
                "UNION ALL " +
                "SELECT t.account_id account_id,s.amount_minor value FROM transaction_splits s " +
                "JOIN transactions t ON t.id=s.transaction_id JOIN categories c ON c.id=s.category_id " +
                "WHERE t.type=? AND t.affects_balance=1 AND c.exclude_from_expense=1" +
                ") GROUP BY account_id";
        try (Cursor c = db.rawQuery(sql, new String[]{FinanceRules.EXPENSE, FinanceRules.EXPENSE})) {
            while (c.moveToNext()) refunds.put(c.getLong(0), c.getLong(1));
        }
        for (Map.Entry<Long, Long> refund : refunds.entrySet()) {
            db.execSQL("UPDATE accounts SET balance_minor=balance_minor+? WHERE id=?",
                    new Object[]{refund.getValue(), refund.getKey()});
        }
    }

    public List<Models.Account> getAccounts() {
        List<Models.Account> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id,name,currency,balance_minor,built_in FROM accounts WHERE active=1 ORDER BY position,id", null)) {
            while (c.moveToNext()) {
                Models.Account a = new Models.Account();
                a.id = c.getLong(0); a.name = c.getString(1); a.currency = c.getString(2); a.balanceMinor = c.getLong(3); a.builtIn = c.getInt(4) == 1;
                result.add(a);
            }
        }
        return result;
    }

    public Models.Account getAccount(long id) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id,name,currency,balance_minor FROM accounts WHERE id=?", new String[]{String.valueOf(id)})) {
            if (!c.moveToFirst()) return null;
            Models.Account a = new Models.Account();
            a.id = c.getLong(0); a.name = c.getString(1); a.currency = c.getString(2); a.balanceMinor = c.getLong(3);
            return a;
        }
    }

    public List<Models.Category> getCategories(String kind) {
        List<Models.Category> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id,name,kind,emoji,exclude_from_expense FROM categories WHERE kind=? AND archived=0 ORDER BY position,id", new String[]{kind})) {
            while (c.moveToNext()) {
                Models.Category category = new Models.Category();
                category.id = c.getLong(0); category.name = c.getString(1); category.kind = c.getString(2); category.emoji = c.getString(3); category.excludeFromExpense = c.getInt(4) == 1;
                result.add(category);
            }
        }
        return result;
    }

    public Map<String, Long> getTotals() {
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("ILS", 0L); result.put("USD", 0L); result.put("JOD", 0L);
        try (Cursor c = getReadableDatabase().rawQuery("SELECT currency,SUM(balance_minor) FROM accounts WHERE active=1 GROUP BY currency", null)) {
            while (c.moveToNext()) result.put(c.getString(0), c.getLong(1));
        }
        return result;
    }

    public List<Models.Transaction> getTransactions(int limit) {
        List<Models.Transaction> result = new ArrayList<>();
        String sql = "SELECT t.id,t.type,t.amount_minor,t.currency,t.account_id,t.destination_account_id,a.name,d.name,c.name,c.emoji,t.note,t.raw_text,t.needs_review,t.created_at," +
                "EXISTS(SELECT 1 FROM transaction_splits s WHERE s.transaction_id=t.id),t.affects_balance " +
                "FROM transactions t JOIN accounts a ON a.id=t.account_id " +
                "LEFT JOIN accounts d ON d.id=t.destination_account_id LEFT JOIN categories c ON c.id=t.category_id " +
                "ORDER BY t.created_at DESC,t.id DESC LIMIT ?";
        try (Cursor c = getReadableDatabase().rawQuery(sql, new String[]{String.valueOf(limit)})) {
            while (c.moveToNext()) {
                Models.Transaction tx = new Models.Transaction();
                tx.id = c.getLong(0); tx.type = c.getString(1); tx.amountMinor = c.getLong(2); tx.currency = c.getString(3);
                tx.accountId = c.getLong(4); tx.destinationAccountId = c.isNull(5) ? null : c.getLong(5);
                tx.accountName = c.getString(6); tx.destinationName = c.isNull(7) ? null : c.getString(7);
                tx.categoryName = c.isNull(8) ? null : c.getString(8); tx.categoryEmoji = c.isNull(9) ? null : c.getString(9);
                tx.note = c.getString(10); tx.rawText = c.isNull(11) ? null : c.getString(11); tx.needsReview = c.getInt(12) == 1; tx.createdAt = c.getLong(13); tx.hasSplits = c.getInt(14) == 1; tx.affectsBalance = c.getInt(15) == 1;
                populateSpecial(tx);
                result.add(tx);
            }
        }
        return result;
    }

    /** Local, bound-parameter history search. Offset pagination avoids a 1,000-card UI. */
    public List<Models.Transaction> searchTransactions(String text, String type, String currency,
                                                        int pageSize, int offset) {
        TransactionSearchRules.Query query = TransactionSearchRules.build(text,type,currency,pageSize,offset);
        List<Models.Transaction> result = new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery(query.sql,query.args)) {
            while(c.moveToNext()) {
                Models.Transaction tx=new Models.Transaction();
                tx.id=c.getLong(0);tx.type=c.getString(1);tx.amountMinor=c.getLong(2);
                tx.currency=c.getString(3);tx.accountId=c.getLong(4);
                tx.destinationAccountId=c.isNull(5)?null:c.getLong(5);
                tx.accountName=c.getString(6);
                tx.destinationName=c.isNull(7)?null:c.getString(7);
                tx.categoryName=c.isNull(8)?null:c.getString(8);
                tx.categoryEmoji=c.isNull(9)?null:c.getString(9);
                tx.note=c.getString(10);
                tx.rawText=c.isNull(11)?null:c.getString(11);
                tx.needsReview=c.getInt(12)==1;tx.createdAt=c.getLong(13);
                tx.hasSplits=c.getInt(14)==1;tx.affectsBalance=c.getInt(15)==1;
                populateSpecial(tx);
                result.add(tx);
            }
        }
        return result;
    }

    /** Fetch one original financial row for linked debt history, not an unbounded history scan. */
    public Models.Transaction getTransactionById(long transactionId) {
        String sql="SELECT t.id,t.type,t.amount_minor,t.currency,t.account_id,t.destination_account_id," +
                "a.name,d.name,c.name,c.emoji,t.note,t.raw_text,t.needs_review,t.created_at," +
                "EXISTS(SELECT 1 FROM transaction_splits s WHERE s.transaction_id=t.id),t.affects_balance " +
                "FROM transactions t JOIN accounts a ON a.id=t.account_id " +
                "LEFT JOIN accounts d ON d.id=t.destination_account_id " +
                "LEFT JOIN categories c ON c.id=t.category_id WHERE t.id=?";
        try(Cursor c=getReadableDatabase().rawQuery(sql,new String[]{String.valueOf(transactionId)})) {
            if(!c.moveToFirst())return null;
            Models.Transaction tx=new Models.Transaction();
            tx.id=c.getLong(0);tx.type=c.getString(1);tx.amountMinor=c.getLong(2);
            tx.currency=c.getString(3);tx.accountId=c.getLong(4);
            tx.destinationAccountId=c.isNull(5)?null:c.getLong(5);
            tx.accountName=c.getString(6);
            tx.destinationName=c.isNull(7)?null:c.getString(7);
            tx.categoryName=c.isNull(8)?null:c.getString(8);
            tx.categoryEmoji=c.isNull(9)?null:c.getString(9);
            tx.note=c.getString(10);tx.rawText=c.isNull(11)?null:c.getString(11);
            tx.needsReview=c.getInt(12)==1;tx.createdAt=c.getLong(13);
            tx.hasSplits=c.getInt(14)==1;tx.affectsBalance=c.getInt(15)==1;
            populateSpecial(tx);
            return tx;
        }
    }

    public List<Models.Transaction> getExpenseTransactions(String currency, long startInclusive, long endExclusive) {
        List<Models.Transaction> result = new ArrayList<>();
        String sql = "SELECT t.id,t.type,t.amount_minor,t.currency,t.account_id,t.destination_account_id,a.name,d.name,c.name,c.emoji,t.note,t.raw_text,t.needs_review,t.created_at," +
                "EXISTS(SELECT 1 FROM transaction_splits s WHERE s.transaction_id=t.id),t.affects_balance " +
                "FROM transactions t JOIN accounts a ON a.id=t.account_id " +
                "LEFT JOIN accounts d ON d.id=t.destination_account_id LEFT JOIN categories c ON c.id=t.category_id " +
                "WHERE t.type=? AND t.special_kind IS NULL AND t.currency=? AND t.created_at>=? AND t.created_at<? AND (" +
                "(NOT EXISTS(SELECT 1 FROM transaction_splits x WHERE x.transaction_id=t.id) AND COALESCE(c.exclude_from_expense,0)=0) OR " +
                "EXISTS(SELECT 1 FROM transaction_splits s JOIN categories sc ON sc.id=s.category_id " +
                "WHERE s.transaction_id=t.id AND sc.exclude_from_expense=0)) " +
                "ORDER BY t.created_at DESC,t.id DESC";
        String[] args = {FinanceRules.EXPENSE, currency, String.valueOf(startInclusive), String.valueOf(endExclusive)};
        try (Cursor c = getReadableDatabase().rawQuery(sql, args)) {
            while (c.moveToNext()) {
                Models.Transaction tx = new Models.Transaction();
                tx.id = c.getLong(0); tx.type = c.getString(1); tx.amountMinor = c.getLong(2); tx.currency = c.getString(3);
                tx.accountId = c.getLong(4); tx.destinationAccountId = c.isNull(5) ? null : c.getLong(5);
                tx.accountName = c.getString(6); tx.destinationName = c.isNull(7) ? null : c.getString(7);
                tx.categoryName = c.isNull(8) ? null : c.getString(8); tx.categoryEmoji = c.isNull(9) ? null : c.getString(9);
                tx.note = c.getString(10); tx.rawText = c.isNull(11) ? null : c.getString(11); tx.needsReview = c.getInt(12) == 1;
                tx.createdAt = c.getLong(13); tx.hasSplits = c.getInt(14) == 1; tx.affectsBalance = c.getInt(15) == 1;
                populateSpecial(tx);
                result.add(tx);
            }
        }
        return result;
    }

    public List<Models.Transaction> getUncategorizedTransactions() {
        List<Models.Transaction> result = new ArrayList<>();
        String sql = "SELECT t.id,t.type,t.amount_minor,t.currency,t.account_id,t.destination_account_id,a.name,d.name,c.name,c.emoji,t.note,t.raw_text,t.needs_review,t.created_at," +
                "EXISTS(SELECT 1 FROM transaction_splits s WHERE s.transaction_id=t.id),t.affects_balance " +
                "FROM transactions t JOIN accounts a ON a.id=t.account_id " +
                "LEFT JOIN accounts d ON d.id=t.destination_account_id LEFT JOIN categories c ON c.id=t.category_id " +
                "WHERE t.type IN (?,?) AND t.special_kind IS NULL AND t.category_id IS NULL " +
                "AND NOT EXISTS(SELECT 1 FROM transaction_splits x WHERE x.transaction_id=t.id) " +
                "ORDER BY t.created_at DESC,t.id DESC";
        String[] args = {FinanceRules.EXPENSE, FinanceRules.INCOME};
        try (Cursor c = getReadableDatabase().rawQuery(sql, args)) {
            while (c.moveToNext()) {
                Models.Transaction tx = new Models.Transaction();
                tx.id = c.getLong(0); tx.type = c.getString(1); tx.amountMinor = c.getLong(2); tx.currency = c.getString(3);
                tx.accountId = c.getLong(4); tx.destinationAccountId = c.isNull(5) ? null : c.getLong(5);
                tx.accountName = c.getString(6); tx.destinationName = c.isNull(7) ? null : c.getString(7);
                tx.categoryName = c.isNull(8) ? null : c.getString(8); tx.categoryEmoji = c.isNull(9) ? null : c.getString(9);
                tx.note = c.getString(10); tx.rawText = c.isNull(11) ? null : c.getString(11); tx.needsReview = c.getInt(12) == 1; tx.createdAt = c.getLong(13); tx.hasSplits = c.getInt(14) == 1; tx.affectsBalance = c.getInt(15) == 1;
                populateSpecial(tx);
                result.add(tx);
            }
        }
        return result;
    }

    /** Expense classification reminders must not fire with zero unclassified expenses. */
    public int getPendingExpenseClassificationCount() {
        String sql = "SELECT (SELECT COUNT(*) FROM transactions t WHERE t.type=? " +
                "AND t.special_kind IS NULL AND t.category_id IS NULL " +
                "AND NOT EXISTS(SELECT 1 FROM transaction_splits s WHERE s.transaction_id=t.id)) " +
                "+ (SELECT COUNT(*) FROM capture_inbox WHERE status='PENDING' AND inferred_type=?)";
        try (Cursor c = getReadableDatabase().rawQuery(sql,
                new String[]{FinanceRules.EXPENSE, FinanceRules.EXPENSE})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    public CategorySuggestionRules.Suggestion suggestCategory(String kind, String currency,
                    long accountId, long amountMinor, String description) {
        if (!FinanceRules.EXPENSE.equals(kind) && !FinanceRules.INCOME.equals(kind)) return null;
        List<CategorySuggestionRules.Example> samples = new ArrayList<>();
        String sql="SELECT t.category_id,t.account_id,t.amount_minor,t.note,t.raw_text, " +
                "EXISTS(SELECT 1 FROM change_history h WHERE h.table_name='transactions' " +
                "AND h.record_id=t.id AND h.action='EDIT' AND h.undone=0 " +
                "AND h.previous_category_id IS NOT NULL " +
                "AND h.previous_category_id!=h.current_category_id " +
                "AND h.current_category_id=t.category_id) corrected " +
                "FROM transactions t JOIN categories c ON c.id=t.category_id WHERE t.type=? AND t.currency=? " +
                "AND c.archived=0 AND c.kind=t.type AND t.amount_minor>0 " +
                "AND t.special_kind IS NULL AND t.needs_review=0 " +
                "AND NOT EXISTS(SELECT 1 FROM transaction_splits s WHERE s.transaction_id=t.id) " +
                "ORDER BY t.created_at DESC,t.id DESC LIMIT 200";
        try (Cursor c=getReadableDatabase().rawQuery(sql,new String[]{kind,currency})) {
            while(c.moveToNext()) {
                String note=c.isNull(3)?"":c.getString(3);
                String raw=c.isNull(4)?"":c.getString(4);
                // Merchant signals in automatic messages, or a merchant named in a note.
                String merchant=extractMerchant(raw);
                samples.add(new CategorySuggestionRules.Example(c.getLong(0),c.getLong(1),
                        c.getLong(2),note,merchant,c.getInt(5)==1));
            }
        }
        return CategorySuggestionRules.suggest(accountId,amountMinor,description,description,samples);
    }

    private static String extractMerchant(String raw) {
        if (raw==null) return "";
        java.util.regex.Matcher m=java.util.regex.Pattern.compile(
                "(?i)(?:for merchant:|merchant:|عند محل|لدى)\\s*([\\p{L}\\p{N} ]{3,50})").matcher(raw);
        return m.find()?m.group(1).trim():"";
    }

    private static long balanceIncludingInactive(SQLiteDatabase db, long accountId) {
        try(Cursor c=db.rawQuery("SELECT balance_minor FROM accounts WHERE id=?",
                new String[]{String.valueOf(accountId)})) {
            if(!c.moveToFirst())throw new IllegalStateException("حساب التحويل المحذوف غير موجود");
            return c.getLong(0);
        }
    }

    public long addCustomAccount(String name, String currency) {
        SQLiteDatabase db = getWritableDatabase();
        int position = 100;
        try (Cursor c = db.rawQuery("SELECT COALESCE(MAX(position),0)+1 FROM accounts", null)) {
            if (c.moveToFirst()) position = c.getInt(0);
        }
        ContentValues cv = new ContentValues();
        cv.put("name", name.trim()); cv.put("currency", currency); cv.put("balance_minor", 0);
        cv.put("built_in", 0); cv.put("position", position); cv.put("active", 1);
        return db.insertOrThrow("accounts", null, cv);
    }

    public List<Models.SplitDetail> getSplitDetails(long transactionId) {
        List<Models.SplitDetail> result = new ArrayList<>();
        String sql = "SELECT c.id,c.name,c.emoji,s.amount_minor,s.special_kind,s.debt_person_id,p.name,s.debt_direction FROM transaction_splits s " +
                "LEFT JOIN categories c ON c.id=s.category_id LEFT JOIN debt_people p ON p.id=s.debt_person_id " +
                "WHERE s.transaction_id=? ORDER BY s.id";
        try (Cursor c = getReadableDatabase().rawQuery(sql, new String[]{String.valueOf(transactionId)})) {
            while (c.moveToNext()) {
                Models.SplitDetail item = new Models.SplitDetail();
                item.categoryId = c.isNull(0) ? 0 : c.getLong(0);
                item.categoryName = c.isNull(1) ? "" : c.getString(1);
                item.categoryEmoji = c.isNull(2) ? "" : c.getString(2);
                item.amountMinor = c.getLong(3); item.specialKind = c.isNull(4) ? null : c.getString(4);
                item.debtPersonId = c.isNull(5) ? null : c.getLong(5);
                item.debtPersonName = c.isNull(6) ? null : c.getString(6);
                item.debtDirection = c.isNull(7) ? null : c.getString(7);
                result.add(item);
            }
        }
        return result;
    }

    /** Incoming earned income vs incoming money received as a transfer category; debt receipts excluded. */
    public long getIncomeTotal(String currency,long startInclusive,long endExclusive,boolean receivedOnly) {
        String sql="SELECT COALESCE(SUM(t.amount_minor),0) FROM transactions t " +
                "LEFT JOIN categories c ON c.id=t.category_id WHERE t.type=? " +
                "AND t.currency=? AND t.special_kind IS NULL " +
                "AND t.created_at>=? AND t.created_at<? AND t.category_id IS NOT NULL " +
                (receivedOnly?"AND c.name='حوالة مستلمة'":"AND c.name!='حوالة مستلمة'");
        try(Cursor cursor=getReadableDatabase().rawQuery(sql,new String[]{FinanceRules.INCOME,currency,
                String.valueOf(startInclusive),String.valueOf(endExclusive)})) {
            return cursor.moveToFirst()?cursor.getLong(0):0L;
        }
    }

    /** Exact item-level counterpart of getIncomeTotal. Split/debt/internal transfers
     * never appear here; only categorized non-special INCOME rows are counted. */
    public List<Models.Transaction> getIncomeTransactions(String currency, long startInclusive,
                                                          long endExclusive, boolean receivedOnly) {
        List<Models.Transaction> result = new ArrayList<>();
        String sql = "SELECT t.id,t.type,t.amount_minor,t.currency,t.account_id,t.destination_account_id,"
                + "a.name,d.name,c.name,c.emoji,t.note,t.raw_text,t.needs_review,t.created_at,"
                + "EXISTS(SELECT 1 FROM transaction_splits s WHERE s.transaction_id=t.id),t.affects_balance "
                + "FROM transactions t JOIN accounts a ON a.id=t.account_id "
                + "LEFT JOIN accounts d ON d.id=t.destination_account_id "
                + "LEFT JOIN categories c ON c.id=t.category_id "
                + "WHERE t.type=? AND t.currency=? AND t.special_kind IS NULL "
                + "AND t.created_at>=? AND t.created_at<? AND t.category_id IS NOT NULL "
                + (receivedOnly ? "AND c.name='حوالة مستلمة' " : "AND c.name!='حوالة مستلمة' ")
                + "ORDER BY t.created_at DESC,t.id DESC";
        String[] args = {FinanceRules.INCOME,currency,
                String.valueOf(startInclusive),String.valueOf(endExclusive)};
        try(Cursor c=getReadableDatabase().rawQuery(sql,args)) {
            while(c.moveToNext()) {
                Models.Transaction tx = new Models.Transaction();
                tx.id = c.getLong(0); tx.type = c.getString(1); tx.amountMinor = c.getLong(2);
                tx.currency = c.getString(3); tx.accountId = c.getLong(4);
                tx.destinationAccountId = c.isNull(5) ? null : c.getLong(5);
                tx.accountName = c.getString(6); tx.destinationName = c.isNull(7) ? null : c.getString(7);
                tx.categoryName = c.isNull(8) ? null : c.getString(8);
                tx.categoryEmoji = c.isNull(9) ? null : c.getString(9);
                tx.note = c.getString(10); tx.rawText = c.isNull(11) ? null : c.getString(11);
                tx.needsReview = c.getInt(12) == 1; tx.createdAt = c.getLong(13);
                tx.hasSplits = c.getInt(14) == 1; tx.affectsBalance = c.getInt(15) == 1;
                populateSpecial(tx);
                result.add(tx);
            }
        }
        return result;
    }

    public long getExpenseTotal(String currency, long startInclusive, long endExclusive) {
        String sql = "SELECT COALESCE(SUM(value),0) FROM (" +
                "SELECT t.amount_minor value FROM transactions t LEFT JOIN categories c ON c.id=t.category_id " +
                "WHERE t.type=? AND t.special_kind IS NULL AND t.currency=? AND t.created_at>=? AND t.created_at<? " +
                "AND NOT EXISTS(SELECT 1 FROM transaction_splits x WHERE x.transaction_id=t.id) " +
                "AND COALESCE(c.exclude_from_expense,0)=0 " +
                "UNION ALL " +
                "SELECT s.amount_minor value FROM transaction_splits s JOIN transactions t ON t.id=s.transaction_id " +
                "JOIN categories c ON c.id=s.category_id " +
                "WHERE t.type=? AND t.special_kind IS NULL AND t.currency=? AND t.created_at>=? AND t.created_at<? AND c.exclude_from_expense=0)";
        String start = String.valueOf(startInclusive), end = String.valueOf(endExclusive);
        String[] args = {FinanceRules.EXPENSE, currency, start, end, FinanceRules.EXPENSE, currency, start, end};
        try (Cursor c = getReadableDatabase().rawQuery(sql, args)) {
            return c.moveToFirst() ? c.getLong(0) : 0L;
        }
    }

    public List<Models.CategoryTotal> getCategoryExpenseTotals(String currency, long startInclusive, long endExclusive) {
        List<Models.CategoryTotal> result = new ArrayList<>();
        String sql = "SELECT category_id,name,emoji,SUM(value) total FROM (" +
                "SELECT c.id category_id,c.name name,c.emoji emoji,t.amount_minor value FROM transactions t JOIN categories c ON c.id=t.category_id " +
                "WHERE t.type=? AND t.special_kind IS NULL AND t.currency=? AND t.created_at>=? AND t.created_at<? AND c.exclude_from_expense=0 " +
                "AND NOT EXISTS(SELECT 1 FROM transaction_splits x WHERE x.transaction_id=t.id) " +
                "UNION ALL " +
                "SELECT c.id category_id,c.name name,c.emoji emoji,s.amount_minor value FROM transaction_splits s JOIN transactions t ON t.id=s.transaction_id JOIN categories c ON c.id=s.category_id " +
                "WHERE t.type=? AND t.special_kind IS NULL AND t.currency=? AND t.created_at>=? AND t.created_at<? AND c.exclude_from_expense=0" +
                ") GROUP BY category_id,name,emoji ORDER BY total DESC";
        String start = String.valueOf(startInclusive), end = String.valueOf(endExclusive);
        String[] args = {FinanceRules.EXPENSE, currency, start, end, FinanceRules.EXPENSE, currency, start, end};
        try (Cursor c = getReadableDatabase().rawQuery(sql, args)) {
            while (c.moveToNext()) {
                Models.CategoryTotal item = new Models.CategoryTotal();
                item.categoryId = c.getLong(0); item.name = c.getString(1); item.emoji = c.getString(2); item.amountMinor = c.getLong(3);
                result.add(item);
            }
        }
        return result;
    }

    public List<Models.Transaction> getExpenseTransactionsByCategory(String currency, long categoryId,
                                                                      long startInclusive, long endExclusive) {
        List<Models.Transaction> result = new ArrayList<>();
        String sql = "SELECT t.id,t.type,t.amount_minor,t.currency,t.account_id,t.destination_account_id,a.name,d.name,c.name,c.emoji,t.note,t.raw_text,t.needs_review,t.created_at," +
                "EXISTS(SELECT 1 FROM transaction_splits s WHERE s.transaction_id=t.id),t.affects_balance " +
                "FROM transactions t JOIN accounts a ON a.id=t.account_id " +
                "LEFT JOIN accounts d ON d.id=t.destination_account_id LEFT JOIN categories c ON c.id=t.category_id " +
                "WHERE t.type=? AND t.special_kind IS NULL AND t.currency=? AND t.created_at>=? AND t.created_at<? AND (" +
                "(t.category_id=? AND NOT EXISTS(SELECT 1 FROM transaction_splits x WHERE x.transaction_id=t.id)) OR " +
                "EXISTS(SELECT 1 FROM transaction_splits s WHERE s.transaction_id=t.id AND s.category_id=?)) " +
                "ORDER BY t.created_at DESC,t.id DESC";
        String[] args = {FinanceRules.EXPENSE, currency, String.valueOf(startInclusive), String.valueOf(endExclusive),
                String.valueOf(categoryId), String.valueOf(categoryId)};
        try (Cursor c = getReadableDatabase().rawQuery(sql, args)) {
            while (c.moveToNext()) {
                Models.Transaction tx = new Models.Transaction();
                tx.id = c.getLong(0); tx.type = c.getString(1); tx.amountMinor = c.getLong(2); tx.currency = c.getString(3);
                tx.accountId = c.getLong(4); tx.destinationAccountId = c.isNull(5) ? null : c.getLong(5);
                tx.accountName = c.getString(6); tx.destinationName = c.isNull(7) ? null : c.getString(7);
                tx.categoryName = c.isNull(8) ? null : c.getString(8); tx.categoryEmoji = c.isNull(9) ? null : c.getString(9);
                tx.note = c.getString(10); tx.rawText = c.isNull(11) ? null : c.getString(11); tx.needsReview = c.getInt(12) == 1;
                tx.createdAt = c.getLong(13); tx.hasSplits = c.getInt(14) == 1; tx.affectsBalance = c.getInt(15) == 1;
                populateSpecial(tx);
                result.add(tx);
            }
        }
        return result;
    }

    private void populateSpecial(Models.Transaction tx) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT t.special_kind,t.debt_person_id,p.name,t.debt_direction,t.destination_amount_minor,d.currency,t.exchange_rate,t.exchange_rate_source FROM transactions t " +
                        "LEFT JOIN debt_people p ON p.id=t.debt_person_id LEFT JOIN accounts d ON d.id=t.destination_account_id WHERE t.id=?",
                new String[]{String.valueOf(tx.id)})) {
            if (!c.moveToFirst()) return;
            tx.specialKind = c.isNull(0) ? null : c.getString(0);
            tx.debtPersonId = c.isNull(1) ? null : c.getLong(1);
            tx.debtPersonName = c.isNull(2) ? null : c.getString(2);
            tx.debtDirection = c.isNull(3) ? null : c.getString(3);
            tx.destinationAmountMinor = c.isNull(4) ? null : c.getLong(4);
            tx.destinationCurrency = c.isNull(5) ? null : c.getString(5);
            tx.exchangeRate = c.isNull(6) ? null : c.getString(6);
            tx.exchangeRateSource = c.isNull(7) ? null : c.getString(7);
        }
    }

    /** Names remain unique even across archived categories, whose history is preserved. */
    private static void requireUniqueCategoryName(SQLiteDatabase db, String name,
                                                   String kind, long excludedId) {
        try (Cursor existing=db.rawQuery(
                "SELECT 1 FROM categories WHERE kind=? AND name=? COLLATE NOCASE AND id!=? LIMIT 1",
                new String[]{kind,name,String.valueOf(excludedId)})) {
            if(existing.moveToFirst())
                throw new IllegalArgumentException("يوجد تصنيف آخر بهذا الاسم، اختر اسمًا مختلفًا");
        }
    }

    public long addCustomCategory(String name, String kind, String emoji) {
        String cleanName=CategoryNamingRules.validateNewCategory(name,kind);
        String cleanEmoji=emoji==null||emoji.trim().isEmpty()?"🏷️":emoji.trim();
        if(cleanEmoji.codePointCount(0,cleanEmoji.length())>8)
            throw new IllegalArgumentException("الرمز طويل جدًا");
        SQLiteDatabase writable=getWritableDatabase();
        writable.beginTransaction();
        try {
            requireUniqueCategoryName(writable,cleanName,kind,-1L);
            int position=100;
            try (Cursor c=writable.rawQuery(
                    "SELECT COALESCE(MAX(position),0)+1 FROM categories WHERE kind=?",
                    new String[]{kind})) {
                if(c.moveToFirst())position=c.getInt(0);
            }
            ContentValues cv=new ContentValues();
            cv.put("name",cleanName);cv.put("kind",kind);cv.put("emoji",cleanEmoji);
            cv.put("position",position);cv.put("exclude_from_expense",0);
            long id=writable.insertOrThrow("categories",null,cv);
            writable.setTransactionSuccessful();
            return id;
        } finally { writable.endTransaction(); }
    }

    public void renameCategory(long categoryId, String newName) {
        String clean=CategoryNamingRules.cleanName(newName);
        SQLiteDatabase writable=getWritableDatabase();
        writable.beginTransaction();
        try {
            String oldName,kind;
            boolean excluded;
            try(Cursor c=writable.rawQuery(
                    "SELECT name,kind,exclude_from_expense FROM categories WHERE id=? AND archived=0",
                    new String[]{String.valueOf(categoryId)})) {
                if(!c.moveToFirst())throw new IllegalArgumentException("التصنيف غير موجود أو مؤرشف");
                oldName=c.getString(0);kind=c.getString(1);excluded=c.getInt(2)!=0;
            }
            if(oldName.equals(clean)) { writable.setTransactionSuccessful(); return; }
            CategoryNamingRules.validateRename(oldName,kind,excluded,clean);
            requireUniqueCategoryName(writable,clean,kind,categoryId);
            ContentValues updated=new ContentValues();
            updated.put("name",clean);
            if(writable.update("categories",updated,"id=? AND archived=0",
                    new String[]{String.valueOf(categoryId)})!=1)
                throw new IllegalStateException("تعذّر تغيير اسم التصنيف");
            // Existing category audit trigger records the change. Transactions retain their
            // category ID and amount, so balances, debts and old historical rows are untouched.
            writable.setTransactionSuccessful();
        } finally { writable.endTransaction(); }
    }

    public Long getAccountBalance(String name, String currency) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT balance_minor FROM accounts WHERE active=1 AND name=? AND currency=? ORDER BY position,id LIMIT 1",
                new String[]{name, currency})) {
            return c.moveToFirst() ? c.getLong(0) : null;
        }
    }

    public void setAccountBalance(long accountId, long balanceMinor) {
        NonNegativeBalanceRules.requireNonNegative(balanceMinor);
        ContentValues cv = new ContentValues();
        cv.put("balance_minor", balanceMinor);
        if (getWritableDatabase().update("accounts", cv, "id=? AND active=1",
                new String[]{String.valueOf(accountId)}) != 1)
            throw new IllegalArgumentException("الحساب غير موجود");
        BalanceWidgetProvider.updateAll(appContext);
    }

    public void moveAccount(long accountId, int direction) {
        if (direction != -1 && direction != 1) return;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            long otherId = -1;
            int currentPosition = 0;
            int otherPosition = 0;
            try (Cursor current = db.rawQuery("SELECT position FROM accounts WHERE id=? AND active=1", new String[]{String.valueOf(accountId)})) {
                if (!current.moveToFirst()) return;
                currentPosition = current.getInt(0);
            }
            String compare = direction < 0 ? "<" : ">";
            String order = direction < 0 ? "DESC" : "ASC";
            String sql = "SELECT id,position FROM accounts WHERE active=1 AND position" + compare + "? ORDER BY position " + order + ",id " + order + " LIMIT 1";
            try (Cursor other = db.rawQuery(sql, new String[]{String.valueOf(currentPosition)})) {
                if (!other.moveToFirst()) return;
                otherId = other.getLong(0); otherPosition = other.getInt(1);
            }
            ContentValues first = new ContentValues(); first.put("position", otherPosition);
            ContentValues second = new ContentValues(); second.put("position", currentPosition);
            db.update("accounts", first, "id=?", new String[]{String.valueOf(accountId)});
            db.update("accounts", second, "id=?", new String[]{String.valueOf(otherId)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public boolean removeAccount(long accountId) {
        SQLiteDatabase db = getWritableDatabase();
        Models.Account account = getAccount(accountId);
        if (account == null) throw new IllegalArgumentException("الحساب غير موجود");
        if (account.balanceMinor != 0) throw new IllegalArgumentException("يجب أن يكون رصيد الحساب صفرًا قبل حذفه");
        long references = 0;
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM transactions WHERE account_id=? OR destination_account_id=?", new String[]{String.valueOf(accountId), String.valueOf(accountId)})) {
            if (c.moveToFirst()) references = c.getLong(0);
        }
        if (references == 0) {
            db.delete("accounts", "id=?", new String[]{String.valueOf(accountId)});
            BalanceWidgetProvider.updateAll(appContext);
            return false;
        }
        ContentValues cv = new ContentValues(); cv.put("active", 0);
        db.update("accounts", cv, "id=?", new String[]{String.valueOf(accountId)});
        BalanceWidgetProvider.updateAll(appContext);
        return true;
    }

    public long addTransaction(String type, long amountMinor, String currency, long accountId,
                               Long destinationId, Long categoryId, String note,
                               List<Models.SplitInput> splits, boolean affectsBalance) {
        if (amountMinor <= 0) throw new IllegalArgumentException("اكتب مبلغًا صحيحًا");
        if (!FinanceRules.EXPENSE.equals(type) && !FinanceRules.INCOME.equals(type)
                && !FinanceRules.TRANSFER.equals(type))
            throw new IllegalArgumentException("نوع العملية غير صحيح");
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            Models.Account source = getAccount(accountId);
            if (source == null || !isActiveAccount(db, accountId)) throw new IllegalArgumentException("الحساب غير موجود أو غير نشط");
            if (!source.currency.equals(currency)) throw new IllegalArgumentException("عملة الحساب لا تطابق العملية");
            Models.Account destination = destinationId == null ? null : getAccount(destinationId);
            if (FinanceRules.TRANSFER.equals(type)) {
                if (destination == null || !isActiveAccount(db, destination.id) || destination.id == source.id) throw new IllegalArgumentException("اختر حسابًا مستلمًا مختلفًا");
                if (!destination.currency.equals(source.currency)) throw new IllegalArgumentException("التحويل يدعم نفس العملة فقط حاليًا");
                if (categoryId != null || (splits != null && !splits.isEmpty()))
                    throw new IllegalArgumentException("التحويل لا يقبل تصنيفًا أو تقسيمًا");
            } else {
                if (destinationId != null) throw new IllegalArgumentException("الحساب المستلم متاح للتحويل فقط");
                if (splits != null && !splits.isEmpty()) {
                    if (!FinanceRules.EXPENSE.equals(type))
                        throw new IllegalArgumentException("التقسيم متاح للمصروف فقط");
                    SplitDebtRules.checkParts(amountMinor, splits);
                    for (Models.SplitInput split : splits) {
                        if (split.specialKind == null) requireCategoryForType(db, split.categoryId, type);
                        else { validateDebtDirection(split.debtDirection); requireDebtPerson(split.debtPersonId); }
                    }
                    categoryId = null;
                } else {
                    if (categoryId == null) throw new IllegalArgumentException("اختر تصنيف العملية");
                    requireCategoryForType(db, categoryId, type);
                }
            }

            long createdAt = System.currentTimeMillis();
            ContentValues tx = new ContentValues();
            tx.put("type", type); tx.put("amount_minor", amountMinor); tx.put("currency", currency);
            tx.put("account_id", accountId);
            if (destinationId == null) tx.putNull("destination_account_id"); else tx.put("destination_account_id", destinationId);
            if (categoryId == null || (splits != null && !splits.isEmpty())) tx.putNull("category_id"); else tx.put("category_id", categoryId);
            tx.put("note", note == null ? "" : note.trim()); tx.putNull("raw_text"); tx.put("created_at", createdAt); tx.put("source", "MANUAL");
            tx.put("affects_balance", affectsBalance ? 1 : 0);
            tx.put("base_affects_balance", affectsBalance ? 1 : 0);
            long txId = db.insertOrThrow("transactions", null, tx);

            if (splits != null) {
                for (Models.SplitInput split : splits) {
                    ContentValues part = new ContentValues();
                    part.put("transaction_id", txId); part.put("amount_minor", split.amountMinor);
                    if (split.specialKind == null) part.put("category_id", split.categoryId);
                    else {
                        part.putNull("category_id"); part.put("special_kind", split.specialKind);
                        part.put("debt_person_id", split.debtPersonId); part.put("debt_direction", split.debtDirection);
                    }
                    db.insertOrThrow("transaction_splits", null, part);
                }
            }

            Map<String, Models.SplitInput> groupedDebt = new LinkedHashMap<>();
            if (splits != null) for (Models.SplitInput split : splits) if (split.specialKind != null) {
                String key = split.specialKind + ":" + split.debtDirection + ":" + split.debtPersonId;
                Models.SplitInput grouped = groupedDebt.get(key);
                if (grouped == null) groupedDebt.put(key, Models.SplitInput.debt(
                        split.amountMinor, split.specialKind, split.debtPersonId, split.debtDirection));
                else grouped.amountMinor = Math.addExact(grouped.amountMinor, split.amountMinor);
            }
            for (int stage = 0; stage < 2; stage++) for (Models.SplitInput split : groupedDebt.values()) {
                if ((stage == 0) != FinanceRules.SPECIAL_DEBT.equals(split.specialKind)) continue;
                if (stage == 0) {
                    long debtId = findOrCreateOpenDebt(db, split.debtPersonId, split.debtDirection,
                            currency, note, createdAt);
                    insertDebtMovement(db, debtId, txId, "OPEN", split.amountMinor, createdAt,
                            "جزء من عملية مقسمة");
                    recomputeDebt(db, debtId);
                } else allocatePayment(db, split.debtPersonId, split.debtDirection, currency,
                        txId, split.amountMinor, createdAt, "سداد من عملية مقسمة");
            }

            if (affectsBalance) {
                long balanceImpact = FinanceRules.EXPENSE.equals(type)
                        ? expenseBalanceImpact(db, amountMinor, categoryId, splits) : amountMinor;
                applyBalanceEffect(db, type, balanceImpact, source.id, destination == null ? null : destination.id);
            }
            db.setTransactionSuccessful();
            return txId;
        } finally {
            db.endTransaction();
            BalanceWidgetProvider.updateAll(appContext);
            BudgetAlertManager.checkAll(appContext);
        }
    }

    /** Creates the transaction and its debt-ledger movement in one SQLite transaction. */
    public long addDebtTransaction(String type, long amountMinor, String currency, long accountId,
                                   String note, boolean baseAffectsBalance, String specialKind,
                                   long personId, String direction) {
        if (amountMinor <= 0) throw new IllegalArgumentException("اكتب مبلغًا صحيحًا");
        if (!FinanceRules.EXPENSE.equals(type) && !FinanceRules.INCOME.equals(type))
            throw new IllegalArgumentException("حركة الدين يجب أن تكون مصروفًا أو دخلًا");
        validateDebtDirection(direction); requireDebtPerson(personId);
        String actualKind = DebtLedgerRules.movementKind(type, direction);
        if (!actualKind.equals(specialKind))
            throw new IllegalArgumentException("اتجاه الدين لا يطابق نوع الحركة المالية");

        SQLiteDatabase writable = getWritableDatabase();
        writable.beginTransaction();
        try {
            Models.Account account = getAccount(accountId);
            if (account == null || !isActiveAccount(writable, accountId) || !currency.equals(account.currency))
                throw new IllegalArgumentException("الحساب غير صالح لهذه العملة");
            long createdAt = System.currentTimeMillis();
            boolean affectsBalance = FinanceRules.SPECIAL_DEBT_PAYMENT.equals(actualKind) && baseAffectsBalance;
            ContentValues tx = new ContentValues();
            tx.put("type", type); tx.put("amount_minor", amountMinor); tx.put("currency", currency);
            tx.put("account_id", accountId); tx.putNull("destination_account_id"); tx.putNull("category_id");
            tx.put("note", note == null ? "" : note.trim()); tx.putNull("raw_text");
            tx.put("created_at", createdAt); tx.put("source", "MANUAL");
            tx.put("affects_balance", affectsBalance ? 1 : 0);
            tx.put("base_affects_balance", baseAffectsBalance ? 1 : 0);
            tx.put("special_kind", actualKind); tx.put("debt_person_id", personId); tx.put("debt_direction", direction);
            long txId = writable.insertOrThrow("transactions", null, tx);
            if (affectsBalance) applyBalanceEffect(writable, type, amountMinor, accountId, null);
            if (FinanceRules.SPECIAL_DEBT.equals(actualKind)) {
                long debtId = findOrCreateOpenDebt(writable, personId, direction, currency, note, createdAt);
                insertDebtMovement(writable, debtId, txId, "OPEN", amountMinor, createdAt, note);
                recomputeDebt(writable, debtId);
            } else allocatePayment(writable, personId, direction, currency, txId, amountMinor, createdAt, note);
            writable.setTransactionSuccessful();
            return txId;
        } finally {
            writable.endTransaction();
            BalanceWidgetProvider.updateAll(appContext);
            BudgetAlertManager.checkAll(appContext);
        }
    }

    /** New multi-currency transfer only; leaves all legacy same-currency transfers untouched. */
    public long addFxTransfer(long sourceId, long destinationId, long sourceAmountMinor,
                              String destUnitsPerSourceUnit, String rateSource, long checkedAt) {
        if (sourceId==destinationId) throw new IllegalArgumentException("اختر حسابين مختلفين");
        if (checkedAt<=0) throw new IllegalArgumentException("وقت التحقق من سعر الصرف غير صالح");
        String provenance=rateSource==null?"MANUAL":rateSource.trim();
        if (!"MANUAL".equals(provenance) && !"BOP_INDICATIVE_BUY_USER_CONFIRMED".equals(provenance))
            throw new IllegalArgumentException("مصدر سعر الصرف غير معروف");
        SQLiteDatabase writable=getWritableDatabase();
        writable.beginTransaction();
        try {
            Models.Account source=getAccount(sourceId), destination=getAccount(destinationId);
            if(source==null || destination==null || !isActiveAccount(writable,sourceId)
                    || !isActiveAccount(writable,destinationId))throw new IllegalArgumentException("الحساب غير متاح");
            if(source.currency.equals(destination.currency))throw new IllegalArgumentException("التحويل بنفس العملة متاح بالطريقة المعتادة");
            if("BOP_INDICATIVE_BUY_USER_CONFIRMED".equals(provenance)
                    && !("USD".equals(source.currency) && "ILS".equals(destination.currency)))
                throw new IllegalArgumentException("تسعيرة شراء الدولار من بنك فلسطين صالحة لمسار USD إلى ILS فقط");
            long destinationMinor=FxTransferMath.destinationMinor(sourceAmountMinor,destUnitsPerSourceUnit);
            long[] newBalances=FxTransferMath.balancesAfter(source.balanceMinor,destination.balanceMinor,
                    sourceAmountMinor,destinationMinor);
            NonNegativeBalanceRules.requireNonNegative(newBalances[0]); // reject source overdraft atomically
            ContentValues tx=new ContentValues();
            tx.put("type",FinanceRules.TRANSFER);tx.put("amount_minor",sourceAmountMinor);
            tx.put("currency",source.currency);tx.put("account_id",source.id);
            tx.put("destination_account_id",destination.id);
            tx.put("destination_amount_minor",destinationMinor);
            tx.put("exchange_rate",destUnitsPerSourceUnit.trim());
            tx.put("exchange_rate_source",provenance);
            tx.put("exchange_rate_checked_at",checkedAt);
            tx.put("note","");tx.put("created_at",System.currentTimeMillis());
            tx.put("source","MANUAL_FX");tx.put("affects_balance",1);tx.put("base_affects_balance",1);
            long id=writable.insertOrThrow("transactions",null,tx);
            ContentValues sourceUpdate=new ContentValues();sourceUpdate.put("balance_minor",newBalances[0]);
            if(writable.update("accounts",sourceUpdate,"id=? AND active=1",new String[]{String.valueOf(source.id)})!=1)
                throw new IllegalStateException("تعذر تحديث الحساب المصدر");
            ContentValues destinationUpdate=new ContentValues();destinationUpdate.put("balance_minor",newBalances[1]);
            if(writable.update("accounts",destinationUpdate,"id=? AND active=1",new String[]{String.valueOf(destination.id)})!=1)
                throw new IllegalStateException("تعذر تحديث الحساب المستلم");
            writable.setTransactionSuccessful();return id;
        } finally {
            writable.endTransaction();
            BalanceWidgetProvider.updateAll(appContext);
            BudgetAlertManager.checkAll(appContext);
        }
    }

    public long insertCapture(String provider, String channel, String sender, String rawText,
                              TransactionParser.Result parsed, String fingerprint, long receivedAt) {
        ContentValues cv = new ContentValues();
        cv.put("provider", provider); cv.put("channel", channel); cv.put("sender", sender); cv.put("raw_text", rawText);
        if (parsed.amountMinor == null) cv.putNull("amount_minor"); else cv.put("amount_minor", parsed.amountMinor);
        cv.put("currency", parsed.currency); cv.put("inferred_type", parsed.type); cv.put("confidence", parsed.confidence);
        cv.put("counterparty", parsed.counterparty);
        if (parsed.balanceMinor == null) cv.putNull("balance_minor"); else cv.put("balance_minor", parsed.balanceMinor);
        cv.put("status", "PENDING"); cv.put("fingerprint", fingerprint); cv.put("received_at", receivedAt);
        return getWritableDatabase().insertWithOnConflict("capture_inbox", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public boolean hasMatchingCapture(String provider, String channel, Long amountMinor, String currency, String type,
                                      String counterparty, Long balanceMinor, long receivedAt) {
        if (amountMinor == null || currency == null) return false;
        long window = 48 * 60 * 60_000L;
        String normalizedParty = counterparty == null ? "" : counterparty.trim().toLowerCase(Locale.ROOT);
        String sql = "SELECT channel,counterparty,inferred_type,balance_minor,received_at FROM capture_inbox WHERE provider=? AND currency=? AND amount_minor=? AND received_at BETWEEN ? AND ?";
        String[] args = {provider, currency, String.valueOf(amountMinor), String.valueOf(receivedAt - window), String.valueOf(receivedAt + window)};
        try (Cursor c = getReadableDatabase().rawQuery(sql, args)) {
            while (c.moveToNext()) {
                String otherChannel = c.getString(0); if (channel.equals(otherChannel)) continue;
                String otherParty = c.isNull(1) ? "" : c.getString(1).trim().toLowerCase(Locale.ROOT);
                String otherType = c.isNull(2) ? null : c.getString(2);
                Long otherBalance = c.isNull(3) ? null : c.getLong(3);
                long timeGap = Math.abs(receivedAt - c.getLong(4));
                boolean typeMatches = type == null || otherType == null || type.equals(otherType);
                if (!typeMatches) continue;
                if (!normalizedParty.isEmpty() && normalizedParty.equals(otherParty)) return true;
                if (balanceMinor != null && balanceMinor.equals(otherBalance)) return true;
                if (timeGap <= 10 * 60_000L) return true;
            }
        }
        return false;
    }

    public int getPendingCaptureCount() {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM capture_inbox WHERE status='PENDING'", null)) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    public List<Models.CaptureItem> getPendingCaptures() {
        List<Models.CaptureItem> result = new ArrayList<>();
        String sql = "SELECT id,provider,channel,sender,raw_text,amount_minor,currency,inferred_type,confidence,counterparty,balance_minor,received_at FROM capture_inbox WHERE status='PENDING' ORDER BY received_at DESC,id DESC";
        try (Cursor c = getReadableDatabase().rawQuery(sql, null)) {
            while (c.moveToNext()) result.add(captureFrom(c));
        }
        return result;
    }

    public Models.CaptureItem getCapture(long id) {
        String sql = "SELECT id,provider,channel,sender,raw_text,amount_minor,currency,inferred_type,confidence,counterparty,balance_minor,received_at FROM capture_inbox WHERE id=?";
        try (Cursor c = getReadableDatabase().rawQuery(sql, new String[]{String.valueOf(id)})) {
            return c.moveToFirst() ? captureFrom(c) : null;
        }
    }

    private Models.CaptureItem captureFrom(Cursor c) {
        Models.CaptureItem item = new Models.CaptureItem();
        item.id = c.getLong(0); item.provider = c.getString(1); item.channel = c.getString(2); item.sender = c.isNull(3) ? null : c.getString(3);
        item.rawText = c.getString(4); item.amountMinor = c.isNull(5) ? null : c.getLong(5); item.currency = c.isNull(6) ? null : c.getString(6);
        item.inferredType = c.isNull(7) ? null : c.getString(7); item.confidence = c.getInt(8); item.counterparty = c.isNull(9) ? null : c.getString(9);
        item.balanceMinor = c.isNull(10) ? null : c.getLong(10); item.receivedAt = c.getLong(11);
        return item;
    }

    public long importCapture(long captureId, String type) {
        if (!isCapturePending(captureId)) return -1;
        SQLiteDatabase writable = getWritableDatabase();
        writable.beginTransaction();
        try {
            Models.CaptureItem item = getCapture(captureId);
            if (item == null || item.amountMinor == null || item.currency == null ||
                    (!FinanceRules.EXPENSE.equals(type) && !FinanceRules.INCOME.equals(type))) return -1;
            long accountId = findProviderAccount(item.provider, item.currency);
            if (accountId < 0) return -1;
            Models.Account account = getAccount(accountId);
            ContentValues tx = new ContentValues();
            tx.put("type", type); tx.put("amount_minor", item.amountMinor); tx.put("currency", item.currency); tx.put("account_id", accountId);
            tx.putNull("destination_account_id"); tx.putNull("category_id"); tx.put("note", item.counterparty == null ? "" : item.counterparty);
            tx.put("raw_text", item.rawText); tx.put("needs_review", 1); tx.put("created_at", item.receivedAt); tx.put("source", "CAPTURED");
            tx.put("affects_balance", 1); tx.put("base_affects_balance", 1);
            long txId = writable.insertOrThrow("transactions", null, tx);
            long nextBalance = item.balanceMinor != null ? item.balanceMinor : FinanceRules.balancesAfter(type, item.amountMinor, account.balanceMinor, null)[0];
            NonNegativeBalanceRules.requireNonNegative(nextBalance); // reject overdraft and negative captured balance
            ContentValues balance = new ContentValues(); balance.put("balance_minor", nextBalance);
            writable.update("accounts", balance, "id=?", new String[]{String.valueOf(accountId)});
            ContentValues done = new ContentValues(); done.put("status", "IMPORTED"); done.put("transaction_id", txId);
            writable.update("capture_inbox", done, "id=? AND status='PENDING'", new String[]{String.valueOf(captureId)});
            writable.setTransactionSuccessful();
            return txId;
        } finally {
            writable.endTransaction();
            BalanceWidgetProvider.updateAll(appContext);
            BudgetAlertManager.checkAll(appContext);
        }
    }

    public long importCaptureTransfer(long captureId, long sourceId, long destinationId) {
        if (!isCapturePending(captureId)) return -1;
        Models.CaptureItem item = getCapture(captureId);
        if (item == null || item.amountMinor == null || item.currency == null) return -1;
        long txId = addCapturedTransfer(item, sourceId, destinationId);
        ContentValues done = new ContentValues(); done.put("status", "IMPORTED"); done.put("transaction_id", txId);
        getWritableDatabase().update("capture_inbox", done, "id=? AND status='PENDING'", new String[]{String.valueOf(captureId)});
        return txId;
    }

    private long addCapturedTransfer(Models.CaptureItem item, long sourceId, long destinationId) {
        SQLiteDatabase writable = getWritableDatabase();
        writable.beginTransaction();
        try {
            Models.Account source = getAccount(sourceId), destination = getAccount(destinationId);
            if (source == null || destination == null || source.id == destination.id || !source.currency.equals(item.currency) || !destination.currency.equals(item.currency)) return -1;
            ContentValues tx = new ContentValues(); tx.put("type", FinanceRules.TRANSFER); tx.put("amount_minor", item.amountMinor); tx.put("currency", item.currency);
            tx.put("account_id", sourceId); tx.put("destination_account_id", destinationId); tx.putNull("category_id"); tx.put("note", ""); tx.put("raw_text", item.rawText);
            tx.put("needs_review", 0); tx.put("created_at", item.receivedAt); tx.put("source", "CAPTURED");
            tx.put("affects_balance", 1); tx.put("base_affects_balance", 1);
            long id = writable.insertOrThrow("transactions", null, tx);
            long[] balances = FinanceRules.balancesAfter(FinanceRules.TRANSFER, item.amountMinor, source.balanceMinor, destination.balanceMinor);
            NonNegativeBalanceRules.requireNonNegative(balances[0]); // inbound capture cannot overdraw source
            ContentValues a = new ContentValues(); a.put("balance_minor", balances[0]); writable.update("accounts", a, "id=?", new String[]{String.valueOf(sourceId)});
            ContentValues b = new ContentValues(); b.put("balance_minor", balances[1]); writable.update("accounts", b, "id=?", new String[]{String.valueOf(destinationId)});
            writable.setTransactionSuccessful(); return id;
        } finally {
            writable.endTransaction();
            BalanceWidgetProvider.updateAll(appContext);
            BudgetAlertManager.checkAll(appContext);
        }
    }

    private long findProviderAccount(String provider, String currency) {
        String name = "BOP".equals(provider) ? "بنك فلسطين" : "JAWAL".equals(provider) ? "Jawwal Pay" : "PALPAY".equals(provider) ? "PalPay" : null;
        if (name == null) return -1;
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id FROM accounts WHERE active=1 AND name=? AND currency=? ORDER BY position,id LIMIT 1", new String[]{name, currency})) {
            return c.moveToFirst() ? c.getLong(0) : -1;
        }
    }

    public void ignoreCapture(long captureId) {
        ContentValues cv = new ContentValues(); cv.put("status", "IGNORED");
        getWritableDatabase().update("capture_inbox", cv, "id=?", new String[]{String.valueOf(captureId)});
    }

    private boolean isCapturePending(long captureId) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT 1 FROM capture_inbox WHERE id=? AND status='PENDING'", new String[]{String.valueOf(captureId)})) {
            return c.moveToFirst();
        }
    }

    public void updateTransactionCategory(long transactionId, long categoryId) {
        String type; long accountId;
        try (Cursor c = getReadableDatabase().rawQuery("SELECT type,account_id FROM transactions WHERE id=?",
                new String[]{String.valueOf(transactionId)})) {
            if (!c.moveToFirst()) throw new IllegalArgumentException("العملية غير موجودة");
            type = c.getString(0); accountId = c.getLong(1);
        }
        reclassifyTransaction(transactionId, type, accountId, null, categoryId, new ArrayList<>());
    }

    public void convertTransactionToTransfer(long transactionId, long sourceId, long destinationId) {
        reclassifyTransaction(transactionId, FinanceRules.TRANSFER, sourceId, destinationId, null, new ArrayList<>());
    }

    public void reclassifyTransaction(long transactionId, String newType, long sourceId,
                                      Long destinationId, Long categoryId,
                                      List<Models.SplitInput> splits) {
        SQLiteDatabase writable = getWritableDatabase();
        writable.beginTransaction();
        try {
            // The generic category/type editor cannot safely reverse multi-currency
            // exchange or debt ledger transactions. Route these to
            // their dedicated flows; never silently orphan the related records.
            try(Cursor restricted=writable.rawQuery(
                    "SELECT 1 FROM transactions t WHERE t.id=? AND (" +
                    "t.destination_amount_minor IS NOT NULL OR t.exchange_rate IS NOT NULL " +
                    "OR t.special_kind IS NOT NULL OR t.debt_person_id IS NOT NULL " +
                    "OR t.debt_direction IS NOT NULL " +
                    "OR EXISTS(SELECT 1 FROM debt_movements d WHERE d.transaction_id=t.id))",
                    new String[]{String.valueOf(transactionId)})) {
                if(restricted.moveToFirst())
                    throw new IllegalArgumentException("العملية مرتبطة بتحويل أو دين؛ استخدم تعديلها المخصص");
            }
            String oldType; long amount; long oldSourceId; Long oldDestinationId; String currency; boolean affectsBalance; boolean baseAffectsBalance;
            try (Cursor c = writable.rawQuery("SELECT type,amount_minor,account_id,destination_account_id,currency,affects_balance,base_affects_balance FROM transactions WHERE id=?", new String[]{String.valueOf(transactionId)})) {
                if (!c.moveToFirst()) throw new IllegalArgumentException("العملية غير موجودة");
                oldType = c.getString(0); amount = c.getLong(1); oldSourceId = c.getLong(2); oldDestinationId = c.isNull(3) ? null : c.getLong(3); currency = c.getString(4); affectsBalance = c.getInt(5) == 1; baseAffectsBalance = c.getInt(6) == 1;
            }
            if (!FinanceRules.EXPENSE.equals(newType) && !FinanceRules.INCOME.equals(newType) && !FinanceRules.TRANSFER.equals(newType))
                throw new IllegalArgumentException("نوع العملية غير صحيح");
            Models.Account source = getAccount(sourceId);
            if (source == null || !currency.equals(source.currency) ||
                    (sourceId != oldSourceId && !isActiveAccount(writable, sourceId)))
                throw new IllegalArgumentException("الحساب المصدر غير صالح أو غير نشط");
            Models.Account destination = destinationId == null ? null : getAccount(destinationId);
            if (FinanceRules.TRANSFER.equals(newType)) {
                if (destination == null || source.id == destination.id ||
                        (destinationId != null && !destinationId.equals(oldDestinationId)
                                && !isActiveAccount(writable, destinationId)))
                    throw new IllegalArgumentException("اختر حسابين نشطين ومختلفين");
                if (!currency.equals(destination.currency)) throw new IllegalArgumentException("الحسابان يجب أن يكونا بعملة " + currency);
                categoryId = null;
                splits = new ArrayList<>();
            } else {
                destinationId = null;
                if (splits != null && !splits.isEmpty()) {
                    if (!FinanceRules.EXPENSE.equals(newType)) throw new IllegalArgumentException("التقسيم متاح للمصروف فقط");
                    SplitDebtRules.checkParts(amount, splits);
                    for (Models.SplitInput split : splits) {
                        if (split.specialKind == null)
                            requireCategoryForType(writable, split.categoryId, newType);
                        else {
                            validateDebtDirection(split.debtDirection);
                            requireDebtPerson(split.debtPersonId);
                        }
                    }
                    categoryId = null;
                } else {
                    if (categoryId == null) throw new IllegalArgumentException("اختر تصنيف العملية");
                    requireCategoryForType(writable, categoryId, newType);
                }
            }

            try(Cursor fx=writable.rawQuery("SELECT destination_amount_minor FROM transactions WHERE id=?",
                    new String[]{String.valueOf(transactionId)})) {
                if(fx.moveToFirst() && !fx.isNull(0))
                    throw new IllegalArgumentException("التحويل متعدد العملات: احذفه بعد التأكد وأضف تحويلًا جديدًا لتغيير المبلغ أو الحسابات");
            }
            long oldBalanceImpact = FinanceRules.EXPENSE.equals(oldType)
                    ? existingExpenseBalanceImpact(writable, transactionId, amount) : amount;
            ensureDebtOpeningCanBeRemoved(writable, transactionId);
            if (affectsBalance) reverseBalanceEffect(writable, oldType, oldBalanceImpact, oldSourceId, oldDestinationId);
            List<Long> affectedDebts = removeDebtMovementsForTransaction(writable, transactionId);
            // A fully repaid debt must reopen before a revised PAYMENT can be allocated to it.
            for (Long debtId : affectedDebts) recomputeDebt(writable, debtId);
            writable.delete("transaction_splits", "transaction_id=?", new String[]{String.valueOf(transactionId)});
            ContentValues cv = new ContentValues(); cv.put("type", newType); cv.put("account_id", sourceId);
            if (destinationId == null) cv.putNull("destination_account_id"); else cv.put("destination_account_id", destinationId);
            if (categoryId == null) cv.putNull("category_id"); else cv.put("category_id", categoryId);
            cv.putNull("special_kind"); cv.putNull("debt_person_id"); cv.putNull("debt_direction");
            cv.put("affects_balance", baseAffectsBalance ? 1 : 0);
            cv.put("needs_review", 0);
            writable.update("transactions", cv, "id=?", new String[]{String.valueOf(transactionId)});
            if (splits != null) for (Models.SplitInput split : splits) {
                ContentValues part = new ContentValues();
                part.put("transaction_id", transactionId); part.put("amount_minor", split.amountMinor);
                if (split.specialKind == null) part.put("category_id", split.categoryId);
                else { part.putNull("category_id"); part.put("special_kind", split.specialKind);
                    part.put("debt_person_id", split.debtPersonId); part.put("debt_direction", split.debtDirection); }
                writable.insertOrThrow("transaction_splits", null, part);
            }
            // Several split lines for the same person may share a single debt record.
            // Coalesce them before posting; debt_movements has UNIQUE(transaction_id,debt_id,kind).
            Map<String, Models.SplitInput> groupedDebt = new LinkedHashMap<>();
            if (splits != null) for (Models.SplitInput split : splits) if (split.specialKind != null) {
                String key = split.specialKind + ":" + split.debtDirection + ":" + split.debtPersonId;
                Models.SplitInput item = groupedDebt.get(key);
                if (item == null) groupedDebt.put(key, Models.SplitInput.debt(split.amountMinor, split.specialKind, split.debtPersonId, split.debtDirection));
                else item.amountMinor = Math.addExact(item.amountMinor, split.amountMinor);
            }
            // Debt OPEN first, then payments; every action is in the same database transaction.
            for (int stage=0; stage<2; stage++) for (Models.SplitInput split : groupedDebt.values()) {
                if ((stage == 0) != FinanceRules.SPECIAL_DEBT.equals(split.specialKind)) continue;
                if (stage == 0) {
                    long debtId = findOrCreateOpenDebt(writable, split.debtPersonId, split.debtDirection,
                            currency, "جزء من: " + transactionId, System.currentTimeMillis());
                    insertDebtMovement(writable, debtId, transactionId, "OPEN", split.amountMinor,
                            System.currentTimeMillis(), "جزء من عملية مقسمة");
                    recomputeDebt(writable, debtId);
                } else {
                    allocatePayment(writable, split.debtPersonId, split.debtDirection, currency, transactionId,
                            split.amountMinor, System.currentTimeMillis(), "سداد من عملية مقسمة");
                }
            }
            if (baseAffectsBalance) {
                long newBalanceImpact = FinanceRules.EXPENSE.equals(newType)
                        ? expenseBalanceImpact(writable, amount, categoryId, splits) : amount;
                applyBalanceEffect(writable, newType, newBalanceImpact, sourceId, destinationId);
            }
            for (Long debtId : affectedDebts) recomputeDebt(writable, debtId);
            writable.setTransactionSuccessful();
        } finally {
            writable.endTransaction();
            BalanceWidgetProvider.updateAll(appContext);
            BudgetAlertManager.checkAll(appContext);
        }
    }

    public long addDebtPerson(String name) {
        String clean = name == null ? "" : name.trim().replaceAll("\\s+", " ");
        if (clean.length() < 2) throw new IllegalArgumentException("اكتب اسم الشخص");
        SQLiteDatabase writable = getWritableDatabase();
        try (Cursor c = writable.rawQuery("SELECT id FROM debt_people WHERE name=?", new String[]{clean})) {
            if (c.moveToFirst()) {
                ContentValues active = new ContentValues(); active.put("active", 1);
                writable.update("debt_people", active, "id=?", new String[]{String.valueOf(c.getLong(0))});
                return c.getLong(0);
            }
        }
        ContentValues values = new ContentValues(); values.put("name", clean); values.put("active", 1);
        values.put("created_at", System.currentTimeMillis());
        return writable.insertOrThrow("debt_people", null, values);
    }

    public List<Models.DebtPerson> getDebtPeople() {
        List<Models.DebtPerson> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT id,name,active FROM debt_people WHERE active=1 ORDER BY name COLLATE NOCASE", null)) {
            while (c.moveToNext()) {
                Models.DebtPerson person = new Models.DebtPerson();
                person.id = c.getLong(0); person.name = c.getString(1); person.active = c.getInt(2) == 1;
                result.add(person);
            }
        }
        return result;
    }

    public List<Models.DebtPerson> getDebtPeopleWithOpenBalance(String direction, String currency) {
        validateDebtDirection(direction);
        List<Models.DebtPerson> result = new ArrayList<>();
        String sql = "SELECT p.id,p.name,p.active FROM debt_people p JOIN debts d ON d.person_id=p.id " +
                "JOIN debt_movements m ON m.debt_id=d.id WHERE p.active=1 AND d.direction=? AND d.currency=? AND d.closed_at IS NULL " +
                "GROUP BY p.id,p.name,p.active HAVING SUM(CASE WHEN m.kind='OPEN' THEN m.amount_minor ELSE -m.amount_minor END)>0 ORDER BY p.name COLLATE NOCASE";
        try (Cursor c = getReadableDatabase().rawQuery(sql, new String[]{direction, currency})) {
            while (c.moveToNext()) { Models.DebtPerson person=new Models.DebtPerson(); person.id=c.getLong(0); person.name=c.getString(1); person.active=c.getInt(2)==1; result.add(person); }
        }
        return result;
    }

    public void renameDebtPerson(long personId, String name) {
        String clean = name == null ? "" : name.trim().replaceAll("\\s+", " ");
        if (clean.length() < 2) throw new IllegalArgumentException("اكتب اسم الشخص");
        ContentValues values = new ContentValues(); values.put("name", clean);
        if (getWritableDatabase().update("debt_people", values, "id=? AND active=1",
                new String[]{String.valueOf(personId)}) != 1) throw new IllegalArgumentException("الشخص غير موجود");
    }

    public void removeDebtPerson(long personId) {
        long references = 0;
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM debts WHERE person_id=?",
                new String[]{String.valueOf(personId)})) { if (c.moveToFirst()) references = c.getLong(0); }
        if (references > 0) throw new IllegalArgumentException("لا يمكن حذف شخص له سجل ديون؛ يمكنك تعديل اسمه");
        getWritableDatabase().delete("debt_people", "id=?", new String[]{String.valueOf(personId)});
    }

    public long createManualDebt(long personId, String direction, String currency, long amountMinor, String note) {
        if (amountMinor <= 0) throw new IllegalArgumentException("اكتب مبلغًا صحيحًا");
        validateDebtDirection(direction); requireDebtPerson(personId);
        SQLiteDatabase writable = getWritableDatabase(); writable.beginTransaction();
        try {
            long debtId = findOrCreateOpenDebt(writable, personId, direction, currency, note, System.currentTimeMillis());
            insertDebtMovement(writable, debtId, null, "OPEN", amountMinor, System.currentTimeMillis(), note);
            recomputeDebt(writable, debtId); writable.setTransactionSuccessful(); return debtId;
        } finally { writable.endTransaction(); }
    }

    public List<Models.DebtSummary> getDebtSummaries(String direction, boolean closed) {
        validateDebtDirection(direction);
        List<Models.DebtSummary> result = new ArrayList<>();
        String condition = closed ? "d.closed_at IS NOT NULL" : "d.closed_at IS NULL";
        String sql = "SELECT d.id,d.person_id,p.name,d.direction,d.currency,d.created_at,d.updated_at,d.closed_at," +
                "COALESCE(SUM(CASE WHEN m.kind='OPEN' THEN m.amount_minor ELSE 0 END),0)," +
                "COALESCE(SUM(CASE WHEN m.kind IN ('PAYMENT','MANUAL_PAYMENT') THEN m.amount_minor ELSE 0 END),0)," +
                "COALESCE(MAX(m.created_at),d.updated_at) FROM debts d JOIN debt_people p ON p.id=d.person_id " +
                "LEFT JOIN debt_movements m ON m.debt_id=d.id WHERE d.direction=? AND " + condition +
                " GROUP BY d.id,d.person_id,p.name,d.direction,d.currency,d.created_at,d.updated_at,d.closed_at " +
                "ORDER BY COALESCE(MAX(m.created_at),d.updated_at) DESC,d.id DESC";
        try (Cursor c = getReadableDatabase().rawQuery(sql, new String[]{direction})) {
            while (c.moveToNext()) {
                Models.DebtSummary item = new Models.DebtSummary();
                item.id = c.getLong(0); item.personId = c.getLong(1); item.personName = c.getString(2);
                item.direction = c.getString(3); item.currency = c.getString(4); item.createdAt = c.getLong(5);
                item.lastMovementAt = c.getLong(10); item.originalMinor = c.getLong(8); item.paidMinor = c.getLong(9);
                item.remainingMinor = Math.max(0L, item.originalMinor - item.paidMinor); item.closed = !c.isNull(7);
                result.add(item);
            }
        }
        return result;
    }

    public List<Models.DebtSummary> getDebtSummariesForPerson(long personId, String direction, boolean closed) {
        validateDebtDirection(direction);
        List<Models.DebtSummary> result = new ArrayList<>();
        String condition = closed ? "d.closed_at IS NOT NULL" : "d.closed_at IS NULL";
        String sql = "SELECT d.id,d.person_id,p.name,d.direction,d.currency,d.created_at,d.updated_at,d.closed_at," +
                "COALESCE(SUM(CASE WHEN m.kind='OPEN' THEN m.amount_minor ELSE 0 END),0)," +
                "COALESCE(SUM(CASE WHEN m.kind IN ('PAYMENT','MANUAL_PAYMENT') THEN m.amount_minor ELSE 0 END),0)," +
                "COALESCE(MAX(m.created_at),d.updated_at) FROM debts d JOIN debt_people p ON p.id=d.person_id " +
                "LEFT JOIN debt_movements m ON m.debt_id=d.id WHERE d.person_id=? AND d.direction=? AND " + condition +
                " GROUP BY d.id,d.person_id,p.name,d.direction,d.currency,d.created_at,d.updated_at,d.closed_at " +
                "ORDER BY COALESCE(MAX(m.created_at),d.updated_at) DESC,d.id DESC";
        try (Cursor c = getReadableDatabase().rawQuery(sql, new String[]{String.valueOf(personId), direction})) {
            while (c.moveToNext()) result.add(debtSummaryFrom(c));
        }
        return result;
    }

    private Models.DebtSummary debtSummaryFrom(Cursor c) {
        Models.DebtSummary item = new Models.DebtSummary();
        item.id = c.getLong(0); item.personId = c.getLong(1); item.personName = c.getString(2);
        item.direction = c.getString(3); item.currency = c.getString(4); item.createdAt = c.getLong(5);
        item.lastMovementAt = c.getLong(10); item.originalMinor = c.getLong(8); item.paidMinor = c.getLong(9);
        item.remainingMinor = Math.max(0L, item.originalMinor - item.paidMinor); item.closed = !c.isNull(7);
        return item;
    }

    public List<Models.DebtMovement> getDebtMovements(long debtId) {
        List<Models.DebtMovement> result = new ArrayList<>();
        // Read-only provenance: a split portion must link back to the original
        // financial operation, including its full total and note.
        String sql="SELECT m.id,m.debt_id,m.transaction_id,m.kind,m.amount_minor,m.created_at,m.note," +
                "(SELECT COUNT(*) FROM transaction_splits s WHERE s.transaction_id=m.transaction_id)," +
                "t.amount_minor,t.note FROM debt_movements m " +
                "LEFT JOIN transactions t ON t.id=m.transaction_id " +
                "WHERE m.debt_id=? ORDER BY m.created_at DESC,m.id DESC";
        try (Cursor c = getReadableDatabase().rawQuery(sql,new String[]{String.valueOf(debtId)})) {
            while (c.moveToNext()) {
                Models.DebtMovement item = new Models.DebtMovement();
                item.id=c.getLong(0); item.debtId=c.getLong(1);
                item.transactionId=c.isNull(2)?null:c.getLong(2);
                item.kind=c.getString(3); item.amountMinor=c.getLong(4);
                item.createdAt=c.getLong(5); item.note=c.getString(6);
                item.linkedSplitPartCount=c.getInt(7);
                item.fromSplitTransaction=item.transactionId!=null && item.linkedSplitPartCount>0;
                item.linkedTransactionAmountMinor=c.isNull(8)?null:c.getLong(8);
                item.linkedTransactionNote=c.isNull(9)?null:c.getString(9);
                result.add(item);
            }
        }
        return result;
    }

    /** How many original financial operations would disappear if this entire debt is deleted. */
    public int countDebtLinkedFinancialTransactions(long debtId) {
        try(Cursor c=getReadableDatabase().rawQuery(
                "SELECT COUNT(DISTINCT transaction_id) FROM debt_movements " +
                "WHERE debt_id=? AND transaction_id IS NOT NULL",
                new String[]{String.valueOf(debtId)})) {
            return c.moveToFirst()?c.getInt(0):0;
        }
    }

    public void markDebtPaid(long debtId) {
        SQLiteDatabase writable = getWritableDatabase(); writable.beginTransaction();
        try {
            long remaining = debtRemaining(writable, debtId);
            if (remaining <= 0) throw new IllegalArgumentException("الدين مسدد بالفعل");
            insertDebtMovement(writable, debtId, null, "MANUAL_PAYMENT", remaining, System.currentTimeMillis(), "تم السداد يدويًا");
            recomputeDebt(writable, debtId); writable.setTransactionSuccessful();
        } finally { writable.endTransaction(); }
    }

    public void addManualDebtPayment(long debtId, long amountMinor, String note) {
        if (amountMinor <= 0) throw new IllegalArgumentException("اكتب مبلغًا صحيحًا");
        SQLiteDatabase writable = getWritableDatabase(); writable.beginTransaction();
        try {
            long remaining = debtRemaining(writable, debtId);
            if (remaining <= 0) throw new IllegalArgumentException("الدين مسدد بالفعل");
            if (amountMinor > remaining) throw new IllegalArgumentException("المبلغ أكبر من المتبقي");
            insertDebtMovement(writable, debtId, null, "MANUAL_PAYMENT", amountMinor,
                    System.currentTimeMillis(), note == null || note.trim().isEmpty() ? "سداد جزئي يدوي" : note);
            recomputeDebt(writable, debtId); writable.setTransactionSuccessful();
        } finally { writable.endTransaction(); }
    }

    public void updateManualDebtMovement(long movementId, long amountMinor, String note) {
        if (amountMinor <= 0) throw new IllegalArgumentException("اكتب مبلغًا صحيحًا");
        SQLiteDatabase writable = getWritableDatabase(); writable.beginTransaction();
        try {
            long debtId; String kind;
            try (Cursor c = writable.rawQuery("SELECT debt_id,kind,transaction_id FROM debt_movements WHERE id=?",
                    new String[]{String.valueOf(movementId)})) {
                if (!c.moveToFirst()) throw new IllegalArgumentException("حركة الدين غير موجودة");
                debtId = c.getLong(0); kind = c.getString(1);
                if (!c.isNull(2)) throw new IllegalArgumentException("عدّل هذه الحركة من تفاصيل العملية المالية");
            }
            if (!"OPEN".equals(kind) && !"MANUAL_PAYMENT".equals(kind)) throw new IllegalArgumentException("حركة الدين غير موجودة");
            ContentValues values = new ContentValues(); values.put("amount_minor", amountMinor); values.put("note", note == null ? "" : note.trim());
            writable.update("debt_movements", values, "id=?", new String[]{String.valueOf(movementId)});
            ensureDebtTotalsValid(writable, debtId); recomputeDebt(writable, debtId); writable.setTransactionSuccessful();
        } finally { writable.endTransaction(); }
    }

    public void deleteManualDebtMovement(long movementId) {
        SQLiteDatabase writable = getWritableDatabase(); writable.beginTransaction();
        try {
            long debtId;
            try (Cursor c = writable.rawQuery("SELECT debt_id,transaction_id FROM debt_movements WHERE id=?",
                    new String[]{String.valueOf(movementId)})) {
                if (!c.moveToFirst()) throw new IllegalArgumentException("حركة الدين غير موجودة");
                debtId = c.getLong(0);
                if (!c.isNull(1)) throw new IllegalArgumentException("احذف هذه الحركة من تفاصيل العملية المالية");
            }
            if(writable.delete("debt_movements", "id=? AND transaction_id IS NULL",
                    new String[]{String.valueOf(movementId)})!=1)
                throw new IllegalStateException("تعذّر حذف حركة الدين؛ لم يتغير السجل");
            ensureDebtTotalsValid(writable, debtId); recomputeDebt(writable, debtId); writable.setTransactionSuccessful();
        } finally { writable.endTransaction(); }
    }

    public void deleteDebt(long debtId) {
        List<Long> transactionIds = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT DISTINCT transaction_id FROM debt_movements WHERE debt_id=? AND transaction_id IS NOT NULL",
                new String[]{String.valueOf(debtId)})) { while (c.moveToNext()) transactionIds.add(c.getLong(0)); }
        for (Long txId : transactionIds) {
            try (Cursor c = getReadableDatabase().rawQuery("SELECT 1 FROM transaction_splits WHERE transaction_id=? LIMIT 1",
                    new String[]{String.valueOf(txId)})) {
                if (c.moveToFirst()) throw new IllegalArgumentException("الدين مرتبط بمصروف مقسّم؛ عدّل أو احذف العملية من تفاصيلها لحماية الأجزاء الأخرى");
            }
            try (Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(DISTINCT debt_id) FROM debt_movements WHERE transaction_id=?",
                    new String[]{String.valueOf(txId)})) {
                if (c.moveToFirst() && c.getInt(0) > 1)
                    throw new IllegalArgumentException("هذا الدين مرتبط بسداد موزع على أكثر من سجل؛ احذف العملية المشتركة أولًا");
            }
        }
        SQLiteDatabase writable = getWritableDatabase(); writable.beginTransaction();
        try {
            for (Long txId : transactionIds) {
                String type; long amount, sourceId; Long destinationId; boolean affects;
                try (Cursor c = writable.rawQuery("SELECT type,amount_minor,account_id,destination_account_id,affects_balance,base_affects_balance," +
                    "category_id,currency,note,raw_text,source,needs_review,created_at," +
                    "special_kind,debt_person_id,debt_direction,destination_amount_minor,exchange_rate FROM transactions WHERE id=?",
                        new String[]{String.valueOf(txId)})) {
                    if (!c.moveToFirst()) continue;
                    type=c.getString(0); amount=c.getLong(1); sourceId=c.getLong(2); destinationId=c.isNull(3)?null:c.getLong(3); affects=c.getInt(4)==1;
                }
                if (affects) reverseBalanceEffect(writable, type, amount, sourceId, destinationId);
                ContentValues capture = new ContentValues(); capture.put("status", "IGNORED"); capture.putNull("transaction_id");
                writable.update("capture_inbox", capture, "transaction_id=?", new String[]{String.valueOf(txId)});
                writable.delete("transactions", "id=?", new String[]{String.valueOf(txId)});
            }
            writable.delete("debt_movements", "debt_id=?", new String[]{String.valueOf(debtId)});
            writable.delete("debts", "id=?", new String[]{String.valueOf(debtId)});
            writable.setTransactionSuccessful();
        } finally {
            writable.endTransaction(); BalanceWidgetProvider.updateAll(appContext); BudgetAlertManager.checkAll(appContext);
        }
    }

    private void ensureDebtTotalsValid(SQLiteDatabase readable, long debtId) {
        long original = 0, paid = 0;
        try (Cursor c = readable.rawQuery("SELECT COALESCE(SUM(CASE WHEN kind='OPEN' THEN amount_minor ELSE 0 END),0)," +
                "COALESCE(SUM(CASE WHEN kind IN ('PAYMENT','MANUAL_PAYMENT') THEN amount_minor ELSE 0 END),0) FROM debt_movements WHERE debt_id=?",
                new String[]{String.valueOf(debtId)})) { if (c.moveToFirst()) { original=c.getLong(0); paid=c.getLong(1); } }
        if (paid > original) throw new IllegalArgumentException("لا يمكن أن يصبح مجموع السداد أكبر من أصل الدين");
    }

    public void reclassifyTransactionAsDebt(long transactionId, String specialKind, long personId, String direction) {
        if (!FinanceRules.SPECIAL_DEBT.equals(specialKind) && !FinanceRules.SPECIAL_DEBT_PAYMENT.equals(specialKind))
            throw new IllegalArgumentException("نوع حركة الدين غير صحيح");
        validateDebtDirection(direction); requireDebtPerson(personId);
        SQLiteDatabase writable = getWritableDatabase(); writable.beginTransaction();
        try {
            String type; long amount; long sourceId; Long destinationId; String currency; boolean affectsBalance; boolean baseAffects; long createdAt; String note;
            try (Cursor c = writable.rawQuery("SELECT type,amount_minor,account_id,destination_account_id,currency,affects_balance,base_affects_balance,created_at,note FROM transactions WHERE id=?",
                    new String[]{String.valueOf(transactionId)})) {
                if (!c.moveToFirst()) throw new IllegalArgumentException("العملية غير موجودة");
                type=c.getString(0); amount=c.getLong(1); sourceId=c.getLong(2); destinationId=c.isNull(3)?null:c.getLong(3);
                currency=c.getString(4); affectsBalance=c.getInt(5)==1; baseAffects=c.getInt(6)==1; createdAt=c.getLong(7); note=c.getString(8);
            }
            if (!FinanceRules.EXPENSE.equals(type) && !FinanceRules.INCOME.equals(type))
                throw new IllegalArgumentException("حوّل العملية أولًا إلى مصروف أو دخل");
            if (FinanceRules.SPECIAL_DEBT_PAYMENT.equals(specialKind)) {
                String required = FinanceRules.INCOME.equals(type) ? FinanceRules.DEBT_RECEIVABLE : FinanceRules.DEBT_PAYABLE;
                if (!required.equals(direction)) throw new IllegalArgumentException("اتجاه السداد لا يطابق اتجاه العملية المالية");
            }
            long oldImpact = FinanceRules.EXPENSE.equals(type) ? existingExpenseBalanceImpact(writable, transactionId, amount) : amount;
            ensureDebtOpeningCanBeRemoved(writable, transactionId);
            if (affectsBalance) reverseBalanceEffect(writable, type, oldImpact, sourceId, destinationId);
            List<Long> oldDebts = removeDebtMovementsForTransaction(writable, transactionId);
            for (Long debtId : oldDebts) recomputeDebt(writable, debtId);
            writable.delete("transaction_splits", "transaction_id=?", new String[]{String.valueOf(transactionId)});
            ContentValues values = new ContentValues(); values.putNull("destination_account_id"); values.putNull("category_id");
            values.put("special_kind", specialKind); values.put("debt_person_id", personId); values.put("debt_direction", direction);
            boolean actualMoney = FinanceRules.SPECIAL_DEBT_PAYMENT.equals(specialKind) && baseAffects;
            values.put("affects_balance", actualMoney ? 1 : 0); values.put("needs_review", 0);
            writable.update("transactions", values, "id=?", new String[]{String.valueOf(transactionId)});
            if (actualMoney) applyBalanceEffect(writable, type, amount, sourceId, null);
            if (FinanceRules.SPECIAL_DEBT.equals(specialKind)) {
                long debtId = findOrCreateOpenDebt(writable, personId, direction, currency, note, createdAt);
                insertDebtMovement(writable, debtId, transactionId, "OPEN", amount, createdAt, note); recomputeDebt(writable, debtId);
            } else allocatePayment(writable, personId, direction, currency, transactionId, amount, createdAt, note);
            for (Long debtId : oldDebts) recomputeDebt(writable, debtId);
            writable.setTransactionSuccessful();
        } finally {
            writable.endTransaction(); BalanceWidgetProvider.updateAll(appContext); BudgetAlertManager.checkAll(appContext);
        }
    }

    public void updateDebtTransaction(long transactionId, long amountMinor, long personId, String direction) {
        if (amountMinor <= 0) throw new IllegalArgumentException("اكتب مبلغًا صحيحًا");
        validateDebtDirection(direction); requireDebtPerson(personId);
        SQLiteDatabase writable = getWritableDatabase(); writable.beginTransaction();
        try {
            String type, special, currency, note; long oldAmount, sourceId, createdAt; boolean affects, baseAffects;
            try (Cursor c = writable.rawQuery("SELECT type,special_kind,currency,amount_minor,account_id,created_at,note,affects_balance,base_affects_balance FROM transactions WHERE id=?",
                    new String[]{String.valueOf(transactionId)})) {
                if (!c.moveToFirst()) throw new IllegalArgumentException("العملية غير موجودة");
                type=c.getString(0); special=c.isNull(1)?null:c.getString(1); currency=c.getString(2); oldAmount=c.getLong(3);
                sourceId=c.getLong(4); createdAt=c.getLong(5); note=c.getString(6); affects=c.getInt(7)==1; baseAffects=c.getInt(8)==1;
            }
            if (special == null) throw new IllegalArgumentException("هذه ليست حركة دين");
            String actualSpecial = DebtLedgerRules.movementKind(type, direction);
            if (FinanceRules.SPECIAL_DEBT_PAYMENT.equals(actualSpecial)) {
                String required = FinanceRules.INCOME.equals(type) ? FinanceRules.DEBT_RECEIVABLE : FinanceRules.DEBT_PAYABLE;
                if (!required.equals(direction)) throw new IllegalArgumentException("اتجاه السداد لا يطابق العملية");
            }
            ensureDebtOpeningCanBeRemoved(writable, transactionId);
            if (affects) reverseBalanceEffect(writable, type, oldAmount, sourceId, null);
            List<Long> oldDebts = removeDebtMovementsForTransaction(writable, transactionId);
            for (Long debtId : oldDebts) recomputeDebt(writable, debtId);
            ContentValues values = new ContentValues(); values.put("amount_minor", amountMinor); values.put("debt_person_id", personId); values.put("debt_direction", direction); values.put("special_kind", actualSpecial);
            values.put("affects_balance", FinanceRules.SPECIAL_DEBT_PAYMENT.equals(actualSpecial) && baseAffects ? 1 : 0);
            writable.update("transactions", values, "id=?", new String[]{String.valueOf(transactionId)});
            if (FinanceRules.SPECIAL_DEBT_PAYMENT.equals(actualSpecial) && baseAffects) applyBalanceEffect(writable, type, amountMinor, sourceId, null);
            if (FinanceRules.SPECIAL_DEBT.equals(actualSpecial)) {
                long debtId=findOrCreateOpenDebt(writable,personId,direction,currency,note,createdAt);
                insertDebtMovement(writable,debtId,transactionId,"OPEN",amountMinor,createdAt,note); recomputeDebt(writable,debtId);
            } else allocatePayment(writable,personId,direction,currency,transactionId,amountMinor,createdAt,note);
            for (Long debtId : oldDebts) recomputeDebt(writable, debtId);
            writable.setTransactionSuccessful();
        } finally { writable.endTransaction(); BalanceWidgetProvider.updateAll(appContext); BudgetAlertManager.checkAll(appContext); }
    }

    /** Restore only a simple deleted manual income/expense. One SQLite transaction for
     *  row, balance and audit metadata; never restore capture, transfer or debt. */
    public void undoDeletedSimpleTransaction(long auditId) {
        SQLiteDatabase writable=getWritableDatabase();writable.beginTransaction();
        try {
            long transactionId,accountId,categoryId,amount,impact,createdAt;
            int affects,baseAffects,needsReview;
            String type,currency,note,rawText,source;
            try(Cursor c=writable.rawQuery(
                "SELECT s.transaction_id,s.account_id,s.category_id,s.type,s.amount_minor," +
                "s.balance_impact_minor,s.created_at,s.currency,s.note,s.raw_text,s.source," +
                "s.needs_review,s.affects_balance,s.base_affects_balance " +
                "FROM deleted_transaction_snapshots s JOIN change_history h ON h.id=s.audit_id " +
                "WHERE s.audit_id=? AND s.restored_at IS NULL AND h.undone=0 AND h.action='DELETE' " +
                "AND h.table_name='transactions' AND h.record_id=s.transaction_id " +
                "AND NOT EXISTS(SELECT 1 FROM transactions t WHERE t.id=s.transaction_id)",
                new String[]{String.valueOf(auditId)})) {
                if(!c.moveToFirst())throw new IllegalArgumentException("هذه العملية غير قابلة للتراجع أو سبق استرجاعها");
                transactionId=c.getLong(0);accountId=c.getLong(1);categoryId=c.getLong(2);
                type=c.getString(3);amount=c.getLong(4);impact=c.getLong(5);
                createdAt=c.getLong(6);currency=c.getString(7);note=c.getString(8);
                rawText=c.isNull(9)?null:c.getString(9);source=c.getString(10);
                needsReview=c.getInt(11);affects=c.getInt(12);baseAffects=c.getInt(13);
            }
            if(!SimpleUndoRules.isEligible(type,source,amount,true,false,false,false,false,needsReview))
                throw new IllegalArgumentException("التراجع المالي غير آمن لهذه العملية");
            long balance;
            try(Cursor c=writable.rawQuery("SELECT balance_minor,currency FROM accounts WHERE id=?",
                    new String[]{String.valueOf(accountId)})) {
                if(!c.moveToFirst() || !currency.equals(c.getString(1)))
                    throw new IllegalArgumentException("الحساب الأصلي لم يعد موجودًا بنفس العملة");
                balance=c.getLong(0);
            }
            try(Cursor c=writable.rawQuery("SELECT kind FROM categories WHERE id=?",
                    new String[]{String.valueOf(categoryId)})) {
                if(!c.moveToFirst() || !type.equals(c.getString(0)))
                    throw new IllegalArgumentException("التصنيف الأصلي غير متاح لاسترجاع العملية");
            }
            // The stored impact is the precise amount reversed at deletion, even if
            // exclusion/category preferences changed afterward.
            long restoredBalance=SimpleUndoRules.restoredBalance(balance,type,affects==1?impact:0);
            NonNegativeBalanceRules.requireNonNegative(restoredBalance); // Undo must not overdraw
            ContentValues tx=new ContentValues();tx.put("id",transactionId);tx.put("type",type);
            tx.put("amount_minor",amount);tx.put("currency",currency);tx.put("account_id",accountId);
            tx.put("category_id",categoryId);tx.put("note",note);
            if(rawText==null)tx.putNull("raw_text");else tx.put("raw_text",rawText);
            tx.put("needs_review",needsReview);tx.put("created_at",createdAt);tx.put("source",source);
            tx.put("affects_balance",affects);tx.put("base_affects_balance",baseAffects);
            writable.insertOrThrow("transactions",null,tx);
            ContentValues restoreEvent=new ContentValues();restoreEvent.put("action","RESTORE");
            if(writable.update("change_history",restoreEvent,
                    "id=(SELECT MAX(id) FROM change_history WHERE table_name='transactions' AND record_id=?) AND action='ADD'",
                    new String[]{String.valueOf(transactionId)})!=1)
                throw new IllegalStateException("تعذّر توثيق حدث الاسترجاع");
            ContentValues account=new ContentValues();account.put("balance_minor",restoredBalance);
            if(writable.update("accounts",account,"id=?",new String[]{String.valueOf(accountId)})!=1)
                throw new IllegalStateException("الحساب الأصلي غير موجود");
            ContentValues restored=new ContentValues();restored.put("restored_at",System.currentTimeMillis());
            if(writable.update("deleted_transaction_snapshots",restored,"audit_id=? AND restored_at IS NULL",
                    new String[]{String.valueOf(auditId)})!=1)throw new IllegalStateException("الاسترجاع تكرر");
            ContentValues undo=new ContentValues();undo.put("undone",1);
            if(writable.update("change_history",undo,"id=? AND undone=0",new String[]{String.valueOf(auditId)})!=1)
                throw new IllegalStateException("سجل التراجع تغيّر");
            writable.setTransactionSuccessful();
        } finally {
            writable.endTransaction();
            BalanceWidgetProvider.updateAll(appContext);
            BudgetAlertManager.checkAll(appContext);
        }
    }

    private static boolean canSnapshotSimpleDelete(SQLiteDatabase writable,long transactionId,
                      String type,long amount,long categoryId,String source,int needsReview) {
        if(!SimpleUndoRules.isEligible(type,source,amount,categoryId>0,false,false,false,false,needsReview))
            return false;
        String[] arg={String.valueOf(transactionId)};
        try(Cursor c=writable.rawQuery("SELECT 1 FROM transaction_splits WHERE transaction_id=? LIMIT 1",arg)) {
            if(c.moveToFirst())return false;
        }
        try(Cursor c=writable.rawQuery("SELECT 1 FROM debt_movements WHERE transaction_id=? LIMIT 1",arg)) {
            if(c.moveToFirst())return false;
        }
        try(Cursor c=writable.rawQuery("SELECT 1 FROM capture_inbox WHERE transaction_id=? LIMIT 1",arg)) {
            return !c.moveToFirst();
        }
    }

    /**
     * Only ordinary manually entered, confirmed expense/income rows can be edited
     * in-place. Debt, FX, imports, split and capture-linked operations remain on
     * their dedicated edit paths to protect financial integrity.
     */
    private static final String SAFE_SIMPLE_MANUAL_EDIT =
            "t.source='MANUAL' AND t.needs_review=0 " +
            "AND t.type IN ('EXPENSE','INCOME') AND t.category_id IS NOT NULL AND t.amount_minor>0 " +
            "AND t.destination_account_id IS NULL AND t.special_kind IS NULL " +
            "AND t.debt_person_id IS NULL AND t.debt_direction IS NULL " +
            "AND t.destination_amount_minor IS NULL AND t.exchange_rate IS NULL " +
            "AND t.exchange_rate_source IS NULL AND t.exchange_rate_checked_at IS NULL " +
            "AND NOT EXISTS(SELECT 1 FROM transaction_splits s WHERE s.transaction_id=t.id) " +
            "AND NOT EXISTS(SELECT 1 FROM debt_movements d WHERE d.transaction_id=t.id) " +
            "AND NOT EXISTS(SELECT 1 FROM capture_inbox i WHERE i.transaction_id=t.id) " +
            "AND EXISTS(SELECT 1 FROM categories c WHERE c.id=t.category_id AND c.kind=t.type) ";

    public boolean canEditSimpleManualTransaction(long transactionId) {
        try(Cursor c=getReadableDatabase().rawQuery(
                "SELECT 1 FROM transactions t WHERE t.id=? AND " + SAFE_SIMPLE_MANUAL_EDIT,
                new String[]{String.valueOf(transactionId)})) {
            return c.moveToFirst();
        }
    }

    public void editSimpleManualTransaction(long transactionId, long expectedAmountMinor,
                                            String expectedNote, long newAmountMinor, String newNote) {
        if(newAmountMinor<=0)throw new IllegalArgumentException("المبلغ يجب أن يكون أكبر من صفر");
        String cleanNote=newNote==null?"":newNote.trim();
        if(cleanNote.length()>2000)throw new IllegalArgumentException("الملاحظة طويلة جدًا");
        SQLiteDatabase writable=getWritableDatabase();
        writable.beginTransaction();
        try {
            String type, oldNote;
            long oldAmount, accountId, categoryId;
            boolean affectsBalance;
            try(Cursor c=writable.rawQuery(
                    "SELECT t.type,t.amount_minor,t.account_id,t.category_id,t.affects_balance,t.note " +
                    "FROM transactions t WHERE t.id=? AND "+SAFE_SIMPLE_MANUAL_EDIT,
                    new String[]{String.valueOf(transactionId)})) {
                if(!c.moveToFirst())
                    throw new IllegalArgumentException("هذه العملية ليست يدوية بسيطة؛ استخدم تعديلها المخصص");
                type=c.getString(0);oldAmount=c.getLong(1);accountId=c.getLong(2);
                categoryId=c.getLong(3);affectsBalance=c.getInt(4)==1;
                oldNote=c.isNull(5)?"":c.getString(5);
            }
            if(oldAmount!=expectedAmountMinor ||
                    !oldNote.equals(expectedNote==null?"":expectedNote))
                throw new IllegalStateException("تغيرت العملية بعد فتحها؛ افتحها من جديد قبل تعديلها");
            // Do not create a fake EDIT audit record or wake widgets for a no-op.
            if(oldAmount==newAmountMinor && oldNote.equals(cleanNote)) {
                writable.setTransactionSuccessful();
                return;
            }
            boolean excludedExpense=FinanceRules.EXPENSE.equals(type) && categoryExcluded(writable,categoryId);
            long delta=SimpleTransactionEditRules.balanceDelta(type,oldAmount,newAmountMinor,
                    affectsBalance,excludedExpense);
            if(delta!=0L)adjustBalance(writable,accountId,delta);
            ContentValues cv=new ContentValues();
            cv.put("amount_minor",newAmountMinor);
            cv.put("note",cleanNote);
            if(writable.update("transactions",cv,"id=?",new String[]{String.valueOf(transactionId)})!=1)
                throw new IllegalStateException("تعذّر حفظ تعديل العملية");
            // Audited by the existing transaction_update trigger; account + row
            // changes commit atomically and roll back together on any error.
            writable.setTransactionSuccessful();
        } finally { writable.endTransaction(); }
        BalanceWidgetProvider.updateAll(appContext);
    }

    public void deleteTransaction(long transactionId) {
        SQLiteDatabase writable = getWritableDatabase();
        writable.beginTransaction();
        try {
            String type; long amount; long sourceId; Long destinationId; boolean affectsBalance;
            boolean baseAffects; Long categoryId;String currency,note,rawText,source;int needsReview;long createdAt;
            boolean hasSpecialMetadata;boolean canUndo;
            // Keep the projection in sync with all cursor indices used below. Previous
            // code selected only FIVE columns but accessed 0..17, crashing on deletion.
            try (Cursor c = writable.rawQuery("SELECT type,amount_minor,account_id,destination_account_id,affects_balance," +
                    "base_affects_balance,category_id,currency,note,raw_text,source,needs_review,created_at," +
                    "special_kind,debt_person_id,debt_direction,destination_amount_minor," +
                    "exchange_rate,exchange_rate_source,exchange_rate_checked_at FROM transactions WHERE id=?",
                    new String[]{String.valueOf(transactionId)})) {
                if (!c.moveToFirst()) throw new IllegalArgumentException("العملية غير موجودة");
                type=c.getString(0);amount=c.getLong(1);sourceId=c.getLong(2);
                destinationId=c.isNull(3)?null:c.getLong(3);affectsBalance=c.getInt(4)==1;
                baseAffects=c.getInt(5)==1;categoryId=c.isNull(6)?null:c.getLong(6);
                currency=c.getString(7);note=c.getString(8);rawText=c.isNull(9)?null:c.getString(9);
                source=c.getString(10);needsReview=c.getInt(11);createdAt=c.getLong(12);
                hasSpecialMetadata=!c.isNull(13)||!c.isNull(14)||!c.isNull(15)||!c.isNull(16)||
                        !c.isNull(17)||!c.isNull(18)||!c.isNull(19);
            }
            long balanceImpact = FinanceRules.EXPENSE.equals(type)
                    ? existingExpenseBalanceImpact(writable, transactionId, amount) : amount;
            Long fxDestinationMinor=null;
            try(Cursor fx=writable.rawQuery("SELECT destination_amount_minor FROM transactions WHERE id=?",
                    new String[]{String.valueOf(transactionId)})) {
                if(fx.moveToFirst() && !fx.isNull(0))fxDestinationMinor=fx.getLong(0);
            }
            canUndo=!hasSpecialMetadata && destinationId==null && canSnapshotSimpleDelete(writable,transactionId,
                    type,amount,categoryId==null?-1:categoryId,source,needsReview);
            ensureDebtOpeningCanBeRemoved(writable, transactionId);
            if (affectsBalance) {
                if(fxDestinationMinor!=null) {
                    if(!FinanceRules.TRANSFER.equals(type) || destinationId==null)
                        throw new IllegalStateException("سجل تحويل متعدد العملات غير متسق");
                    long sourceBalance=balanceIncludingInactive(writable, sourceId);
                    long destinationBalance=balanceIncludingInactive(writable, destinationId);
                    ContentValues a=new ContentValues();a.put("balance_minor",Math.addExact(sourceBalance,amount));
                    writable.update("accounts",a,"id=?",new String[]{String.valueOf(sourceId)});
                    ContentValues b=new ContentValues();b.put("balance_minor",
                            NonNegativeBalanceRules.afterDelta(destinationBalance,-fxDestinationMinor));
                    writable.update("accounts",b,"id=?",new String[]{String.valueOf(destinationId)});
                } else reverseBalanceEffect(writable, type, balanceImpact, sourceId, destinationId);
            }
            List<Long> affectedDebts = debtIdsForTransaction(writable, transactionId);
            ContentValues capture = new ContentValues(); capture.put("status", "IGNORED"); capture.putNull("transaction_id");
            writable.update("capture_inbox", capture, "transaction_id=?", new String[]{String.valueOf(transactionId)});
            if(writable.delete("transactions", "id=?", new String[]{String.valueOf(transactionId)})!=1)
                throw new IllegalStateException("فشل حذف العملية");
            if(canUndo) {
                long auditId;
                try(Cursor audit=writable.rawQuery("SELECT id FROM change_history WHERE table_name='transactions' " +
                        "AND action='DELETE' AND record_id=? ORDER BY id DESC LIMIT 1",
                        new String[]{String.valueOf(transactionId)})) {
                    if(!audit.moveToFirst())throw new IllegalStateException("سجل حذف العملية غير موجود");
                    auditId=audit.getLong(0);
                }
                ContentValues snap=new ContentValues();snap.put("audit_id",auditId);
                snap.put("transaction_id",transactionId);snap.put("type",type);snap.put("amount_minor",amount);
                snap.put("currency",currency);snap.put("account_id",sourceId);snap.put("category_id",categoryId);
                snap.put("note",note);if(rawText==null)snap.putNull("raw_text");else snap.put("raw_text",rawText);
                snap.put("needs_review",needsReview);snap.put("created_at",createdAt);snap.put("source",source);
                snap.put("affects_balance",affectsBalance?1:0);snap.put("base_affects_balance",baseAffects?1:0);
                snap.put("balance_impact_minor",balanceImpact);snap.put("deleted_at",System.currentTimeMillis());
                writable.insertOrThrow("deleted_transaction_snapshots",null,snap);
            }
            for (Long debtId : affectedDebts) recomputeDebt(writable, debtId);
            writable.setTransactionSuccessful();
        } finally {
            writable.endTransaction();
            BalanceWidgetProvider.updateAll(appContext);
            BudgetAlertManager.checkAll(appContext);
        }
    }

    private void requireDebtPerson(long personId) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT 1 FROM debt_people WHERE id=? AND active=1",
                new String[]{String.valueOf(personId)})) {
            if (!c.moveToFirst()) throw new IllegalArgumentException("اختر شخصًا من قائمة الديون");
        }
    }

    private void ensureDebtOpeningCanBeRemoved(SQLiteDatabase readable, long transactionId) {
        String sql = "SELECT m.debt_id," +
                "COALESCE(SUM(CASE WHEN allm.kind='OPEN' AND (allm.transaction_id IS NULL OR allm.transaction_id<>?) THEN allm.amount_minor ELSE 0 END),0) remaining_open," +
                "COALESCE(SUM(CASE WHEN allm.kind IN ('PAYMENT','MANUAL_PAYMENT') THEN allm.amount_minor ELSE 0 END),0) paid " +
                "FROM debt_movements m JOIN debt_movements allm ON allm.debt_id=m.debt_id " +
                "WHERE m.transaction_id=? AND m.kind='OPEN' GROUP BY m.debt_id";
        try (Cursor c = readable.rawQuery(sql, new String[]{String.valueOf(transactionId), String.valueOf(transactionId)})) {
            while (c.moveToNext()) if (c.getLong(2) > c.getLong(1))
                throw new IllegalArgumentException("لا يمكن تغيير أو حذف أصل هذا الدين بعد تسجيل سداد عليه. عدّل أو احذف حركات السداد أولًا.");
        }
    }

    private static void validateDebtDirection(String direction) {
        if (!FinanceRules.DEBT_RECEIVABLE.equals(direction) && !FinanceRules.DEBT_PAYABLE.equals(direction))
            throw new IllegalArgumentException("اتجاه الدين غير صحيح");
    }

    private long findOrCreateOpenDebt(SQLiteDatabase writable, long personId, String direction,
                                      String currency, String note, long createdAt) {
        try (Cursor c = writable.rawQuery("SELECT id FROM debts WHERE person_id=? AND direction=? AND currency=? AND closed_at IS NULL ORDER BY id DESC LIMIT 1",
                new String[]{String.valueOf(personId), direction, currency})) {
            if (c.moveToFirst()) return c.getLong(0);
        }
        ContentValues values = new ContentValues(); values.put("person_id", personId); values.put("direction", direction);
        values.put("currency", currency); values.put("created_at", createdAt); values.put("updated_at", createdAt);
        values.put("note", note == null ? "" : note.trim());
        return writable.insertOrThrow("debts", null, values);
    }

    private void allocatePayment(SQLiteDatabase writable, long personId, String direction, String currency,
                                 long transactionId, long amountMinor, long createdAt, String note) {
        List<Long> debtIds = new ArrayList<>(); List<Long> remaining = new ArrayList<>();
        String sql = "SELECT d.id,COALESCE(SUM(CASE WHEN m.kind='OPEN' THEN m.amount_minor ELSE -m.amount_minor END),0) remaining " +
                "FROM debts d LEFT JOIN debt_movements m ON m.debt_id=d.id WHERE d.person_id=? AND d.direction=? AND d.currency=? AND d.closed_at IS NULL " +
                "GROUP BY d.id,d.created_at HAVING SUM(CASE WHEN m.kind='OPEN' THEN m.amount_minor ELSE -m.amount_minor END)>0 ORDER BY d.created_at,d.id";
        try (Cursor c = writable.rawQuery(sql, new String[]{String.valueOf(personId), direction, currency})) {
            while (c.moveToNext()) { debtIds.add(c.getLong(0)); remaining.add(c.getLong(1)); }
        }
        long[] allocations = DebtLedgerRules.allocatePayment(amountMinor, remaining);
        for (int i = 0; i < debtIds.size(); i++) {
            long part = allocations[i]; if (part <= 0) continue;
            insertDebtMovement(writable, debtIds.get(i), transactionId, "PAYMENT", part, createdAt, note);
            recomputeDebt(writable, debtIds.get(i));
        }
    }

    private void insertDebtMovement(SQLiteDatabase writable, long debtId, Long transactionId, String kind,
                                    long amountMinor, long createdAt, String note) {
        ContentValues values = new ContentValues(); values.put("debt_id", debtId);
        if (transactionId == null) values.putNull("transaction_id"); else values.put("transaction_id", transactionId);
        values.put("kind", kind); values.put("amount_minor", amountMinor); values.put("created_at", createdAt);
        values.put("note", note == null ? "" : note.trim()); writable.insertOrThrow("debt_movements", null, values);
    }

    private long debtRemaining(SQLiteDatabase readable, long debtId) {
        try (Cursor c = readable.rawQuery("SELECT COALESCE(SUM(CASE WHEN kind='OPEN' THEN amount_minor ELSE -amount_minor END),0) FROM debt_movements WHERE debt_id=?",
                new String[]{String.valueOf(debtId)})) { return c.moveToFirst() ? Math.max(0L, c.getLong(0)) : 0L; }
    }

    private void recomputeDebt(SQLiteDatabase writable, long debtId) {
        long original = 0, paid = 0, updatedAt = System.currentTimeMillis();
        try (Cursor c = writable.rawQuery("SELECT COALESCE(SUM(CASE WHEN kind='OPEN' THEN amount_minor ELSE 0 END),0)," +
                "COALESCE(SUM(CASE WHEN kind IN ('PAYMENT','MANUAL_PAYMENT') THEN amount_minor ELSE 0 END),0),COALESCE(MAX(created_at),0) FROM debt_movements WHERE debt_id=?",
                new String[]{String.valueOf(debtId)})) {
            if (c.moveToFirst()) { original = c.getLong(0); paid = c.getLong(1); if (c.getLong(2) > 0) updatedAt = c.getLong(2); }
        }
        if (original == 0) { writable.delete("debts", "id=?", new String[]{String.valueOf(debtId)}); return; }
        boolean closed = paid >= original;
        ContentValues values = new ContentValues(); values.put("updated_at", updatedAt);
        if (closed) values.put("closed_at", updatedAt); else values.putNull("closed_at");
        writable.update("debts", values, "id=?", new String[]{String.valueOf(debtId)});
    }

    private List<Long> debtIdsForTransaction(SQLiteDatabase readable, long transactionId) {
        List<Long> result = new ArrayList<>();
        try (Cursor c = readable.rawQuery("SELECT DISTINCT debt_id FROM debt_movements WHERE transaction_id=?",
                new String[]{String.valueOf(transactionId)})) { while (c.moveToNext()) result.add(c.getLong(0)); }
        return result;
    }

    private List<Long> removeDebtMovementsForTransaction(SQLiteDatabase writable, long transactionId) {
        List<Long> result = debtIdsForTransaction(writable, transactionId);
        writable.delete("debt_movements", "transaction_id=?", new String[]{String.valueOf(transactionId)});
        return result;
    }

    private void reverseBalanceEffect(SQLiteDatabase writable, String type, long amount, long sourceId, Long destinationId) {
        if (FinanceRules.EXPENSE.equals(type)) adjustBalance(writable, sourceId, amount);
        else if (FinanceRules.INCOME.equals(type)) adjustBalance(writable, sourceId, -amount);
        else if (FinanceRules.TRANSFER.equals(type)) {
            adjustBalance(writable, sourceId, amount);
            if (destinationId != null) adjustBalance(writable, destinationId, -amount);
        }
    }

    private void applyBalanceEffect(SQLiteDatabase writable, String type, long amount, long sourceId, Long destinationId) {
        if (FinanceRules.EXPENSE.equals(type)) adjustBalance(writable, sourceId, -amount);
        else if (FinanceRules.INCOME.equals(type)) adjustBalance(writable, sourceId, amount);
        else if (FinanceRules.TRANSFER.equals(type)) {
            adjustBalance(writable, sourceId, -amount);
            if (destinationId != null) adjustBalance(writable, destinationId, amount);
        }
    }

    private long expenseBalanceImpact(SQLiteDatabase readable, long total, Long categoryId,
                                      List<Models.SplitInput> splits) {
        List<Long> includedParts = new ArrayList<>();
        if (splits != null && !splits.isEmpty()) {
            for (Models.SplitInput split : splits)
                if (SplitDebtRules.affectsOutgoingBalance(split,
                        split.specialKind == null && categoryExcluded(readable, split.categoryId)))
                    includedParts.add(split.amountMinor);
            // A split made entirely of excluded categories intentionally has zero balance impact.
            if (includedParts.isEmpty()) return 0L;
            return FinanceRules.expenseBalanceImpact(total, false, includedParts);
        }
        return FinanceRules.expenseBalanceImpact(total,
                categoryId != null && categoryExcluded(readable, categoryId), null);
    }

    private long existingExpenseBalanceImpact(SQLiteDatabase readable, long transactionId, long total) {
        List<Long> includedParts = new ArrayList<>();
        int splitCount = 0;
        try (Cursor c = readable.rawQuery("SELECT s.amount_minor,COALESCE(c.exclude_from_expense,0),s.special_kind FROM transaction_splits s " +
                "LEFT JOIN categories c ON c.id=s.category_id WHERE s.transaction_id=?",
                new String[]{String.valueOf(transactionId)})) {
            while (c.moveToNext()) {
                splitCount++;
                String special = c.isNull(2) ? null : c.getString(2);
                if (FinanceRules.SPECIAL_DEBT_PAYMENT.equals(special) ||
                        (special == null && c.getInt(1) == 0)) includedParts.add(c.getLong(0));
            }
        }
        if (splitCount > 0) return includedParts.isEmpty() ? 0L
                : FinanceRules.expenseBalanceImpact(total, false, includedParts);
        try (Cursor c = readable.rawQuery("SELECT COALESCE(c.exclude_from_expense,0) FROM transactions t " +
                "LEFT JOIN categories c ON c.id=t.category_id WHERE t.id=?",
                new String[]{String.valueOf(transactionId)})) {
            boolean excluded = c.moveToFirst() && c.getInt(0) == 1;
            return FinanceRules.expenseBalanceImpact(total, excluded, null);
        }
    }

    private boolean categoryExcluded(SQLiteDatabase readable, long categoryId) {
        try (Cursor c = readable.rawQuery("SELECT exclude_from_expense FROM categories WHERE id=?",
                new String[]{String.valueOf(categoryId)})) {
            if (!c.moveToFirst()) throw new IllegalArgumentException("التصنيف غير موجود");
            return c.getInt(0) == 1;
        }
    }

    private static void requireCategoryForType(SQLiteDatabase readable, long categoryId, String type) {
        try (Cursor c = readable.rawQuery("SELECT kind FROM categories WHERE id=? AND archived=0",
                new String[]{String.valueOf(categoryId)})) {
            if (!c.moveToFirst() || !type.equals(c.getString(0)))
                throw new IllegalArgumentException("التصنيف غير متاح لنوع العملية");
        }
    }

    private static boolean isActiveAccount(SQLiteDatabase readable, long accountId) {
        try (Cursor c = readable.rawQuery("SELECT 1 FROM accounts WHERE id=? AND active=1",
                new String[]{String.valueOf(accountId)})) {
            return c.moveToFirst();
        }
    }

    private void adjustBalance(SQLiteDatabase writable, long accountId, long delta) {
        long current;
        try (Cursor c = writable.rawQuery("SELECT balance_minor FROM accounts WHERE id=?",
                new String[]{String.valueOf(accountId)})) {
            if (!c.moveToFirst()) throw new IllegalArgumentException("الحساب غير موجود");
            current = c.getLong(0);
        }
        ContentValues values = new ContentValues();
        values.put("balance_minor", NonNegativeBalanceRules.afterDelta(current, delta));
        if (writable.update("accounts", values, "id=?", new String[]{String.valueOf(accountId)}) != 1)
            throw new IllegalStateException("تعذر تحديث رصيد الحساب");
    }
}
