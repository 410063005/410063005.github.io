---
title: 解决OOM问题
tags:
 - android
 - 优化
---

# 背景
2.6.4.3上线后一周

crash率




2017-2-23至2017-3-1，共8天

1.20 1.19 1.24 1.23 2.0 3.39 2.96

crash次数176次，OOM共56次，占比31.8%

内存泄露非常严重

# 分析

接入[LeakCanary][https://github.com/square/leakcanary]

# 问题

SplashActivity内存泄露

1.5MB

解决方法

+ 先重构, 减少SplashActivity复杂性
+ LoginTIMCallBack实现TIMCallBack接口，SplashActivity不要实现 TIMCallBack接口， 而是实现LoginTIMCallBack.TIMCallBackListener来跟LoginTIMCallBack交互。登录时使用TIMCallBack接口，可避免泄露SplashActivity这个大对象，最多会泄露LoginTIMCallBack这个小对象

原因

IM SDK 登录过程中持有了实现TIMCallBack接口的对象

```java

public class LoginTIMCallBack implements TIMCallBack {

    private TIMCallBackListener mTIMCallBackListener;

    public void setTIMCallBackListener(TIMCallBackListener listener) {
        mTIMCallBackListener = listener;
    }

    @Override
    public void onError(int i, String s) {
        if (mTIMCallBackListener != null) {
            mTIMCallBackListener.onError(i, s);
        }
    }

    @Override
    public void onSuccess() {
        if (mTIMCallBackListener != null) {
            mTIMCallBackListener.onSuccess();
        }
    }

    public interface TIMCallBackListener {
        void onError(int i, String s);

        void onSuccess();
    }
}


public class SplashActivity implements LoginTIMCallBack.TIMCallBackListener {
    private LoginTIMCallBack mLoginTIMCallBack;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLoginTIMCallBack = new LoginTIMCallBack();
        mLoginTIMCallBack.setTIMCallBackListener(this);
    }    
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLoginTIMCallBack.setTIMCallBackListener(null);
    }    
}
```

----

LoginActivity内存泄露

```java
public class LoginPresenterImpl {
    private Activity mActivity;

    private IUiListener mLoginListener = new BaseUiListener();
    
    @Override
    public void doQQLogin() {
        Tencent tencent = Tencent.createInstance(Config.QQ_SDK_APP_ID, mContext);

        if (tencent != null && !tencent.isSessionValid()) {
            tencent.login(mActivity, "all", mLoginListener);
        } else {
            StatService.reportError(mActivity.getApplicationContext(), "Tencent is null or !Tencent.isSessionValid()?");
        }
    }
    
    private class BaseUiListener implements IUiListener {
    }    
}
```

原因：`doQQLogin()`QQ登录时，登录SDK最终会持有`mLoginListener`的引用，而`mLoginListener`为内部类，它持有外部对象(LoginPresenterImpl的对象)的引用。而LoginPresenterImpl的对象没有释放对mActivity的引用，导致内存泄露

SDK -> mLoginListener -> LoginPresenterImpl obj -> mActivity


1. Activity作为`LoginPresenterImpl.doQQLogin()`的参数，而不作为LoginPresenterImpl的成员，
2. BaseUiListener作为嵌套类，避免mLoginListener引用外部对象

```java
public class LoginPresenterImpl {

    private IUiListener mLoginListener = new BaseUiListener();
    
    @Override
    public void doQQLogin(Activity activity) {
        Tencent tencent = Tencent.createInstance(Config.QQ_SDK_APP_ID, activity);
        if (tencent != null && !tencent.isSessionValid()) {
            tencent.login(activity, "all", mLoginListener);
        } else {
            StatService.reportError(activity.getApplicationContext(), "Tencent is null or !Tencent.isSessionValid()?");
        }
    }
    
    private static class BaseUiListener implements IUiListener {
    }    
}
```

----

IndexActivity内存泄露

```java
package com.tencent.imsdk;

public class QLogImpl {


    private static Handler retryInitHandler = new Handler(Looper.getMainLooper());

}
```

IM SDK中的QLogImpl有一个静态字段`retryInitHandler`，`QLogImpl`加载后这个字段会初始化。

它会引用到主线程的`mQueue`，导致一旦有在主线程的handler执行操作时，leakcanary会报内存泄露

我们在IndexActivity中并没有主动在主线程的handler执行操作，但View有

```java
   
public class View {   
    public void notifyViewAccessibilityStateChangedIfNeeded(int changeType) {
        if (!AccessibilityManager.getInstance(mContext).isEnabled() || mAttachInfo == null) {
            return;
        }
        if (mSendViewStateChangedAccessibilityEvent == null) {
            mSendViewStateChangedAccessibilityEvent =
                    new SendViewStateChangedAccessibilityEvent();
        }
        mSendViewStateChangedAccessibilityEvent.runOrPost(changeType);
    }
    
    private class SendViewStateChangedAccessibilityEvent implements Runnable {
        ...
    }
}
```

可以理解为这里为误报，另外影响有限，不算严重

QLogImpl.retryInitHandler字段并没有直接用到，proguard时可考虑去掉

## SearchFriendActivity 内存泄露

原因：RxBus是我们基于RxJava封装的bus库，


SearchFriendActivity -> UserListAdapter -> RxBus

UserListAdapter 使用 RxBus 监听，但没有取消监听

```java
public class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.ViewHolder> {
    private CompositeSubscription mSubscription = new CompositeSubscription();

    public UserListAdapter(FragmentActivity fragmentActivity) {
        RxBus.getRxBusSingleton().subscribe(mSubscription, new RxBus.EventLisener() {
            @Override
            public void dealRxEvent(Object event) {

                ...
            }
        });
    }
    
    public void onDestroy() {
        if (!mSubscription.isUnsubscribed()) {
            mSubscription.unsubscribe();
        }
    }    
}
```

解决方法：Activity.onDestroy() 中调用 UserListAdapter.onDestroy() ，取消监听

![](search_friend.jpg)
![](phone_contacts.jpg)


注：类似问题发生在 SearchFriendActivity  PhoneContactsActivity MayKnowFriendsActivity

## BindMobileActivity 内存泄露

![](bind_mobile.jpg)

原因：获取验证码时使用`CountDownTimer`倒计时1分钟，处理上不严密。TimeCount是内部类，会持有外部对象的引用。某些条件下会出现`BindMobileActivity.onDestroy()` 已执行的情况下，倒计时未结束，导致 BindMobileActivity 内存泄露

```java
public class BindMobileActivity {

    class TimeCount extends CountDownTimer {
        
    }
}
````

解决方法：及时调用`TimeCount.cancel()` 取消倒计时

## PaperPlaneActivity

### 一
原因：代码逻辑导致没有取消消息监听

1. 创建Presenter , Presenter 作为监听器注册到 IM SDK
2. 将Presenter关联到当前View (Presenter持有View的引用)
3. 退出界面时取消Presenter跟View的关联 (这个过程也会取消监听器)

但View中的某些逻辑导致重复进行第1个操作，退出界面时监听器其实没有真正取消

![](paper_plane_heap_marked.jpg)

### 二
原因：代码逻辑导致没有取消监听器 (还是有些不太明白为什么leakcanary中的对象引用关系是这样的)

不过能看到 `references array Object[].[12]` 中的数字越来越大，数组大小？ 猜测是没有取消监听引起的。

源码在哪里？

ChatActity PaperPlaneActivity PickUpPlaneActivity的 `onCreate()` 方法中均可能调用 `registerHeadsetPlugReceiver()` 方法

```
    public void registerHeadsetPlugReceiver(Context context) {
        mHeadsetReceiver = new HeadsetReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.HEADSET_PLUG");
        context.registerReceiver(mHeadsetReceiver, intentFilter);
    }

    public void unRegisterHeadsetPlugReceiver(Context context) {
        try {
            context.unregisterReceiver(mHeadsetReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }
```

![](papaer_plane3.png)

![](papaer_plane3_why.jpg)

```java
    private int mCount;

    public void registerHeadsetPlugReceiver(Context context) {
        if (mCount <= 0) {
            mHeadsetReceiver = new HeadsetReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.HEADSET_PLUG");
            context.registerReceiver(mHeadsetReceiver, intentFilter);
        }
        mCount++;
    }

    public void unRegisterHeadsetPlugReceiver(Context context) {
        mCount--;
        if (mCount <= 0) {
            try {
                context.unregisterReceiver(mHeadsetReceiver);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            mCount = 0;
        }
    }
```

## 

# 经验

`StatService.reportError(mActivity, "Tencent is null or !Tencent.isSessionValid()?");` 

-> 

`StatService.reportError(mActivity.getApplicationContext(), "Tencent is null or !Tencent.isSessionValid()?");` 

避免SDK可能持有activity

# 动手实验
webview的内存问题  [WebView内存泄漏][webview]


# 参考

[WebView内存泄漏][webview]

[webview]: http://lipeng1667.github.io/2016/08/06/memory-optimisation-for-webview-in-android/
