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


原始apk上传微云  https://share.weiyun.com/6ff3513edd6ffe752419d5c2355fb58a


## 内存泄漏

[square/leakcanary: A memory leak detection library for Android and Java.](https://github.com/square/leakcanary)

LeakCanary可以非常方便地检查内存泄漏，其原理是？  [ref](https://github.com/square/leakcanary/wiki/FAQ#how-does-it-work)

如何定制LeakCanary

检查内存泄漏的技巧

1. 切换屏幕方向

## gc数量

## 代码层面的问题

1. 创建过多的对象  一次网络IO产生了多少对象，有没办法优化

# 启动速度

# 网络流量


---

[Android APP 性能优化的一些思考](https://mp.weixin.qq.com/s?__biz=MzAxMTI4MTkwNQ==&mid=2650824669&idx=1&sn=370163b924a1784ddd16e000babed894&chksm=80b78b43b7c00255f112e5baa5945c5b5a3728779d54b94f0f573b232774c194bb5546ffe609&mpshare=1&scene=1&srcid=1212VV6ItvMPbpZLGdX0JJ6C#rd)
[必知必会 | Android 性能优化的方面方面都在这儿](https://mp.weixin.qq.com/s?__biz=MzAxMTI4MTkwNQ==&mid=2650824552&idx=1&sn=a634748d786072ecb083e46f27362d87&chksm=80b78bf6b7c002e09b949b7fbc14b9ae0eb97d8794aca6fa6d42f80afcd27d07947641bab083&scene=21#wechat_redirect)