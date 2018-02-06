---
title: (译)使用LeakCanary检查内存泄漏
date: 2018-01-30 14:27:33
tags:
 - Android
---
答辩时被问到LeakCanary内存检测泄漏原理，没能答上来。原本答辩前是计划看这一块的内容的，结果时间紧没有去翻相关资料。好吧，我是在找借口。知识有欠缺就是有欠缺，找借口不如赶紧恶补。
<!--more-->

本文翻译自[LeakCanary: Detect all memory leaks! – Square Corner Blog – Medium](https://medium.com/square-corner-blog/leakcanary-detect-all-memory-leaks-875ff8360745)。

> java.lang.OutOfMemoryError
>        at android.graphics.Bitmap.nativeCreate(Bitmap.java:-2)
>        at android.graphics.Bitmap.createBitmap(Bitmap.java:689)
>        at com.squareup.ui.SignView.createSignatureBitmap(SignView.java:121)

# 没人喜欢OOM
在Square的注册界面，我们在Bitmap cache上绘制用户签名。该Bitmap的尺寸大小跟屏幕大小一致，创建该Bitmap时出现很多OOM(OutOfMemoryError)。

![](https://cdn-images-1.medium.com/max/2000/0*TpsPt3DHu_aMeoa2.webp)

我们尝试以上解决方法，没有一个能解决问题：

+ 使用`Bitmap.Config.ALPHA_8`减少内存占用(用户签名不需要颜色)
+ 捕获OOM并强制GC后再来重试几次(灵感来自于[GCUtils](https://android.googlesource.com/platform/packages/inputmethods/LatinIME/+/ics-mr1/java/src/com/android/inputmethod/latin/Utils.java))
+ 我们没有想过在Java堆以外分配Bitmap。好在那时还没有[Fresco](https://github.com/facebook/fresco)。(？ We didn’t think of allocating bitmaps off the Java heap. Lucky for us, Frescodidn’t exist yet.)

# 我们使用错误的方式看问题
Bitmap大小并不是问题。当内存快满时，OOM可能在任何地方发生。而它更倾向于发生在创建大对象的地方，比如Bitmap。OOM象征着另一个更深层次的问题：**内存泄漏**。

# 什么是内存泄漏
对象生命周期有限。一些对象完成其工作之后，本应该被垃圾回收。如果一个引用链在某个对象生命周期完成后仍然持有访对象，会导致内存泄漏。当内存泄漏不断累积，应用就会内存不足。

比如，`Activity.onDestroy()`回调后，该Activity，Activity的View树，以及相应的Bitmap应该是可以垃圾回收的。如果后台某个线程持有该Activity的引用，那么对应的内存就不能被回收。它最终会导致OOM崩溃。

# 找到内存泄漏
寻找内存泄漏是一个手动过程，Raizlab的[Wrangling Dalvik](http://www.raizlabs.com/dev/2014/03/wrangling-dalvik-memory-management-in-android-part-1-of-2/)系列中对此有很好的描述。

关键步骤如下：

1. 通过[Bugsnag](https://bugsnag.com/), [Crashlytics](https://try.crashlytics.com/), [Developer Console](https://play.google.com/apps/publish/)(译者注，或其他某些crash上报工具)了解应用中OOM的具体情况
2. 尝试复现问题。需要一部出现OOM崩溃的手机来复现问题，你可以去买、借甚至偷(^_^)  (并不是所有的设备都会出现内存泄漏！) 你还需要弄清楚哪种操作流程会导致内存泄漏，可能需要通过暴力方式强制内存泄漏
3. 发生OOM时导出heap ([这里总结了做法](https://gist.github.com/pyricau/4726389fd64f3b7c6f32))
4. 使用[MAT](http://eclipse.org/mat/)或[YourKit](https://www.yourkit.com/)分析上一步得到的heap，并找到一个应当被回收却没有回收的对象
5. 计算从GC roots到该对象最短的强引用路径
6. 弄清楚该路径中哪个引用不应当存在，修复该内存泄漏问题

如果有一个库可以在发生OOM之前帮你完成这些工作，而你只用集中精力修复内存泄漏那该有多好！

# LeakCanary介绍
[LeakCanary](https://github.com/square/leakcanary)是一个用于检测内存泄漏的Java开源库。

看这个例子：

```java
class Cat {
}
class Box {
  Cat hiddenCat;
}
class Docker {
  static Box container;
}
// ...
Box box = new Box();
Cat schrodingerCat = new Cat();
box.hiddenCat = schrodingerCat;
Docker.container = box;
```

可以创建`RefWatcher`实例来监控一个对象：

```java
// We expect schrodingerCat to be gone soon (or not), let's watch it.
refWatcher.watch(schrodingerCat);
```

当检查到泄漏时，会自动出现内存泄漏trace:

```
* GC ROOT static Docker.container
* references Box.hiddenCat
* leaks Cat instance
```

我们知道你忙于给应用写功能，所以让整个配置过程非常简单。只用一行代码，LeakCanary就会自动检查Activity泄漏：

```java
public class ExampleApplication extends Application {
  @Override public void onCreate() {
    super.onCreate();
    LeakCanary.install(this);
  }
}
```

你会看到Notification提示框以及图形化的展示：

![](https://cdn-images-1.medium.com/max/2000/0*5zhG12WlfCp1nIlc.webp)

# 结论
启用LeakCanary后我们发现并修复了我们应用当中许多内存泄漏。我们甚至发现少量[Android SDK中的内存泄漏]([leaks in the Android SDK](https://github.com/square/leakcanary/blob/master/library/leakcanary-android/src/main/java/com/squareup/leakcanary/AndroidExcludedRefs.java))

结果让人吃惊。我们将OOM crash降低了94%。

![](https://cdn-images-1.medium.com/max/2000/0*8DpD5hZX4R4O4Vvr.webp)

如果你想消除OOM崩溃，赶紧[现在就安装LeakCanary](https://github.com/square/leakcanary)吧！
