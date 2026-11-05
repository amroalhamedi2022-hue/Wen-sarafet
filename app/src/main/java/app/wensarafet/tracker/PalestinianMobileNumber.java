package app.wensarafet.tracker;

/** Convert Palestinian mobile contact forms into the exact local 05XXXXXXXX dial format. */
public final class PalestinianMobileNumber {
    private PalestinianMobileNumber() {}

    public static String normalize(String input) {
        if(input==null) return null;
        StringBuilder digits=new StringBuilder();
        boolean plusSeen=false;
        for(int i=0;i<input.length();) {
            int codepoint=input.codePointAt(i);
            i+=Character.charCount(codepoint);
            int digit=Character.digit(codepoint,10);
            if(digit>=0) digits.append((char)('0'+digit));
            else if(codepoint=='+' && digits.length()==0 && !plusSeen)plusSeen=true;
            else if(codepoint==' ' || codepoint=='-' || codepoint=='(' || codepoint==')'
                    || codepoint=='.' || codepoint=='\u00A0' || codepoint=='\u200E'
                    || codepoint=='\u200F' || codepoint=='\u202A' || codepoint=='\u202C')
                continue;
            else return null; // Reject extensions or USSD-control characters.
        }
        String raw=digits.toString();
        if(raw.startsWith("00970") || raw.startsWith("00972"))raw=raw.substring(5);
        else if(raw.startsWith("970") || raw.startsWith("972"))raw=raw.substring(3);
        else if(plusSeen)return null;
        if(raw.length()==9 && raw.startsWith("5"))raw="0"+raw;
        if(raw.matches("05[0-9]{8}") && (raw.startsWith("059") || raw.startsWith("056")))
            return raw;
        return null;
    }

    public static String dialTail(String input) {
        String local=normalize(input);
        return local==null?null:local.substring(2);
    }
}
