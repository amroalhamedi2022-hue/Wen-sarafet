package app.wensarafet.tracker;

import java.util.List;
import java.util.Locale;

/**
 * FRIEND BRANCH ONLY. Supplements, never replaces, the current English parser.
 * Arabic and unrecognised-source receipts are review-only (confidence < 90).
 * No mutation of an account, debt, audit trail or financial transaction.
 */
public final class FriendArabicCapturePolicy {
    private FriendArabicCapturePolicy() {}

    public static boolean isSecurityMessage(String raw) {
        return raw != null && (TransactionParser.isVerificationMessage(raw)
                || ArabicSmsParserPilot.analyse(raw).verification);
    }

    public static boolean isFinancialMessage(String raw) {
        if(raw==null || raw.trim().isEmpty() || raw.length()>5000 || isSecurityMessage(raw))
            return false;
        ArabicSmsParserPilot.Candidate candidate=ArabicSmsParserPilot.analyse(raw);
        if(candidate.amountMinor==null || candidate.currency==null || candidate.amountMinor<=0)
            return false;
        // An unknown sender is accepted only when the text indicates money movement.
        // Bare balances, marketing messages, dates, amounts and OTP are ignored.
        String text=ArabicSmsParserPilot.normalizeForLab(raw);
        return text.contains("تحويل") || text.contains("حوالة")
                || text.contains("استلمت") || text.contains("تم دفع")
                || text.contains("خصم") || text.contains("سحب")
                || text.contains("ايداع") || text.contains("شراء")
                || text.contains("transferred") || text.contains("paid")
                || text.contains("payment") || text.contains("withdraw");
    }

    private static boolean hasArabicMovement(String raw) {
        if(raw==null)return false;
        String text=ArabicSmsParserPilot.normalizeForLab(raw);
        return text.contains("تحويل") || text.contains("حوالة") ||
                text.contains("استلم") || text.contains("ايداع") ||
                text.contains("خصم") || text.contains("دفع") ||
                text.contains("شراء") || text.contains("سحب");
    }

    public static TransactionParser.Result parse(String provider,String raw,List<String> ownNames) {
        TransactionParser.Result original=TransactionParser.parse(provider,raw,ownNames);
        if (isSecurityMessage(raw)) return new TransactionParser.Result();
        ArabicSmsParserPilot.Candidate candidate=ArabicSmsParserPilot.analyse(raw);
        if(candidate.amountMinor==null || candidate.currency==null || candidate.verification) {
            // A legacy high-confidence trigger must never auto-post an ambiguous
            // new Arabic receipt (such as amount + fee) as a single expense.
            if(hasArabicMovement(raw)) original.confidence=Math.min(original.confidence,40);
            return original;
        }

        // Preserve old English rules when Arabic pilot cannot resolve a movement.
        if(candidate.type==null && (original.type==null || original.confidence<90)) {
            original.amountMinor=candidate.amountMinor;
            original.currency=candidate.currency;
            if(candidate.balanceMinor!=null)original.balanceMinor=candidate.balanceMinor;
            original.confidence=Math.min(original.confidence,30);
            return original;
        }
        if(candidate.type==null)return original;

        // A recognised own-name transfer still requires explicit account selection.
        if(FinanceRules.TRANSFER.equals(original.type)) {
            original.amountMinor=candidate.amountMinor;
            original.currency=candidate.currency;
            if(candidate.balanceMinor!=null)original.balanceMinor=candidate.balanceMinor;
            original.confidence=45;
            return original;
        }
        original.type=candidate.type;
        original.amountMinor=candidate.amountMinor;
        original.currency=candidate.currency;
        if(candidate.balanceMinor!=null)original.balanceMinor=candidate.balanceMinor;
        original.confidence=65; // deliberate review gate: never silently import new Arabic rules
        return original;
    }
}
