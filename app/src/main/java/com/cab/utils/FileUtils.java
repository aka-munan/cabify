package com.cab.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class FileUtils {
    public static Bitmap getBitmapFromUri(Context context, Uri uri) throws IOException {
        final String scheme = uri.getScheme();
        if (ContentResolver.SCHEME_CONTENT.equals(scheme)
                || ContentResolver.SCHEME_FILE.equals(scheme)) {
            ImageDecoder.Source src = ImageDecoder.createSource(context.getContentResolver(),uri);
           return ImageDecoder.decodeBitmap(src);
        }
        return null;
    }
    public static byte[] getBytesFromBitmap(Bitmap bitmap,int quality){
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.WEBP,quality,outputStream);
        return outputStream.toByteArray();
    }
}
