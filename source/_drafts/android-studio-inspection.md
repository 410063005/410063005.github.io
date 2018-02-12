---
title: android-studio-inspection
tags:
 - Android
---

> Unused resources make applications larger and slow down builds

随着项目开发过程，会出现越来越多的无用资源，由于偷懒、赶进度或者担心需求变化，我们经常有意无意保留这些无用资源：包括图片、XML资源和代码。随着无用资源越来越多，APK包增大，编译速度变化，而且数量众多的资源难以清理。

Android Studio中通过Analyze > Run Inspection by Name打开代码检查功能，这个功能非常强大，其中的`Unused resources`的功能可以帮我们快速地检查并删除无用资源。

![](/images/1516017616206.png)

及时去掉无用资源能提升减少包大小，减少内存占用。可以说它是最简单的一种傻瓜式性能优化方法了。

我们项目检查`Unused resources`结果如下，一共有404个无用资源：

![](/images/1516018280206.png)

逐个删除。删除过程最好能不时检查下能否编译。

也可以使用自带的一键删除功能

删除前后包大小对比

如果项目大，无用资源多，检查过程和自动清理过程会花费一段时间，需要耐心等待。


WebP requires API 15; current minSdkVersion is 14  http://tools.android.com/tech-docs/webp

![](/images/1516070217052.png)