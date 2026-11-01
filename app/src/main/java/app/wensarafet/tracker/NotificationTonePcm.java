package app.wensarafet.tracker;

import java.io.ByteArrayOutputStream;

/** Five original, low-volume notification earcons, generated without media assets. */
public final class NotificationTonePcm {
    private static final int RATE=22050;
    private NotificationTonePcm() {}

    public static byte[] wav(String type) {
        int[][] melody;
        if("review".equals(type))melody=new int[][]{{523,105},{0,55},{659,125}};
        else if("saved".equals(type))melody=new int[][]{{440,90},{0,25},{587,140}};
        else if("reminder".equals(type))melody=new int[][]{{392,70},{0,65},{392,70},{0,65},{494,100}};
        else if("budget".equals(type))melody=new int[][]{{659,100},{0,45},{523,115},{0,45},{392,130}};
        else if("sync".equals(type))melody=new int[][]{{349,125},{0,65},{440,100}};
        else throw new IllegalArgumentException("نوع نغمة غير معروف");
        int samples=0;
        for(int[] note:melody)samples+=RATE*note[1]/1000;
        int bytes=samples*2;
        ByteArrayOutputStream out=new ByteArrayOutputStream(44+bytes);
        ascii(out,"RIFF");little(out,36+bytes,4);ascii(out,"WAVE");
        ascii(out,"fmt ");little(out,16,4);little(out,1,2);little(out,1,2);
        little(out,RATE,4);little(out,RATE*2,4);little(out,2,2);little(out,16,2);
        ascii(out,"data");little(out,bytes,4);
        for(int[] note:melody) {
            int count=RATE*note[1]/1000;
            for(int i=0;i<count;i++) {
                double fade=Math.min(1.0,Math.min(i/(RATE*.008), (count-i-1)/(RATE*.030)));
                double t=(double)i/RATE;
                double sample=note[0]==0?0:
                        .19*fade*Math.sin(2.0*Math.PI*note[0]*t);
                int pcm=(int)Math.round(sample*32767);
                little(out,pcm,2);
            }
        }
        return out.toByteArray();
    }

    private static void ascii(ByteArrayOutputStream out,String four) {
        for(int i=0;i<four.length();i++)out.write((byte)four.charAt(i));
    }
    private static void little(ByteArrayOutputStream out,int value,int size) {
        for(int i=0;i<size;i++)out.write((value >>> (8*i)) & 0xff);
    }
}
