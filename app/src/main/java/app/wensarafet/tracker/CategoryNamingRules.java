package app.wensarafet.tracker;

/** Category labels have a financial meaning: keep the receipt-income bucket stable. */
public final class CategoryNamingRules {
    public static final String RECEIVED_TRANSFER = "حوالة مستلمة";
    private CategoryNamingRules() {}

    public static String cleanName(String text) {
        if (text == null) throw new IllegalArgumentException("اكتب اسم التصنيف");
        String clean = text.replaceAll("\\s+", " ").trim();
        int length = clean.codePointCount(0, clean.length());
        if (length < 2 || length > 50)
            throw new IllegalArgumentException("اسم التصنيف يجب أن يكون من حرفين إلى 50 حرفًا");
        return clean;
    }

    public static void validateRename(String oldName, String kind, boolean excludedFromExpense,
                                      String newName) {
        if (!FinanceRules.EXPENSE.equals(kind) && !FinanceRules.INCOME.equals(kind))
            throw new IllegalArgumentException("نوع التصنيف غير صحيح");
        if (excludedFromExpense || (FinanceRules.INCOME.equals(kind) && RECEIVED_TRANSFER.equals(oldName)))
            throw new IllegalArgumentException("هذا تصنيف مالي محمي ولا يمكن تغيير اسمه");
        String clean = cleanName(newName);
        if (FinanceRules.INCOME.equals(kind) && RECEIVED_TRANSFER.equals(clean))
            throw new IllegalArgumentException("اسم الحوالات المستلمة محجوز للتصنيف الأصلي");
    }

    public static String validateNewCategory(String name, String kind) {
        if (!FinanceRules.EXPENSE.equals(kind) && !FinanceRules.INCOME.equals(kind))
            throw new IllegalArgumentException("نوع التصنيف غير صحيح");
        String clean = cleanName(name);
        if (FinanceRules.INCOME.equals(kind) && RECEIVED_TRANSFER.equals(clean))
            throw new IllegalArgumentException("تصنيف الحوالات المستلمة موجود أصلًا، استخدمه بدلًا من تكراره");
        return clean;
    }
}
