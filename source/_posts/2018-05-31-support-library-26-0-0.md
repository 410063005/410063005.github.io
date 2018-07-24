---
title: 记support库兼容性相关的一个bug
tags: android
toc: true
date: 2018-05-31 19:31:42
---

26.0.0版本的support库对min SDK version的要求是至少14，所以一些用于兼容14以下的代码直接从support库中移除了，比如AnimatorCompatHelper类。Android开发中使用第三方库时要注意是否有依赖的support库版本不统一的问题，避免运行时找不到support库中的类引起crash。
<!--more-->
# 问题描述
应用中出现了一个如下crash，Didn't find class "android.support.v4.animation.AnimatorCompatHelper"。日志如下。

```
java.lang.ClassNotFoundException: Didn't find class "android.support.v4.animation.AnimatorCompatHelper" on path: ...
java.lang.NoClassDefFoundError:Failed resolution of: Landroid/support/v4/animation/AnimatorCompatHelper;
android.support.v7.widget.DefaultItemAnimator.resetAnimation(Unknown Source)
......
java.lang.ClassNotFoundException:Didn't find class "android.support.v4.animation.AnimatorCompatHelper" on path: ...
dalvik.system.BaseDexClassLoader.findClass(BaseDexClassLoader.java:56)
java.lang.ClassLoader.loadClass(ClassLoader.java:380)
java.lang.ClassLoader.loadClass(ClassLoader.java:312)
android.support.v7.widget.DefaultItemAnimator.resetAnimation(Unknown Source)
android.support.v7.widget.DefaultItemAnimator.animateMove(Unknown Source)
android.support.v7.widget.DefaultItemAnimator.animateChange(Unknown Source)
android.support.v7.widget.SimpleItemAnimator.animateChange(Unknown Source)
android.support.v7.widget.RecyclerView.animateChange(Unknown Source)
android.support.v7.widget.RecyclerView.dispatchLayoutStep3(Unknown Source)
android.support.v7.widget.RecyclerView.dispatchLayout(Unknown Source)
android.support.v7.widget.RecyclerView.onLayout(Unknown Source)
android.view.View.layout(View.java:17702)
android.view.ViewGroup.layout(ViewGroup.java:5631)
android.widget.FrameLayout.layoutChildren(FrameLayout.java:325)
android.widget.FrameLayout.onLayout(FrameLayout.java:261)
android.view.View.layout(View.java:17702)
android.view.ViewGroup.layout(ViewGroup.java:5631)
com.dependency.phoenix.PullToRefreshView.void onLayout(boolean,int,int,int,int)(Unknown Source)
```

异常信息非常明确，是Adapter数据更新时出现问题。

Adapter中一条数据更新时，我们会调用`Adapter.notifyItemChanged()`通知界面更新。该方法执行时系统会有一个默认的动画效果，动画效果用到了`AnimatorCompatHelper`，而`AnimatorCompatHelper`来自support library库，具体的类是android/support/v4/animation/AnimatorCompatHelper。

但是我们的应用中正确地添加了support库的，而生成的APK在运行时却无法加载`AnimatorCompatHelper`类，非常奇怪。

# 问题分析
一开始以为是因为RecyclerView和Adapter的用法不正确引起的偶现问题。尝试修改代码逻辑，强行让应用每次都可以执行`Adapter.notifyItemChanged()`，复现问题了，而且必现的。仍然输出类似日志：

