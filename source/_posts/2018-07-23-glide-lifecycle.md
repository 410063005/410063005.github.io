---
title: Glide生命周期绑定
tags: Android
toc: true
date: 2018-07-23 16:17:37
---

Glide中如何实现生命周期绑定呢？
<!--more-->

# 添加RequestManagerFragment
RequestManagerFragment是实现Glide生命周期的关键。RequestManagerFragment是一个没有界面的Fragment，用于存放RequestManager。RequestManagerFragment可以管理(包括启动和停止)它自身所在的Fragment或Activity中的Target上关联的Glide Request。

当你调用`Glide.with(activity)`时，Glide已经偷偷在这个activity中添加了一个RequestManagerFragment。

注意：Android中系统内置了一种Activity，support库提供了另一种Activity(FragmentActivity)。为了兼容不同的Activity，`Glide.with()`方法有几个重载版本，对前者而言是`Glide.with(android.app.Activity)`，对后者是`Glide.with(android.support.v4.app.FragmentActivity)`。不同的`Glide.with()`方法添加的Fragment有所不同。

|Activity类型|添加的Fragment类型|
|-----------------|----------------|
|android.support.v4.app.FragmentActivity|SupportRequestManagerFragment|
|android.app.Activity                   |RequestManagerFragment|

Glide已经注意到其中的差异，它并不会弄混。我们只要知道其中的差异即可。下文的讨论中关注的重点是Glide"偷偷"添加Fragment的流程，而忽略了Fragment类型的差异。

`Glide.with()`最终会调用到`RequestManagerRetriever.get()`方法。`RequestManagerRetriever.get()`源码如下：

```java
  // RequestManagerRetriever.java
  public RequestManager get(Activity activity) {
	...
    android.app.FragmentManager fm = activity.getFragmentManager();
    return fragmentGet(activity, fm, null /*parentHint*/);
  }

  private RequestManager fragmentGet(Context context, android.app.FragmentManager fm,
      android.app.Fragment parentHint) {
    RequestManagerFragment current = getRequestManagerFragment(fm, parentHint);
    RequestManager requestManager = current.getRequestManager();
    if (requestManager == null) {
      Glide glide = Glide.get(context);
      requestManager =
          factory.build(glide, current.getGlideLifecycle(), current.getRequestManagerTreeNode());
      current.setRequestManager(requestManager);
    }
    return requestManager;
  }

  RequestManagerFragment getRequestManagerFragment(
      final android.app.FragmentManager fm, android.app.Fragment parentHint) {
    RequestManagerFragment current = (RequestManagerFragment) fm.findFragmentByTag(FRAGMENT_TAG);
    if (current == null) {
      current = pendingRequestManagerFragments.get(fm);
      if (current == null) {
        current = new RequestManagerFragment();
        current.setParentFragmentHint(parentHint);
        pendingRequestManagerFragments.put(fm, current);
        fm.beginTransaction().add(current, FRAGMENT_TAG).commitAllowingStateLoss();
        handler.obtainMessage(ID_REMOVE_FRAGMENT_MANAGER, fm).sendToTarget();
      }
    }
    return current;
  }
```

从代码可以看到调用`Glide.with(activity)`时Glide的确添加了Fragment。很容易用以下代码验证是否真的添加了Fragment。

```java
    private static final String FRAGMENT_TAG = "com.bumptech.glide.manager";
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_target);

        Glide.with(this);

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment fragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
                if (fragment instanceof SupportRequestManagerFragment) {
                    System.out.println(fragment);
                }
            }
        });
    }
```

点击Button输出如下：

```
07-15 03:53:37.272 26036-26036/? I/System.out: SupportRequestManagerFragment{ccdb477 #0 com.bumptech.glide.manager}{parent=null}
```

# RequestManagerFragment生命周期

![](glide-lifecycle.png)

+ RequestManagerFragment - 有一个类型为ActivityFragmentLifecycle的lifecycle成员。RequestManagerFragment生命周期事件会通知给lifecycle相应的方法。
+ ActivityFragmentLifecycle - 有一个类型为Set<LifecycleListener>的lifecycleListeners成员。onStart/onStop/onDestroy被调用时会回调lifecycleListeners中的每个listener。
+ RequestManager - 它实现了LifecycleListener接口，有一个类型为TargetTracker的成员。RequestManager的onStart/onStop/onDestroy被调用时会通知TargetTracker。RequestManager自身作为LifecycleListener被添加到ActivityFragmentLifecycle。
+ TargetTracker - 它实现了LifecycleListener接口，用于管理多个Target。TargetTracker的onStart/onStop/onDestory被调用时会通知Target。
+ Target - Target继承自LifecycleListener。

