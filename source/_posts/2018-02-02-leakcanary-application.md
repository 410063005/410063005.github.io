---
title: LeakCanary实战
tags:
  - Android
date: 2018-02-02 15:56:46
toc: true
---

LeakCanary的傻瓜式配置在实际项目中其实是不够用的，本文讨论了如何对LeakCanary进行自定义配置以便更好应用。
<!--more-->

如何使用LeakCanary呢？按照[官网](https://github.com/square/leakcanary)说明简单配置即可。

在`build.gradle`中添加如下依赖：

```xml
dependencies {
  debugCompile 'com.squareup.leakcanary:leakcanary-android:1.5.4'
  releaseCompile 'com.squareup.leakcanary:leakcanary-android-no-op:1.5.4'
}
```

在`Application`类中添加代码：


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

配置好之后我们就可以检测内存泄漏了。非常好用，完全傻瓜式的。很快就找到了不少内存泄漏，得意地向小伙伴秀一下。

可以就此打住吗？可以啊，为什么不可以。(之前就是停留在这个层面，于是被问到LeakCanary原理时一脸懵逼)

有兴趣往下看的话，先问自己几个问题：

+ 为什么LeakCanary总是报Activity泄漏，Fragment不会泄漏吗? 其他对象不会泄漏吗？
+ LeakCanary工作时很耗资源容易导致应用卡顿，即使debug包也我们倾向于关闭它(也就是根本不使用)，有优化方案吗？
+ LeakCanary检查结果可以上报到后台吗？实际中往往有比内存泄漏优先级更高的任务要处理，我们最好能保存结果供后续分析

上面问题的答案是Fragment也会泄漏，所有对象都可能泄漏。Activity和Fragment的生命周期非常明确，所以我们通常是检查它们存在内存泄漏。而其他对象，只要有明确的生命周期，也是可以检查是否有内存泄漏的。

具体如何做呢？对LeakCanary进行自定义配置即可。[官网](https://github.com/square/leakcanary/wiki/Customizing-LeakCanary)有提供方案，接下来我们尝试将这些方式整合到实际项目中。

注：本文关注LeakCanary的应用，所以略过了LeakCanary的很多细节。更多细节请参考[LeakCanary wiki](https://github.com/square/leakcanary/wiki/)

注：本文中提供的方案参考整理自[LeakCanary wiki](https://github.com/square/leakcanary/wiki/)，我的工作是如何将这些方案整合并应用于实际工程项目。

# RefWatcher和HeapDumper
要实现LeakCanary开关功能，先要看看[RefWatcher](https://github.com/square/leakcanary/blob/master/leakcanary-watcher/src/main/java/com/squareup/leakcanary/RefWatcher.java)和[HeapDumper](https://github.com/square/leakcanary/blob/master/leakcanary-watcher/src/main/java/com/squareup/leakcanary/HeapDumper.java)。

[RefWatcher](https://github.com/square/leakcanary/blob/master/leakcanary-watcher/src/main/java/com/squareup/leakcanary/RefWatcher.java)是LeakCanary的核心类。见名知义，它用于监视对象引用，当它发现某个弱引用指向的对象应当是只能由弱引用可达而事实并非如此，将触发[HeapDumper](https://github.com/square/leakcanary/blob/master/leakcanary-watcher/src/main/java/com/squareup/leakcanary/HeapDumper.java)开始工作。 (When the link RefWatcher detects that a reference might not be weakly reachable when it should, it triggers the HeapDumper 原文是这样的，我翻译得很绕口。简单点说就是某个对象应该是弱引用的，但由于某个强引用导致它不能回收，就出现我们所说的内存泄漏啦)

```java
public interface HeapDumper {
    HeapDumper NONE = new HeapDumper() {
        public File dumpHeap() {
            return RETRY_LATER;
        }
    };
    File RETRY_LATER = null;

    File dumpHeap();
}
```

`HeapDumper`接口仅有`dumpHeap()`方法，我们实现自己的 **可开关** HeapDumper 如下：

```java
public class TogglableHeapDumper implements HeapDumper {
    private final HeapDumper defaultDumper;
    private boolean enabled = false;

    public TogglableHeapDumper(HeapDumper defaultDumper) {
        this.defaultDumper = defaultDumper;
    }

    public void toggle() {
        enabled = !enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override public File dumpHeap() {
        return enabled? defaultDumper.dumpHeap() : HeapDumper.RETRY_LATER;
    }
}
```

然后自定义`RefWatcher`

```java
    RefWatcher installLeakCanary(Context application) {
        LeakDirectoryProvider leakDirectoryProvider = new DefaultLeakDirectoryProvider(application);
        AndroidHeapDumper defaultDumper = new AndroidHeapDumper(application, leakDirectoryProvider);
        heapDumper = new TogglableHeapDumper(defaultDumper);

        // Build a customized RefWatcher
        return LeakCanary.refWatcher(application)
                .heapDumper(heapDumper)
                .buildAndInstall();
    }
```

App中添加开关入口控制`TogglableHeapDumper`的开启状态即可。

## buildType问题
Gradle打包时分不同的buildType，缺省包括`debug`和`release`。

实际工程中可能会遇到`release`包找不到LeakCanary中某些类而无法编译的问题。这是正常的，因为`release`包依赖的是`leakcanary-android-no-op`，`leakcanary-android-no-op`中只有`RefWatcher`和`LeakCanary`两个空类，并不包含那些可以用于自定义`RefWatcher`和`HeapDumper`的辅助类，所以我们需要通过gradle的build type特性解决这个问题。可以参考[官网](https://github.com/square/leakcanary/wiki/Customizing-LeakCanary#using-the-no-op-dependency)。本文提供基于同一思路采用如下方式实现。

注意到`build.gradle`中有这样几行注释：

```
android {
    sourceSets {
        // Move the build types to build-types/<type>
        // For instance, build-types/debug/java, build-types/debug/AndroidManifest.xml, ...
        // This moves them out of them default location under src/<type>/... which would
        // conflict with src/ being used by the main source set.
        // Adding new build types or product flavors should be accompanied
        // by a similar customization.
        debug.setRoot('build-types/debug')
        release.setRoot('build-types/release')
    }
}
```

所以我们为`debug`和`release`分别封装不同的`LeakCanaryWrapper`即可。

![](/images/1517477051927.webp)

debug版本的`LeakCanaryWrapper`

```java
public class LeakCanaryWrapper {

    public static RefWatcher installLeakCanary(Application application) {
        // Build a customized RefWatcher
        RefWatcher refWatcher = LeakCanary.refWatcher(application)
                .watchDelay(10, TimeUnit.SECONDS)
                .buildAndInstall();
        return refWatcher;

    }
}
```

release版本的`LeakCanaryWrapper`

```java
public class LeakCanaryWrapper {

    public static RefWatcher installLeakCanary(Application application) {
        return RefWatcher.DISABLED;
    }
}
```

上述代码参考自[官网](https://github.com/square/leakcanary/wiki/Customizing-LeakCanary)

# Fragment内存泄漏
Fragment同样也会内存泄漏。为什么LeakCanary可以自动检测Activity的内存泄漏却没有自动检查Fragment内存泄漏？部分原因在于，Fragment跟Activity关联，如果Fragment出现泄漏，最终一定会导致Activity泄漏。所以某种程度上，我们只需要检测Activity的泄漏即可。

但如果想自动检查Fragment内存泄漏，该怎么做？我们先来看看LeakCanary是如何实现自动检测Activity的内存泄漏的。关键在于[`ActivityRefWatcher.watchActivities()`](https://github.com/square/leakcanary/blob/master/leakcanary-android/src/main/java/com/squareup/leakcanary/ActivityRefWatcher.java#L80)

```java
  private final Application.ActivityLifecycleCallbacks lifecycleCallbacks =
      new Application.ActivityLifecycleCallbacks() {
        
		// 这里代码有省略
        
	    @Override public void onActivityDestroyed(Activity activity) {
          ActivityRefWatcher.this.onActivityDestroyed(activity);
        }
      };

  public void watchActivities() {
    // Make sure you don't get installed twice.
    stopWatchingActivities();
    application.registerActivityLifecycleCallbacks(lifecycleCallbacks);
  }
```

`watchActivities()`方法给`application`对象注册了一个用于监听Activity生命的回调接口，可以很方便地监听每一个Activity对象的`onDestroyed()`回调。这里有一个基本前提就是`onDestroyed()`回调后的Activity应该是可以GC回收掉的，如果没有，那一定是这个Activity发生泄漏了。

有没有类似`Application.registerActivityLifecycleCallbacks()`的方法可以用来实现自动检测Fragment的内存泄漏呢？答案是有的。

[Support Library 25.2.0](https://developer.android.com/topic/libraries/support-library/rev-archive.html#25-2-0)中将[FragmentManager.FragmentLifecycleCallbacks](https://developer.android.com/reference/android/support/v4/app/FragmentManager.FragmentLifecycleCallbacks.html)修改成static类。从25.2.0版本开始，开发者很方便就能使用`FragmentLifecycleCallbacks`。它的用法如下：

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final RefWatcher refWatcher = MyApplication.getRefWatcher(this);
    // initialization code
    getSupportFragmentManager()
            .registerFragmentLifecycleCallbacks(new FragmentLifecycleCallbacks() {
                @Override
                public void onFragmentDestroyed(FragmentManager fm, Fragment f) {
                    super.onFragmentDestroyed(fm, f);
                    refWatcher.watch(f);
                }

                @Override
                public void onFragmentViewDestroyed(FragmentManager fm, Fragment f) {
                    super.onFragmentViewDestroyed(fm, f);
                    refWatcher.watch(f.getView());
                }
            }, true);
}
```

如果提示`android.support.v4.app.Fragment is not an enclosing class`，请检查你的SupportLibrary版本是否低于25.2.0。

`Fragment.onDestory()`和`Fragment.onDestoryView()`中分别如何检查内存泄漏可能容易让人混淆。[issue 806](https://github.com/square/leakcanary/issues/806)中提到正确的做法是在`onDestroy()`中检查`Fragment`这个对象是否有泄漏，在`onDestroyView()`中检查`Fragment.getView()`返回的对象是否有泄漏。

由于25.2.0及之后的SupportLibrary库才能方便地调用`FragmentManager.registerFragmentLifecycleCallbacks()`方法，兼容性不好，这应该是LeakCanary官方没有实现`FragmentRefWatcher`的另一个原因。我们如何检查Fragment是否内存泄漏呢？

## 方法一
一种容易想到的方法在[官网](https://github.com/square/leakcanary/wiki/FAQ#how-do-i-use-it)中有提到，即改造`BaseFragment`。这种方法的不足之处在于不能保证App中所有的Fragment都继承自`BaseFragment`。比方说，很可能`DialogFragment`或`BottomSheetDialogFragment`更好用、更适合我的需求，很显然我会继承自它们而不是`BaseFragment`。该怎么办？或许可以使用`BaseFragment`类似的方式定义`BaseDialogFragment`和`BottomSheetDialogFragment`，整体上还是非常繁琐的。

```java
public abstract class BaseFragment extends Fragment {

  @Override public void onDestroy() {
    super.onDestroy();
    RefWatcher refWatcher = ExampleApplication.getRefWatcher(getActivity());
    refWatcher.watch(this);
  }
}
```

## 方法二
修改LeakCanary的源码。[ActivityRefWatcher](https://github.com/square/leakcanary/blob/master/leakcanary-android/src/main/java/com/squareup/leakcanary/ActivityRefWatcher.java#L38)代码修改如下：

```java
  private final Application.ActivityLifecycleCallbacks lifecycleCallbacks =
      new Application.ActivityLifecycleCallbacks() {
        @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
			if (activity instanceof FragmentActivity) {
				FragmentActivity fa = (FragmentActivity) activity;
				fa.getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentLifecycleCallbacks() {
					// 省略
				});
			}
        }

        @Override public void onActivityDestroyed(Activity activity) {
          ActivityRefWatcher.this.onActivityDestroyed(activity);
        }
      };
```

但是这么改的话就让LeakCanary产生兼容性问题，使用25.2.0及以上版本的SupportLibrary的应用才能使用LeakCanary。虽然截止到2018年2月2日最新的SupportLibrary是2017年11月份发布的[27.0.2](https://developer.android.com/topic/libraries/support-library/revisions.html)，25.2.0已经是比较旧的版本了，但LeakCanary作为一个library却对应用的依赖有额外要求显然不合适。

## 方法三
在`BaseActivity`中进行Fragment内存泄漏检查。代码如下：

```java
public abstract class BaseActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    
        final RefWatcher refWatcher = MyApplication.getRefWatcher(this);
        // initialization code
        getSupportFragmentManager()
                .registerFragmentLifecycleCallbacks(new FragmentLifecycleCallbacks() {
                    @Override
                    public void onFragmentDestroyed(FragmentManager fm, Fragment f) {
                        super.onFragmentDestroyed(fm, f);
                        refWatcher.watch(f);
                    }
    
                    @Override
                    public void onFragmentViewDestroyed(FragmentManager fm, Fragment f) {
                        super.onFragmentViewDestroyed(fm, f);
                        refWatcher.watch(f.getView());
                    }
                }, true);
    }
}
```

## 小结
对比后可以看到上述第三种方式对采用了25.0.2及以上版本的SupportLibrary的应用来说是最合理的方案，可以简单有效地检查Fragment内存泄漏问题，不像方案一那么复杂，也不会像方案二那样导致LeakCanary库兼容性降低。所以我们应用中采用这一方案。

# Heap上传
前面提到过有时我们并不会立即去分析LeakCanary的检查结果，原因可能是手头有更重要的任务，也可能是内存泄漏并不是发生在你自己的设备上。所以有必要让实现LeakCanary检查结果上传功能。对此[LeakCanary wiki](https://github.com/square/leakcanary/wiki/Customizing-LeakCanary#uploading-to-a-server)仍有提供解决方案。

我们可以创建自己的`AbstractAnalysisResultService`来调整LeakCanary的缺省行为，将原来的分析heap dump修改成上传leak trace和heap dump到后台服务器。不过没必要从头开始创建`AbstractAnalysisResultService`。建议的做法是继承`DisplayLeakService`：

```java
public class LeakUploadService extends DisplayLeakService {
  @Override protected void afterDefaultHandling(HeapDump heapDump, AnalysisResult result, String leakInfo) {
    if (!result.leakFound || result.excludedLeak) {
      return;
    }
    myServer.uploadLeakBlocking(heapDump.heapDumpFile, leakInfo);
  }
}
```

不要忘记在`AndroidManifest.xml`中配置这个service。如何实现这里的上传service呢？热心网友直接给出了[源码](https://gist.github.com/pyricau/06c2c486d24f5f85f7f0)。不过源码中使用的是[Slack Channel](https://slack.com/)或[HipChat](https://www.atlassian.com/software/hipchat)。这两个看起来更为高大上，但考虑在我们应用的用户主要是在国内，所以还是使用比较接地气的腾讯云对象存储服务(COS)。

粗略地看了下COS的文档，可以选择API的方式接入或SDK方式。使用API更轻量，使用SDK更简单。不过有个问题需要注意：对于大文件(超过20MB)是采用分片上传方式，所以调用的API跟小文件上传有所不同，较为繁琐。又有一个问题需要注意：LeakCanary的heap dump文件有多大呢？保存在哪里？

## HeapDump文件
具体代码见[DefaultLeakDirectoryProvider](https://github.com/square/leakcanary/blob/master/leakcanary-android/src/main/java/com/squareup/leakcanary/DefaultLeakDirectoryProvider.java)

HeapDump文件，即hprof文件，保存的位置是`Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)`目录。具体的策略是：

1. 优先使用`externalStorageDirectory()`返回的目录
2. 其次是`appStorageDirectory()`返回的目录

```java
  private File externalStorageDirectory() {
    File downloadsDirectory = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS);
    return new File(downloadsDirectory, "leakcanary-" + context.getPackageName());
  }

  private File appStorageDirectory() {
    File appFilesDirectory = context.getFilesDir();
    return new File(appFilesDirectory, "leakcanary");
  }
```

<div align="center">
<img src="/images/1517369532736.webp" width="30%" height="30%" ><img src="/images/1517369491050.webp" width="30%" height="30%">
</div>

1. 两张图中的保存位置分别是`Download\leakcanary\detected_leaks`和`Download\leakcanary-<包名>\`
2. 上面第一张图中的hprof大小居然是170MB，暗示该应用可能存在比较严重的内存泄漏

另外，对每一个hprof文件会有一个对应的result文件。hprof文件一般较大，其大小通常就是heap dump操作时应用占用的内存数量。你会发现hprof和result文件数并不多，最多一共14个。这是因为`DefaultLeakDirectoryProvider.DEFAULT_MAX_STORED_HEAP_DUMPS`值限制了hprof文件的数量，默认为7。我们可以根据自己的需求调整这个值。当然考虑到hprof文件可能较大，如果我们把`DEFAULT_MAX_STORED_HEAP_DUMPS`调整到很高，最后可能会占用很多存储空间。

## 上传到COS
好吧，heap dump文件可能非常大，如果要上传COS的话，还是乖乖使用[COS SDK](https://github.com/tencentyun/qcloud-sdk-android/releases)。这里提供一个基于COS Android SDK封装的[`CosClient`](https://gist.github.com/410063005/d0e75c5e5bfe21ea62e11ba3db129c52)。最后的上传service如下：

```java
public class LeakUploadService extends DisplayLeakService {

    @Override protected void afterDefaultHandling(HeapDump heapDump, AnalysisResult result, String leakInfo) {
        if (!result.leakFound || result.excludedLeak) {
            return;
        }

        try {
            CosClient cosClient = CosClient.create(getApplicationContext(), getAssets().open("heap.json"));
             cosClient.upload(heapDump.heapDumpFile, heapDump.heapDumpFile.getName(), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

创建`RefWatcher`时指定`LeakUploadService`即可。

```java
    RefWatcher refWatcher = LeakCanary.refWatcher(this)
      .listenerServiceClass(LeakUploadService.class);
      .buildAndInstall();
```

![](/images/1517556169211.webp)

注：考虑到heap dump文件实在太大，而简单的leak info常常也足够分析出内存泄漏问题，所以实际上我只上传了leak info。

# 总结
最终我对LeakCanary做了自定义配置，比起最简单的傻瓜式配置它有几个额外的好处：

+ 使用方便
 + Debug包中有开关控制LeakCanary的开启，默认是关闭状态。但必要时，我们可以随时打开开关检查内存泄漏问题
 + 内存泄漏问题上传后台，可以合理安排时间进行统一集中的处理
+ 检查更为全面。除了能自动检查Activity内存泄漏，还可以自动检查Fragment内存泄漏

但是，这里仍然有问题没有解决。由于LeakCanary的heap dump以及heap analysis非常耗时耗资源，所以官方要求一定不要用在release包中以免影响性能。这么做的结果是我们在无法对真实用户的设备监测内存泄漏问题。

我的理解是，weak reference可达性检测才是检查内存泄漏最为关键的一步，而这一步对性能的影响可能较小(仅触发一次GC而已)。而heap dump及后续的heap analysis不过是计算 **shortest strong reference path to the GC Roots** ，一来确认前一步weak reference可达性的正确性，二来方便我们快速定位问题。如果不去计算 shortest strong reference path to the GC roots会怎样呢？一是检查的准确性降低，二是定位问题相对不方便，但同时也减少性能风险。基于此，应该可以设计一种内存泄漏检查方案，该方案特点如下：

+ 对性能影响小，可用于relase包检查用户真实设备上的内存泄漏问题
+ 内存泄漏检查得足够准，但不提供详情的reference path辅助定位内存泄漏问题 (全靠人肉分析)

怎么实现，容我再想想。

# 参考
[LeakCanary wiki](https://github.com/square/leakcanary/wiki)

[LeakCanary 中文使用说明](https://www.liaohuqiu.net/cn/posts/leak-canary-read-me/)