```
05-31 02:01:56.965 3102-3102/com.aaa.bbb E/eup: sys default last handle start!
05-31 02:01:56.965 3102-3102/com.aaa.bbb E/AndroidRuntime: FATAL EXCEPTION: main
                                                                 Process: com.aaa.bbb, PID: 3102
                                                                 java.lang.NoClassDefFoundError: Failed resolution of: Landroid/support/v4/animation/AnimatorCompatHelper;
                                                                     at android.support.v7.widget.DefaultItemAnimator.resetAnimation(DefaultItemAnimator.java:515)
                                                                     at android.support.v7.widget.DefaultItemAnimator.animateChange(DefaultItemAnimator.java:322)
                                                                     at android.support.v7.widget.SimpleItemAnimator.animateChange(SimpleItemAnimator.java:149)
                                                                     at android.support.v7.widget.RecyclerView.animateChange(RecyclerView.java:3790)
                                                                     at android.support.v7.widget.RecyclerView.dispatchLayoutStep3(RecyclerView.java:3597)
                                                                     at android.support.v7.widget.RecyclerView.dispatchLayout(RecyclerView.java:3277)
                                                                     at android.support.v7.widget.RecyclerView.onLayout(RecyclerView.java:3798)
                                                                     at android.view.View.layout(View.java:17519)
                                                                     at android.view.ViewGroup.layout(ViewGroup.java:5612)
                                                                     at android.widget.RelativeLayout.onLayout(RelativeLayout.java:1079)
                                                                     at android.view.View.layout(View.java:17519)
                                                                     at android.view.ViewGroup.layout(ViewGroup.java:5612)
                                                                     at android.widget.FrameLayout.layoutChildren(FrameLayout.java:323)
                                                                     at android.widget.FrameLayout.onLayout(FrameLayout.java:261)
                                                                     at android.view.View.layout(View.java:17519)
                                                                     at android.view.ViewGroup.layout(ViewGroup.java:5612)
                                                                     at android.widget.LinearLayout.setChildFrame(LinearLayout.java:1741)
                                                                     at android.widget.LinearLayout.layoutVertical(LinearLayout.java:1585)
                                                                     at android.widget.LinearLayout.onLayout(LinearLayout.java:1494)
                                                                     at android.view.View.layout(View.java:17519)
                                                                     at android.view.ViewGroup.layout(ViewGroup.java:5612)
                                                                     at android.widget.FrameLayout.layoutChildren(FrameLayout.java:323)
                                                                     at android.widget.FrameLayout.onLayout(FrameLayout.java:261)
                                                                     at android.view.View.layout(View.java:17519)
                                                                     at android.view.ViewGroup.layout(ViewGroup.java:5612)
                                                                     at android.widget.RelativeLayout.onLayout(RelativeLayout.java:1079)
                                                                     at android.view.View.layout(View.java:17519)
                                                                     at android.view.ViewGroup.layout(ViewGroup.java:5612)
                                                                     at com.dependency.phoenix.PullToRefreshView.onLayout(PullToRefreshView.java:407)
                                                                     at android.view.View.layout(View.java:17519)
```

全局搜索代码，上面提到一共有7处`Adapter.notifyItemChanged()`调用。其他地方的调用会不会报错？仍然通过修改代码的方式，强行让应用执行其他一处 `Adapter.notifyItemChanged()`，发现居然也崩溃了。可以排除RecyclerView和Adapter的用法不正确这一推测。

在Android Studio中可以看到我们项目引用的库是support库版本是 25.2.0，其中的确有 AnimatorCompatHelper，但错误日志又表明APK包中是没有这个类的。 一个说有，一个说没有，到底有没有？ 反编译APK包看一看。

![](v7.png)

![](v4.png)

反编译后可以看到APK包中有RecyclerView，没有AnimatorCompatHelper。所以可以确定原因： **应用中少了AnimatorCompatHelper类**

分别反编译混淆前的APK包以及混淆前的APK包，观察是否混淆代码导致APK包中的AnimatorCompatHelper类被移除。发现无论混淆与否，APK中都找不到AnimatorCompatHelper。排除混淆的问题。

