package app.wensarafet.tracker;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Read-only provider serving five generated tones to Android SystemUI.
 * No user data, contacts, license, PIN or database content is exposed.
 */
public final class NotificationSoundProvider extends ContentProvider {
    public static final String AUTHORITY="app.wensarafet.tracker.notification-sounds";
    private static final String DIR="notification_tones";
    public static Uri uri(String type) {
        if(!valid(type))throw new IllegalArgumentException("نوع إشعار غير صالح");
        return Uri.parse("content://"+AUTHORITY+"/"+type);
    }
    public static boolean isOurSound(Uri uri) {
        return uri!=null && "content".equals(uri.getScheme()) &&
                AUTHORITY.equals(uri.getAuthority()) && valid(uri.getLastPathSegment());
    }
    private static boolean valid(String type) {
        return "review".equals(type) || "saved".equals(type) || "reminder".equals(type) ||
                "budget".equals(type) || "sync".equals(type);
    }
    public static synchronized File ensureSound(Context context,String type) throws IOException {
        if(!valid(type))throw new FileNotFoundException("Unknown notification sound");
        File directory=new File(context.getFilesDir(),DIR);
        if(!directory.exists() && !directory.mkdirs() && !directory.exists())
            throw new IOException("Could not create local sound folder");
        File finalFile=new File(directory,type+".wav");
        if(finalFile.isFile() && finalFile.length()>44)return finalFile;
        File temporary=new File(directory,type+".wav.tmp");
        byte[] audio=NotificationTonePcm.wav(type);
        try(FileOutputStream stream=new FileOutputStream(temporary)) {
            stream.write(audio);stream.getFD().sync();
        }
        if(!temporary.renameTo(finalFile) && !finalFile.isFile())
            throw new IOException("Could not save original notification sound");
        if(!finalFile.isFile() || finalFile.length()!=audio.length)
            throw new IOException("Notification sound was not saved completely");
        return finalFile;
    }
    @Override public boolean onCreate() { return true; }
    @Override public String getType(Uri uri) {
        return isOurSound(uri)?"audio/wav":null;
    }
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode)
            throws FileNotFoundException {
        if(!"r".equals(mode) || !isOurSound(uri))
            throw new FileNotFoundException("Only original read-only notification tones are available");
        try {
            return ParcelFileDescriptor.open(ensureSound(getContext(),uri.getLastPathSegment()),
                    ParcelFileDescriptor.MODE_READ_ONLY);
        } catch(IOException error) {
            FileNotFoundException missing=new FileNotFoundException("Sound not available");
            missing.initCause(error);
            throw missing;
        }
    }
    @Override public Cursor query(Uri uri,String[] columns,String selection,
                                  String[] selectionArgs,String sortOrder) {return null;}
    @Override public Uri insert(Uri uri,ContentValues values) {
        throw new UnsupportedOperationException("Read-only sound provider");
    }
    @Override public int delete(Uri uri,String selection,String[] args) {
        throw new UnsupportedOperationException("Read-only sound provider");
    }
    @Override public int update(Uri uri,ContentValues values,String selection,String[] args) {
        throw new UnsupportedOperationException("Read-only sound provider");
    }
}
