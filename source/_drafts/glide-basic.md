---
title: glide-basic
tags: android
toc: true
---

学习Glide源码前需要了解几个基本概念

+ Lifecycle
+ Target
+ Resource
+ Model

# LifecycleListener
Glide中使用LifecycleListener监听Fragment和Activity的生命周期。

```java
public interface LifecycleListener {
	void onStart();
	void onStop();
	void onDestroy();	
}
```

# Target

简单来说，Glide加载(load)模型(Model)，将返回的数据解码(decode)成资源(Resource)，最后将资源设置到Target中。

```java
Glide.with(context).asBitmap().apply().load(model).into(target)
```

一个看似简单的调用，包含了相当丰富的信息量：

+ with(context) - 生命周期
+ asBitmap() - 解码数据为Resource
+ apply(requestOptions) - 缓存策略，...
+ load(model) - 加载模型
+ into(target) - 给Target设置Resource

什么是Target呢？Target可以理解为Resource的最终载体，比如ImageView就是一个Target。Glide中Target是一个接口。

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

load resource过程中Target会经历对应的生命周期事件。生命周期事件包括：

+ onLoadStarted()
+ onResourceReady()
+ onLoadCleared()
+ onLoadFailed()

典型的生命周期过程如下：

onLoadStarted -> onResourceReady -> onLoadCleared
onLoadStarted -> onLoadFailed -> onLoadCleared

注意，但并不保证一定执行某些方法。比如，如果可以在memory中找到resource，或者mode对象为null，都会导致`onLoadStarted()`不被执行。

以下类实现了Target接口：

![](target.png)

这些类的层级关系是这样的：

![](target-arch.png)

有一个比较特别的Target是FutureTarget，它有一个实现类RequestFutureTarget。

FutureTarget的行为有点像Future。(注意FutureTarget.get()方法必须在工作线程中调用！)

```java
FutureTarget<Bitmap> futureTarget = Glide.with(fragment)
                                      .load("http://goo.gl/1asf12")
                                      .asBitmap()
                                      .into(250, 250);
Bitmap myBitmap = futureTarget.get();
 ... // do things with bitmap and then release when finished:
futureTarget.cancel(false);
```

可以使用RequestFutureTarget实现下载图片文件的功能。

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

# Resource
Resource对某些类型的数据进行包装，以便池化和重用。在Glide中Resource是一个接口。

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

(注意观察，RequestManager中有对应的asFile(), asBitmap(), asGif(), asDrawable()等方法)

以下类实现了Resource接口：

![](resource.png)

这些类的层级关系是这样的：

![](resource-arch.png)

# Model和ModelLoader

Model可以理解为待加载的数据。比如网络图片的url地址或者Android工程的图片id，都抽象成Model。由于Model类型简单，所以Glide没有将其封装成新的类，而是直接使用已有的数据类型，包括：

+ String
+ Uri
+ File
+ int (resourceId)
+ URL
+ byte[]

对应每一种类型都有一个`RequestBuilder.loadXXX()`方法

Model很简单，但对应的ModelLoader可不简单。在Glide中ModleLoader是一个接口

/**
 * A factory interface for translating an arbitrarily complex data model into a concrete data type
 * that can be used by an {@link DataFetcher} to obtain the data for a resource represented by the
 * model.
 *
 * <p> This interface has two objectives: 1. To translate a specific model into a data type that can
 * be decoded into a resource.
 *
 * 2. To allow a model to be combined with the dimensions of the view to fetch a resource of a
 * specific size.
 *
 * This not only avoids having to duplicate dimensions in xml and in your code in order to determine
 * the size of a view on devices with different densities, but also allows you to use layout weights
 * or otherwise programmatically put the dimensions of the view without forcing you to fetch a
 * generic resource size.
 *
 * The smaller the resource you fetch, the less bandwidth and battery life you use, and the lower
 * your memory footprint per resource. </p>
 
 ModleLoader用于将任意复杂的数据模型转换成具体的数据类型，以便DataFetcher可以获取为Resource获取代表Model的数据。该接口有两个作用： 1. 用于将特定的Model转换成可解码为Resource的数据， 2. 允许Model根据View的大小获取特定尺寸的Resource。
 
这么做有两个好处。一是可以不必强制让你获取通用大小的Resource，二是获取的资源越小，消耗的带宽、电量越小，而且占用的内存也小。

```java
public interface ModelLoader<Model, Data> {
  LoadData<Data> buildLoadData(Model model, int width, int height, Options options);
  boolean handles(Model model);
}
```


# 参考
<!-- http://km.oa.com/group/31047/articles/show/315884?kmref=search&from_page=1&no=1&loginParam=disposed&length=35&lengh=35&sessionKey=A64C691AE6E6D0676C7DBF8B30A7341903A66732363194626679AF92377B0D30 -->
