package app.wensarafet.tracker;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Network I/O always runs off the main thread. Fails closed into manual-rate mode. */
public final class BopRateFetcher {
    private BopRateFetcher() {}
    public static String fetchBuyUsdIls() throws Exception {
        URL url=new URL("https://web.bankofpalestine.com/ar/retail/treasury");
        HttpURLConnection c=(HttpURLConnection)url.openConnection();
        c.setConnectTimeout(7000);c.setReadTimeout(7000);
        c.setRequestProperty("Accept","text/html");
        try{
            if(c.getResponseCode()!=200)throw new IllegalArgumentException("تعذّر الوصول لصفحة البنك");
            try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] buffer=new byte[4096];int n;
                while((n=in.read(buffer))!=-1){
                    if(out.size()+n>1_000_000)throw new IllegalArgumentException("رد البنك كبير جدًا");
                    out.write(buffer,0,n);
                }
                return BopIndicativeRate.usdIlsBuyRate(new String(out.toByteArray(), StandardCharsets.UTF_8));
            }
        }finally{c.disconnect();}
    }
}
