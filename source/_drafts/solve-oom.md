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

# 经验

`StatService.reportError(mActivity, "Tencent is null or !Tencent.isSessionValid()?");` 

-> 

`StatService.reportError(mActivity.getApplicationContext(), "Tencent is null or !Tencent.isSessionValid()?");` 

避免SDK可能持有activity

