---
title: is_in_edit_mode
tags:
---

自定义`PoPImageView`在xml编辑器中出错

不是什么大问题，但开发UI过程不太方便

```
java.lang.NullPointerException
	at com.tencent.PmdCampus.CampusApplication.getCampusApplicationContext(CampusApplication.java:242)
	at com.tencent.PmdCampus.comm.widget.PoPImageView.<init>(PoPImageView.java:41)
	at com.tencent.PmdCampus.comm.widget.PoPImageView.<init>(PoPImageView.java:35)
	at sun.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
	at sun.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:62)
	at sun.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45)
	at java.lang.reflect.Constructor.newInstance(Constructor.java:423)
	at org.jetbrains.android.uipreview.ViewLoader.createNewInstance(ViewLoader.java:465)
	at org.jetbrains.android.uipreview.ViewLoader.loadClass(ViewLoader.java:172)
	at org.jetbrains.android.uipreview.ViewLoader.loadView(ViewLoader.java:105)
	at com.android.tools.idea.rendering.LayoutlibCallbackImpl.loadView(LayoutlibCallbackImpl.java:186)
	at android.view.BridgeInflater.loadCustomView(BridgeInflater.java:334)
	at android.view.BridgeInflater.loadCustomView(BridgeInflater.java:345)
	at android.view.BridgeInflater.createViewFromTag(BridgeInflater.java:245)
	at android.view.LayoutInflater.createViewFromTag(LayoutInflater.java:727)
	at android.view.LayoutInflater.rInflate_Original(LayoutInflater.java:858)
	at android.view.LayoutInflater_Delegate.rInflate(LayoutInflater_Delegate.java:70)
	at android.view.LayoutInflater.rInflate(LayoutInflater.java:834)
	at android.view.LayoutInflater.rInflateChildren(LayoutInflater.java:821)
	at android.view.LayoutInflater.rInflate_Original(LayoutInflater.java:861)
	at android.view.LayoutInflater_Delegate.rInflate(LayoutInflater_Delegate.java:70)
	at android.view.LayoutInflater.rInflate(LayoutInflater.java:834)
	at android.view.LayoutInflater.rInflateChildren(LayoutInflater.java:821)
	at android.view.LayoutInflater.inflate(LayoutInflater.java:518)
	at android.view.LayoutInflater.inflate(LayoutInflater.java:397)
	at com.android.layoutlib.bridge.impl.RenderSessionImpl.inflate(RenderSessionImpl.java:324)
	at com.android.layoutlib.bridge.Bridge.createSession(Bridge.java:429)
	at com.android.ide.common.rendering.LayoutLibrary.createSession(LayoutLibrary.java:389)
	at com.android.tools.idea.rendering.RenderTask$2.compute(RenderTask.java:548)
	at com.android.tools.idea.rendering.RenderTask$2.compute(RenderTask.java:533)
	at com.intellij.openapi.application.impl.ApplicationImpl.runReadAction(ApplicationImpl.java:966)
	at com.android.tools.idea.rendering.RenderTask.createRenderSession(RenderTask.java:533)
	at com.android.tools.idea.rendering.RenderTask.lambda$inflate$70(RenderTask.java:659)
	at java.util.concurrent.FutureTask.run(FutureTask.java:266)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)
	at java.lang.Thread.run(Thread.java:745)

```


```java
    public PoPImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        screenWidht = SystemUtils.getScreenWidth(CampusApplication.getCampusApplicationContext());
        //screenHeight = SystemUtils.getScreenHeight(CampusApplication.getCampusApplicationContext());
        mDensity = SystemUtils.getDensity(context);
    }
```

分析

`CampusApplication.getCampusApplicationContext()` 初始化过程复杂，这里没有必要使用`CampusApplication.getCampusApplicationContext()`，直接使用context即可

```java
    public PoPImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        screenWidht = SystemUtils.getScreenWidth(context);
        //screenHeight = SystemUtils.getScreenHeight(CampusApplication.getCampusApplicationContext());
        mDensity = SystemUtils.getDensity(context);
    }
```

# 

```
The following classes could not be found:
- com.tencent.PmdCampus.emoji.EmojiTextView (Fix Build Path, Edit XML, Create Class)
 Tip: Try to build the project.  Tip: Try to refresh the layout.
```

