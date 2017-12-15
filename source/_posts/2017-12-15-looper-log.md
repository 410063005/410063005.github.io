---
title: Android之输出Handler日志
tags:
  - Android
toc: false
date: 2017-12-15 14:58:04
---

今天看到一篇文章提到了给Looper加日志以观察GC，发现这个Looper打日志这个小技巧有助于调试UI卡顿问题，我们学学这个技巧吧。
<!--more-->

通过统计主线程`Looper`中每个Message的耗时，很容易找出主线程耗时过多的操作。这些操作往往是导致UI卡顿的罪魁祸首。

你可能会说，我在`Handler.handMessage()`中也可以添加计时代码呀，在`Looper`中统计有什么好处？如果是你自己写的`Handler`的确也很容易统计，但如果是别人写的呢，如果是第三方库中的呢？另一方面，即便自己写的，如何保证新加的`Handler`不会遗漏计时代码？所以这种做法并不可取。看看更好的做法是怎样的。

# setMessageLogging()
注释文档中说，`setMessageLogging()`用于当前Looper处理Message时打印日志。

+ 传null参数关闭日志功能，传非null的`printer`开启日志功能
+ 开启日志功能后，会在每个Message分发的开始以及结束时输出日志信息到`printer`，具体的日志信息包括区分Message的目标Hander以及Message内容

对照`loop()`方法源码(省略无关部分)，跟上面描述一致。所以这里不再赘述。

```java
    // android.os.Looper.java
	
    public static void loop() {
        final Looper me = myLooper();
        final MessageQueue queue = me.mQueue;
        ...
        for (;;) {
            Message msg = queue.next(); // might block
            ...
            // This must be in a local variable, in case a UI event sets the logger
            final Printer logging = me.mLogging;
            if (logging != null) {
                logging.println(">>>>> Dispatching to " + msg.target + " " +
                        msg.callback + ": " + msg.what);
            }
            ...
            msg.target.dispatchMessage(msg);
            ...
            if (logging != null) {
                logging.println("<<<<< Finished to " + msg.target + " " + msg.callback);
            }
            ...
        }
    }
	
    /**
     * Control logging of messages as they are processed by this Looper.  If
     * enabled, a log message will be written to <var>printer</var>
     * at the beginning and ending of each message dispatch, identifying the
     * target Handler and message contents.
     *
     * @param printer A Printer object that will receive log messages, or
     * null to disable message logging.
     */
    public void setMessageLogging(@Nullable Printer printer) {
        mLogging = printer;
    }
```

# 输出Handler日志

界面上有一个使用`Handler`实现的轮播图，每隔5秒发消息切换到下一张图片。我们给它加上日志功能，代码如下：

```java
Looper.getMainLooper().setMessageLogging(new Printer() {
    @Override
    public void println(String x) {
		// 为了便于logcat中观察, 我们只输出当前页面的日志
        if (x != null && x.contains("EventCenterFragment")) {
            Log.i(CmPerf.TAG, x);
        }
    }
});	
```

![](looper-log.gif)

每次切换图片时会输出如下格式的日志：

```
12-12 10:35:38.614 9542-9542 I: >>>>> Dispatching to Handler (com.tc.igame.view.common.fragment.EventCenterFragment$1) {101c258} null: 1
12-12 10:35:38.615 9542-9542 I: <<<<< Finished to Handler (com.tc.igame.view.common.fragment.EventCenterFragment$1) {101c258} null
12-12 10:35:43.617 9542-9542 I: >>>>> Dispatching to Handler (com.tc.igame.view.common.fragment.EventCenterFragment$1) {101c258} null: 1
12-12 10:35:43.618 9542-9542 I: <<<<< Finished to Handler (com.tc.igame.view.common.fragment.EventCenterFragment$1) {101c258} null
```

`Dispatching to Handler`和`Finished to Handler`对应首一次Message处理过程。我们可以根据这种特定的日志格式为每个Message处理加上耗时统计。具体做这可参考《为你的android程序加上gc监控吧！》，这里不展开。

---
补充

《为你的android程序加上gc监控吧！》中提到的gc监控做法太过复杂，并不可取。实际上，Android 6.0(API 23)之后art虚拟机支持如下方式获取gc次数和gc耗时：

```java
Debug.getRuntimeStat("art.gc.gc-count");
Debug.getRuntimeStat("art.gc.gc-time");
```

我们直接调用系统API即可很方便地获取到gc次数和gc耗时。

添加如下代码并在Android Monitor中强制gc，观察输出的日志。

```java
Looper.getMainLooper().setMessageLogging(new Printer() {
    @Override
    public void println(String x) {
        if (x != null && x.contains("EventCenter") && x.contains("Finished to Handler")) {
            Log.i(CmPerf.TAG, "looper println: count=" + Debug.getRuntimeStat("art.gc.gc-count"));
            //Log.i(CmPerf.TAG, "looper println: time=" + Debug.getRuntimeStat("art.gc.gc-time"));
        }
    }
});
```

<video src='art-gc-count.mp4' type='video/mp4' controls='controls'  width='100%' height='100%'>
</video>

GC日志参考

+ [Android GC Log解读 | 黯羽轻扬](http://www.ayqy.net/blog/android-gc-log%E8%A7%A3%E8%AF%BB/)
+ [Android GC 那点事 - Android - 掘金](https://juejin.im/entry/5625144060b2b199f769ef74)
+ [Dalvik与ART的GC调试 - Gityuan博客 | 袁辉辉博客](http://gityuan.com/2015/10/03/Android-GC/)

[ref]: http://km.oa.com/group/17326/articles/show/325749?kmref=guess_post