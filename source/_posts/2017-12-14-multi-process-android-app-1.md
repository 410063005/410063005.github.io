---
title: (译)Android多进程应用
tags:
  - Android
date: 2017-12-14 10:13:02
---

Android官网上只是简单地提到了进程，但应用开发中使用多进程会遇到各种各样的问题。本文讨论了其中一些问题，也许能给你解决问题的思路。

<!--more-->
原文：[Making Multi-process Android applications](http://engineering.life360.com/engineering/2016/06/10/multi-process-android-apps/)
译文：[译文](#)

可以让Android应用中的不同组件运行在不同的进程。有时这种做法是必要的，可以改进应用性能。但你必须意识到，官方网站上并没有太多关于Android多进程应用的文档，而且多进程应用也不容易理解。我们看看何时它会很有用，有何挑战以及如何解决。

缺省时所有Android应用组件都运行在相同的Linux进程中。但也可以让不同组件运行在不同的进程。许多场景下多进程很有用。我们来看看。

Android会不时杀死进程回收内存，以提供给更重要的进程。Android使用重要性等级来决定该杀死和保留哪个进程。占用内存高的后台进程更可能被Android杀死。如果你将后台组件(service, content provider)和前台组件(activity)放在同一个进程，该进程会使用更多的内存。当应用进入后台，它更可能被杀死。Android杀死进程时后杀死其中所有的组件。为了将后台组件跟UI的生命周期解耦，最好将其独立到一个独立的进程。

比如，如果应用需要在客户端和服务器端之间同步数据时，它可以在独立进程中的service中进行同步。这可以让后台进程更少重启，让其独立于UI进程的生命周期。另外，UI进程中的崩溃和异常对后台进程没有任何影响。反之亦然。

使用多进程的另一个原因是多个特性完全不同且独立。比如，如果应用支持Email, Calendar, Contacts, Notes以及Tasks，其中每个特性可以在独立进程中运行以保持各自的生命周期独立。

# 如何运行多进程
`<activity>`, `<service>`, `<receiver>`以及`<provider>`都支持`<android:process>`属性，该属性可以指定组件运行的进程。多个组件也可以共享同一个进程，指定多少进程就能启动多少进程。

# 挑战
当Android应用使用多进程时，需要处理进程间的数据一致性。甚至你的代码结构良好，在运行时也不太容易知道哪段代码在哪个进程运行。

## 共享数据
尽管可以在多个独立进程中运行不同独立逻辑，某些时候它们仍然需要共享数据。如果你使用文件，数据库或者shared preferences，你可能遇到跨进程数据不一致问题。如果使用文件，你需要监听其他进程对文件进行的修改。如果从多个进程访问数据库，你可能会遇到非常难以调试的数据库冲突或死锁问题。如果使用shared preferences，必须使用多进程模式打开它，并且使用`OnSharedPreferenceChangeListener`监听其他进程导致的变化。

## 单例
单例是在Android应用中跨组件共享状态信息和数据的简单办法。可以使用`synchronized`关键字保证单例的方法是线程安全的。但如果在多进程应用中使用单例，可能生成的单例对象数量跟进程数量一样多。因为进程并不共享地址空间，所以一个进程中的单例对象对其他进程并不可见。如果使用shared preferences，数据库或文件中的数据初始化单例，要让(不同进程中的)每个单例对象保持一致的数据非常困难，而且很可能它们在运行时有不同的数据状态。另外，`synchronized`对这种情况不起作用。看看代码中单例的通常用法。

![](http://engineering.life360.com/images/android-multiproc-singletons.webp)

假设我们有一个单例类，不妨称之为`SubscriptionManager`，它会跟踪用户订阅。在这个类中更新订阅内容。同时你将数据保存到文件以供离线访问。如果数据文件存在的话，由这些文件初始化单例。有两个进程：主进程和后台进程。在一个工具类中检查用户是否订阅了某个内容，该类会被主进程和后台进程调用。用户调用`SubscriptionManager.getInstance(context).updateSubscription(feature, subscription);`从主进程订阅。这个调用更新了订阅内容，发送到后台并且更新了文件。

在这种情况下，后台进程中的`SubscriptionManager`并不知道主进程中发生的这次订阅，除非你在数据文件中设置了`FileObserver`，监听数据变化更进行相应更新。或者，你也可以使用广播机制来通知这次订阅。所以说，使用这些方式在进程间共享数据非常复杂，并不是最佳方案。

# 解决办法
Android通过`Binder`接口提供进程间通信(IPC)。[Content providers](https://developer.android.com/guide/topics/providers/content-providers.html)和[Bound services](https://developer.android.com/guide/components/services.html#CreatingBoundService)使用binder接口来跨进程通信。所以本方案使用这种方式来维护多进程间数据和状态的一致性。

## Content Provider
ContentProvider用于管理对结构化数据的访问。它们封装数据并且提供数据安全机制。ContentProvider是在当前进程的数据跟其他进程的代码之间建立联系的标准接口。(Content providers are the standard interface that connects data in one process with code running in another process)

![](http://engineering.life360.com/images/android-multiproc-content-providers.webp)

尽管ContentProvider本是用于应用间共享数据，它也可以用于在多进程之间共享数据。Android保证跨进程时ContentProvider的单一性。[ContentResolver](https://developer.android.com/reference/android/content/ContentResolver.html)提供易用的接口，所以应用代码不必担心IPC细节。

注意：如果不想在应用外共享数据， **不要** 在manifest中对外暴露ContentProvider(`android:export="false"`)

尽管ContentProvider和ContentResolver的"CURD" API主要被设计为共享SQL数据库中的数据，也可以扩展更通用的[`call(android.net.Uri, java.lang.String, java.lang.String, android.os.Bundle)`](https://developer.android.com/reference/android/content/ContentResolver.html) API以实现应用特定的目的。

比如：在上述例子的`SubscriptionManager`，你可以提供如下API来检查和更新订阅：

```java
ContentResolver resolver = getContentResolver(); 
Bundle result = resolver.call(“SubcriptionCheck”, arg, extras);
```

## Bound Service
Bound service是客户端-服务器接口中的服务器端。Bound service允许组件(比如activity)绑定到service，发送请求，接收响应，甚至执行IPC

![](http://engineering.life360.com/images/android-multiproc-bound-service.webp)

有两种方式实现IPC：Messengers和AIDL。Messenger方式更简单一些。两种方式中Android框架都做好了IPC底层工作(marshalling, unmarshalling, RPC)，IPC对调用方是透明的。跟service通信的步骤如下：

+ bindService
+ 连接到service时收到回调
+ 使用binder接口发送请求，或调用service API
+ 从service接收响应

## Service
可以使用intent在另一个进程中启动service，或者从代码中的任何地方发送广播，并且在另外的进程中接收该广播并启动一个service来处理它。

比如你有一个测量系统用于捕获应用中的某些事件，可以将每次测量发送到service，让service将其写入数据库或发送到后台，而不是让代码中到处都是数据库或网络操作。

# Application类的坑
你可以实现`Application`的子类，并且在`AndroidManifest.xml`的`<application>`标签的`<android:name`>属性中指定这个类的全名。当应用进程启动时，你实现的`Application`子类会最先初始化。

要注意的是，应用中启动的每一个进程都会执行上述初始化。不幸的是，并不能为不同的进程指定不同的Application类。

![](http://engineering.life360.com/images/android-multiproc-application.webp)

如果你使用Application类，很可能你会在它的`onCreate()`方法中做一些初始化工作。 **请确保这些初始化工作对进程是恰当且必要的**  也在`Application.onCreate()`中使用下面的代码确定进程上下文：

```java
int pid = android.os.Process.myPid(); 
ActivityManager manager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE); 
for (ActivityManager.RunningAppProcessInfo processInfo : manager.getRunningAppProcesses()) { 
    if (processInfo.pid == pid) { 
        String currentProcName = processInfo.processName; 
        if (!TextUtils.isEmpty(currentProcName) && currentProcName.equals(":background")) {
           //Rest of the initializations are not needed for the background
           //process
           return; 
        } 
    } 
}

/* Initializations for the UI process */
```

通过这种方式你可以降低内存占用，避免某些大块内存分配，甚至有可能加快应用启动速度。这里提到初始化包括：加载专有字体，初始化Google Maps，及以初始化第三方SDK。通常只需要在主进程中做这些初始化。

# 总结
希望这篇博客能帮助你理解多进程应用，知道使用多进程中会遇到的问题，以及如何去解决这些问题。

# 注意
+ 如果不必在应用外访问provider和service，就不要暴露它们
+ 限制广播的只对自己的应用可见