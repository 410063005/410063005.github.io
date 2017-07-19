---
title: skia简介
tags:
---
# download
[下载](https://skia.org/user/download)

![](download_source.jpg)

# build
[编译](https://skia.org/user/build)

这里讲的是windows平台上的编译。

Skia can build on Windows with Visual Studio 2015 Update 3, or Visual Studio 2017 by setting msvc = 2017 in GN. No older versions are supported. The bots use a packaged 2015 toolchain, which Googlers can download like this:

```
# python infra/bots/assets/win_toolchain/download.py -t C:/toolchain
python infra\bots\assets\win_toolchain\download.py -t C:\toolchain
```

download.py脚本报错：`CIPD binary not found on your path`

错误分析，忘记安装depot_tools

安装方式见(这里)[https://skia.org/user/download]

```
git clone 'https://chromium.googlesource.com/chromium/tools/depot_tools.git'
export PATH="${PWD}/depot_tools:${PATH}"
```

遇到困难 暂时放弃编译skia

# 知识点
[参考](http://blog.csdn.net/fengbingchun/article/details/38492061) src目录下:

+ android
+ c
+ codec
+ core - Skia的核心，基本都是一些图形绘制函数
+ effects - 图形图像的特效
+ fonts
+ images - 支持常见图像的解码、部分图像的编码和动画
+ ports - 是Skia的一些接口在不同系统上的实现，平台相关的代码，比如字体、线程、时间等。这些与Skia的接口，需要针对不同的操作系统实现
+ svg - 对矢量图SVG的支持
+ utils - 辅助工具类
+ views - Skia界面库
+ xml - 处理xml数据
+ pdf - 处理pdf文档

Skia引擎代码

+ 头文件 - android/external/skia/include
+ 源文件 - android/external/skia/src
+ 封装层
 + android/framework/base/core/jni   
 + android/framework/base/core/jni/android/graphics
 


