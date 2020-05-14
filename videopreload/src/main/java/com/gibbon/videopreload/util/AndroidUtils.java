package com.gibbon.videopreload.util;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.danikula.videocache.file.Md5FileNameGenerator;
import com.gibbon.videopreload.PlayerEnvironment;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author zhipeng.zhuo
 * @date 2020-05-14
 */
public class AndroidUtils {

    public static String textToMD5(String plainText) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(plainText.getBytes());
            byte[] b = md.digest();

            int i;

            StringBuilder buf = new StringBuilder();
            for (final byte b1 : b) {
                i = b1;
                if (i < 0)
                    i += 256;
                if (i < 16)
                    buf.append("0");
                buf.append(Integer.toHexString(i));
            }
            return buf.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }

    }

    public static final String TEMP_POSTFIX = ".download";

    public static boolean hasEnoughCache(Context context, Md5FileNameGenerator generator, String url) {
        try {
            File cacheRoot = StorageUtils.getIndividualCacheDirectory(context);
            String path = cacheRoot.getAbsolutePath();

            String name = generator.generate(url);
            if(TextUtils.isEmpty(name)){
                return false;
            }
            File file = new File(path, name);
            if (file.exists() && file.canRead() && file.length() > 1024) {
                return true;
            }

            file = new File(path, name + TEMP_POSTFIX);

            if (file.exists() && file.canRead() && file.length() > 102400) {
                return true;
            }
        } catch (Throwable e) {
        }
        return false;
    }

    public static String appendUrl(String url, String cacheKey) {
        StringBuilder appendQuery = new StringBuilder(100);
        appendQuery.append(PlayerEnvironment.VIDEO_CACHE_ID + "=" + cacheKey);
        return AndroidUtils.appendUri(url, appendQuery);
    }

    public static String appendUri(String uri, StringBuilder appendQuery) {
        String result = uri;
        try {
            Uri olduri = Uri.parse(uri);
            String newQuery = olduri.getEncodedQuery();
            if (TextUtils.isEmpty(newQuery)) {
                newQuery = appendQuery.toString();
            } else {
                newQuery = appendQuery + "&" + newQuery;
            }
            //todo <scheme>://<authority><absolute path>?<query>#<fragment>
            Uri.Builder builder = new Uri.Builder();
            builder.scheme(olduri.getScheme()).encodedAuthority(olduri.getEncodedAuthority()).encodedPath(olduri.getEncodedPath()).encodedQuery(newQuery).fragment(olduri.getEncodedFragment());
            result = builder.build().toString();
        } catch (Exception e) {

        }
        return result;
    }
}
