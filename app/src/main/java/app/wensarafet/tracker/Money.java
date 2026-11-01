package app.wensarafet.tracker;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public final class Money {
    private Money() {}

    /**
     * Exact two-decimal money parsing. Never silently round a price or read
     * grouped "1,000" as a one-unit decimal amount.
     */
    public static Long parseMinor(String raw) {
        try {
            if (raw == null || raw.trim().isEmpty()) return null;
            StringBuilder normalized = new StringBuilder();
            for (int offset = 0; offset < raw.length();) {
                int cp=raw.codePointAt(offset);
                offset+=Character.charCount(cp);
                int digit=Character.digit(cp,10);
                if(digit>=0)normalized.append((char)('0'+digit));
                else if(cp==0x066B)normalized.append('.');
                else if(cp==0x066C)normalized.append(',');
                else if(cp==0x2212)normalized.append('-');
                else normalized.appendCodePoint(cp);
            }
            String value=normalized.toString().trim();
            String sign="";
            if(value.startsWith("-") || value.startsWith("+")) {
                sign=value.substring(0,1);
                value=value.substring(1);
            }
            if(value.isEmpty())return null;
            int comma=value.lastIndexOf(','),dot=value.lastIndexOf('.');
            char decimal=0,group=0;
            if(comma>=0 && dot>=0) {
                decimal=comma>dot?',':'.';
                group=decimal==','?'.':',';
            }
            String whole=value, fraction="";
            if(decimal!=0) {
                int index=value.lastIndexOf(decimal);
                whole=value.substring(0,index);
                fraction=value.substring(index+1);
                if(!fraction.matches("[0-9]{1,2}") || !groupedInteger(whole,group)) return null;
                whole=whole.replace(String.valueOf(group),"");
            } else if(comma>=0 || dot>=0) {
                char separator=comma>=0?',':'.';
                String separatorText=String.valueOf(separator);
                int occurrences=value.length()-value.replace(separatorText,"").length();
                // Commas are a common thousands grouping separator; dots with
                // exactly three fractional digits are ambiguous, so reject them.
                boolean grouped=value.matches("[0-9]{1,3}("+java.util.regex.Pattern.quote(separatorText)+"[0-9]{3})+");
                if(grouped && (separator==',' || occurrences>1)) {
                    whole=value.replace(separatorText,"");
                } else if(occurrences==1) {
                    int index=value.indexOf(separator);
                    whole=value.substring(0,index);
                    fraction=value.substring(index+1);
                    if(!whole.matches("[0-9]+") || !fraction.matches("[0-9]{1,2}")) return null;
                } else return null;
            }
            if(!whole.matches("[0-9]+"))return null;
            BigDecimal number=new BigDecimal(sign+whole+(fraction.isEmpty()?"":"."+fraction));
            return number.setScale(2,RoundingMode.UNNECESSARY).movePointRight(2).longValueExact();
        } catch (Exception ignored) { return null; }
    }

    private static boolean groupedInteger(String text, char group) {
        String quoted=java.util.regex.Pattern.quote(String.valueOf(group));
        return text.matches("[0-9]{1,3}("+quoted+"[0-9]{3})+");
    }

    public static String format(long minor, String currency) {
        BigDecimal value = BigDecimal.valueOf(minor, 2);
        return new DecimalFormat("#,##0.00").format(value) + " " + currency;
    }
}
