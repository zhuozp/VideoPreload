package com.gibbon.videopreload;


import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.danikula.videocache.HttpProxyCacheServer;
import com.danikula.videocache.file.Md5FileNameGenerator;
import com.gibbon.videopreload.util.AndroidUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * @author zhipeng.zhuo
 * @date 2020-05-09
 */
@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class PreLoadManager {

    public static final String TAG = PreLoadManager.class.getSimpleName();

    public Stack<String> mBusIdStack = new Stack<>();
    public ArrayMap<String, VideoPreLoadFuture> videoPreLoadFutureArrayMap = new ArrayMap<>();
    public List<PreLoadTask> preLoadTaskPool = new ArrayList<>();

    public String currentBusId;

    public HttpProxyCacheServer httpProxyCacheServer;
    public Md5FileNameGenerator fileNameGenerator;
    public Context context;
    public Handler handler;

    private static volatile PreLoadManager sInstance;

    public static final String[] MOCK_DATA = {
            "30002464111",
            "30002248304",
            "30001730420",
            "30002202143",
            "30000996828",
            "30002168746",
            "30002096723",
            "30002532679",
            "30002532677",
            "30002532667",
            "30002532661",
            "30002532652",
            "30002532645",
            "30002532631",
            "30002532630",
            "30002532613",
            "30002532610",
            "30002532604",
            "30002532599",
            "30002532594",
            "30002532587",
            "30002532586",
            "30002532585",
            "30002532584",
            "30002532575",
            "30002532574",
            "30002532567",
            "30002532566",
            "30002532559",
            "30002532558",
            "30002532556",
            "30002532549",
            "30002532541",
            "30002532539",
            "30002532533",
            "30002532532",
            "30002532529",
    };

    private PreLoadManager(Context context) {
        httpProxyCacheServer = PlayerEnvironment.getProxy(context);
        fileNameGenerator = new Md5FileNameGenerator();
        this.context = context;
    }

    public static PreLoadManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (PreLoadManager.class) {
                if (sInstance == null) {
                    sInstance = new PreLoadManager(context);
                }
            }
        }

        return sInstance;
    }


    protected void putFuture(String busId, VideoPreLoadFuture videoPreLoadFuture) {
        videoPreLoadFutureArrayMap.put(busId, videoPreLoadFuture);
    }

    protected void removeFuture(String busId) {
        videoPreLoadFutureArrayMap.remove(busId);
    }

    public VideoPreLoadFuture getVideoPreLoadFuture(String busId) {
        return videoPreLoadFutureArrayMap.get(busId);
    }

    public void currentVideoPlay(String busId, String url) {
        if (TextUtils.isEmpty(busId) || TextUtils.isEmpty(url)) {
            return;
        }

        VideoPreLoadFuture videoPreLoadFuture = getVideoPreLoadFuture(busId);

        if (videoPreLoadFuture != null) {
            videoPreLoadFuture.currentPlayUrl(url);
        }
    }

    public boolean hasEnoughCache(String url) {
        return AndroidUtils.hasEnoughCache(context, fileNameGenerator, url);
    }

    protected synchronized PreLoadTask createTask(final String busId, String url, int index) {
        PreLoadTask preLoadTask = null;
        if (preLoadTaskPool.size() > 0) {
            preLoadTask = preLoadTaskPool.get(0);
            preLoadTaskPool.remove(0);
            Log.d(TAG, "get PreLoadTask from pool");
        }

        if (preLoadTask == null) {
            preLoadTask = new PreLoadTask(context, url, index);
            Log.d(TAG, "new PreLoadTask");
            final PreLoadTask tmpPreLoadTask = preLoadTask;
            preLoadTask.setiTaskCallback(new PreLoadTask.ITaskCallback() {
                @Override
                public void finish() {
                    VideoPreLoadFuture videoPreLoadFuture = getVideoPreLoadFuture(busId);
                    if (videoPreLoadFuture != null) {
                        videoPreLoadFuture.removeTask(tmpPreLoadTask);
                    }
                    recyclerPreLoadTask(tmpPreLoadTask);
                }
            });
        } else {
            preLoadTask.init(url, index);
        }

        return preLoadTask;
    }

    protected synchronized void recyclerPreLoadTask(PreLoadTask task) {
        if (preLoadTaskPool.size() <= 20) {
            Log.d(TAG, "recycler PreLoadTask into pool");
            preLoadTaskPool.add(task);
        }
    }

    protected String getLocalUrlAppendWithUrl(String url) {
        if (httpProxyCacheServer != null) {
            return httpProxyCacheServer.getProxyUrl(url);
        }

        return url;
    }
}


