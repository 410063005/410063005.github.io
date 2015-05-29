---
layout: post
title: "android studio testing support"
date: 2015-05-29 10:43:34 +0800
comments: true
categories: android
published: false
---

---
https://www.bignerdranch.com/blog/triumph-android-studio-1-2-sneaks-in-full-testing-support/

我一直在呼唤Android Studio支持这个功能。我们观察Android界testing状态已有一段时间了，期待有一天Android开发流程中可以完全支持testing。好吧，这天终于来了。

2015年1月我写过博客介绍[如何在Android Studio配置单元测试](https://www.bignerdranch.com/blog/triumph-android-studio-1-2-sneaks-in-full-testing-support/)。如果你有耐心看完整个帖子你会记得这个配置过程有多繁琐。我们知道好的测试正在开发中，我在前文中给出的方式只是权宜之计。

感谢Android Tools Team，现在所有的替代方案完全是不必要的。在Android Studio 1.2中(现在还是beta版，注：作者写这篇博客时是2015.4.10)，不必再找第三方的办法来支持Robolectric。基本工作已经在2015年2月发布的Android Studio 1.1中完成。该版本包括一个实验性的设置用于开启单元测试。上周从1.2 beta版本开始，这个选项不再是实验选项了。很奇怪[release notice for v1.2](http://tools.android.com/recent/androidstudio12betaavailable)居然没有提到这个变化，但不管怎样现在已经支持单元测试了。

今天要讲讲如何极大地简化了在Android Studio中配置单元测试的过程。还会讲到如何避免使用我1月份那种复杂的配置方式。

# Fresh Setup
讲解如何在Android Studio 1.1中配置单元测试的[unit testing how-to](http://tools.android.com/tech-docs/unit-testing-support)对Android Studio 1.2已经不再有效。Android Studio 1.2中唯一需要做的就是选择正确的Build Variant Test Artifact。是的，就这么简单。`build.gradle`文件如下：

```
dependencies {
    compile fileTree(dir: 'libs', include: ['*.jar'])

    ...

    testCompile 'junit:junit:4.12'
    testCompile('org.robolectric:robolectric:3.0-rc2') {
        exclude group: 'commons-logging', module: 'commons-logging'
        exclude group: 'org.apache.httpcomponents', module: 'httpclient'
    }
}
```

# Robolectric 3.0
细心的读者会发现上面脚本中使用最新的Robolectric v3.0发布版本。当然我们也在Android Studio中使用Robolectric 2.4测试过这个新的配置方法，工作得也很好。

---



---

# test



## test22


