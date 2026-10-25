package app.wensarafet.tracker;

public final class Models {
    private Models() {}

    public static final class Account {
        public long id;
        public String name;
        public String currency;
        public long balanceMinor;
        public boolean builtIn;

        public String displayName() { return name + " — " + currency; }
    }

    public static final class SplitDetail {
        public long categoryId;
        public String categoryName;
        public String categoryEmoji;
        public long amountMinor;
        public String specialKind;
        public Long debtPersonId;
        public String debtPersonName;
        public String debtDirection;

        public String displayName() {
            if (FinanceRules.SPECIAL_DEBT.equals(specialKind)) return "🤝 دَين — " + debtPersonName + " (إلي)";
            if (FinanceRules.SPECIAL_DEBT_PAYMENT.equals(specialKind)) return "✓ سداد دَين — " + debtPersonName + " (عليّ)";
            return categoryEmoji + " " + categoryName;
        }
    }

    public static final class Category {
        public long id;
        public String name;
        public String kind;
        public String emoji;
        public boolean excludeFromExpense;

        public String displayName() { return emoji + " " + name + (excludeFromExpense ? " — لا يُحسب مصروفًا" : ""); }
    }

    public static final class Transaction {
        public long id;
        public String type;
        public long amountMinor;
        public String currency;
        public long accountId;
        public Long destinationAccountId;
        public Long destinationAmountMinor;
        public String destinationCurrency;
        public String exchangeRate;
        public String exchangeRateSource;
        public String accountName;
        public String destinationName;
        public String categoryName;
        public String categoryEmoji;
        public String note;
        public String rawText;
        public boolean needsReview;
        public boolean hasSplits;
        public boolean affectsBalance;
        public String specialKind;
        public Long debtPersonId;
        public String debtPersonName;
        public String debtDirection;
        public long createdAt;
    }

    public static final class DebtPerson {
        public long id;
        public String name;
        public boolean active;
    }

    public static final class DebtSummary {
        public long id;
        public long personId;
        public String personName;
        public String direction;
        public String currency;
        public long originalMinor;
        public long paidMinor;
        public long remainingMinor;
        public long createdAt;
        public long lastMovementAt;
        public boolean closed;
    }

    public static final class DebtMovement {
        public long id;
        public long debtId;
        public Long transactionId;
        public String kind;
        public long amountMinor;
        public long createdAt;
        public String note;
    }

    public static final class CaptureItem {
        public long id;
        public String provider;
        public String channel;
        public String sender;
        public String rawText;
        public Long amountMinor;
        public String currency;
        public String inferredType;
        public int confidence;
        public String counterparty;
        public Long balanceMinor;
        public long receivedAt;
    }

    public static final class CategoryTotal {
        public long categoryId;
        public String name;
        public String emoji;
        public long amountMinor;
    }

    public static final class SplitInput {
        public long categoryId;
        public long amountMinor;
        public String specialKind;
        public Long debtPersonId;
        public String debtDirection;

        public SplitInput(long categoryId, long amountMinor) {
            this.categoryId = categoryId;
            this.amountMinor = amountMinor;
        }
        public static SplitInput debt(long amountMinor, String specialKind, long personId, String direction) {
            SplitInput value = new SplitInput(0, amountMinor);
            value.specialKind = specialKind;
            value.debtPersonId = personId;
            value.debtDirection = direction;
            return value;
        }
    }
}
