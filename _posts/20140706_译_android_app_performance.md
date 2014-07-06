---
layout: post
title: 译_android_app_performance
keywords: android
description: android
categories: [android]
tags: [android]
group: archive
icon: globe
---
(翻译) Android Application Development: 10 Tips for More Efficient Apps

在 Google Play 上放一个耗电又耗内存且界面缓慢的 app，是成为一个失败的开发者的最好办法。这些特点
同时很可能招致负面的用户评论，得到不好的名声，即使你的应用有非常棒的设计和创意。

产品在效率、耗电量、内存使用上的任何缺陷都会影响应用的成功。所以开发仔细优化，运行流畅的应用以便 
Android 系统不会预防这些应用 [FIXME]。 我们不讨论高效编程，因为不是说你的代码必须通过什么性能
测试。但好的代码也会消耗运行时间。本文中，我们讨论如何最小化运行时间，以便用户喜欢你的应用。

# 使用多线程提高效率

## 技巧1 怎样将操作放到后台线程中

通常应用中所有的操作都跑在前台的主线程中，所以应用的响应速度会受到影响，可能马上引起挂起、冻结甚至系统
错误

为提高响应速度，你应当将耗时任务，比如网络或数据库操作、复杂的计算从应用的主线程中移到独立的
后台线程中。最有效的实现方式是在类级别进行的。可以使用 AsyncTask [TODO] 或 IntentService
[TODO] 类来组织后台任务。一旦实现 IntentService，它会在必要时启动工作线程来处理 Intent 请求。

使用 IntentService 时要考虑以下限制：
+ 这个类不会将结果回送到 UI，所以需要使用 Activity 来向用户显示结果
+ 任何请求的处理过程不能被中断

## 技巧2 如何避免 ANR 和保持响应性

上面的由后台线程完成耗时操作的技巧同样适用于减少 ANR 错误。要做的不过是通过继承 AsyncTask 创建后台工作线程，并实现 doBackground() [TODO] 方法。

另一个方法就是创建自己的 Thread[TODO] 或 HandlerThread[TODO] 类。要记住，除非你指定这些线程
的优先级为 "background"，否则它仍然可能减慢应用运行速度，因为新线程缺省的优先级跟 UI 线程相同。

## 技巧3 怎样在新的线程中发起请求

数据并不能马上显示出来，尽管你可以通过使用 CursorLoader [TODO] 对象来加速这个过程：当请求仍然在
后台处理时，避免干扰 Activity 与用户的交互。

使用 CursorLoader 你的应用可以在一个独立的后台线程发起 ContentProvider 请求，并且仅当请求完成
时向 Activity 回送结果。

## 技巧4 还能做什么

+ 使用 StrictMode [TODO] 来检查 UI 线程中潜在的耗时操作。使用特殊工具，比如 systrace [TODO]、
traceview [TODO]，可以找到应用响应速度的瓶颈。
+ 使用进度条向用户显示进度
+ 如果初始启动非常耗时，可以考虑显示启动画面。

# 电池使用时间优化
用户愤怒地卸载那些耗电大户 app 时，我们无可非议。对电池使用时间最大的威胁包括：

+ 经常唤醒设备以更新数据
+ 使用 EDGE 或 3G 来传输数据
+ 在没有 JIT 的情况下进行文本解析或正则式匹配

## 技巧5 如何优化网络问题

+ 确认应用在没有数据连接的情况下会略过网络操作；只在 3G 或  WiFi 连接且无漫游时更新数据
+ 使用紧凑的数据格式，比如，将文本数据和二进制数据打包到同一个请求中的二进制格式
+ 使用高效的解析器，比如优先考虑流解析器而不是基于树的解析器
+ 减短服务器请求响应时间
+ 尽可能使用 Gzip 压缩文本数据

## 技巧6 如何优化前台 App

使用 Wakelock 时，应尽可能降低它的级别。为避免你没注意到的 bug 引起电量消耗，使用指定 timeout
的 Wakelock。

开启 android:keepScreenOn

考虑手动重用 Java 对象，比如 XmlPullParserFactory 和 BitmapFactory；正则式中 使用
Matcher.reset() ；字符串中使用 StringBuilder.setLength(0)

注意同步问题，尽管在主线程中一般都是线程安全的。

ListView 中大量使用对象重用策略

尽量使用网络定位而不是 GPS 定位。简单对比下，GPS 要花 1mAh (25 sec * 140mA)，而网络定位只
要 0.1mAh (2 sec * 180mA)

确认取消 GPS location update，因为即使用 onPause() 后 GPS 定位仍会继续。当所有的应用取消
注册后，users can enable GPS in Settings without blowing the battery budget [FIXME]。

浮点数计算非常耗 CPU，可以考虑使用微度 (microdegree) 用于大量的 GEO 数学计算；当使用 DisplayMetrices 进行 DPI 相关任务时可以缓存计算结果。

## 技巧7 如何优化后台 APP

每个进程会消耗至少 2MB 内存，且前台应用需要内存时后台进程可能被杀死重启，所以 Service 最好是
短生命周期的。

减少内存占用

只在设备处于唤醒状态时每 30 分钟更新一次数据。 Service 任务过重或休眠都不好，所以应用使用 
AlarmManager 或 <receiver> ：当 Service 完成时就调用 stopSelf()。使用 AlarmManager 启动
 Service 时一定要谨慎使用 *_WAKEUP 类型的可唤醒 Alarm。尽量使用 setInexactRepeating() 让 Android 系统批处理应用的数据更新。当使用 <receiver> 时，动态启用或禁用 manifest 中的组件，尤其
是无任何操作时。

## 技巧8 还能做什么

进行完全的数据更新前可以检查电量和网络状态；可以在更好的状态下进行批量数据数据传输；应用设置中考虑
提供电量使用设置，比如更新频率和后台行为。

# 实现占用内存少的 UI
## 技巧9 如何识别布局性能问题

when creating UI sticking soley to basic features of layout managers，你可能开发出耗内存且
界面缓慢的应用。开发出界面流畅、耗内存少的应用，第一步就是使用 Android SDK 中的 Hierarchy Viewer [TODO] 来检查布局的性能瓶颈。

另一个非常棒的性能问题检查工具就是 Lint [TODO]。它会扫描应用的源码以检查潜在的 bug，包括 view 结构的优化问题。

## 技巧10  如何修复问题
如果布局存在缺陷，性能过低，可以考虑使用 RelativeLayout[TODO] 布局来代替 LinearLayout[TODO] 扁平化布局，减少层次结构。

# 追求完美和卓越

尽管上面的每个小技巧看起来不过是一些小的改进，但如果这些技巧成为你每天编码的一部分，你会发现意外的效率提升。期待 Google Play 上出现更多优秀的应用，流畅、快速、耗电少，使用 Android 世界更趋完美。



