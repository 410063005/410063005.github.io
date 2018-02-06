---
title: 统计启动时间
tags:
  - Android
toc: true
date: 2018-01-12 21:49:41
---

{% post_link app-launch-time %}中提到启动慢通常出现在创建Application和Activity阶段，具体来说就是`Application.onCreate()`和`Activity.onCreate()`。这两个方法负载过重，导致启动时间长。优化启动速度的第一步是统计启动时间，如何统计呢？
<!--more-->

# 统计启动时间
需要说明的是，应当使用关闭了debug模式的APK进行测试，否则统计结果可能跟真实情况差别很大。

## Displayed关键字
一个简单的统计启动时间的办法是在logcat日志中搜索`Displayed`关键字，可以看到类似这样的日志：

![](/images/1515746480768.webp)

这里可以看到我们应用首页的启动时间。测试是在在x86模拟器进行的，使用的是关闭了debug模式的包，一共测试了三次，每次都是杀死进程后重启启动的(冷启动)

## adb命令
另一种统计办法是使用adb命令。

```
adb [-d|-e|-s <serialNumber>] shell am start -S -W
com.example.app/.MainActivity
-c android.intent.category.LAUNCHER
-a android.intent.action.MAIN
```

![](/images/1515747098555.webp)

+ ThisTime - 当前Activity启动的耗时，这个时间跟上文说到的logat `Displayed`日志中的第一个时间是对应的
+ TotalTime - 总的耗时，从App进程启动开始算起
+ WaitTime - (不清楚，待求证?)

# 更准确的统计
直观上感觉我们应用的启动速度并不快。我想优化启动速度，但不清楚到底是哪段代码拖慢了启动速度。如何找到瓶颈所在呢？

前文说过启动慢通常发生在`Application.onCreate()`和`Activity.onCreate()`。所以重点关注`App`，`SplashActivity`，`IndexActivity`三个对象`onCreate()`中各个方法调用的具体耗时：

- `App` - App是应用入口，它是`android.app.Application`的子类。我们在`App.onCreate()`有大量初始化代码，或许某些代码很慢
- `SplashActivity` - SplashActivity是应用的闪屏页，闪屏页通常没有太多代码，可能不是性能瓶颈。但最好先看数据再下结论
- `IndexActivity` - IndexActivity是应用的首页，首页有4个Tab，分别会加载四个不同的Fragment

