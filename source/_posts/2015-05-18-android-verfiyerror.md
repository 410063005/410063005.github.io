---
layout: post
title: "VerfiyError浅析"
date: 2015-05-18 15:57:04 +0800
comments: true
categories: android java
---

# VerifyError是什么
[Android开发文档][VerifyError]中关于`VerifyError`是这么介绍的：

> Thrown when the VM notices that an attempt is made to load a class which does not pass the class verification phase.

即，Java虚拟机进行类加载时该类无法通过类校验(class verification)。具体来讲

> 这种错误表示JVM中的校验器检测到某个类，虽然格式正确，但内部存在不一致或有安全问题。`VerifyError`类继承自`LinkageError`类，后者用于表示一类错误情形：一个类(称为A)依赖于另外的类(称为B)，而类B发生了兼容性上的改变。此外，`LinkageError`类继承自`Error`类，`Error`用于表示应用不应捕获的严重问题。方法不应该在`throw`子句中声明这些错误，因为它们表示不应发生的异常情况。

> JVM包含一个字节码校验器，用于字节码执行前的验证，以保证字节码序列的完整性。验证过程通常包括以下检查：

> + Branches point to valid locations (跳转分支有效? ) - 类加载时发生
> + 数据被初始化，引用类型安全 - 类加载时发生
> + 对私有方法或数据的访问受到控制 - 动态进行, 方法或数据首次被访问时发生

# VerifyError何时发生
工程师实际开发中通常很难碰到`VerifyError`，而用户使用App时出现的Crash中确实有少量`VerifyError`。那么`VerifyError`到底怎么发生的呢。

有一种说法是，在[低版本的机器上调用不支持的API会造成`VerifyError`问题](http://stackoverflow.com/questions/8951666/getting-error-java-lang-verifyerror)。在我的三星S5830(Android 2.3.6)上运行以下代码

```
@Override
protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	System.out.println("before test");
	new Location("test").isFromMockProvider(); // Android 4.3中新加的API, 在Android 2.3.6什么后果
	System.out.println("after test");
}
```

的确崩溃了，但不是`VerifyError`，而是`NoSuchMethodError`。(不清楚不同机器上异常信息是否会有所不同)

下面找了一些`VerifyError`的例子。虽然这里的例子都比较简单，但对我们分析用户反馈中的`VerifyError`应该还是有所帮助的。

## 例一
[例子来源][VerifyErrorDemo]

在Android手机(HTC One X, MUI-4.9.5, Android 4.4.4)上，这个例子的确会产生`VerifyError`。但在PC上(Java版本是1.6.0_13)，这个例子却能正常运行。这个例子不是很容易理解，有些疑惑。

## 例二
[例子来源][VerifyErrorDemo]

相于前一个例子，这个容易理解多了，简单来说就是"将可继承的父类修改为不可继承，而子类保持不变"。步骤概括如下：

1. 定义`TestClassA`和`TestClassB`两个类
2. `javac`分别将它们编译成class文件
3. 修改`TestClassA`类，将其修改为`final`(不可继承的类)
4. 重新编译`TestClassA`为class文件 (`TestClassB`仍是之前的class文件)
5. `java`运行`TestClassB`

错误日志如下

`java.lang.VerifyError: Cannot inherit from final class`

除了继承`final`类会产生`VerifyError`，类似的办法还包括：

1. 子类覆盖父类中的`final`方法
2. 修改继承关系，引起方法参数类型错误。具体见[这里][solve-verifyerror]

## 实际例子
有个App使用定位SDK时出现[如下crash](http://crashes.to/s/0f287e83c35)

```
java.lang.VerifyError: android/location/ILocationListener$Stub
       at android.location.LocationManager._requestLocationUpdates(LocationManager.java:683)
       at android.location.LocationManager.requestLocationUpdates(LocationManager.java:593)
       at ct.bt.a()
       at ct.bu.a()
```

crash的地方对应的代码如下

		try {
			manager.requestLocationUpdates(
				LocationManager.GPS_PROVIDER,
				millis, 
				0.0f, 
				this, 
				handler.getLooper());
		} catch (Exception e) {
		}
		
不清楚错误原因，已有信息总结如下

1. 2015.5.11至2015.5.19，该问题共发生2例
2. 机型分别是 HUAWEI G520(Android 4.1.1)和HUAWEI Y300(Android 4.1.2)

错误日志表明, `ILocationListener$Stub`通不过检验，看对应的源码，问题大概来自 `LocationListener` 相关代码。

```
    private void _requestLocationUpdates(String provider, Criteria criteria, long minTime,
            float minDistance, boolean singleShot, LocationListener listener, Looper looper) {
        if (minTime < 0L) {
            minTime = 0L;
        }
        if (minDistance < 0.0f) {
            minDistance = 0.0f;
        }

        try {
            synchronized (mListeners) {
                ListenerTransport transport = mListeners.get(listener);
                if (transport == null) {
                    transport = new ListenerTransport(listener, looper);
                }
                mListeners.put(listener, transport);
                mService.requestLocationUpdates(provider, criteria, minTime, minDistance,
                        singleShot, transport, mContext.getPackageName());
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "requestLocationUpdates: DeadObjectException", ex);
        }
    }
```

这里并没有直接出现`ILocationListener$Stub`，唯一跟 `ILocationListener$Stub`有关的是`ListenerTransport`。`ListenerTransport`继承自`ILocationListener.Stub`，`ILocationListener`及`ILocationListener.Stub`由`ILocationListener.aidl`生成。

`ILocationListener$Stub`是编译ROM时就已生成的类，正常来说不可能发生任何改变，也就不应该出现"版本兼容性"问题。但如果用户升级ROM出错或刷机呢？<font color="red">理解有限，无法评估了。</font>

<font color="red">系统代码为什么会出现VerifyError呢?</font>

# VerifyError怎么处理
了解`VerifyError`发生的原因之后，就不难知道避免`VerifyError`的办法很简单，那就是使用相同版本的JDK编译全部的类。另外，一旦某个类发生改变，应确保重新编译整个项目。最后，如果应用使用了外部的库，确认版本是正确的。

上面提到的处理方法的前提是我们可以控制源码，而Android平台上多数情况下我们是没有这种能力的，只能尽量保证API版本正确。

# 参考资料
[JVM Specification: Chapter 4.10. Verification of class Files.](http://examples.javacodegeeks.com/java-basics/exceptions/java-lang-verifyerror-how-to-solve-verifyerror/docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.10)

[The Byte Code Verification Process.](http://www.oracle.com/technetwork/java/security-136118.html)


[VerifyError]: http://developer.android.com/reference/java/lang/VerifyError.html 
[VerifyErrorDemo]: https://github.com/aectann/android-verify-error-example
[solve-verifyerror]: http://examples.javacodegeeks.com/java-basics/exceptions/java-lang-verifyerror-how-to-solve-verifyerror/