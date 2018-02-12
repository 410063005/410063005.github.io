---
title: leakcanary-usage
tags:
 - Android
---

发现应用进入后台一段时间后，内存占用仍然在50MB左右。

TODO

凭经验，一定是有内存泄漏。Android Studio中Dump Java Heap，发现果然有3个Activity存在没被释放。

TODO

但Dump Java Heap并不能告诉我为什么这些Activity没被释放。简单的办法是求助于[square/leakcanary: A memory leak detection library for Android and Java.](https://github.com/square/leakcanary)。

# 配置和用法
LeakCanary的配置非常简单。`build.gradle`中添加以下依赖：

```
dependencies {
  debugCompile 'com.squareup.leakcanary:leakcanary-android:1.5.4'
  releaseCompile 'com.squareup.leakcanary:leakcanary-android-no-op:1.5.4'
}
```

在自己的`Application`中添加代码：

```java
public class ExampleApplication extends Application {

  @Override public void onCreate() {
    super.onCreate();
    if (LeakCanary.isInAnalyzerProcess(this)) {
      // This process is dedicated to LeakCanary for heap analysis.
      // You should not init your app in this process.
      return;
    }
    LeakCanary.install(this);
    // Normal app init code...
  }
}
```

我们的应用是多进程的，主要关注主进程的内存泄漏问题，所以跟上面代码稍稍有所不同：

```java
public class XXXApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }

        final boolean mainProcess = MsfSdkUtils.isMainProcess(this);
        if (mainProcess) {
            LeakCanary.install(this);
			// Normal app init code...
		}
	}
}
```


![](/images/1514537624693.png)



