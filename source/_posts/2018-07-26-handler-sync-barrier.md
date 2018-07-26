---
title: Handler问题记录
tags: android
toc: true
date: 2018-07-26 16:45:49
---

为什么onStart()不被回调，主线程在忙什么？
<!--more-->

# 问题
最近项目中碰到一个诡异的问题，现象如下：

1. IdleHandler机制失效了
2. onStart()居然不回调了 (其实会被回调，但延时非常大)
3. onStop()居然需要 10s 才会被执行 ([参考](http://km.oa.com/articles/show/373058?kmref=home_recommend_read))

IdleHandler和`onStart()`不能及时被执行引起各种问题。最后定位出来的原因是因为某段代码使用Handler不当，发送了大量Message，导致主线程负载过重(负载很重但又未明显卡顿)。

分析问题时绕了很多弯，发现其实快速定位问题的方法很简单。`Looper.setMessageLogging()`方法监听主线程MessageQueue上的Message日志，观察输出日志很容易发现大量异常的Message:

```
07-25 22:53:33.345 12819-12819/com.xxx.yyy I/IndexActivity: println: >>>>> Dispatching to Handler (com.xxx.yyy.view.common.widget.yyyTextSwitchView$ViewHandler) {2cefb4d} null: 1
    println: <<<<< Finished to Handler (com.xxx.yyy.view.common.widget.yyyTextSwitchView$ViewHandler) {2cefb4d} null
    println: >>>>> Dispatching to Handler (com.xxx.yyy.view.common.widget.yyyTextSwitchView$ViewHandler) {2cefb4d} null: 1
    println: <<<<< Finished to Handler (com.xxx.yyy.view.common.widget.yyyTextSwitchView$ViewHandler) {2cefb4d} null
    println: >>>>> Dispatching to Handler (com.xxx.yyy.view.common.widget.yyyTextSwitchView$ViewHandler) {2cefb4d} null: 1
    println: <<<<< Finished to Handler (com.xxx.yyy.view.common.widget.yyyTextSwitchView$ViewHandler) {2cefb4d} null
    println: >>>>> Dispatching to Handler (com.xxx.yyy.view.common.widget.yyyTextSwitchView$ViewHandler) {2cefb4d} null: 1
    println: <<<<< Finished to Handler (com.xxx.yyy.view.common.widget.yyyTextSwitchView$ViewHandler) {2cefb4d} null
    println: >>>>> Dispatching to Handler (com.xxx.yyy.view.common.widget.yyyTextSwitchView$ViewHandler) {2cefb4d} null: 1
```

具体方法在[Android之输出Handler日志](../looper-log)中有讨论，这里不再展开。我绕了不少弯，有些知识点是以前未掌握的，所以记录下来还是很有意义的。

# IdleHandler

```java
    public static interface IdleHandler {
        boolean queueIdle();
    }
```

线程由于没有message需要处理而被阻塞时(简单来说就是线程空闲)，IdleHandler会被回调。所以这样接口适用于处理一些不那么紧急的任务，可以充分利用CPU但又不跟更重紧急的任务争夺CPU。

IdleHandler用法如下，代码输出`queueIdle after onPause`。

```java
Looper.myQueue().addIdleHandler(new MessageQueue.IdleHandler() {
    @Override
    public boolean queueIdle() {
        Log.i(TAG, "queueIdle after onPause");
        return false;
    }
});
```

Android系统中也有用到IdleHandler。见[ActivityThread.scheduleGcIdler()](https://github.com/aosp-mirror/platform_frameworks_base/blob/master/core/java/android/app/ActivityThread.java#L2192)。

```java
final class GcIdler implements MessageQueue.IdleHandler {
    @Override
    public final boolean queueIdle() {
        doGcIfNeeded();
        return false;
    }
}

void scheduleGcIdler() {
    if (!mGcIdlerScheduled) {
        mGcIdlerScheduled = true;
        Looper.myQueue().addIdleHandler(mGcIdler);
    }
    mH.removeMessages(H.GC_WHEN_IDLE);
}

void unscheduleGcIdler() {
    if (mGcIdlerScheduled) {
        mGcIdlerScheduled = false;
        Looper.myQueue().removeIdleHandler(mGcIdler);
    }
    mH.removeMessages(H.GC_WHEN_IDLE);
}
```


看看MessageQueue.next()方法是如何处理IdleHandler的。

```java
    Message next() {
        ...
        int pendingIdleHandlerCount = -1; // -1 only during first iteration
        for (;;) {
            ...

            // 如果当前有需要处理的message, 这里直接返回message
            // return message;

            // pendingIdleHandlerCount < 0 表示当前是for循环的第1个迭代
            // mMessages == null           表示没有需要处理的message(消息队列是空的)
            if (pendingIdleHandlerCount < 0
                    && (mMessages == null || now < mMessages.when)) {
                pendingIdleHandlerCount = mIdleHandlers.size();
            }

            if (pendingIdleHandlerCount <= 0) {
                // 没有IdleHandler, 继续循环
                mBlocked = true;
                continue;
            }

            if (mPendingIdleHandlers == null) {
                mPendingIdleHandlers = new IdleHandler[Math.max(pendingIdleHandlerCount, 4)];
            }
            mPendingIdleHandlers = mIdleHandlers.toArray(mPendingIdleHandlers);

            // 运行IdleHandler
            for (int i = 0; i < pendingIdleHandlerCount; i++) {
                final IdleHandler idler = mPendingIdleHandlers[i];

                boolean keep = false;
                try {
                    keep = idler.queueIdle();
                } catch (Throwable t) {
                    Log.wtf(TAG, "IdleHandler threw exception", t);
                }

                // 自动删除不被保留的IdleHandler
				...
            }


            pendingIdleHandlerCount = 0;
        }
    }
```

正如文档所述，IdleHandler仅在线程空闲时才被执行。

+ 如果队列中有消息，不会执行IdleHandler
+ 每次`next()`调用时IdleHandler最多只有一次被执行的机会

回到我们的问题，为什么IdleHandler不被执行？这还用问，当然是队列中的消息太多。那么如何观察队列中有多少消息？这里提供一个[Helper](https://gist.github.com/410063005/d67fe6977619811d2d02ccdeb45b27c9)工具类用于观察队列中的消息。

```java
public class Helper implements Choreographer.FrameCallback {

    private static final String TAG = "IndexHelper";

    private Field messagesField;
    private Field nextField;

    public Helper() {
        try {
            messagesField = MessageQueue.class.getDeclaredField("mMessages");
            messagesField.setAccessible(true);
            nextField = Message.class.getDeclaredField("next");
            nextField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private void printMessages() {
        MessageQueue queue = Looper.myQueue();
        try {
            Message msg = (Message) messagesField.get(queue);
            StringBuilder sb = new StringBuilder();
            while (msg != null) {
                sb.append(msg.toString());
                sb.append("\n");
                msg = (Message) nextField.get(msg);
            }
            Log.i(TAG, sb.toString());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        Choreographer.getInstance().postFrameCallback(this);

        printMessages();
    }
}
```

调用`Choreographer.getInstance().postFrameCallback(new Helper())`，将输出每一帧队列中的消息。可是发现了一个非常奇怪的barrier消息，它没有target

```
07-26 01:10:06.083 23987-23987/com.xxx.yyy I/IndexHelper: { when=-1ms barrier=105 }
    { when=+1s132ms callback=android.view.View$ScrollabilityCache target=android.view.ViewRootImpl$ViewRootHandler }
```

`MessageQueue.next()`中关于barrier消息的处理也很特别。for循环中找下一条消息时，如果发现了barrier消息(target为null的消息)，会忽略之后所有的同步消息(异步消息仍然正常处理)，不进行任务实际处理。也就是说，同步消息一直被保留在消息队列中没被处理，IdleHandler当然更没机会被执行！

```java
for (;;) {
    ...
    nativePollOnce(ptr, nextPollTimeoutMillis);
    ...
    Message msg = mMessages;
    if (msg != null && msg.target == null) {
        // Stalled by a barrier.  Find the next asynchronous message in the queue.
        do {
            prevMsg = msg;
            msg = msg.next;
        } while (msg != null && !msg.isAsynchronous());
    }
    ...
    // 如果当前有需要处理的message, 这里直接返回message
    // return msg;
    ...
}
```

[Activity 的 onStop 居然需要 10s 才会被执行？记一次艰辛的问题排查](#)中提到了barrier消息问题。接下来看看到底什么是barrier消息。

# barrier消息

## 什么是barrier消息

`MessageQueue.postSyncBarrier()`方法注释文档中对此有比较清楚的描述。

```java
    /**
     * Posts a synchronization barrier to the Looper's message queue.
     *
     * Message processing occurs as usual until the message queue encounters the
     * synchronization barrier that has been posted.  When the barrier is encountered,
     * later synchronous messages in the queue are stalled (prevented from being executed)
     * until the barrier is released by calling {@link #removeSyncBarrier} and specifying
     * the token that identifies the synchronization barrier.
     *
     * This method is used to immediately postpone execution of all subsequently posted
     * synchronous messages until a condition is met that releases the barrier.
     * Asynchronous messages (see {@link Message#isAsynchronous} are exempt from the barrier
     * and continue to be processed as usual.
     *
     * This call must be always matched by a call to {@link #removeSyncBarrier} with
     * the same token to ensure that the message queue resumes normal operation.
     * Otherwise the application will probably hang!
     *
     * @return A token that uniquely identifies the barrier.  This token must be
     * passed to {@link #removeSyncBarrier} to release the barrier.
     *
     * @hide
     */
    public int postSyncBarrier() {
        return postSyncBarrier(SystemClock.uptimeMillis());
    }
```

总结如下：

> 向Looper的MessageQueue发送一个同步barrier消息。没有遇到barrier消息时一切按正常方式处理。遇到barrier消息时，队列中晚一些的同步消息被挂起(也就是说，不会执行)，直到barrier消息被`removeSyncBarrier()`方法移除。
> 该方法用于立刻延迟所有同步消息，直到某个条件被满足时释放该barrier。异步消息不受barrier消息影响，仍然正常处理
> `postSyncBarrier()`调用一定要跟`removeSyncBarrier()`一同调用，并且使用同一个token参数，以保证MessageQueue中的消息可以恢复到正常处理状态。否则 **应用很可能被挂起**

另外，`postSyncBarrier()`是一个隐藏的(@hide)方法，普通方式无法调用它。通常由Android系统调用`postSyncBarrier()`

## 谁在发送barrier消息
如何找到谁在发送barrier消息。 方法很简单，在`MessageQueue.postSyncBarrier()`方法处断点调试。这个技巧参考自[Activity 的 onStop 居然需要 10s 才会被执行？记一次艰辛的问题排查](#)

`MessageQueue.postSyncBarrier()`被调用时，会在断点处停下来。我们可以沿着调度信息不难找到调用方。

![](post-barrier.png)

是ViewRootImpl.scheduleTraversals()方法发出了barrier消息，出乎我们意料吧？[ViewRootImpl](https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/view/ViewRootImpl.java)

```java
    void scheduleTraversals() {
        if (!mTraversalScheduled) {
            mTraversalScheduled = true;
            mTraversalBarrier = mHandler.getLooper().getQueue().postSyncBarrier();
            mChoreographer.postCallback(
                    Choreographer.CALLBACK_TRAVERSAL, mTraversalRunnable, null);
            if (!mUnbufferedInputDispatch) {
                scheduleConsumeBatchedInput();
            }
            notifyRendererOfFramePending();
            pokeDrawLockIfNeeded();
        }
    }
	
    void doTraversal() {
        if (mTraversalScheduled) {
            mTraversalScheduled = false;
            mHandler.getLooper().getQueue().removeSyncBarrier(mTraversalBarrier);
            if (mProfile) {
                Debug.startMethodTracing("ViewAncestor");
            }
            performTraversals();
            if (mProfile) {
                Debug.stopMethodTracing();
                mProfile = false;
            }
        }
    }	

```

简单解释一下ViewRootImpl.scheduleTraversals()为什么会发送barrier消息。

> 为了让View能够有快速的布局和绘制，android中定义了一个Barrier的概念，当View在绘制和布局时会向Looper中添加了Barrier(监控器)，这样后续的消息队列中的同步的消息将不会被执行，以免会影响到UI绘制 参考自[Android Barrier](https://blog.csdn.net/degwei/article/details/50602445) [Android 屏幕刷新机制](https://www.cnblogs.com/dasusu/p/8311324.html)

# onStart和onStop
出于多种原因考虑，ActivityThread并不是立即回调`onStart()`或`onStop()`，而是在[ActivityThread.H](https://github.com/aosp-mirror/platform_frameworks_base/blob/master/core/java/android/app/ActivityThread.java#L1464)这个Hander中回调`onStart()`和`onStop()`。

[ActivityThread.handleStopActivity()](https://github.com/aosp-mirror/platform_frameworks_base/blob/master/core/java/android/app/ActivityThread.java)代码如下：

```java
private void handleStopActivity(IBinder token, boolean show, int configChanges, int seq) {
    ...

    // Schedule the call to tell the activity manager we have
    // stopped.  We don't do this immediately, because we want to
    // have a chance for any other pending work (in particular memory
    // trim requests) to complete before you tell the activity
    // manager to proceed and allow us to go fully into the background.
    info.activity = r;
    info.state = r.state;
    info.persistentState = r.persistentState;
    mH.post(info);
    mSomeActivitiesChanged = true;
}
```

很不幸，这个Handler和我们在主线程中创建的Handler共享同一个MessageQueue。大量向MessageQueue中发送消息时，必然会影响到`onStart()`或`onStop()`的处理速度。

# 总结
+ ActivityThread.H(Hander)中回调`onStart()`和`onStop()`。 这个Handler和我们在主线程中创建的Handler共享同一个MessageQueue。大量向MessageQueue中发送消息时，必然会影响到`onStart()`或`onStop()`的处理速度。本文最初提到的`onStart()`不回调就是因为消息过多引起。
+ IdleHandler可以用来处理一些不紧急的任务，比如ActivityThread使用它来执行gc任务
+ `Looper.setMessageLogging()`方法用于打印消息日志。当你发现消息有异常时，不妨试试这个方法
+ [Helper](https://gist.github.com/410063005/d67fe6977619811d2d02ccdeb45b27c9)可以打印当前MessageQueue中的所有Message

# 参考

+ [Android Handler Looper机制 | Jacks Blog](https://blog.dreamtobe.cn/2016/03/11/android_handler_looper/)
+ [聊一聊Android的消息机制 - 悠然红茶的个人页面 - 开源中国](https://my.oschina.net/youranhongcha/blog/492591)
+ [android 利用Handler机制中SyncBarrier的特性实现预加载 - CSDN博客](https://blog.csdn.net/cdecde111/article/details/54670136)
+ [Android Handler Looper机制 | Jacks Blog](https://blog.dreamtobe.cn/2016/03/11/android_handler_looper/)
+ [Android应用程序消息处理机制 - 点点滴滴 - SegmentFault 思否](https://segmentfault.com/a/1190000002982318)
+ [2.3.3 nativePollOnce函数分析 · 深入理解Android：卷2 · 看云](https://www.kancloud.cn/alex_wsc/android-deep2/413394)
+ [Android消息循环机制（二）Looper性能检测的缺陷 - 简书](https://www.jianshu.com/p/9849026e7232)
+ [Android Barrier - CSDN博客](https://blog.csdn.net/degwei/article/details/50602445)
+ [Android 屏幕刷新机制 - 请叫我大苏 - 博客园](https://www.cnblogs.com/dasusu/p/8311324.html)

<!-- http://km.oa.com/articles/show/373058?kmref=home_recommend_read -->