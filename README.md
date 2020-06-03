# VideoPreload
对AndroidVideoCache开源库的补充，支持预加载短视频数据的能力。
AndroidVideoCache只支持边下边播以及缓存的能力，但是一般情况下，为了短视频首帧能秒出，以MP4为例，如果不提前预加载的数据的情况下，播放器需要先下载MP4格式的头部数据以及几帧数据之后才开始渲染，这其中无疑下载的耗时最大的决定了首帧出现的时间，从而在秒出效果上是有影响的。因此提前做预加载就显得有必要。

#### 几个重要的类
考虑到一种场景如feed信息流中就不乏存在短视频，在点击某个短视频进入全屏页面的时候，一般也像抖音那样可以上下滑动列表的全屏列表页。

因此VideoPreload库涉及几个类：

1. VideoPreLoadFuture：每个需要用到短视频列表的页面需要初始化， 后续拿着该实例进行相应操作（如下两个方法）
```
/**
  * @param  context
  * @param  preloadBusId 每个页面对应一个preloadBusId
 /
public VideoPreLoadFuture(Context context, String preloadBusId)
// 增量添加视频列表
public void addUrls(List<String> urls);
// 全量添加视频列表
public void updateUrls(List<String> urls)；
```

2. PreloadManager: 预加载VideoPreLoadFuture能力管理类
```
// 为方便管理，使用者可通过preloadBusId获取VideoPreLoadFuture实例，可选调用
public VideoPreLoadFuture getVideoPreLoadFuture(String preloadBusId)；

/**
  *接入者的播放组件在开始播放的时候调用该方法，参数preloadBusId和VideoPreLoadFuture
  * 初始化的VideoPreLoadFuture保持一致，url为短视频播放地址
*/
public void currentVideoPlay(String preloadBusId, String url) 
```

#### 接入例子
1. 在某个Activity或者Fragment下，初始化VideoPreLoadFuture
```
  if (videoPreLoadFuture == null) {
       videoPreLoadFuture = new VideoPreLoadFuture(context, "test");
  }
```
2. 请求完短视频列表数据之后，进行增量或者全量设置视频url列表，用于预加载
```
  videoPreLoadFuture.addUrls(Arrays.asList(PreloadManager.MOCK_DATA));
```
或
```
  PreLoadManager.getInstance(context).getVideoPreLoadFuture("test").addUrls(Arrays.asList(PreloadManager.MOCK_DATA));
```
3. 在播放组件中，如XXXVideoView（一般情况，每个业务方都会有关于播放器的封装view）的开始播放方法如start方法设置当前正在播放的url，
   PreloadManager的currentVideoPlay会根据当前播放url对应所在url列表中的位置，进行附近url的提前预加载
```
  public void start() {
    // 通过该方法可打印出对应url是否已经预加载完成并存在相关的数据
    if (PreloadManager.getInstance().hasEnoughCache(url)) {
            Log.d(PreloadManager.TAG, url + " has a cache");
    }
    
    // 参数preloadBusId和VideoPreLoadFuture初始化的VideoPreLoadFuture保持一致，url为当前短视频播放地址
    PreloadManager.getInstance().currentVideoPlay(preloadBusId, url);
  }
```

很简单的几步就可以完成短视频数据的预加载，而且也完美的配合AndroidVideoCache的能力

#### 实现原理
![image](https://github.com/zhuozp/VideoPreload/blob/master/images/preload.jpg)

#### 看个demo录屏

![示例](https://github.com/zhuozp/VideoPreload/blob/master/images/device-2020-05-15-170554.gif)

#### 点个star
觉得不错的话还请点个赞呗，接下来将对不同网络环境下预加载的处理以及根据线上环境，来进一步优化预加载的逻辑
