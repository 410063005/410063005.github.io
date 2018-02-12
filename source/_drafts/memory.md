---
title: memory
tags:
  - Android
  - 性能
toc: true
---
翻译自[Manage Your App's Memory | Android Developers](https://developer.android.com/topic/performance/memory.html)
<!--more-->

在任何软件开发中RAM都是宝贵的资源，而在移动操作系统中由于内存受限所以更加宝贵。虽然ART和Davik虚拟机都能回收内存，但并不表示你可以忽略内存的分配和回收的时机和位置。你仍然需要避免内存泄漏，并生命周期回调中的恰当时间释放对象引用。

## 监视可用内存和已使用内存

Memory Profiler可以帮助诊断内存问题：

1. 观察分配了多少内存
2. 发起内存回收，导出Java堆
3. 记录内存分配并检查分配的对象

## 响应并释放内存
 [Overview of Android Memory Management](https://developer.android.com/topic/performance/memory-overview.html)中提到Android可以从应用中以多种方式回收内存，必要时直接杀死应用进程以给更高优先级的任务分配内存。

[(42) Trimming and Sharing Memory (Android Performance Patterns Season 3 ep5) - YouTube](https://www.youtube.com/watch?v=x8Hddx1eOZo&index=5&list=PLWz5rJ2EKKc9CBxr3BVjPTPoDPLdPIFCE)

使用[ComponentCallbacks2](https://developer.android.com/reference/android/content/ComponentCallbacks2.html)接口。该接口中的`onTrimMemory()`回调方法允许应用在前台或后台监听内存相关事件，应用可响应这些事件来回收内存。代码如下：

```java
import android.content.ComponentCallbacks2;
// Other import statements ...

public class MainActivity extends AppCompatActivity
    implements ComponentCallbacks2 {

    // Other activity code ...

    /**
     * Release memory when the UI becomes hidden or when system resources become low.
     * @param level the memory-related event that was raised.
     */
    public void onTrimMemory(int level) {

        // Determine which lifecycle or system event was raised.
        switch (level) {

            case ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN:

                /*
                   Release any UI objects that currently hold memory.

                   The user interface has moved to the background.
                */

                break;

            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW:
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL:

                /*
                   Release any memory that your app doesn't need to run.

                   The device is running low on memory while the app is running.
                   The event raised indicates the severity of the memory-related event.
                   If the event is TRIM_MEMORY_RUNNING_CRITICAL, then the system will
                   begin killing background processes.
                */

                break;

            case ComponentCallbacks2.TRIM_MEMORY_BACKGROUND:
            case ComponentCallbacks2.TRIM_MEMORY_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_COMPLETE:

                /*
                   Release as much memory as the process can.

                   The app is on the LRU list and the system is running low on memory.
                   The event raised indicates where the app sits within the LRU list.
                   If the event is TRIM_MEMORY_COMPLETE, the process will be one of
                   the first to be terminated.
                */

                break;

            default:
                /*
                  Release any non-critical data structures.

                  The app received an unrecognized memory level value
                  from the system. Treat this as a generic low-memory message.
                */
                break;
        }
    }
}
```

API level 14中添加的`onTrimMemory()`方法，而之前版本中可使用`onLowMemory()`方法。

## 检查可供使用的内存
Android为分配给每个应用的堆大小设置了设置，以允许多个进程运行。基于当前设备有多少内存，这个值大小会有所不同。当应用的堆大小到达上限时继续分配内存，系统会抛出`OutOfMemoryError`。

使用`getMemoryInfo()`查询当前设备上的可用内存，该方法返回`ActivityManager.MemoryInfo`对象。该对象描述了设备当前内存状态，包括可用内存、总内存以及内存阈值——系统在什么内存状态下开始杀应用进程。`lowMemory`字段表示设备内存是否过低。

```java
public void doSomethingMemoryIntensive() {

    // Before doing something that requires a lot of memory,
    // check to see whether the device is in a low memory state.
    ActivityManager.MemoryInfo memoryInfo = getAvailableMemory();

    if (!memoryInfo.lowMemory) {
        // Do memory intensive work ...
    }
}

// Get a MemoryInfo object for the device's current memory status.
private ActivityManager.MemoryInfo getAvailableMemory() {
    ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
    ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
    activityManager.getMemoryInfo(memoryInfo);
    return memoryInfo;
}
```

## 更多地使用节省内存的代码结构
### 少用Service
让一个不必要的service运行是Andrdoid应用最糟糕的内存问题。

启动service之后，系统总是会让该service所在进程保持运行。这种行为让service进程非常昂贵，因为它占用的内存无法分给其它进程。这导致LRU缓存中的cached process变少。

建议使用[JobScheduler](https://developer.android.com/reference/android/app/job/JobScheduler.html)代替service。如果一定要使用service，请优先使用[IntentService](https://developer.android.com/reference/android/app/IntentService.html)

## 使用优化的数据结构
Java语言中的一些类并没有专门为移动设备优化。比如[HashMap](https://developer.android.com/reference/java/util/HashMap.html)使用内存的效率就很低。

Android框架提供

- [SparseArray](https://developer.android.com/reference/android/util/SparseArray.html)
- [SparseBooleanArray](https://developer.android.com/reference/android/util/SparseBooleanArray.html)
- [LongSparseArray](https://developer.android.com/reference/android/support/v4/util/LongSparseArray.html)

### 少用抽象
抽象是个好的编程实践，可以增加代码灵活性，更容易维护。但抽象是有代价的，需要执行更多的代码，需要更多执行时间，更多内存空间来加载。所以如果没有足够的好处，不要使用抽象。

enum通常比常量多占用两倍内存，所以Android应用中一定要避免enum。

### 使用nano protobuf
常规的protobuf生成非常冗余松散的代码，会导致内存增加，APK包大小增加，拖慢执行速度。所以请用nano protobuf代替。

### 避免内存抖动
GC通常不会影响应用性能。但短时间内大量发生GC会消耗帧时间。

内存抖动是指短时间内分配了大量临时对象。比如在`for`循环中创建许多临时对象。或者在View的`onDraw()`中创建`Paint`或`Bitmap`。这些情形下，应用会以非常快的速度创建很多对象。它们迅速占用可用内存，导致必须GC。

## 去掉占用内存过多的资源和库
有些资源和库会在你不知情的情况下增加内存占用。检查APK中可能影响内存使用的第三方库和资源。从代码中去掉冗余，不必要以及过于臃肿的组件、资源和库。

### 减少包大小
减少包大小的同时也可以减少内存占用。关于减少包大小的更多信息见[Reduce APK Size](https://developer.android.com/topic/performance/reduce-apk-size.html)

### 使用Dagger 2
使用依赖注入框架可以简化代码，并提供响应式环境以便于测试和配置。如果需要使用依赖注入，建议使用[Dagger 2](http://google.github.io/dagger/)。因为它不使用反射。其他一些依赖注入框架使用反射，会对代码中的注解进行扫描。该过程占用相当多的CPU和内存，当应用启动时会产生明显延迟。

### 慎用第三方库
很多外部库通常不是专门为移动设备环境开发的，所以用于移动客户端时非常低效。虽然[ProGuard](https://developer.android.com/tools/help/proguard.html)可以移除不必要的API和资源，但它并不能移除库内部的大部分依赖。而你想使用的功能很可能只是库当中的一小部分。当你使用第三方库中的Activity子类时，这通常都是问题。使用反射的第三方库(很常见)也意味着你需要不少时间去调整ProGuard配置。




