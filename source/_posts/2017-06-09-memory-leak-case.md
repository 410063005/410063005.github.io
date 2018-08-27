---
title: 内存泄露分析
tags:
 - Android
date: 2017-06-09 18:08:49
toc: true
---

Android应用中很容易出现内存泄漏，而泄漏的主要原因归根结底是应用组件生命周期方法处理不当。本文使用[LeakCanary](https://github.com/square/leakcanary)分析了几个内存泄露的例子，记录如下。
<!-- more -->

# Context泄露
## 例一
![](case_1.webp)

![](case_1_code.webp)

分析

+ `Foregrounds`用于记录app前后台切换
+ `Foregrounds`调用MTA SDK的`StatService.trackCustomEndEvent()`方法进行事件统计

我们将一个activity实例作为参数传给`StatService.trackCustomEndEvent()`方法，结果MTA SDK内部持有这个activity的引用，导致activity无法释放。

安全的调用方式如下：

```java
StatService.trackCustomEndEvent(
    activity.getApplicationContext(),  // 使用application context
    "AppTime", 
    "time_on_page"
);
```

不过从业务上来讲这段统计代码没有意义，所以干脆去掉。

## 例二
![](case_2.webp)

![](case_2_code.webp)

分析

这个例子跟前一个类似，也是Context相关的问题。

+ `AudioPlayManger`是单例
+ `AudioPlayManger.getInstance()`的参数是Context

如果不小心将activity实例作为Context参数，这个activity将无法释放。<font color="red">很不幸，方法调用方太容易犯这种错误了</font>。

同上，安全的做法是不要直接使用`context`对象，而是`context.getApplicationContext()`。

# 线程泄露

![](case_3.webp)

![](case_3_code.webp)

分析

![](case_3.2.gif)

+ 继承自`TextSwitcher`实现了自定义控件用于文本轮播效果，代码中使用了Timer以及TimerTask
+ Timer会启动新的线程

自定义控件中没有考虑到退出activity时中止Timer相关线程，导致activity泄露。

Handler机制相较Timer更优雅些，不必启动新的线程自然也就没有停止线程的问题。可以用Handler替换Timer以避免线程导致activity泄露的问题，不过仍然要注意中止消息以停止这里的轮播效果。

一个好的参考例子是`android.widget.ViewFlipper`，`ViewFlipper`是在`onDetachedFromWindow()`方法中停止消息更新的。我们可以借鉴这种处理方式。

