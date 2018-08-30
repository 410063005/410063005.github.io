---
title: 两个小的技术问题
tags:
 - Kotlin
 - Android
toc: true
date: 2018-06-04 20:14:13
---
最近一次技术分享中遗留的两个疑问。分享时并不完全了解，下来后简单整理了一下，记录之。
<!--more-->

# Java中调用Kotlin的扩展方法

Kotlin中分别给Int和String添加如下扩展函数`cm()`。完整代码见[这里](https://github.com/410063005/FragmentLifeCircle/blob/master/demo-arch/src/test/java/com/example/cm/demo_arch/ExtensionDemoTest.java)

```kotlin
// ExtensionDemo.kt
fun Int.cm() {
    println("$this")
}

fun String.toInt(): Int {
    return try {
        Integer.parseInt(this)
    } catch (e: Exception) {
        0
    }
}
```

如何在Java代码中调用？形式是否能像Kotlin中那样简洁？

**答案是否**。Java代码调用方式如下：

```java
public void test() {
    ExtensionDemoKt.cm(1);
    ExtensionDemoKt.toInt("123");
}
```

看看Kotlin生成的Java代码就容易明白为什么要这样调用。

```java
@Metadata(
   mv = {1, 1, 7},
   bv = {1, 0, 2},
   k = 2,
   d1 = {"\u0000\u0012\n\u0000\n\u0002\u0010\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n
   \u0000\u001a\n\u0010\u0000\u001a\u00020\u0001*\u00020\u0002\u001a\n\u0010\u0003\u001a\u00020
   \u0002*\u00020\u0004¨\u0006\u0005"},
   d2 = {"cm", "", "", "toInt", "", "test sources for module demo-arch"}
)
public final class ExtensionDemoKt {
   public static final void cm(int $receiver) {
      String var1 = "" + $receiver;
      System.out.println(var1);
   }

   public static final int toInt(@NotNull String $receiver) {
      Intrinsics.checkParameterIsNotNull($receiver, "$receiver");

      int var1;
      try {
         var1 = Integer.parseInt($receiver);
      } catch (Exception var3) {
         var1 = 0;
      }

      return var1;
   }
}
```

总结：Kotlin的扩展函数本质上是生成以被扩展类的对象作为"receiver"参数的static方法。所以，在Java中调用Kotlin扩展函数就只能以调用普通static方法的方式进行。

# Lifecycle的顺序问题

有一个LifecycleObserver，它的`test5()`响应`Lifecycle.Event.ON_CREATE`事件。在Activity.onCreate()期间执行addObserver()添加这个LifecycleObserver，`test5()`会被正确执行吗(能否输出"test5: ON_CREATE"吗) 完整代码见[这里](https://github.com/410063005/FragmentLifeCircle/blob/master/demo-arch/src/main/java/com/example/cm/demo_arch/lifecycle/MyLifecycleObserver.java)

```java
public class LifecycleOwnerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLifecycle().addObserver(new MyLifecycleObserver());
    }
}

public class MyLifecycleObserver implements LifecycleObserver {

    private static final String TAG = "MyLifecycleObserver";

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    public void test5() {
        Log.d(TAG, "test5: ON_CREATE");
    }
}
```

答案是 **可以正确输出**。由于`getLifecycle().addObserver()`最终会执行到`android.arch.lifecycle.LifecycleRegistry.addObserver()`方法，所以对`LifecycleRegistry.addObserver()`源码进行分析。


```java
// android.arch.lifecycle.LifecycleRegistry.java
@Override
public void addObserver(@NonNull LifecycleObserver observer) {
    State initialState = mState == DESTROYED ? DESTROYED : INITIALIZED;
    ObserverWithState statefulObserver = new ObserverWithState(observer, initialState);
    ObserverWithState previous = mObserverMap.putIfAbsent(observer, statefulObserver);

    if (previous != null) {
        return;
    }
    LifecycleOwner lifecycleOwner = mLifecycleOwner.get();
    if (lifecycleOwner == null) {
        // it is null we should be destroyed. Fallback quickly
        return;
    }

    boolean isReentrance = mAddingObserverCounter != 0 || mHandlingEvent;
    State targetState = calculateTargetState(observer);
    mAddingObserverCounter++;
    while ((statefulObserver.mState.compareTo(targetState) < 0
            && mObserverMap.contains(observer))) {
        pushParentState(statefulObserver.mState);
        statefulObserver.dispatchEvent(lifecycleOwner, upEvent(statefulObserver.mState));
        popParentState();
        // mState / subling may have been changed recalculate
        targetState = calculateTargetState(observer);
    }

    if (!isReentrance) {
        // we do sync only on the top level.
        sync();
    }
    mAddingObserverCounter--;
}

static class ObserverWithState {
    State mState;
    GenericLifecycleObserver mLifecycleObserver;

    ObserverWithState(LifecycleObserver observer, State initialState) {
        mLifecycleObserver = Lifecycling.getCallback(observer);
        mState = initialState;
    }

    void dispatchEvent(LifecycleOwner owner, Event event) {
        State newState = getStateAfter(event);
        mState = min(mState, newState);
        mLifecycleObserver.onStateChanged(owner, event);
        mState = newState;
    }
}
```

首先，addObserver()会将原始的observer包装成带状态的statefulObserver。由于是在Activity.onCreate()方法调用addObserver()，所以statefulObserver的当前状态是`INITIALIZED`。

`upEvent(statefulObserver.mState)`根据当前状态生成Event对象，这个对象被`statefulObserver.dispatchEvent()`方法分发给mLifecycleObserver(注意它是另一个对原始observer的包装)。

分发Event后statefulObserver进入下一状态，`CREATED`。更多状态变化见下图。

![](lifecycle-states.png)

而`ObserverWithState.mLifecycleObserver`是通过`Lifecycling.getCallback()`静态方法获取到的，

```java
GenericLifecycleObserver mLifecycleObserver = Lifecycling.getCallback(observer)
```

正是通过`Lifecycling.getCallback()`方法，我们可一览`LifecycleObserver`家族全貌。其中，除`LifecycleObserver`接口对外公开之外，全部是lifecycle内部使用的类。

+ LifecycleObserver
+ FullLifecycleObserver
+ GenericLifecycleObserver
+ FullLifecycleObserverAdapter
+ SingleGeneratedAdapterObserver
+ CompositeGeneratedAdaptersObserver
+ ReflectiveGenericLifecycleObserver

对于我们的`MyLifecycleObserver`，`Lifecycling.getCallback(observer)`方法将其包装成ReflectiveGenericLifecycleObserver。而ReflectiveGenericLifecycleObserver的关键在于CallbackInfo和ClassesInfoCache。代码如下。

```java
class ReflectiveGenericLifecycleObserver implements GenericLifecycleObserver {
    private final Object mWrapped;
    private final CallbackInfo mInfo;

    ReflectiveGenericLifecycleObserver(Object wrapped) {
        mWrapped = wrapped;
        mInfo = ClassesInfoCache.sInstance.getInfo(mWrapped.getClass());
    }

    @Override
    public void onStateChanged(LifecycleOwner source, Event event) {
        mInfo.invokeCallbacks(source, event, mWrapped);
    }
}

static class CallbackInfo {
    final Map<Lifecycle.Event, List<MethodReference>> mEventToHandlers;
    final Map<MethodReference, Lifecycle.Event> mHandlerToEvent;
}
```

不难理解`ClassesInfoCache.createInfo(Class klass, @Nullable Method[] declaredMethods)`方法的作用是：

解析其klass参数，找到带有`OnLifecycleEvent`注解的方法，将这些方法包装成`MethodReference`。最后，MethodReference会保存在mEventToHandlers。

最后梳理一下，整个调用顺序是这样的：

```
LifecycleRegistry.addObserver()

ObserverWithState.dispatchEvent()

ReflectiveGenericLifecycleObserver.onStateChanged()

CallbackInfo.invokeCallbacks()

MethodReference.invokeCallback()

Method.invoke()
```

这里的Method，正是`MyLifecycleObserver.test5()`方法，它以反射的方法被lifecycle库调用。

# 补充知识

`LifecycleOwner`是一个接口，实现该接口的类具有生命周期`Lifecycle`。[Fragment](https://developer.android.com/reference/android/support/v4/app/Fragment)和[AppCompatActivity](https://developer.android.com/reference/android/support/v7/app/AppCompatActivity.html)实现了`LifecycleOwner`接口，具有生命周期。

实现了`LifecycleObserver`接口的组件可以跟`LifecycleOwner`协同工作，因为它能观察`LifecycleOwner`的`Lifecycle`。

# 参考资料
[lifecycle](https://developer.android.com/topic/libraries/architecture/lifecycle)