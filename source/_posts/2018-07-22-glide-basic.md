---
title: Glide基础
tags: Android
toc: true
date: 2018-07-22 21:57:50
---

截至[Glide 4.0版本](https://github.com/bumptech/glide/tree/v4.0.0/library)，其代码量已经相当多了。所以学习Glide源码前很有必要了解Glide中几个关键概念，并从整体上理解这些关键概念之间的关系，否则很容易迷失在代码的实现细节中。
<!--more-->
# Glide简介
Glide涉及到的知识点很多，包括Bitmap重用，Bitmap缓存，生命周期管理，数据加载，资源解码，图片变换，线程调度，等等。另外Glide充分解耦，大量使用接口、泛型以及各种设计模式，并支持外部HttpClient和模块配置。所以学习Glide源码前很有必要了解Glide中几个关键概念，并从整体上理解这些关键概念之间的关系，否则很容易迷失在代码的实现细节中。

注意，下文中的分析均基于[Glide 4.0版本](https://github.com/bumptech/glide/tree/v4.0.0/library)。

下面分别从代码视角和包结构视角看看Glide的关键概念以及它们之间的关系。

## 代码视角
一个典型的Glide调用如下：

```java
Glide.with(context).asBitmap().apply().load(model).into(target)
```

看似简单的调用实际上包含相当丰富的信息量。

简单来说，Glide加载(load)模型(Model)，将返回的数据解码(decode)成资源(Resource)，并将资源设置到Target中。

|方法                |功能                                               |对应的接口和类|
|---------------|------------------------------------|----------------|
| with(context) |生命周期管理                                 |Lifecycle           |
|asBitmap()      |数据解码                                        |Resource, ResourceDecoder|
|apply()            |缓存等等                                        |TODO              |
|load(model)   |数据加载                                        |ModelLoader, ModelLoaderFactory, DataFetcher|
|into(target)    |资源的显示                                     |Target              |


Resouce, Data, Source的区分

+ Resouce (Data) - 已解码的资源(已解码，采样，或转换)
+ (Source) Data - 原始的数据(未解码), ResourceDecoder.decode(data)得到Resource
+ Source - 原始的数据源，ModelLoad.load(model)得到(Source) Data

如何理解Encode操作？ TODO, load, fetch, cache, decode, transcode都好理解

## 包结构视角
Glide作为图片加载库，其工作分为核心部分和其他部分。Glide清晰的包结构体现了这种划分：

**核心工作**

![](rel1.png)

engine负责load指定的model, 并将得到的data解码成resource。

![](rel2.png)

对resource进行某种transition操作后将其显示到target。

**其他工作**

![](rel2.png)

+ manager - 负责各种管理工作，包括生命周期，网络连接以及网络请求
+ module和provider - 为Glide的模块配置提供支持

# 生命周期管理
Glide使用LifecycleListener监听Fragment和Activity的生命周期。`LifecycleListener`接口定义如下：

```java
public interface LifecycleListener {
	void onStart();
	void onStop();
	void onDestroy();	
}
```

详见[Glide生命周期管理](../glide-lifecycle)。

# Target
Target可以理解为Resource的最终载体，如ImageView。`Target`接口定义如下：

```java
public interface Target<R> extends LifecycleListener {
	void onLoadStarted(@Nullable Drawable placeholder);
	void onLoadFailed(@Nullable Drawable errorDrawable);
	void onResourceReady(R resource, Transition<? super R> transition);
	void onLoadCleared(@Nullable Drawable placeholder);
	void getSize(SizeReadyCallback cb);
	void removeCallback(SizeReadyCallback cb);
	void setRequest(@Nullable Request request);
	Request getRequest();
}
```

load resource过程中Target会经历对应的生命周期事件，包括：

+ onLoadStarted()
+ onResourceReady()
+ onLoadCleared()
+ onLoadFailed()

典型的生命周期过程如下：

onLoadStarted -> onResourceReady -> onLoadCleared
onLoadStarted -> onLoadFailed -> onLoadCleared

注意：不保证一定执行某些方法。比如，如果可以在memory中找到resource，或者mode对象为null，都会导致`onLoadStarted()`不被执行。

Target接口的实现类非常多：

![](target.png)

这些类的层级关系是这样的：

![](target-arch.png)

FutureTarget有点特别，其行为跟`Future`类似。(注意FutureTarget.get()方法必须在工作线程中调用！)

```java
FutureTarget<Bitmap> futureTarget =
    Glide.with(fragment)
      .load("http://goo.gl/1asf12")
      .asBitmap()
      .into(250, 250);
Bitmap myBitmap = futureTarget.get();
 ... // do things with bitmap and then release when finished:
futureTarget.cancel(false);
```

RequestFutureTarget是FutureTarget的实现类。可以使用RequestFutureTarget实现下载图片文件的功能。

```java
 FutureTarget<File> target = null;
 RequestManager requestManager = Glide.with(context);
 try {
 	target = requestManager
		.downloadOnly()
		.load(model)
		.submit();
	File downloadedFile = target.get();
	// ... do something with the file (usually throws IOException)
} catch (ExecutionException | InterruptedException | IOException e) {
	// ... bug reporting or recovery
} finally {
	// make sure to cancel pending operations and free resources
	if (target != null) {
	target.cancel(true); // mayInterruptIfRunning
}
```

# Resource和ResourceDecoder
Resource对某些类型的数据进行包装，以便池化和重用。`Resource`接口定义如下：

```java
public interface Resource<Z> {
  Class<Z> getResourceClass();
  Z get();
  int getSize();	
  void recycle();
}
```

具体来说，Resource包括以下类型：

+ File
+ Bitmap
+ Drawable
+ BitmapDrawable
+ GifDrawable
+ Bytes

(你可能会注意到RequestManager中有对应的asFile(), asBitmap(), asGif(), asDrawable()等方法)

以下类实现了Resource接口：

![](resource.png)

这些类的层级关系是这样的：

![](resource-arch.png)

ResourceDecoder

以下类实现了ResourceDecoder接口：

![](resource-decoder.png)

+ VideoBitmapDecoder - 从一个包含视频文件的ParcelFileDescriptor中解码出一帧Bitmap
+ StreamGifDecoder  - 一个不太高效的GifDrawable解码器
+ StreamBitmapDecoder - 从InputStream中解码出Bitmap
+ GifFrameResourceDecoder - 从一个表示Gif图片的GifDecoder解码出一帧
+ FileDecoder - 从文件创建FileResource
+ TODO

# Model和ModelLoader
Model可以理解为待加载的数据。比如网络图片的url地址或者Android工程的图片id，都是Model。

Glide没有将Model封装成新的类，而是直接使用已存在的数据类型，包括：

+ String
+ Uri
+ File
+ int (resourceId)
+ URL
+ byte[]

`RequestBuilder`对每一种Model类型都有一个`loadXXX()`方法。

ModelLoader用于加载Model。ModelLoader可以将任意复杂的数据模型转换成具体的数据类型，以便DataFetcher可以获取为Resource获取代表Model的数据。`ModelLoader`接口定义如下：

```java
public interface ModelLoader<Model, Data> {
  LoadData<Data> buildLoadData(Model model, int width, int height, Options options);
  boolean handles(Model model);
}
```

+ ModelLoader中有一个静态类LoadData
+ buildLoadData() - 根据指定的Model, width, height, options等条件返回一个能够对数据进行加载的LoadData
+ handles() - 判断是否能够加载指定model类型的数据

ModelLoader接口有两个作用： 

1. 用于将特定的Model转换成可解码为Resource的数据
2. 允许Model根据View的大小获取特定尺寸的Resource
 
ModelLoader是抽象的，具体实现的子类中都会实现自己的ModelLoaderFactory。

```java
public class HttpGlideUrlLoader implements ModelLoader<GlideUrl, InputStream> {
    public LoadData<InputStream> buildLoadData(GlideUrl model, int width, int height, Options options) {
    }
	
    public boolean handles(GlideUrl model) {
        return true;
    }
	
    public static class Factory implements ModelLoaderFactory<GlideUrl, InputStream> {
    }
}
```

# DataFetcher
ModelLoader并不是直接加载资源，而是每次加载资源时都会创建一个新的DataFetcher，由DataFetcher延迟加载数据。

`DataFetcher`接口定义如下。

```java
public interface DataFetcher<T> {
    void loadData(Priority var1, DataFetcher.DataCallback<? super T> var2);

    void cleanup();

    void cancel();

    Class<T> getDataClass();

    DataSource getDataSource();

    public interface DataCallback<T> {
        void onDataReady(@Nullable T var1);

        void onLoadFailed(Exception var1);
    }
}
```

# 配置

见[Glide的Registry和GlideModule](glide-registry)

# 参考

[Glide源码分析（二）——磁盘缓存实现 - CSDN博客](https://blog.csdn.net/yxz329130952/article/details/65447622)


<!-- http://km.oa.com/group/31047/articles/show/315884?kmref=search&from_page=1&no=1&loginParam=disposed&length=35&lengh=35&sessionKey=A64C691AE6E6D0676C7DBF8B30A7341903A66732363194626679AF92377B0D30 -->

<!--
Modelloader和DataFetcher两者结合起来构成了Glide的数据加载核心。当缓存中给定的数据不存在的时候，Glide就会通过指定的Modelloader和DataFetcher进行数据加载，这些数据可能来自文件、网络、byte数组等地方-->



