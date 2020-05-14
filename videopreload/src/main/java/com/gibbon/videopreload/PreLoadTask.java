package com.gibbon.videopreload;


import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.gibbon.videopreload.util.AndroidUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author zhipeng.zhuo
 * @date 2020-05-14
 */
public class PreLoadTask implements Runnable {

    public static final int STATUS_INIT = 0;
    public static final int STATUS_PRELOADING = 1;
    public static final int STATUS_LOADING = 2;
    public static final int STATUS_COMPLETED = 3;
    public static final int STATUS_CANCEL = 4;

    private volatile int status = STATUS_INIT;
    public volatile String url;
    public volatile int index;
    private volatile String cacheKey;

    private Context context;
    private long startTime;

    private ITaskCallback iTaskCallback;
    private ReentrantLock lock = new ReentrantLock();
    private Condition waitCondition = lock.newCondition();

    public PreLoadTask(Context context, final String url, final int index) {
        this.context = context;
        this.url = url;
        this.index = index;
        if (!TextUtils.isEmpty(url)) {
            this.cacheKey = AndroidUtils.textToMD5(url);
        }
    }

    public void init(String url, int index) {
        lock.lock();
        try {
            this.url = url;
            this.index = index;
            this.cacheKey = AndroidUtils.textToMD5(url);
            this.status = STATUS_INIT;
        } finally {
            lock.unlock();
        }
    }

    public void setiTaskCallback(ITaskCallback callback) {
        this.iTaskCallback = callback;
    }

    public void setStatus(int status) {
        lock.lock();
        try {
            this.status = status;
            Log.d("TTTT", "status change1 " + this.status + " index: " + index);
        } finally {
            lock.unlock();
        }
    }

    public void run() {
        Log.d(PreLoadManager.TAG, Thread.currentThread().getName() + "----task run begin----");
        if (status == STATUS_CANCEL) {
            Log.d(PreLoadManager.TAG, Thread.currentThread().getName() + " has cancel");
            finish();
            return;
        }

        if (TextUtils.isEmpty(this.url)) {
            Log.d(PreLoadManager.TAG, Thread.currentThread().getName() + " url is empty");
            finish();
            return;
        }

        status = STATUS_PRELOADING;
        preload();

        Log.d(PreLoadManager.TAG, Thread.currentThread().getName() + "----task run end----");
    }

    private void preload() {
        if (status != STATUS_PRELOADING) {
            Log.d(PreLoadManager.TAG, Thread.currentThread().getName() + "preload() " + "status is: " + status);
            return;
        }

        if (PreLoadManager.getInstance(context).hasEnoughCache(this.url)) {
            Log.d(PreLoadManager.TAG, Thread.currentThread().getName() + "videoId " + url + " has enough cache");
            finish();
            return;
        }

        InputStream inputStream  = null;
        long start = System.currentTimeMillis();
        boolean flag = false;
        try {
            URL url = new URL(PreLoadManager.getInstance(context).getLocalUrlAppendWithUrl(this.url));
            URLConnection urlConnection = url.openConnection();
            urlConnection.setRequestProperty("Range","bytes=0-204799");
            urlConnection.setConnectTimeout(5000);
            urlConnection.connect();

            inputStream = urlConnection.getInputStream();
            status = STATUS_LOADING;
            Log.d(PreLoadManager.TAG, Thread.currentThread().getName() + "PreLoadTask run: loading" );
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int length = 0;
            int tmp = 0;
            while (status == STATUS_LOADING && (tmp = inputStream.read(buffer)) != -1) {
                //Since we just need to kick start the prefetching, dont need to do anything here
                //  or we can use ByteArrayOutputStream to write down the data to disk
                length += tmp;
//                Log.d(PreloadManager.TAG, Thread.currentThread().getName() + " downloaded length: " + length + "");
                if (!flag) {
                    Log.d("TTTT", "status change2: " + status + " index: " + index);
                    flag = true;
                }

                if (length >= 102400) {
                    status = STATUS_COMPLETED;
                }
            }

            if (status == STATUS_CANCEL) {
                Log.d("TTTT", Thread.currentThread().getName() + "task cancel!");
            }

            inputStream.close();
        } catch (IOException e) {
            Log.d(PreLoadManager.TAG, e.getMessage() + "");
        }  catch (Exception e) {
            Log.d(PreLoadManager.TAG, e.getMessage() + "");
        } finally {
            Log.d(PreLoadManager.TAG, Thread.currentThread().getName() + "preload video url [url: " + PreLoadTask.this.url + ", time: "
                    + (System.currentTimeMillis() - start) + "ms, index: " + PreLoadTask.this.index + "， status: " + this.status + "]");

            finish();
        }

    }

    private void finish() {
        if (iTaskCallback != null) {
            iTaskCallback.finish();
        }
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof PreLoadTask) {
//            Log.d(PreloadManager.TAG, "equals [" + this.url + ", " + ((PreLoadTask)obj).url + "]");
            return !TextUtils.isEmpty(this.url) && this.url.equals(((PreLoadTask)obj).url);
        }

//        Log.d(PreloadManager.TAG, "two PreLoadTask not equal");
        return false;
    }

    /**
     * 此处没涉及map/set操作,涉及需要重写该方法
     * */
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    interface ITaskCallback {
        void finish();
    }
}