找不到类?

` com.tencent.PmdCampus.emoji.EmojiTextView` 来自另一个module, 对该module执行操作 "Build - Make Module 'app'"    异常消失

# 

java.lang.ClassNotFoundException: com.tencent.PmdCampus.comm.widget.ImageViewShowMore
	at org.jetbrains.android.uipreview.ModuleClassLoader.load(ModuleClassLoader.java:160)
	at com.android.tools.idea.rendering.RenderClassLoader.findClass(RenderClassLoader.java:54)
	at org.jetbrains.android.uipreview.ModuleClassLoader.findClass(ModuleClassLoader.java:90)
	at java.lang.ClassLoader.loadClass(ClassLoader.java:424)
	at java.lang.ClassLoader.loadClass(ClassLoader.java:357)
	at org.jetbrains.android.uipreview.ModuleClassLoader.loadClass(ModuleClassLoader.java:193)
	at java.lang.Class.getDeclaredConstructors0(Native Method)
	at java.lang.Class.privateGetDeclaredConstructors(Class.java:2671)
	at java.lang.Class.getConstructor0(Class.java:3075)
	at java.lang.Class.getConstructor(Class.java:1825)
	at org.jetbrains.android.uipreview.ViewLoader.createNewInstance(ViewLoader.java:396)
	at org.jetbrains.android.uipreview.ViewLoader.loadClass(ViewLoader.java:172)
	at org.jetbrains.android.uipreview.ViewLoader.loadView(ViewLoader.java:105)
	at com.android.tools.idea.rendering.LayoutlibCallbackImpl.loadView(LayoutlibCallbackImpl.java:186)
	at android.view.BridgeInflater.loadCustomView(BridgeInflater.java:334)
	at android.view.BridgeInflater.loadCustomView(BridgeInflater.java:345)
	at android.view.BridgeInflater.createViewFromTag(BridgeInflater.java:245)
	at android.view.LayoutInflater.createViewFromTag(LayoutInflater.java:727)
	at android.view.LayoutInflater.rInflate_Original(LayoutInflater.java:858)
	at android.view.LayoutInflater_Delegate.rInflate(LayoutInflater_Delegate.java:70)
	at android.view.LayoutInflater.rInflate(LayoutInflater.java:834)
	at android.view.LayoutInflater.rInflateChildren(LayoutInflater.java:821)
	at android.view.LayoutInflater.rInflate_Original(LayoutInflater.java:861)
	at android.view.LayoutInflater_Delegate.rInflate(LayoutInflater_Delegate.java:70)
	at android.view.LayoutInflater.rInflate(LayoutInflater.java:834)
	at android.view.LayoutInflater.rInflateChildren(LayoutInflater.java:821)
	at android.view.LayoutInflater.rInflate_Original(LayoutInflater.java:861)
	at android.view.LayoutInflater_Delegate.rInflate(LayoutInflater_Delegate.java:70)
	at android.view.LayoutInflater.rInflate(LayoutInflater.java:834)
	at android.view.LayoutInflater.rInflateChildren(LayoutInflater.java:821)
	at android.view.LayoutInflater.inflate(LayoutInflater.java:518)
	at android.view.LayoutInflater.inflate(LayoutInflater.java:397)
	at com.android.layoutlib.bridge.impl.RenderSessionImpl.inflate(RenderSessionImpl.java:324)
	at com.android.layoutlib.bridge.Bridge.createSession(Bridge.java:429)
	at com.android.ide.common.rendering.LayoutLibrary.createSession(LayoutLibrary.java:389)
	at com.android.tools.idea.rendering.RenderTask$2.compute(RenderTask.java:548)
	at com.android.tools.idea.rendering.RenderTask$2.compute(RenderTask.java:533)
	at com.intellij.openapi.application.impl.ApplicationImpl.runReadAction(ApplicationImpl.java:966)
	at com.android.tools.idea.rendering.RenderTask.createRenderSession(RenderTask.java:533)
	at com.android.tools.idea.rendering.RenderTask.lambda$inflate$70(RenderTask.java:659)
	at java.util.concurrent.FutureTask.run(FutureTask.java:266)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)
	at java.lang.Thread.run(Thread.java:745)
