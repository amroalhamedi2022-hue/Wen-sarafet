package app.wensarafet.tracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Time-only boundaries for a read-only category analysis checkpoint.
 * Never edits a transaction, split, account, debt, or monthly budget.
 * Each segment is [startInclusive, endExclusive), so every expense belongs
 * to exactly one period, including when timestamps equal a checkpoint.
 */
public final class CategoryCheckpointRules {
    public static final int MAX_CHECKPOINTS = 24;
    private CategoryCheckpointRules() {}

    public static List<Long> decode(String stored) {
        if(stored==null || stored.isEmpty())return Collections.emptyList();
        String[] parts=stored.split(",",-1);
        if(parts.length>MAX_CHECKPOINTS)
            throw new IllegalArgumentException("وصلت لأقصى عدد من نقاط البداية لهذا التصنيف");
        List<Long> result=new ArrayList<>(parts.length);
        long previous=0L;
        for(String part:parts) {
            long timestamp;
            try {
                if(!part.matches("[0-9]{1,17}"))
                    throw new NumberFormatException("Invalid timestamp");
                timestamp=Long.parseLong(part);
            } catch(NumberFormatException error) {
                throw new IllegalArgumentException("تواريخ نقاط البداية المحفوظة غير صالحة",error);
            }
            if(timestamp<=previous)
                throw new IllegalArgumentException("ترتيب نقاط البداية المحفوظة غير صالح");
            result.add(timestamp);
            previous=timestamp;
        }
        return Collections.unmodifiableList(result);
    }

    public static String append(String stored,long timestamp) {
        List<Long> old=decode(stored);
        if(timestamp<=0 || (!old.isEmpty() && timestamp<=old.get(old.size()-1)))
            throw new IllegalArgumentException("انتظر قليلًا قبل إنشاء نقطة بداية جديدة");
        if(old.size()>=MAX_CHECKPOINTS)
            throw new IllegalArgumentException("وصلت لأقصى عدد من نقاط البداية لهذا التصنيف");
        return old.isEmpty()?Long.toString(timestamp):stored+","+timestamp;
    }

    public static String removeLast(String stored) {
        List<Long> current=decode(stored);
        if(current.isEmpty())return "";
        if(current.size()==1)return "";
        return stored.substring(0,stored.lastIndexOf(','));
    }

    public static long periodStart(List<Long> checkpoints,int periodIndex) {
        if(periodIndex<0 || periodIndex>checkpoints.size())
            throw new IllegalArgumentException("رقم الفترة غير صالح");
        return periodIndex==0?0L:checkpoints.get(periodIndex-1);
    }
    public static long periodEnd(List<Long> checkpoints,int periodIndex,long nowExclusive) {
        if(periodIndex<0 || periodIndex>checkpoints.size() || nowExclusive<=0)
            throw new IllegalArgumentException("نهاية الفترة غير صالحة");
        return periodIndex==checkpoints.size()?nowExclusive:checkpoints.get(periodIndex);
    }
}
