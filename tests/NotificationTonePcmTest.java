import app.wensarafet.tracker.NotificationTonePcm;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class NotificationTonePcmTest {
    static void check(boolean condition,String reason){
        if(!condition)throw new AssertionError(reason);
    }
    public static void main(String[] args) {
        Set<Integer> fingerprints=new HashSet<>();
        for(String type:new String[]{"review","saved","reminder","budget","sync"}) {
            byte[] wave=NotificationTonePcm.wav(type);
            check(wave.length>4000 && wave.length<45000,"short original notification "+type);
            check(new String(wave,0,4,StandardCharsets.US_ASCII).equals("RIFF"),"RIFF "+type);
            check(new String(wave,8,4,StandardCharsets.US_ASCII).equals("WAVE"),"WAVE "+type);
            check(new String(wave,12,4,StandardCharsets.US_ASCII).equals("fmt "),"fmt "+type);
            check(new String(wave,36,4,StandardCharsets.US_ASCII).equals("data"),"data "+type);
            ByteBuffer reader=ByteBuffer.wrap(wave).order(ByteOrder.LITTLE_ENDIAN);
            check(reader.getInt(4)+8==wave.length,"RIFF length "+type);
            check(reader.getInt(24)==22050,"rate "+type);
            check(reader.getShort(34)==16,"PCM16 "+type);
            check(reader.getInt(40)+44==wave.length,"PCM samples "+type);
            int nonzero=0;
            for(int i=44;i<wave.length;i+=2)
                if(reader.getShort(i)!=0)nonzero++;
            check(nonzero>100,"tone not silent "+type);
            check(fingerprints.add(Arrays.hashCode(wave)),"sound must be unique "+type);
        }
        try { NotificationTonePcm.wav("../finance"); throw new AssertionError("unknown tone");}
        catch(IllegalArgumentException expected){}
        System.out.println("NotificationTonePcmTest PASS: five distinct short low-volume original WAV tones");
    }
}
