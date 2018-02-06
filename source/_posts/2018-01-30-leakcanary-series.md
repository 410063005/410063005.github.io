---
title: 玩转LeakCanary
date: 2018-01-30 11:18:25
tags:
 - Android
---
[TODO] 总结了LeakCanary原理及用法。
<!--more-->

+ {% post_link leakcanary-detect-all-memory-leaks (译)为什么要使用LeakCanary %}
+ {% post_link leakcanary-get-object-size LeakCanary之如何获取对象大小 %}
+ [LeakCanary简介](#)
+ [LeakCanary原理](#)
+ [LeakCanary高级用法](#)
+ {% post_link leakcanary-application LeakCanary实战 %}

# 如何使用LeakCanary

`LeakCanary.install()`返回一个预先配置好的`RefWatcher`。该方法同时还会自动使用`ActivityRefWatcher`，这个对象自动检测`Activity.onDestroy()`后activity是否有泄漏。

```java
public class ExampleApplication extends Application {

  public static RefWatcher getRefWatcher(Context context) {
    ExampleApplication application = (ExampleApplication) context.getApplicationContext();
    return application.refWatcher;
  }

  private RefWatcher refWatcher;

  @Override public void onCreate() {
    super.onCreate();
    if (LeakCanary.isInAnalyzerProcess(this)) {
      // This process is dedicated to LeakCanary for heap analysis.
      // You should not init your app in this process.
      return;
    }
    refWatcher = LeakCanary.install(this);
  }
}
```

可以使用`RefWatcher`用到检查应当被GC的引用：

```
RefWatcher refWatcher = {...};

// We expect schrodingerCat to be gone soon (or not), let's watch it.
refWatcher.watch(schrodingerCat);
```

可以使用`RefWatcher`检查fragment的泄漏情况：

```java
public abstract class BaseFragment extends Fragment {

  @Override public void onDestroy() {
    super.onDestroy();
    RefWatcher refWatcher = ExampleApplication.getRefWatcher(getActivity());
    refWatcher.watch(this);
  }
}
```

# 工作原理
1. `RefWatcher.watch()`创建指向被检查对象的[KeyedWeakReference](https://github.com/square/leakcanary/blob/master/leakcanary-watcher/src/main/java/com/squareup/leakcanary/KeyedWeakReference.java)
2. 之后，在后台线程中检查引用是否被清除，如果没有的话GC一下
3. 如果引用仍然没有清除，则导出heap到`.hprof`文件，并保存到文件系统
4. 一个独立的进程启动`HeapAnalyzerService`，`HeapAnalyzer`使用[HAHA](https://github.com/square/haha)解析heap dump
5. `HeapAnalyzer`在使用key在heap dump中找到`KeyedWeakReference`，定位到泄漏的引用
6. `HeapAnalyzer`计算 *到GC根节点的最短强引用* 以确定是否有内存泄漏，然后重新构建导致内存泄漏的引用链
7. 分析结果发送到应用进程的`DisplayLeakService`，并且通过notification进行提示