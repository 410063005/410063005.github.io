---
title: genymotion-usage
tags:
 - android
---
介绍了Genymotion的用法以及遇到的问题

<!-- more -->

我们应用越来越大，真机开发调试速度很慢。试试Genymotion虚拟机如何。

# 问题
安装完成后遇到几个问题。

+ 无法连接Wi-Fi
+ 浏览器下载文件时崩溃
+ 无法安装APK文件

原因分别如下

+ 公司网络需要代理，在模拟器中给Wi-Fi连接设置代理即可
+ Genymotion中安装Google Nexus5 - 6.0.0 - API 23 - 1080x1920镜像后，浏览器默认未开启存储权限。为正常下载需要自行打开存储权限
+ 一是安装策略不允许，自行允许安装未知来源的APK即可。二是APK只包含x86架构的so，与虚拟机CPU架构不匹配

![](wifi-proxy.jpg)
![](browser-perms.jpg)
![](arm-translation.jpg)

# ARM Translation
> An error occured while deploying the file. 
This probably means that the app contains ARM native code and your Genymotion device cannot run ARM instructions. You should either build your native code to x86 or install an ARM translation tool in your device.

安装APK文件出错。这可能意味着应用中包含ARM架构的原生代码，而你当前的Genymotion设备无法运行ARM指令。你可以编译原生代码以支持x86架构，或者在您的设备上安装ARM翻译工具。

从[这里](https://docs.google.com/file/d/0B-p1r5SNN4adcmhtaGdMVml0Qzg/edit)下载ARM翻译工具并安装，重启后，可以正常安装手机QQ。

![](install-arm-translation.jpg)
![](qq.jpg)

