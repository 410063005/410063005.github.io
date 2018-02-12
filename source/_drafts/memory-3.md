---
title: Android内存优化 
tags:
  - Android
  - 性能
---
几轮紧张的迭代之后，我们应用的占用非常恐怖。决定抽空优化。
<!--more-->
# 内存

## 多进程问题

AndroidManifest.xml中使用`android:process`属性给应用配置了三个进程。分别是：

+ `com.tencent.igame` - 主进程
+ `com.tencent.igame:QALSERVICE` - IM进程
+ `com.tencent.igame:mutil_network_service` - 网络进程

```java
public class IGameApplication extends MultiDexApplication {


    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("cm-pref", "onCreate: begin");
        long t1 = System.currentTimeMillis();
		
        // 这里省略了初始化代码

        long t2 = System.currentTimeMillis();
        Log.i("cm-pref", "onCreate: end. costs = " + (t2 - t1));		
	}
}
```

应用冷启动时打出的日志居然是这样：

```
12-11 13:49:25.034 12851-12851/com.tencent.igame:QALSERVICE I/cm-pref: onCreate: begin
12-11 13:49:25.183 12851-12851/com.tencent.igame:QALSERVICE I/cm-pref: onCreate: end. costs = 149
12-11 13:49:25.537 12883-12883/com.tencent.igame:mutil_network_service I/cm-pref: onCreate: begin
12-11 13:49:25.686 12883-12883/com.tencent.igame:mutil_network_service I/cm-pref: onCreate: end. costs = 149
12-11 13:49:28.535 12927-12927/? I/cm-pref: onCreate: begin
12-11 13:49:28.688 12927-12927/? I/cm-pref: onCreate: end. costs = 153
```

从日志看，`Application.onCreate()`被执行了3次。也就是说，<font color="red">我们做了3次初始化！</font>

以上是模拟器的数据，真机的数据慢得多。比如我的OPPO R7上运行时间如下：

```
12-12 20:18:30.850 12747-12747/com.tencent.igame I/cm-pref: onCreate: begin
12-12 20:18:31.850 12747-12747/com.tencent.igame I/cm-pref: onCreate: end. costs = 1005
12-12 20:18:33.780 12965-12965/com.tencent.igame:mutil_network_service I/cm-pref: onCreate: begin
12-12 20:18:34.920 12965-12965/com.tencent.igame:mutil_network_service I/cm-pref: onCreate: end. costs = 1142
12-12 20:18:36.170 13140-13140/com.tencent.igame:QALSERVICE I/cm-pref: onCreate: begin
12-12 20:18:37.320 13140-13140/com.tencent.igame:QALSERVICE I/cm-pref: onCreate: end. costs = 1154
```

