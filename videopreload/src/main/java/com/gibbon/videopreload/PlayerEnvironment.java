package com.gibbon.videopreload;

import android.content.Context;
import android.text.TextUtils;

import com.danikula.videocache.HttpProxyCacheServer;
import com.danikula.videocache.file.Md5FileNameGenerator;
import com.gibbon.videopreload.util.StorageUtils;

import java.io.File;

/**
 * @author zhipeng.zhuo
 * @date 2020-05-13
 */
public class PlayerEnvironment {

    private static HttpProxyCacheServer proxy;

    public static final String VIDEO_CACHE_ID = "videoCacheId";

    public static HttpProxyCacheServer getProxy(Context context) {
        return proxy == null ? (proxy = newProxy(context)) : proxy;
    }

    private static HttpProxyCacheServer newProxy(Context context) {
        return new HttpProxyCacheServer.Builder(context.getApplicationContext())
                .build();
    }



    private static String path;

    public static String getCompleteCachePath(Context context, String url) {
        try {
            if (TextUtils.isEmpty(path)) {
                File cacheRoot = StorageUtils.getIndividualCacheDirectory(context);
                path = cacheRoot.getAbsolutePath();
            }
            String name = new Md5FileNameGenerator().generate(url);
            if(TextUtils.isEmpty(name)){
                return null;
            }
            File file = new File(path, name);
            if (file.exists() && file.canRead() && file.length() > 1024) {
                return file.getAbsolutePath();
            }
        } catch (Throwable e) {
        }
        return null;
    }




    public static String getCachePathForCacheKey(Context context, String cacheKey) {
        try {
            if (TextUtils.isEmpty(cacheKey) || context == null) {
                return null;
            }
            if (TextUtils.isEmpty(path)) {
                File cacheRoot = StorageUtils.getIndividualCacheDirectory(context);
                path = cacheRoot.getAbsolutePath();
            }

            File file = new File(path, cacheKey);
            if (file.exists() && file.canRead() && file.length() > 1024) {
                return file.getAbsolutePath();
            }
        } catch (Throwable e) {
        }
        return null;
    }
}