## TimingLogger
确定 _统计对象_ 后再来看看 _统计工具_ 。[android.util.TimingLogger](https://developer.android.com/reference/android/util/TimingLogger.html)非常适合我们需求场景，它是Android SDK提供的工具类，用于统计方法耗时。用法比较简单：

```java
    TimingLogger timings = new TimingLogger("TAG", "methodA");
    // ... do some work A ...
    timings.addSplit("work A");
    // ... do some work B ...
    timings.addSplit("work B");
    // ... do some work C ...
    timings.addSplit("work C");
    timings.dumpToLog();
```

上面代码在logcat的输出类似这样：

```
    D/TAG     ( 3459): methodA: begin
    D/TAG     ( 3459): methodA:      9 ms, work A
    D/TAG     ( 3459): methodA:      1 ms, work B
    D/TAG     ( 3459): methodA:      6 ms, work C
    D/TAG     ( 3459): methodA: end, 16 ms
```

注意：`TimingLogger`的实现依赖于`Log.isLoggable()`方法。我们知道，需要将对应tag的日志级别至少设置为VERBOSE级别`Log.isLoggable()`才生效。设置方法是`adb shell setprop log.tag.TAG <Level>`，使用`TimingLogger`前需要调用这个命令设置一下。但某些机器上`TimingLogger`不起作用，比如小米，推测是厂商修改ROM关闭这个功能。

## 日志分析
我们分别在模拟器和OPPO手机上测试，得到两份数据。具体数据见[模拟器日志](timing-log-emualtor.txt)和[OPPO手机日志](timing-log-oppo.txt)。OPPO手机日志用于做实际分析，模拟器日志仅仅用作对比验证。

每个方法调用都会消耗CPU时间，我们优先关注耗时最多的那些方法。从OPPO手机日志中找到那些耗时超过 **50ms** 的方法调用(当然50ms只是个经验值，这个值可根据实际情况调整)：

先是`App.onCreate()`中耗时超过50ms的方法调用

+ LoginHelper.getInstance() - 397ms
+ QbSdk.initX5Environment() - 102ms
+ Bugly.init() - 91ms
+ ChannelUtils.initChannel() - 72ms

然后是`IndexActivity.onCreate()`中耗时超过50ms的方法调用

+ IndexActivity.setSubContentView() - 87ms
+ MyPresenter.attach() - 81ms
+ IndexActivity.setupView() - 246ms

`SplashActivity.onCreate()`中并没有出现耗时超过50ms的方法调用，我们可以忽略它。

不过我们从日志文件看到一个奇怪的现象————某些日志出次了多次，看起来就像是代码重复执行。比如这段日志出现了三次：

```
D/cm-perf: App.onCreate2(): begin
D/cm-perf: App.onCreate2():      31 ms, ChannelUtils.initChannel
D/cm-perf: App.onCreate2():      2 ms, StatUtils.getQIMEI
D/cm-perf: App.onCreate2():      1 ms, Logger.setDebuggable
D/cm-perf: App.onCreate2():      0 ms, Env.initEnv
D/cm-perf: App.onCreate2(): end, 34 ms
```

下面逐一对上述数据和现象进行初步分析，并对某些耗时操作给出了大致的解决方案

+ LoginHelper.getInstance() 

腾讯WTLogin SDK初始化，非常非常慢。不仅在OPPO真机上慢，在性能较高的[模拟器](timing-log-emualtor.txt)上也是一如继往的慢。对这个方法调用慢的问题，暂时没想到好的解决办法，总不能不登录吧？

+ QbSdk.initX5Environment()

[腾讯浏览服务SDK(x5内核)](https://x5.tencent.com/tbs/)初始化。x5内核用于替换Android原生的WebView，以提高应用内h5页面性能。很显然没必要在应用启动时就初始化x5内核，毕竟真实用户很可能根本就不会打开h5，在用户真正使用打开h5时再初始化x5内核才是合理的。当然，需要注意的是`QbSdk.initX5Environment()`内部会启动一个新的线程做异步初始化。所以`QbSdk.initX5Environment()`返回并不表示初始化完成！使用没有初始化成功的x5内核，可能会出现一些奇怪的bug，比如WebView黑屏之类的。(是的，我被这个问题坑到了，后面再讲)

+ Bugly.init()

[腾讯Bugly SDK](https://bugly.qq.com/v2/)初始化。能否将这个初始化延后？

+ ChannelUtils.initChannel()

初始化渠道号。这个方法中有IO操作！具体来说就是拿到当前应用对应的APK文件，然后解析其中的渠道号文件，所以耗时多。有没有办法避免这里的IO操作？

+ IndexActivity.setSubContentView()

这个方法加载布局而已，耗时却很多。推测是布局过于复杂引起的。

+ MyPresenter.attach() (注：这里的实际操作是调用Retrofit)

这里耗时多就比较出乎意料。正常来说它应当是一个很轻的操作，不至于慢。但仔细分析就容易明白：MyPresenter中用到了Retrofit生成的Service类，而Service是通过[Java动态代理](https://github.com/square/retrofit/blob/master/retrofit/src/main/java/retrofit2/Retrofit.java#L133)生成的，这个生成过程非常慢。不过Retrofit有做缓存，之后的访问速度会变快。

+ IndexActivity.setupView()

这里耗时多是因为在其中初始化[腾讯云IM SDK](https://cloud.tencent.com/product/im)。

+ 某些日志输出多次的问题

我们应用是多进程的，包括主进程、IM进程和网络进程。`App.onCreate()`中的部分初始化代码不不幸遇到[Android多进程的坑](http://yifeng.studio/2017/06/16/android-multi-process-things/)。那些输出日志的代码在三个进程中被执行，所以能看到三次日志输出。执行不必要的代码肯定会在某种程度上降低启动速度，解决方法是 **根据当前进程功能只做必要的初始化** 。

# 总结
如[Android之应用启动速度](2017-12-21-app-launch-time.md]中提到的那样，应用启动慢通常出现在创建Application和Activity的阶段。而`TimingLogger`可以方便地统计方法耗时，通过方法耗时数据可以清楚地看到性能瓶颈所在。

我们基本弄清楚启动慢的原因，接下来就是如何优化启动速度了。优化的策略不外乎这几个：

+ 减少不需要的初始化
 + 某些旧的没用的代码可能没有删除，它们会引起不必要的初始化
 + 多进程应用中很容易出现不必要的初始化，具体可参考[这里](http://yifeng.studio/2017/06/16/android-multi-process-things/)
+ 延迟初始化/延迟加载
 + 事情分轻重缓急，真的不需要在应用入口做完所有的初始化
 + 最好是延迟到要使用时才初始化，或者在负载较轻时再初始化
+ 避免IO操作
+ 简化布局
+ 缓存

当然说起容易做起来难，真正优化时其实很容易踩坑，比如上面提到的x5 WebView黑屏问题。如何有效优化的同时又避免踩坑呢？未完待续，敬请期待。