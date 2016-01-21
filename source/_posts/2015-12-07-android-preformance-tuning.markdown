---
layout: post
title: "Android性能调优"
date: 2015-12-07 17:26:06 +0800
comments: true
categories: android
published: true
---
尝试对正在开发的Android App进行性能调优，优化过程总结记录如下。
<!--more-->
# 如何衡量性能
首先遇到的是问题如何衡量性能。一开始我们只选取一个最简单的指标，应用及Activity页面(后面简称为页面)的启动速度。

到目前为止，我并不知道有什么工具可以测试页面启动速度。Android中Activity的启动时间主要是耗费在`onCreate()`, `onStart()`和`onResume()`中的时间。所以以想到的一种测试办法如下：

```
public class MyActivity extends Activity {
    
	private long now;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        now = SystemClock.elapsedRealtime();
		super.onCreate(savedInstanceState);
		...
	}
		
	@Override
	protected void onResume() {
		super.onResume();
		
		...
		long cost = SystemClock.elapsedRealtime() - now;
		Log.i("MyActivity", "Start speed: " + cost + "ms");
	}
}
```

通过输出日志，我们可以大致看到从`onCreate()`开始到`onResume()`结束所花费的时间。

```
12-07 17:46:29.833    8675-8675/com.tencent.PmdCampus I/MyActivity Start speed: 107ms
```

对上面过程加以改进，我们可以更容易细致地区分`onCreate()`和`onResume()`各自花费的时间(注：统计`onCreate()`和`onResume()`各自花费的时间跟统计从`onCreate()`开始到`onResume()`结束所花费的时间有些差异，并不能简单地认为这两个时间相等)。

```
public class MyActivity extends Activity {
    
	private long now;
	private TimingLogger logger = new TimingLogger("perf", "start");
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        now = SystemClock.elapsedRealtime();
		
		super.onCreate(savedInstanceState);
		...
		
		logger.addSplit("detail.onCreate");
	}
		
	@Override
	protected void onResume() {
		super.onResume();
		
		...
		
		long cost = SystemClock.elapsedRealtime() - now;
		Log.i("perf", "Start speed: " + cost + "ms");
		
		logger.addSplit("detail.onResume");
        logger.dumpToLog();
	}
}
```

输出结果如下

```
12-07 18:35:49.196  18689-18689/com.tencent.PmdCampus I/perf﹕ Start speed: 113ms
12-07 18:35:49.196  18689-18689/com.tencent.PmdCampus D/perf﹕ start: begin
12-07 18:35:49.196  18689-18689/com.tencent.PmdCampus D/perf﹕ start:      108 ms, detail.onCreate
12-07 18:35:49.196  18689-18689/com.tencent.PmdCampus D/perf﹕ start:      5 ms, detail.onResume
12-07 18:35:49.196  18689-18689/com.tencent.PmdCampus D/perf﹕ start: end, 113 ms
```

根据官方文档[]`onResume`应当是非常轻量级的][[on-resume]]

> An activity can frequently go between the resumed and paused states -- for example when the device goes to sleep, when an activity result is delivered, when a new intent is delivered -- so the code in these methods should be fairly lightweight.

上面的日志显示`onCreate`花费了108ms，而`onResume`花费了5ms，所以`onResume`的实现还算合理。

[这里][launch-time]提到了另一种方式。我们我们在adb log中看到类似这样的一些日志：

```
12-07 18:35:49.368      521-535/system_process I/ActivityManager﹕ Displayed XXXActivity: +293ms
```

这些日志是`ActivityManager`输出的，也可以作为页面启动时间。

# 可优化的项
## Log
去掉不必要的Log!


[launch-time]: http://stackoverflow.com/questions/2324847/launch-time-of-an-app
[on-resume]: https://developer.android.com/intl/zh-cn/reference/android/app/Activity.html#onResume