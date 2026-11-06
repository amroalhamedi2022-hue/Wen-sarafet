package app.wensarafet.tracker;

public final class UssdCodes {
    private UssdCodes() {}

    public static String jawwalFriend(String pin, String mobile, String amount) {
        return "*110*1*" + pin + "*" + mobile + "*" + amount + "*1#";
    }

    public static String jawwalMerchant(String pin, String mobile, String amount) {
        return "*110*2*" + pin + "*" + mobile + "*" + amount + "*1#";
    }

    public static String bankOfPalestine(String pin) {
        return "*267*" + pin + "#";
    }

    public static String palPay(String mobile, String amount) {
        return "*370*1*1*" + mobile + "*" + amount + "#";
    }
}