[Android 开发中遇到的一些疑难杂症 | 幸运码渊](http://www.goluck.top/2017/12/03/Android%20%E5%BC%80%E5%8F%91%E4%B8%AD%E9%81%87%E5%88%B0%E7%9A%84%E4%B8%80%E4%BA%9B%E7%96%91%E9%9A%BE%E6%9D%82%E7%97%87/)中提到

> Caused by: java.lang.ClassNotFoundException: Didn’t find class “android.support.v4.animation.AnimatorCompatHelper” 这一般是引入了多个三方库，版本不统一导致的

库版本不统一，这种情形是比较容易理解的。第三方库自己通常也会依赖别的库。假如项目中使用第三方库A和B，而A和B都依赖C。但A依赖C v1.0，而B依赖C v2.0。Gradle也不清楚该如何 **正确** 处理。所以Gradle除了给出一条警告，它本着优先使用高版本的原则，最终会选择使用C v2.0库。

Gradle默认优先选择高版本的策略是合理的。高版本中通常会有新的功能特性，C v2.0比 C v1.0会多不少东西。选择C v2.0，大概率可以保证A不会出问题。而选择C v1.0，没法保证B不出问题。

但还有一种可能就是高版本中减少功能特性，删除过期的代码。这时选择高版本的策略就未必正确了。很不幸，support库(android.support.v4)真的充当了我们这里所说的C。

对比 25.2.0 和 26.1.0 版本的support库，发现两个版本变化特别大。26.1.0的support库中根本就没有`android.support.v4.animation`这个包。Android官方给出原因：

[android.support.v4.animation missing in API 26 - Stack Overflow](https://stackoverflow.com/questions/47350681/android-support-v4-animation-missing-in-api-26)和[这里](https://developer.android.com/topic/libraries/support-library/revisions.html#26-0-0-alpha1)提到原因：

> Note: The minimum SDK version has been increased to 14. As a result, many APIs that existed only for API < 14 compatibility have been deprecated. Clients of these APIs should migrate to their framework equivalents as noted in the reference page for each deprecated API.

26.0.0版本开始，support库对min SDK version的要求是至少14，所以一些用于兼容14以下的代码直接从support库中移除了。

使用`./gradlew :app:dependencies`分析我们项目的依赖关系，发现`android.arch.lifecycle:extensions`依赖了26.0.1的support库。

```
---- android.arch.lifecycle:extensions:1.1.1
     +--- android.arch.lifecycle:runtime:1.1.1 (*)
     +--- android.arch.core:common:1.1.1 (*)
     +--- android.arch.core:runtime:1.1.1
     |    +--- com.android.support:support-annotations:26.1.0
     |    \--- android.arch.core:common:1.1.1 (*)
     +--- com.android.support:support-fragment:26.1.0 (*)
     +--- android.arch.lifecycle:common:1.1.1 (*)
     +--- android.arch.lifecycle:livedata:1.1.1
     |    +--- android.arch.core:runtime:1.1.1 (*)
     |    +--- android.arch.lifecycle:livedata-core:1.1.1
     |    |    +--- android.arch.lifecycle:common:1.1.1 (*)
     |    |    +--- android.arch.core:common:1.1.1 (*)
     |    |    \--- android.arch.core:runtime:1.1.1 (*)
     |    \--- android.arch.core:common:1.1.1 (*)
     \--- android.arch.lifecycle:viewmodel:1.1.1
          \--- com.android.support:support-annotations:26.1.0
```

这个依赖导致实际编译时用的support库是26.0.1版本而非我们自己指定的25.2.0版本，导致生成的APK包中缺少AnimatorCompatHelper最终报错。

# 解决办法
一种解决办法是升级项目中的support库26.0.2版本。

另一种解决办法是更严格地指定使用25.2.0版本的support库。 [来源](https://stackoverflow.com/questions/43320496/noclassdeffounderror-android-support-v4-animation-animatorcompathelper)

```
configurations.all {
    resolutionStrategy.eachDependency { DependencyResolveDetails details ->
        def requested = details.requested
        if (requested.group == 'com.android.support') {
            if (!requested.name.startsWith("multidex")) {
                details.useVersion '25.2.0'
            }
        }
    }
}
```

# 总结
26.0.0版本的support库对min SDK version的要求是至少14，所以一些用于兼容14以下的代码直接从support库中移除了。Android开发中使用第三方库时要注意是否有support库版本不统一的问题，避免运行时找不到support库中的类引起crash。

# 参考资料
[Android 开发中遇到的一些疑难杂症](http://www.goluck.top/2017/12/03/Android%20%E5%BC%80%E5%8F%91%E4%B8%AD%E9%81%87%E5%88%B0%E7%9A%84%E4%B8%80%E4%BA%9B%E7%96%91%E9%9A%BE%E6%9D%82%E7%97%87/)
[android.support.v4.animation missing in API 26 - Stack Overflow](https://stackoverflow.com/questions/47350681/android-support-v4-animation-missing-in-api-26)
[support中v4 v7库版本错误详解 - CSDN博客](https://blog.csdn.net/xx326664162/article/details/71488551)