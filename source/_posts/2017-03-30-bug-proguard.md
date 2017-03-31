---
title: Bug系列之proguard的坑
tags:
  - android
  - bug
date: 2017-03-30 21:29:50
---


2.6.7发版本时碰到几个跟proguard混淆相关的问题，记录下备忘。

<!--more-->

# 问题一
测试同事反馈某个赠送礼物的功能不正常。发现debug包中该功能完全正常，而release包里面则有问题。

想起之前Android Studio对相关代码检查时，提示礼物的model类中某些字段未被用到，可删除。所以第一反应是：会不会proguard也认为这个字段未用到，所以给删除了。检查proguard的输出文件`usage.txt`中是否存在删除model类的记录，发现并没有。但`mapping.txt`中却有如下记录：

```
com.tencent.PmdCampus.api.GiftsService$InnerGift -> com.tencent.PmdCampus.c.f$a:
    int num -> a
    java.lang.String content -> b
    java.lang.String receiver -> c
    void <init>(java.lang.String) -> <init>
```
    
原来是礼物的model类被混淆了！这种情况下Gson生成的json数据不正确，导致跟后台的通信失败。

**总结：之前赶进度偷懒将model类放在不正确的package下， 结果被混淆了，最终引发问题。**

我们的proguard配置中已经指定model所在的package不被混淆，所以解决办法很简单，将相关的类放到该package下即可。

```
# Application classes that will be serialized/deserialized over Gson
-keep class com.tencent.PmdCampus.model.** { *; }
```

# 问题二
测试同事反馈更换头像或首次发送语音消息时app会crash，必现。两个功能都涉及到一个上传库。

跟前一个问题类似，debug包中该功能完全正常，而release包里面则有问题。找到了一个比较奇怪的崩溃日志，发生crash的地方并不是我们自己的代码。

```
java.lang.NoSuchFieldError: no "J" field "mNativeContext" in class "Lcom/tencent/upload/network/base/ConnectionImpl;" or its superclasses
com.tencent.upload.network.base.ConnectionImpl.native_init(Native Method)
com.tencent.upload.network.base.ConnectionImpl.<clinit>(Unknown Source)
com.tencent.upload.network.base.a.<init>(Unknown Source)
com.tencent.upload.network.base.f.<init>(Unknown Source)
com.tencent.upload.network.b.g.a(Unknown Source)
com.tencent.upload.network.b.g.a(Unknown Source)
com.tencent.upload.network.b.c.a(Unknown Source)
com.tencent.upload.network.b.c.d(Unknown Source)
```

显然，又是某个第三方库代码被不正确地混淆了，导致上传时crash。

分析后发现调用了来自uploadlib.jar的方法，不清楚具体实现的代码，还是不混淆为妙！ 添加以下配置后问题解决。

```
# uploadlib
-keep class com.tencent.upload.** {*;}
```

**总结：无论来自哪里的第三方库，一定要注明来源(最好是官网地址)，并添加官方给出的proguard配置**

注：不知道uploadlib.jar的来源，只好简单地配置为完整不混淆。

---

为什么这么多的混淆问题之前没有发现呢？不得不说一样腾讯云IM SDK的包结构非常坑，很多类放在`com.tencent`包下，而它的[官网]给出的混淆配置居然是这样：

```
-keep class com.tencent.** {*;}
...
```

这明摆着是要让用了云IM SDK的、使用标准package命名方式的腾讯app没法好好混淆吗？

我们之前的版本一直被这个问题困扰，某个版本中优化了`-keep class com.tencent.**{*;}`这个不合理的配置，让`com.tencent`包下大量原本可被混淆的代码能正确地混淆。但同时，之前一些错误的或是遗漏的混淆引发的问题也暴露出来了，如上面提到的两个问题。

[ref]: https://www.qcloud.com/document/product/269/1557#1.6-.E4.BB.A3.E7.A0.81.E6.B7.B7.E6.B7.86.E8.A7.84.E5.88.99