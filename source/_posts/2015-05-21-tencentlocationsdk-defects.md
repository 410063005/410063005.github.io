---
layout: post
title: "TencentLocationSDK缺陷"
date: 2015-05-21 10:27:55 +0800
comments: true
categories: work
published: false
---

# Wifi扫描策略

```
@Override
public void onReceive(Context context, Intent intent) {
	// schedule next wifi scan
	scheduleWifiScan();
}
```

"收到上次的Wifi扫描结果后再定时发起下次Wifi扫描"的策略，缺陷在于：某些(系统API有问题的)机型上Wifi扫描工作不正常，偶尔会出现收不到Wifi扫描结果的情况，无法发起新的扫描，导致整个Wifi扫描过程中断！

注：虽然基站切换会从另一条路径驱动Wifi扫描，但不能保证所有机型上基站切换是正常的！

# 基站更新策略
正常情况下，行进几公里后基站肯定会发生切换。但某些(系统API有问题的)机型在移动过程中，设备的基站并不会切换，导致`onCellLocationChanged()`回调工作不正常。

应对策略是使用`getCellLocation`定时主动读取基站。但根据实际测试，这个策略意义不大。缺陷在于：设备的基站不切换，不仅影响`onCellLocationChanged`，同样影响`getCellLocation`。

# 多线程策略
目前的多线程策略不合理，更合理的策略可以参考[android.widget.Filter](https://developer.android.com/reference/android/widget/Filter.html)的[源码](http://grepcode.com/file/repository.grepcode.com/java/ext/com.google.android/android/4.4_r1/android/widget/Filter.java#Filter)