<!--
[Application | Android Developers](https://developer.android.com/reference/android/app/Application.html)中关于`onCreate()`的说明：

> 应用启动时调用，在所有Activity, Service, Receiver等组件创建之前启动。`onCreate()`的实现应该尽可能快(比如可使用延迟初始化)，因为这个方法中花费的时间会直接影响第一个Activity, Service, 或Receiver的启动性能。

[Processes and Application Lifecycle | Android Developers](https://developer.android.com/guide/components/activities/process-lifecycle.html)中关于`Process`的说明：

> Android平台的特点是应用进程的生命周期并不由自身直接控制，而是由系统来控制。
-->

而`android:process`属性其实是有坑的。它将组件运行到另一个进程，所以会在另一个进程中创建Application实例并运行其`onCreate()`方法。具体可以参考这两篇文章：

+ [android:process 的坑，你懂吗](http://www.rogerblog.cn/2016/03/17/android-proess/)
+ [(译)Android多进程应用](http://www.sunmoonblog.com/2017/12/14/multi-process-android-app-1/)

解决的办法是：我们只在主进程中执行初始化代码。

```java
public class IGameApplication extends MultiDexApplication {


    @Override
    public void onCreate() {
        Log.i("cm-pref", "onCreate: begin");
        long t1 = System.currentTimeMillis();
        super.onCreate();
		
		if (isMainProcess()) {
			// 这里省略了初始化代码
		}

        long t2 = System.currentTimeMillis();
        Log.i("cm-pref", "onCreate: end. costs = " + (t2 - t1));		
	}
}
```

`isMainProcess()`方法的实现可以参考[android:process 的坑，你懂吗](http://www.rogerblog.cn/2016/03/17/android-proess/)或[Android 中的多进程，你值得了解的一些知识 | YiFeng's Zone](http://yifeng.studio/2017/06/16/android-multi-process-things/)

真的能省内存吗？真的！

比较优化前后各进程的内存占用，发现IM进程和网络进程内存占用大大减少。

TODO Android Monitor图片
TODO 手机统计图片

优化前

![](/images/1513259406909.png)
![](/images/1513259431744.png)

优化后

![](/images/1513258784775.png)
![](/images/1513258856038.png)

整理数据，对比

1. 内存占用
2. 对象数


数据1 -- Genymotion Nexus 5x 7.0

主进程 - 优化前后不明显
IM进程 - Dalvik Heap由3190KB减少到1783KB
网络进程 - Dalvik Heap由3527KB减少到764KB

```
优化前

** MEMINFO in pid 1566 [com.tencent.igame] **
                   Pss  Private  Private  SwapPss     Heap     Heap     Heap
                 Total    Dirty    Clean    Dirty     Size    Alloc     Free
                ------   ------   ------   ------   ------   ------   ------
  Native Heap    12723    12672        0        0    31232    27020     4211
  Dalvik Heap    42734    42604        0        0    50062    33678    16384

** MEMINFO in pid 1920 [com.tencent.igame:QALSERVICE] **
                   Pss  Private  Private  SwapPss     Heap     Heap     Heap
                 Total    Dirty    Clean    Dirty     Size    Alloc     Free
                ------   ------   ------   ------   ------   ------   ------
  Native Heap     2532     2476        0        0     6656     5482     1173
  Dalvik Heap     3190     3052        0        0     8062     4837     3225

** MEMINFO in pid 1600 [com.tencent.igame:mutil_network_service] **
                   Pss  Private  Private  SwapPss     Heap     Heap     Heap
                 Total    Dirty    Clean    Dirty     Size    Alloc     Free
                ------   ------   ------   ------   ------   ------   ------
  Native Heap     2439     2380        0        0     7168     5665     1502
  Dalvik Heap     3527     3388        0        0     7852     4712     3140


优化后

** MEMINFO in pid 3815 [com.tencent.igame] **
                   Pss  Private  Private  SwapPss     Heap     Heap     Heap
                 Total    Dirty    Clean    Dirty     Size    Alloc     Free
                ------   ------   ------   ------   ------   ------   ------
  Native Heap    12655    12604        0        0    32256    27418     4837
  Dalvik Heap    44125    43992        0        0    49979    33595    16384

** MEMINFO in pid 4062 [com.tencent.igame:QALSERVICE] **
                   Pss  Private  Private  SwapPss     Heap     Heap     Heap
                 Total    Dirty    Clean    Dirty     Size    Alloc     Free
                ------   ------   ------   ------   ------   ------   ------
  Native Heap     1836     1768        0        0     5120     4158      961
  Dalvik Heap     1783     1636        0        0     7300     4380     2920

** MEMINFO in pid 3689 [com.tencent.igame:mutil_network_service] **
                   Pss  Private  Private  SwapPss     Heap     Heap     Heap
                 Total    Dirty    Clean    Dirty     Size    Alloc     Free
                ------   ------   ------   ------   ------   ------   ------
  Native Heap      823      708        0        0     3584     2163     1420
  Dalvik Heap      764      536        0        0     6879     4128     2751

```


数据2 - OPPO R7sm Android 5.1.1

主进程 - 优化前后不明显
IM进程 - Dalvik Heap由15621KB减少到3215KB
网络进程 - Dalvik Heap由13967KB减少到881KB

```
优化前

** MEMINFO in pid 22941 [com.tencent.igame] **
                   Pss  Private  Private  Swapped     Heap     Heap     Heap
                 Total    Dirty    Clean    Dirty     Size    Alloc     Free
                ------   ------   ------   ------   ------   ------   ------
  Native Heap    14041    14016        0        0    15472    10396     5075
  Dalvik Heap    57582    57556        0        0    62961    49071    13890

** MEMINFO in pid 24705 [com.tencent.igame:QALSERVICE] **
                   Pss  Private  Private  Swapped     Heap     Heap     Heap
                 Total    Dirty    Clean    Dirty     Size    Alloc     Free
                ------   ------   ------   ------   ------   ------   ------
  Native Heap     4019     3992        0        0     5456     4217     1238
  Dalvik Heap    15621    15588        0        0    13565    12601      964

** MEMINFO in pid 23033 [com.tencent.igame:mutil_network_service] **
                   Pss  Private  Private  Swapped     Heap     Heap     Heap
                 Total    Dirty    Clean    Dirty     Size    Alloc     Free
                ------   ------   ------   ------   ------   ------   ------
  Native Heap     3671     3644        0        0     5108     4522      585
  Dalvik Heap    13967    13940        0        0    12863     8901     3962

优化后

** MEMINFO in pid 29496 [com.tencent.igame] **
                   Pss  Private  Private  Swapped     Heap     Heap     Heap
                 Total    Dirty    Clean    Dirty     Size    Alloc     Free
                ------   ------   ------   ------   ------   ------   ------
  Native Heap     8752     8728        0        0    10576     9242     1333
  Dalvik Heap    64924    64900        0        0    61316    53293     8023

** MEMINFO in pid 29687 [com.tencent.igame:QALSERVICE] **
                   Pss  Private  Private  Swapped     Heap     Heap     Heap
                 Total    Dirty    Clean    Dirty     Size    Alloc     Free
                ------   ------   ------   ------   ------   ------   ------
  Native Heap     1731     1700        0        0     3212     3131       80
  Dalvik Heap     3215     3188        0        0     8303     3609     4694

** MEMINFO in pid 29560 [com.tencent.igame:mutil_network_service] **
                   Pss  Private  Private  Swapped     Heap     Heap     Heap
                 Total    Dirty    Clean    Dirty     Size    Alloc     Free
                ------   ------   ------   ------   ------   ------   ------
  Native Heap      766      724        0        0     2272     2212       59
  Dalvik Heap      881      848        0        0     8303     1111     7192
```



原始apk上传微云  https://share.weiyun.com/6ff3513edd6ffe752419d5c2355fb58a


## 内存泄漏

[square/leakcanary: A memory leak detection library for Android and Java.](https://github.com/square/leakcanary)

LeakCanary可以非常方便地检查内存泄漏，其原理是？  [ref](https://github.com/square/leakcanary/wiki/FAQ#how-does-it-work)

如何定制LeakCanary

检查内存泄漏的技巧

1. 切换屏幕方向

## gc数量
https://developer.android.google.cn/studio/profile/investigate-ram.html?#TriggerLeaks

https://developer.android.google.cn/studio/profile/investigate-ram.html?#LogMessages


```java
Debug.getRuntimeStat("art.gc.gc-count");
Debug.getRuntimeStat("art.gc.gc-time");
```

## 代码层面的问题

1. 创建过多的对象  一次网络IO产生了多少对象，有没办法优化

# 启动速度

## 使用打日志的方法分析
### 初步定位
```
12-22 18:07:40.745 19419-19419 I/cm-performance: App start: 4884
                                                 App.onCreate(): 1076
                                                 Splash.onCreate(): 217
                                                 Splash.jumpToNextPage(): 1
                                                 Splash onCreate() to jumpToNextPage(): 199
                                                 Index.onCreate(): 1403
                                                 Index.onResume(): 22
```

瓶颈在`Application.onCreate()`和`IndexActivity.onCreate()`

### 细化

```
12-22 18:23:05.975 25956-25956 D/cm-performance: app.onOnCreate: begin
12-22 18:23:05.975 25956-25956 D/cm-performance: app.onOnCreate:      66 ms, ReservoirHelper
12-22 18:23:05.975 25956-25956 D/cm-performance: app.onOnCreate:      31 ms, getQIMEI
12-22 18:23:05.975 25956-25956 D/cm-performance: app.onOnCreate:      75 ms, initChannel
12-22 18:23:05.975 25956-25956 D/cm-performance: app.onOnCreate:      28 ms, registerToWX
12-22 18:23:05.975 25956-25956 D/cm-performance: app.onOnCreate:      685 ms, LoginHelper
12-22 18:23:05.975 25956-25956 D/cm-performance: app.onOnCreate:      2 ms, QQDownLoader
12-22 18:23:05.975 25956-25956 D/cm-performance: app.onOnCreate:      177 ms, QbSdk
12-22 18:23:05.975 25956-25956 D/cm-performance: app.onOnCreate:      6 ms, lazyInitNetWorkService
12-22 18:23:05.975 25956-25956 D/cm-performance: app.onOnCreate: end, 1070 ms
12-22 18:23:10.035 25956-25956 I/cm-performance: App start: 5136
                                                 App.onCreate(): 1095
                                                 Splash.onCreate(): 331
                                                 Splash.jumpToNextPage(): 1
                                                 Splash onCreate() to jumpToNextPage(): 228
                                                 Index.onCreate(): 1396
                                                 Index.onResume(): 29
```

`App.onCreate(): 1095`一行表示，`Application.onCreate()`方法耗时。我们使用`android.util.TimingLogger`打印耗时细节，即`app.onOnCreate()`。

`TimingLogger`需要一个`tag`和`label`，而这个`tag`至少应该是Log.VERBOSE级别。使用adb设置`tag`对应的日志级别

adb -s bb22c6a8 shell setprop log.tag.cm-performance VERBOSE

`TimingLogger`的两个主要方法：

+ `addSplit(String section)` - 在每个待统计耗时过程之后调用
+ `dump()` - 输出日志

根据以上日志，我们发现瓶颈主要在于

+ `LoginHelper.getInstance(this)` - LoginHelper的初始化
+ `QbSdk.initX5Environment(this, null);` - X5内核初始化

其次是：

+ `ReservoirHelper.init(this);` - ReservoirHelper的初始化
+ `ChannelUtils.initChannel(this);` - 初始化渠道号


对`IndexActivity.onCreate()`作类似分析


```
12-29 10:59:30.642 31987-31987 D/cmperf: IGameApplication.onCreate(): begin
12-29 10:59:30.642 31987-31987 D/cmperf: IGameApplication.onCreate():      4 ms, StatUtils.getQIMEI
12-29 10:59:30.642 31987-31987 D/cmperf: IGameApplication.onCreate():      34 ms, ChannelUtils.initChannel
12-29 10:59:30.642 31987-31987 D/cmperf: IGameApplication.onCreate():      10 ms, ShareUtils.registerToWX
12-29 10:59:30.642 31987-31987 D/cmperf: IGameApplication.onCreate():      2 ms, PrefProvider.storeData
12-29 10:59:30.642 31987-31987 D/cmperf: IGameApplication.onCreate():      0 ms, lazyInitNetWorkService
12-29 10:59:30.642 31987-31987 D/cmperf: IGameApplication.onCreate(): end, 50 ms
12-29 10:59:31.032 31987-31987 D/cmperf: SplashActivity.onCreate(): begin
12-29 10:59:31.032 31987-31987 D/cmperf: SplashActivity.onCreate():      23 ms, setContentView
12-29 10:59:31.032 31987-31987 D/cmperf: SplashActivity.onCreate():      1 ms, setMessageLogging
12-29 10:59:31.032 31987-31987 D/cmperf: SplashActivity.onCreate():      8 ms, StatUtils.startApp
12-29 10:59:31.032 31987-31987 D/cmperf: SplashActivity.onCreate():      1 ms, AppIconUpdate.addNumShortCut
12-29 10:59:31.032 31987-31987 D/cmperf: SplashActivity.onCreate():      24 ms, setupView
12-29 10:59:31.032 31987-31987 D/cmperf: SplashActivity.onCreate(): end, 57 ms
12-29 10:59:32.132 31987-31987 D/cmperf: IndexActivity.onCreate(): begin
12-29 10:59:32.132 31987-31987 D/cmperf: IndexActivity.onCreate():      56 ms, startLocation
12-29 10:59:32.132 31987-31987 D/cmperf: IndexActivity.onCreate():      0 ms, setNeedLogin
12-29 10:59:32.132 31987-31987 D/cmperf: IndexActivity.onCreate():      1 ms, forceLogout
12-29 10:59:32.132 31987-31987 D/cmperf: IndexActivity.onCreate():      346 ms, setupView
12-29 10:59:32.132 31987-31987 D/cmperf: IndexActivity.onCreate():      3 ms, updateMessageNew
12-29 10:59:32.132 31987-31987 D/cmperf: IndexActivity.onCreate():      8 ms, BusProvider.getInstance().register
12-29 10:59:32.132 31987-31987 D/cmperf: IndexActivity.onCreate():      2 ms, checkUpgrade
12-29 10:59:32.132 31987-31987 D/cmperf: IndexActivity.onCreate():      24 ms, mMyPresenter.loadTaskConfig
12-29 10:59:32.132 31987-31987 D/cmperf: IndexActivity.onCreate(): end, 440 ms
```

```
12-29 11:38:05.322 4559-4559 D/cmperf: IndexActivity.startLocation(): begin
12-29 11:38:05.322 4559-4559 D/cmperf: IndexActivity.startLocation():      1 ms, new LocateController
12-29 11:38:05.322 4559-4559 D/cmperf: IndexActivity.startLocation():      55 ms, mLocateController.startLocation
12-29 11:38:05.322 4559-4559 D/cmperf: IndexActivity.startLocation(): end, 56 ms
12-29 11:38:05.682 4559-4559 D/cmperf: IndexActivity.setupView(): begin
12-29 11:38:05.682 4559-4559 D/cmperf: IndexActivity.setupView():      2 ms, UserLocatePref.getUserLocation
12-29 11:38:05.682 4559-4559 D/cmperf: IndexActivity.setupView():      4 ms, addHeaderView
12-29 11:38:05.682 4559-4559 D/cmperf: IndexActivity.setupView():      5 ms, setupFragment
12-29 11:38:05.682 4559-4559 D/cmperf: IndexActivity.setupView():      33 ms, setupBottomBar
12-29 11:38:05.682 4559-4559 D/cmperf: IndexActivity.setupView():      1 ms, setupTeamTips
12-29 11:38:05.682 4559-4559 D/cmperf: IndexActivity.setupView():      209 ms, IMManager.getInstance(this).init
12-29 11:38:05.682 4559-4559 D/cmperf: IndexActivity.setupView(): end, 254 ms
```

```
12-29 11:44:52.324 11250-11250 D/cmperf: IMManager.init(): begin
12-29 11:44:52.324 11250-11250 D/cmperf: IMManager.init():      175 ms, TIMManager.getInstance().init
12-29 11:44:52.324 11250-11250 D/cmperf: IMManager.init():      30 ms, registerUser
12-29 11:44:52.324 11250-11250 D/cmperf: IMManager.init():      0 ms, TIMManager.getInstance().setUserStatusListener
12-29 11:44:52.324 11250-11250 D/cmperf: IMManager.init(): end, 205 ms
```

瓶颈

1. TIMManager.getInstance().init()

可以多纯线程启动吗

# 网络流量


[procrank](http://blog.csdn.net/deng0zhaotai/article/details/34848929)
[procrank](http://gityuan.com/2016/01/02/memory-analysis-command/)
---

[Android APP 性能优化的一些思考](https://mp.weixin.qq.com/s?__biz=MzAxMTI4MTkwNQ==&mid=2650824669&idx=1&sn=370163b924a1784ddd16e000babed894&chksm=80b78b43b7c00255f112e5baa5945c5b5a3728779d54b94f0f573b232774c194bb5546ffe609&mpshare=1&scene=1&srcid=1212VV6ItvMPbpZLGdX0JJ6C#rd)
[必知必会 | Android 性能优化的方面方面都在这儿](https://mp.weixin.qq.com/s?__biz=MzAxMTI4MTkwNQ==&mid=2650824552&idx=1&sn=a634748d786072ecb083e46f27362d87&chksm=80b78bf6b7c002e09b949b7fbc14b9ae0eb97d8794aca6fa6d42f80afcd27d07947641bab083&scene=21#wechat_redirect)

# 细节
问题：停留在首页，无任何操作，内存仍然缓慢增长，如下图。为什么？
<video src='html.mp4' type='video/mp4' controls='controls'  width='100%' height='100%'>
</video>

使用Android Monitor中的"Start Allocation Tracking"功能分析内存在哪里分配的，结果如下图：

![](/images/1513999022475.png)

可以发现`IGameTextSwitchView.updateText()`方法分配的内存占比约78%。`IGameTextSwitchView`是界面上一个文本轮播控件，每隔3秒显示一个新文本。`IGameTextSwitchView.updateText()`实现如下：

```java
    private String[] resources = {
            ""
    };
	
    public void setResources(String[] res) {
        this.resources = res;
    }
	
    private void updateText() {
        if (index < resources.length)
            this.setText(Html.fromHtml(resources[index]));
    }
```

实际上看`Html.fromHtml()`源码我们不难发现，它是一个非常"重"的方法。尝试对以上代码进行优化：

```java
    private String[] resources = {
            ""
    };
    private Spanned[] cachedResources;

    public void setResources(String[] res) {
        this.resources = res;
        this.cachedResources = new Spanned[res.length];
    }
	
    private void updateText() {
        Spanned cached = cachedResources != null ? cachedResources[index] : null;
        if (index < resources.length) {
            if (cached == null) {
                cached = Html.fromHtml(resources[index]);
                cachedResources[index] = cached;
            }
            this.setText(cached);
        }
    }
```
经过上述优化之后，内存仍然在增长，只是速度更慢了。

<video src='html-opt-1.mp4' type='video/mp4' controls='controls'  width='100%' height='100%'>
</video>


而这一次的瓶颈在于`BannerAdapter.instantiateItem()`，这个方法中会生成View

![](/images/1514000523964.png)

来看下`BannerAdapter`的实现：

```
public class BannerAdapter extends PagerAdapter {
    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View view = inflator.inflate(R.layout.igame_album_list_item_banner, container, false);
		...
		return view;
	}
}
```

它有两个问题：

1. `destroyItem()`中未及时移除不需要的View，导致轮播图中的View数量会越来越多
2. 由于是无限轮播，`instantiateItem()`会每3秒就创建一个新的View

使用`Pools.SimplePool`对View的创建和回收进行优化，优化的alloc文件见[](html-opt-3.alloc)

```java
    private Pools.SimplePool<View> mViewPool;

    @Override
    public Object instantiateItem(ViewGroup container, int position) {

        View view = mViewPool.acquire();
        if (view == null) {
			view = ...
		}		
		
		return view;
	}
	
    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        View view = (View) object;
        mViewPool.release(view);		
	}	
```