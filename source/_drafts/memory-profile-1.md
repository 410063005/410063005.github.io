---
title: Memory Profiler使用简介
tags: 
 - android
---

[View the Java Heap and Memory Allocations with Memory Profiler | Android Studio](https://developer.android.com/studio/profile/memory-profiler.html)

Memory Profiler是[Android Profiler](https://developer.android.com/studio/preview/features/android-profiler.html)提供的内存分析组件，它可用于分析内存泄漏、内存抖动，这些内存问题会导致卡顿甚至崩溃。Memory Profiler可展示应用的实时内存图，可导出heap，强制GC以及跟踪内存分配。

通过以下步骤打开Memory Profiler

1. 点击 **View > Tool Windows > Android Profiler** (也可点击工具栏中的 **Android Profiler**)
2. 选择待分析的设备和应用进程
3. 点击 **MEMORY** 时间线中任意地方来打开Memory Profiler

另外，也可以通过[dumpsys](https://developer.android.com/studio/command-line/dumpsys.html)或[观察logcat中的GC事件](https://developer.android.com/studio/debug/am-logcat.html#memory-logs)来检查内存。

## 为什么要分析内存
Android提供[内存管理环境](https://developer.android.com/topic/performance/memory-overview.html)，当它检查到应用不再需要某些对象时垃圾回收器会自动回收不再使用的内存。Android一直在改进找到不再使用内存的算法，但无论哪个版本的Android中一定会在某个时候短暂地停止执行应用代码。大部分时候这种暂停不被察觉到。但当应用分配内存的速度快过系统回收内存的速度，应用在等待系统回收释放内存以重新分配过程中会产生延迟。应用延迟会导致掉帧或可察觉的运行缓慢。

就算应用并不慢，但当它有内存泄漏时，即使在后台它仍然会占用不必要的内存。这种行为会导致不必要的内存回收，拖慢系整体统内存性能。系统可能最终不得不强制杀死内存泄漏的应用进程 。当用户返回到应用时，它不得不完全重启。

为了避免这些问题，你可以使用Memory Profiler：

+ 在时间线中找到可能导致问题的、不必要的内存分配
+ 导出Java堆观察哪些对象正在占用内存。某些周期较长的heap dump中可以发现内存泄漏
+ 记录正常用户操作和极端用户操作时内存的分配过程，分析哪些地方你的代码在短时间内创建了太多对象，或者某些对象存在泄漏。

更多关于减少应用内存占用的编程技巧请参考[Manage Your App's Memory](https://developer.android.com/topic/performance/memory.html。

## Memory Profiler简介
打开Memory Profiler首先看到的内存使用时间线，以及强制GC，导出heap dump，记录内存分配的访问入口。

![](https://developer.android.com/studio/images/profile/memory-profiler-callouts_2x.png)

这里只介绍几个新的功能

+ 5, A button to jump forward to the live memory data
+ 6, The event timeline, which shows the activity states, user input events, and screen rotation events
+ 7, 内存使用时间线，它包括以下部分：
 + 各内存类别占用图，左边的y轴显示了各具体类别
 + 一条表示对象数量的虚线，由右边的y轴表示

但如果使用Android 7.1或以下版本，并不是所有数据缺省可见。如果你看到提示信息"Advanced profiling is unavailable for the selected process"，则需要[开启高级分析功能](https://developer.android.com/studio/preview/features/android-profiler.html#advanced-profiling)以显示以下信息：

+ Event timeline
+ Number of allocated objects
+ Garbage collection events

在Android 8.0或更高版本上，高级分析功能一直会为可调试的app打开。

## 内存是如何统计的
Memory Profiler(图2)中顶部显示的数据是基于应用的私有内存页面统计的。这个数字不包括应用跟系统或其他应用共享的内存页面。

![](https://developer.android.com/studio/images/profile/memory-profiler-counts_2x.png)

内存的类型包括以下几种：

+ **Java**: Java或Kotlin代码创建的对象占用的内存
+ **Native**: C或C++代码创建的对象占用的内存。就算你在应用中并没有使用C++，也可能看到一些native memory，因为Android框架会使用native memory来帮你完成某些工作，比如处理图片资源或其他图像时
+ **Graphics**: graphics buffer queue将像素显示到屏幕时占用的内存，包括GL surface, GL textures等等(注意这里说的是跟CPU共享的内存，而不是专用的GPU内存)
+ **Stack**: 应用中native代码或Java代码使用的栈内存。这通常跟应用中有多少线程在运行有关
+ **Code**: 应用代码和资源占用的内存，比如dex字节码，优化后的dex code, .so库或字体
+ **Other**: 系统不确定该如何分类的内存
+ **Allocated**: Java和Kotlin创建的对象数量。这个数字不包括C或C++对象。当连接到Android 7.1或以下设备上时，这个数字只在Memory Profiler连接到正在运行的应用时开始生效。所以在此之前分配的对象并不会计算在内。但Android 8.0版本开始设备上自带一个可以跟踪所有对象分配过程的工具，所以这个数字能准确表示所有的Java对象数量。

跟之前的Android Monitor tool中的内存数量相比，新的Memory Profiler记录内存的方式有所不同，所以看起来内存占用比以前变高了。Memory Profiler会监听其他类型的内存，导致总量变高，但如果你只关心Java堆内存，那么看到的"Java"类型的内存数量，跟以前应该是一致的。

尽管Java类型的内存占用跟Android Monitor中看到的并不完全一样，但现在是app进程从Zygote fork出来之后所有分配的物理内存页面都有统计，所以新的统计数据在反映物理内存占用方面应该更准确。

> 目前Memory Profiler会将其自身占用的native内存统计到应用上，大约10MB(100k个对象)。后续版本中会从应用内存占用数据中去掉这些数据。

## 查看内存分配
Memory Allocations显示 _内存中每个对象是如何分配的_ 。Memory Profiler还可以显示如下信息：

+ 分配的对象类型，以及占用了多少内存
+ 每个分配过程的调用栈，包括所在线程
+ 对象在什么时候回收(仅在Android 8.0及以上设备上可见)

如果是Android 8.0或以上设备上运行，可以在任意时刻查看对象的分配。点击并按下时间线，然后拖动以选择想要查看内存分配的时间段(如下图)。Android 8.0上自带一个可一直跟踪应用内存分配的工具，所以没必要"Start Allocation Tracking"。

![](https://storage.googleapis.com/androiddevelopers/videos/studio/memory-profiler-allocations-jvmti.mp4)

如果是Android 7.1或以下设备上运行，则需要点击Memory Profiler工具条中的 **Record memory allcations** 。开始后，Android Monitor会一直记录应用中发生的内存分配。点击 **Stop recording** 来查看分配过程。

![](https://storage.googleapis.com/androiddevelopers/videos/studio/memory-profiler-allocations-record.mp4)

一旦选择了时间线区域或完成了一次"Record memory allcations"，已分配对象列表会出现在时间线下面，按类名分组，并按heap count排序。

> 在Android 7.1及以下版本中，可以记录最多65536个分配。如果超过这个数字，仅在记录中保存最近的65535个分配。Android 8.0中没有这个限制。

按以下步骤检查内存分配记录：

1. 浏览对象列表，找到反常的较大的heap count。点 **Class Name** 可以对数据按字母表顺序排序。 右侧的 **Instance View** 面板展示了该类的每个实例，见图3
2. 在 **Instance View** 中点击一个instance, 在下方会出现 **Call Stack** ， call stack中会显示该instance在哪里以及哪个线程分配的。
3. 在 **Call Stack** 标签页，点击任意一行会跳转到相应的代码行

![](https://developer.android.com/studio/images/profile/memory-profiler-allocations-detail_2x.png)

缺省情况下左边的分配列表按类名排列。在列表上方，你可以根据右边的下拉菜单来在下列的方式中切换：

+ **Arrange by class** 按类名分组
+ **Arrange by package** 按包名分组
+ **Arrange by callstack** 按调用栈分组

## 导出heap
导出的heap中可以看到应用中哪些对象正在使用内存。用户使用一段时间之后，你确认应该不再存在的对象仍然出现在heap dump中，可以判断存在内存泄漏。导出heap后可以查看如下内容：

+ 应用创建了哪些对象，每种对象的数量
+ 每个对象使用了多少内存
+ 每个对象被哪里的代码引用
+ 对象在调用栈的什么地方被创建 (Call stacks are currently available with a heap dump only with Android 7.1 and lower when you capture the heap dump while recording allocations.)

点击 **Dump Java heap** 来导出heap。导出时，Java内存总量可能短暂增加。这是正常的，因为heap dump发生在应用进程中，它需要一些内存用于收集数据。

![](https://developer.android.com/studio/images/profile/memory-profiler-dump_2x.png)

如果你需要更精确的heap dump，可以在代码中调用[dumpHprofData()](https://developer.android.com/reference/android/os/Debug.html#dumpHprofData(java.lang.String))来导出heap。

在类列表中，可以看到如下信息：

+ **Alloc Count** : heap中的Allocations数量
+ **Native Size** : 这种对象类型使用的native memory数量。这列数据仅在Android 7.0以及更高设备上可见。
+ **Shallow Size** : 这种对象类型使用的Java内存
+ **Retained Size** : 这个类的所有实例持有的所有内存数量

在类列表顶部，可以使用左边的下拉菜单在以下heap dump中切换：

+ **Default heap** : 当系统没有指定heap时显示default heap
+ **App heap** : 应用主要在这个heap中分配内存
+ **Image heap** : system boot image，包括启动时预加载的类。这里分配的内存保证不会被改变或移除
+ **Zygote heap** : 应用进程在这个heap上从Android系统fork出来

缺省情况下heap中的对象按类名排列。在列表上方，你可以使用下拉菜单来在下列的方式中切换：

+ **Arrange by class** 按类名分组
+ **Arrange by package** 按包名分组
+ **Arrange by callstack** 按调用栈分组。这个选项仅在recording allcations过程中capture heap dump时才有效。所以，heap中可能有对象是在开始record allcations之前创建的，所以这些对象先出现。

列表缺省按 **Retained Size** 列排列。可以点击任一列表头来调整列表的排列顺序。

点击类名打开 **Instance View** 窗口。每个列出的实例包括以下信息：

+ **Depth** : 从GC根节点到当前对象的最短路径
+ **Native Size** : 这种对象类型使用的native memory数量。这列数据仅在Android 7.0以及更高设备上可见。
+ **Shallow Size** : 这种对象类型使用的Java内存
+ **Retained Size** : 这个对象持有的所有内存数量 (见[dominator tree](https://en.wikipedia.org/wiki/Dominator_(graph_theory)))

> 注意：缺省时heap dump中并不显示每个对象的stack trace。想要看到stack trace，在点击 **Dump Java heap** 前必须开始[recording memory allocations](https://developer.android.com/studio/profile/memory-profiler.html#record-allocations)。只有这样才能在 **Instance View** 中选择实例看到 **Reference** 和 **Call Stack** 标签，如下图。但有可能某些对象在你开始recording allcations之前创建，所以这些对象并没有call stack。(由于要求执行allcation recording才能看到stack trace，所以Android 8.0上目前无法看到heap dump中的stack trace)

![](https://developer.android.com/studio/images/profile/memory-profiler-dump-stacktrace_2x.png)

按以下步骤检查heap：

1. 浏览列表找到大小反常的对象，它们可能存在泄漏。点 **Class Name** 可以对数据按字母表顺序排序。 然后点击类列， **Instance View** 面板显示在右边，显示该类的每个实例
2. 在 **Instance View** 面板中点击查看实例。 **References** 标签页在下方，显示指向这个对象的引用。或者也可以点击实例左边的箭头来查看它的字段，然后点击字段名来查看引用。如果想查看字段指向的具体实例，右击字段并选择 **Go to Instance**
3. 如果在 **References** 标签页，你觉得某个引用可能存在泄漏，右击并选择 **Go to Instance** 。 这一操作会从heap dump中选择对应的实例，并显示实例数据

在heap dump中，需要检查以下原因引起的内存泄漏：

+ 指向 `Activity` , `Context`, `View`, `Drawable`的长生命周期引用，以及其他可能引用 `Activity` 或 `Context` 的对象
+ 非静态的内部类，比如 `Runnable`，它可能持有 `Activity` 引用
+ 缓存。缓存可能长时间持有引用，以致超出需要

## heap dump保存为HPROF
一旦导出heap，这些数据仅在Memory Profiler运行时才可见。当你退出Memory Profiler，heap dump会丢失。如何想保存该数据以便以后查看，可使用 **Export capture to file** 将其导出为HPROF文件。请确保以`.hprof`作为文件后缀名。

可将`.hprof`文件拖放到Android Studio来打开。

如果使用其他HPROF分析工具，比如[jhat](https://docs.oracle.com/javase/8/docs/technotes/tools/unix/jhat.html)，你需要把HPROF文件从Android格式转换为标准的Java SE HPROF格式。使用 `android_sdk/platform-tools`目录下的`hprof-conv`工具进行转换。用法如下：

```
hprof-conv heap-original.hprof heap-converted.hprof
```

## 内存分析技巧
暴露内存泄漏问题的一个办法时让应用运行一段时间然后再检查内存。泄漏可能发生在heap的前一部分(Leaks might trickle up to the top of the allocations in the heap.)。当泄漏越小，需要运行应用以发现问题的时间越长。

可以使用下面的方法触发内存泄漏：

+ 在不同的activity状态下切换屏幕方向。由于系统会重新创建`Activity`，切换屏幕方向常常会导致应用泄漏 `Activity`, `Context`或`View`对象。如果应用在别的地方持有这些对象，系统是无法将其回收的
+ 在不同的activity状态下，在当前应用和别的应用之前进行切换。比如，回到Home然后又返回应用

> Tip: 也可以使用[monkeyrunner](https://developer.android.com/tools/help/monkeyrunner_concepts.html)完成上述操作。

<!--
===
## tasks
对比开启20个线程之后，Stack类型的内存占用用什么变化
观察Android Monitor tool和Memory Profiler内存数据差别
-->