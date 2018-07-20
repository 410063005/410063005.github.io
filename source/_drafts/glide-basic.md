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

with(context) - 生命周期
asBitmap() - 解码数据为Resource
apply(requestOptions) - 缓存策略，...
load(model) - 加载模型
into(target) - 给Target设置Resource

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

# Model

RequestBuilder.loadXXX()方法

+ String
+ Uri
+ File
+ int (resourceId)
+ URL
+ byte[]


# 参考
<!-- http://km.oa.com/group/31047/articles/show/315884?kmref=search&from_page=1&no=1&loginParam=disposed&length=35&lengh=35&sessionKey=A64C691AE6E6D0676C7DBF8B30A7341903A66732363194626679AF92377B0D30 -->