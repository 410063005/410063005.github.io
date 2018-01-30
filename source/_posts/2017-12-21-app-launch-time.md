---
title: (译)Android之应用启动速度
tags:
  - Android
date: 2017-12-21 10:13:18
---

译自Android官方的[应用启动性能](https://developer.android.google.cn/topic/performance/launch-time.html?hl=zh-cn)。文章讨论了应用启动过程，如何分析启动性能，以及常见的启动性能问题。非常值得学习。
<!--more-->

用户期望应用快速响应和加载。启动缓慢的应用不符合用户这种预期，会让用户感到失望。糟糕的启动速度会导致用户差评，甚至卸载你的应用。

这篇文章讨论了如何优化应用启动性能。文章首先解释了启动过程内部是如何运作的。然后，讨论了该如何分析启动性能。最后，文章给出了一些常见的启动问题，以及若干如何解决这些问题的建议。

# Launch Internals
应用启动有三种可能的状态，这些状态会对应用启动时间产生影响。启动状态分为：cold start, warm start, lukewarm start。在cold start中，应用是完全从头启动。而其他两种启动状态，系统只需要将应用从后台调到前台。建议你基于cold start来进行优化。对cold start启动速度进行优化，也会提升warm start和lukewarm启动速度。

理解应用启动时在系统及应用层面发生了什么，以及每种启动状态下系统和应用是如何交互的，有助于优化应用启动速度。

## Cold start
cold start是指应用从头启动：在系统中应用进程不存在，直到应用启动并创建进程。cold start发生于设备启动后第一次启动应用，或系统已杀死了应用进程。优化这种状态的启动速度挑战最大，因为系统和应用要比其他启动状态下做更多的事情。

在cold start的开始阶段，系统有三个任务，分别是：

1. 加载并启动应用
2. 启动后立刻显示一个空白窗口
3. 创建应用进程

系统创建应用进程后，应用进程负责接下来的任务，包括：

1. 创建application对象
2. 启动主线程
3. 创建主activity
4. 加载布局
5. Layout
6. Draw

一旦应用进程完成第一次绘制，系统将使用主activity替换原先的空白窗口。到这时，用户可以使用应用了。

![](https://developer.android.google.cn/topic/performance/images/cold-launch.png?hl=zh-cn)

性能问题可能出现在application创建阶段或activity创建阶段。

### Application creation
从启动应用到系统完成应用第一次绘制，屏幕上会一直显示空白的启动窗口。应用进程完成第一次绘制，系统将使用主activity替换原先的启动窗口，允许用户跟应用交互。

如果重载[Application.oncreate()](https://developer.android.google.cn/reference/android/app/Application.html?hl=zh-cn#onCreate())，系统会调用自定义app对象的`onCreate()`方法。此后，应用会创建主线程，并在主线程中创建主activity。

此后，系统层面和应用层面进程继续按照[app lifecycle stages](https://developer.android.google.cn/guide/topics/processes/process-lifecycle.html?hl=zh-cn)进行。

### Activity creation
应用进程创建activity之后，activity执行下列操作：

1. 初始化
2. 调用构造方法
3. 调用跟当前生命周期对应的回调方法，比如`Activity.onCreate()`

通常`onCreate()`方法对加载时间影响最大，因为它承受最大负载：加载view，以及初始化activity运行所需要的对象。

## Warm start
warm start比cold start简单得多，开销也小。warm start中系统要做的不过是将activity调到前台。如果应用中的activity仍然存活，应用不需要再次初始化对象，加载和渲染布局。

某些对象可能在响应`onTrimMemory()`时被释放掉了，则需要在warm start时重新创建。

warm start跟cold start场景有相同的开场：系统会显示一个空白窗口直到应用渲染完activity。

## Lukewark start
lukewarm start包含cold start中的某些操作。同时，它比warm start又少了一些开销。下列状态可以视为lukewarm start。比如：

+ 用户退出应用然后重新启动。进程可能还在运行，但应用必须完全创建activity
+ 系统从内存中移除应用，然后用户重新打开应用。进程和activity都要重启，但重启时可以使用`onCreate()`中的instance state bundle参数

# 分析启动性能
为了诊断启动时间，可以观察应用启动时花了多长时间。

> **注意：** 使用non-debuggable模式的app进行分析。debuggable模式会开启debug特性，可能导致跟真实用户不一样的启动时间。

## 初次显示时间
有两种方式统计初次显示时间，一种是在logcat日志中找包含`Displayed`的行，另一种是使用[ADB Shell Activity Manager](https://developer.android.google.cn/studio/command-line/shell.html?hl=zh-cn#am)。下面分别介绍：

从Android 4.4(API level 19)开始，logcat会输出包含`Displayed`的行。`Displayed`表示启动进程到将activity绘制到屏幕耗费的时间。这个时间包含以下事件：

1. 启动进程
2. 初始化对象
3. 创建和初始化activity
4. 加载布局
5. 绘制activity

`Displayed`类似这样：

```
ActivityManager: Displayed com.android.myexample/.StartupTiming: +3s534ms
```

> The Displayed metric in the logcat output does not necessarily capture the amount of time until all resources are loaded and displayed: it leaves out resources that are not referenced in the layout file or that the app creates as part of object initialization. It excludes these resources because loading them is an inline process, and does not block the app’s initial display.

有时`Displayed`行中还包含一个额外的total time字段。比如：

```
ActivityManager: Displayed com.android.myexample/.StartupTiming: +3s534ms (total +1m22s643ms)
```

> In this case, the first time measurement is only for the activity that was first drawn. The total time measurement begins at the app process start, and could include another activity that was started first but did not display anything to the screen. The total time measurement is only shown when there is a difference between the single activity and total startup times.

也可以使用[ADB Shell Activity Manager](https://developer.android.google.cn/studio/command-line/shell.html?hl=zh-cn#am)。使用法如下：

```
adb [-d|-e|-s <serialNumber>] shell am start -S -W
com.example.app/.MainActivity
-c android.intent.category.LAUNCHER
-a android.intent.action.MAIN
```

logcat中仍然输出`Displayed`日志。而终端以上命令输出结果如下：

```
Starting: Intent
Activity: com.example.app/.MainActivity
ThisTime: 2044
TotalTime: 2044
WaitTime: 2054
Complete
```

具体到我们的app是：

```
adb shell am start -S -W com.tc.igame/.view.common.activity.SplashActivity -c android.intent.category.LAUNCHER -a android.intent.action.MAIN
Stopping: com.tc.igame
Starting: Intent { act=android.intent.action.MAIN cat=[android.intent.category.LAUNCHER] cmp=com.tc.igame/.view.common.activity.SplashActivity }
Status: ok
Activity: com.tc.igame/.view.common.activity.IndexActivity
ThisTime: 1312
TotalTime: 2128
WaitTime: 2131
Complete
```

## 完整显示时间
可以使用[reportFullyDrawn()](https://developer.android.google.cn/reference/android/app/Activity.html?hl=zh-cn#reportFullyDrawn())来统计应用启动到完全显示所有资源和视图的时间。如果应用使用延迟加载，这个方法就非常有价值。在延迟加载过程中，应用并不会阻塞窗口的初始绘制，而是异步加载资源并更新视图。

如果由于延迟加载导致应用的初次展示时并不包含所有资源，可以考虑将完整加载和显示所有资源及视图作为另一种度量标准。

为了解决这种问题，可以手动调用[reportFullyDrawn()](https://developer.android.google.cn/reference/android/app/Activity.html?hl=zh-cn#reportFullyDrawn())通知系统activity已经完成延迟加载。使用这个方法时，logcat中输出的值是app对象创建到[reportFullyDrawn()](https://developer.android.google.cn/reference/android/app/Activity.html?hl=zh-cn#reportFullyDrawn())被调用的时间。输出类似这样：

```
system_process I/ActivityManager: Fully drawn {package}/.MainActivity: +1s54ms
```

注意：这个时间包含前面提到的`total`

定位瓶颈

定位启动性能瓶颈的两个好方式是使用Android Studio提供的[Method Tracer tool](https://developer.android.google.cn/studio/profile/am-methodtrace.html?hl=zh-cn)。注意，现在被新的工具CPU Profiler替换，见[这里](https://developer.android.google.cn/studio/profile/cpu-profiler.html?hl=zh-cn)。

如果不能使用Method Tracer tool，或者无法在准确的时机启动工具来获取日志信息，也可以在app和activity的`onCreate()`方法中使用inline tracing来获取日志。具体见[Trace](https://developer.android.google.cn/reference/android/os/Trace.html?hl=zh-cn)和[Systrace](https://developer.android.google.cn/studio/profile/systrace-commandline.html?hl=zh-cn)。

# 常见问题

## 过重的app初始化
这一部分讨论会影响启动性能的若干严重问题。这些问题主要是关于初始化app和activity对象的，以及加载窗口(the loading of screens)

## 过重的activity初始化
如果你重载了`Application`对象，并且执行了过重或过复杂的逻辑来初始化这个对象，可能导致启动时的性能问题。如果你做了目前还不需要的初始化，其实是在浪费时间。而某些初始化则完全没必要。比如，for example, initializing state information for the main activity, when the app has actually started up in response to an intent. With an intent, the app uses only a subset of the previously initialized state data

另一个问题是应用初始化时的GC事件有影响或过多，或者初始化时正在发生磁盘IO，进一步阻塞初始化。Dalvik运行时中GC是一个需要考虑的影响因素，而Art运行时GC基本是并发的，已经最小化GC对应用的影响。

### 诊断问题
可以使用Method tracing或inline tracing来诊断问题

#### Method tracing
使用Method Tracer tool找到[callApplicationOnCreate()](https://developer.android.google.cn/reference/android/app/Instrumentation.html?hl=zh-cn#callApplicationOnCreate(android.app.Application))方法，该方法最终会调用自定义的`Application.onCreate()`方法。如果显示这个方法执行时间过长，则需要进一步看看具体原因。

#### Inline tracing
使用inline tracing分析可疑的地方，包括：

+ app的`onCreate()`方法
+ app初始化的全局单例对象
+ 磁盘IO，反序列化，瓶颈中可能出现的循环

### 解决办法
可能有很多潜在的瓶颈，但有两个常见的问题及解决办法：

+ view层级越深，app需要越多的时间来加载。解决这个问题的两个步骤：
 + 通过减少冗余或嵌套的布局来对view层级平坦化
 + 启动期间不需要可见的某些UI，不必加载。可以使用[ViewStub](https://developer.android.google.cn/reference/android/view/ViewStub.html?hl=zh-cn)替换，并在更恰当的时机加载这些布局
+ 在主线程中初始化全部资源会拖慢启动速度。按下列方式解决这个问题：
 + 将全部的资源初始化过程放在别的地方，app可以在其他线程中延迟加载
 + 允许应用加载和显示view，但此后再更新那些依赖bitmap和其他资源的视觉属性

## Themed launch screens
可能你修改应用的加载体验进行样式化，以便app的启动页看起来跟app的其他部分一致，而非系统样式。这个办法可以从体验上加快activity的体验速度(实际并没有)。

一种常见的实现方式是使用[windowDisablePreview](https://developer.android.google.cn/reference/android/R.attr.html?hl=zh-cn#windowDisablePreview)来关闭初始的空白窗口。但是这种方法可能导致更长的启动时间。另外，它强制用户在activity启动时必须等待且无任何反馈，让用户以为app功能不正常。

### 诊断问题
当用户启动应用时发现响应很慢。并且屏幕像是"冻"住了，或者对输入无响应。

### 解决办法
建议不要关闭preview window。可以使用activity的`windowBackground`来提供一个简单的自定义drawable

```xml
<layer-list xmlns:android="http://schemas.android.com/apk/res/android" android:opacity="opaque">
  <!-- The background color, preferably the same as your normal theme -->
  <item android:drawable="@android:color/white"/>
  <!-- Your product logo - 144dp color version of your app icon -->
  <item>
    <bitmap
      android:src="@drawable/product_logo_144dp"
      android:gravity="center"/>
  </item>
</layer-list>
```

```
<activity ...
android:theme="@style/AppTheme.Launcher" />
```

```java
public class MyMainActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // Make sure this is before calling super.onCreate
    setTheme(R.style.Theme_MyApp);
    super.onCreate(savedInstanceState);
    // ...
  }
}
```

在manifest文件中引用定义好的drawable文件。之后可以在`onCreate()`方法中的`super.onCreate()`之前调用[setTheme(R.style.AppTheme)](https://developer.android.google.cn/reference/android/view/ContextThemeWrapper.html?hl=zh-cn#setTheme(int))

