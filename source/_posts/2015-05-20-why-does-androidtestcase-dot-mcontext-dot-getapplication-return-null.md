---
layout: post
title: "AndroidTestCase中getApplicationContext()返回null"
date: 2015-05-20 14:33:09 +0800
comments: true
categories: android
---
Android项目单元测试时发现下面代码会打印`null`。

```
public class TmpTest extends AndroidTestCase {

	public void testGetApp() {
		System.out.println(mContext.getApplicationContext());
	}
}
```

找到[这篇帖子][source]，提出了如下非常有效的解决方案

```
public class TmpTest extends AndroidTestCase {

	@Override
	public void setContext(Context context) {
		super.setContext(context);
		
		while (null == context.getApplicationContext()) {
            		SystemClock.sleep(16);
      		}
	}
}
```

问题来了：

1. 为什么会返回`null`呢，是否Android的bug？
2. 为什么这个解决方案是有效的呢？它是什么原理？

Android扩展了标准的JUnit测试框架，`AndroidTestCase`是利用`Instrumentation`来驱动Android相关的测试用例的。使用`Instrumentation`，可以在主程序启动之前，创建模拟的系统对象，如`Context`；控制应用组件的生命周期；发送UI事件给应用程序；在执行期间检查程序状态。

但是，`Instrumentation`在独立的线程中运行而非主线程，所以它可以在不阻塞或干扰主线程(反之也不会被主线程阻塞)的情况下执行。如果需要跟主线程同步，可以使用[Instrumentation.waitForIdleSync()](http://developer.android.com/reference/android/app/Instrumentation.html#waitForIdleSync%28%29)

而像`Application`或`Activity`等上层应用组件对象是由主线程初始化的。主线程初始化这些对象的同时，`Instrumentation`所在的线程也在运行。如果在`Instrumentation`线程中使用这些对象，而又没有线程安全地实现，肯定会出错。很不幸，`AndroidTestCase`的实现不是线程安全的(API 16 Jelly Bean之前)，可以认为是系统的bug。一种实现线程安全的办法是使用[Instrumentation.runOnMainSync(java.lang.Runnable)](http://developer.android.com/reference/android/app/Instrumentation.html#runOnMainSync%28java.lang.Runnable%29)在主线程中运行相关代码。

当然，在`AndroidTestCase中`不能拿到`Instrumentation`实例，所以无论`Instrumentation.waitForIdleSync()`还是`Instrumentation.runOnMainSync()`都无法被调用。

总结一下，答案就是：

1. 该问题是Android的bug，问题根源在于`AndroidTestCase`不是线程安全的
2. 这里的方案修复了`Instrumentation`线程和主线程的同步问题，可以保证线程安全

[source]: http://stackoverflow.com/questions/6516441/why-does-androidtestcase-getcontext-getapplicationcontext-return-null
