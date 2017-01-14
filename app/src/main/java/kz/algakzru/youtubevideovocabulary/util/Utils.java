package kz.algakzru.youtubevideovocabulary.util;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by algakzru on 1/28/15.
 */
public class Utils {

    private final static String TAG = "FFMPEG INSTALL";

    public static void installBinaryFromRaw(Context context, int resId, File file) {
        final InputStream rawStream = context.getResources().openRawResource(resId);
        final OutputStream binStream = getFileOutputStream(file);

        if (rawStream != null && binStream != null) {
            pipeStreams(rawStream, binStream);

            try {
                rawStream.close();
                binStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to close streams!", e);
            }

            doChmod(file, 777);
        }
    }

    public static OutputStream getFileOutputStream(File file) {
        try {
            return new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found attempting to stream file.", e);
        }
        return null;
    }

    private static final int IO_BUFFER_SIZE = 4 * 1024;

    public static void pipeStreams(InputStream is, OutputStream os) {
        byte[] buffer = new byte[IO_BUFFER_SIZE];
        int count;
        try {
            while ((count = is.read(buffer)) > 0) {
                os.write(buffer, 0, count);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error writing stream.", e);
        }
    }

    public static void doChmod(File file, int chmodValue) {
        final StringBuilder sb = new StringBuilder();
        sb.append("chmod");
        sb.append(' ');
        sb.append(chmodValue);
        sb.append(' ');
        sb.append(file.getAbsolutePath());

        try {
            Runtime.getRuntime().exec(sb.toString());
        } catch (IOException e) {
            Log.e(TAG, "Error performing chmod", e);
        }
    }

    public static  void copyFile(File fileFrom, File fileTo) throws IOException {
        fileTo.delete();
        InputStream in = new FileInputStream(fileFrom);
        OutputStream out = new FileOutputStream(fileTo);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    public static  void copyLogo(Context context, int resId, File fileTo) throws IOException {
        fileTo.delete();
        InputStream in = context.getResources().openRawResource(resId);
        OutputStream out = new FileOutputStream(fileTo);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

}