UI上有一个ImageView，调用Glide给这个ImageView加载一张图片，代码如下：

```java
Glide.with(this).load(...).into(imageView);
```

`into()`方法原型是`RequestBuilder.into(ImageView)`。这个方法中，ImageView会被封装成BitmapImageViewTarget。然后调用`RequestBuilder.into(Target)`方法。代码如下：

```java
  public <Y extends Target<TranscodeType>> Y into(@NonNull Y target) {
    Util.assertMainThread();
    Preconditions.checkNotNull(target);
    if (!isModelSet) {
      throw new IllegalArgumentException("You must call #load() before calling #into()");
    }

    Request previous = target.getRequest();

    if (previous != null) {
      requestManager.clear(target);
    }

    requestOptions.lock();
    Request request = buildRequest(target);
    target.setRequest(request);
    requestManager.track(target, request);

    return target;
  }
```

整理出来的流程是这样的：

```
Glide.with() ->
RequestManagerRetriever.get() ->
RequestManager.load() ->
RequestBuilder.into() ->
RequestManager.track() ->
TargetTracker.track() ->
```

通过应用中的一个场景来理解RequestManagerFragment生命周期过程。假设用户按Home键退出应用中某个正在加载图片的页面，有以下流程：

```
RequestManagerFragment.onStop() ->
ActivityFragmentLifecycle.onStop() ->
RequestManager.onStop() ->
TargetTracker.onStop() ->
Target.onStop() ->
```

ActivityFragmentLifecycle如何跟RequestManager关联上的呢？关键在于这段代码：

```java
  private RequestManager fragmentGet(Context context, android.app.FragmentManager fm,
      android.app.Fragment parentHint) {
    RequestManagerFragment current = getRequestManagerFragment(fm, parentHint);
    RequestManager requestManager = current.getRequestManager();
    if (requestManager == null) {
      // TODO(b/27524013): Factor out this Glide.get() call.
      Glide glide = Glide.get(context);
      requestManager =
          factory.build(glide, current.getGlideLifecycle(), current.getRequestManagerTreeNode());
      current.setRequestManager(requestManager);
    }
    return requestManager;
  }

  private static final RequestManagerFactory DEFAULT_FACTORY = new RequestManagerFactory() {
    @Override
    public RequestManager build(Glide glide, Lifecycle lifecycle,
        RequestManagerTreeNode requestManagerTreeNode) {
      return new RequestManager(glide, lifecycle, requestManagerTreeNode);
    }
  };

public class RequestManagerFragment extends Fragment {
  private final ActivityFragmentLifecycle lifecycle;

  public RequestManagerFragment() {
    this(new ActivityFragmentLifecycle());
  }

  RequestManagerFragment(ActivityFragmentLifecycle lifecycle) {
    this.lifecycle = lifecycle;
  }

  ActivityFragmentLifecycle getGlideLifecycle() {
    return lifecycle;
  }
}
```

梳理一下我们不难发现，RequestManager的lifecycle成员跟RequestManagerFragment的lifecycle是同一个对象！

# RequestManagerFragment的数量

1. 有几个RequestManagerFragment? 
2. 有几个RequestManager？
3. 多个RequestManagerFragment之间是什么关系?

```java
public class RequestManagerRetriever implements Handler.Callback {
  final Map<FragmentManager, SupportRequestManagerFragment> pendingSupportRequestManagerFragments =
      new HashMap<>();

  private RequestManager fragmentGet(Context context, android.app.FragmentManager fm,
      android.app.Fragment parentHint) {
    RequestManagerFragment current = getRequestManagerFragment(fm, parentHint);
    RequestManager requestManager = current.getRequestManager();
    if (requestManager == null) {
      // TODO(b/27524013): Factor out this Glide.get() call.
      Glide glide = Glide.get(context);
      requestManager =
          factory.build(glide, current.getGlideLifecycle(), current.getRequestManagerTreeNode());
      current.setRequestManager(requestManager);
    }
    return requestManager;
  }
}
```

先来回答前两个问题。有n个Activity/Fragment使用Glide加载图片的话，则有n个RequestManagerFragment和n个RequestManager。即，每个Activity/Fragment中会被添加一个TAG为"com.bumptech.glide.manager"的RequestManageFragment，RequestManageFragment和RequestManager是一一对应关系。

第3个问题稍麻烦一点。考虑这样一种情况，一个Activity中有两个Fragment，它们都在使用Glide加载图片。一共有3个RequestManagerFragment，它们的关系如下：

![](rmf.png)

```java
public class RequestManagerFragment extends Fragment {
  private final HashSet<RequestManagerFragment> childRequestManagerFragments =
      new HashSet<>();

  @Nullable private RequestManagerFragment rootRequestManagerFragment;
}
```
