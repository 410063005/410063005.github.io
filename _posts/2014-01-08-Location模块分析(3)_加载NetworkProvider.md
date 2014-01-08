---
layout: post
title: Location模块分析(3)_加载NetworkProvider
keywords: Android,Location
description: Android
categories: [Android]
tags: [Android]
group: archive
icon: globe
---
代码路径

+ `LocationManagerService` 代码路径为：platform_frameworks\_base\services\java\com\android\server\LocationManagerService.java
+ `LocationProviderProxy` 代码路径为：platform\_frameworks_base\services\java\com\android\server\location\LocationProviderProxy.java
+ `ServiceWatcher` 代码路径为：platform\_frameworks_base\services\java\com\android\serverServiceWatcher.java
+ `config.xml` 配置文件路径为： platform\_frameworks_base\core\res\res\values\config.xml


我们仍然回到前面提到的 `LocationManagerService.loadProvidersLocked()` 方法。这里重点关注 `NetworkProvider` 的加载过程。

加载的时序如下

![load-nlp]({{ site.url }}/assets/20140108/load-nlp.PNG)

# 1. 绑定远程服务
# 1.1 config.xml 参数
以上 2，3，4 步对应 `LocationProviderProxy.createAndBind()` 方法。 加载 NetworkProvider 时会以如下方式调用该方法：

    // bind to network provider
    LocationProviderProxy networkProvider = LocationProviderProxy.createAndBind(
            mContext,
            LocationManager.NETWORK_PROVIDER,
            NETWORK_LOCATION_SERVICE_ACTION,
            com.android.internal.R.bool.config_enableNetworkLocationOverlay,
            com.android.internal.R.string.config_networkLocationProviderPackageName,
            com.android.internal.R.array.config_locationProviderPackageNames,
            mLocationHandler);

各参数含义分别如下：

+ name： LocationManager.NETWORK_PROVIDER  
Provider 的名字，这里为 "network"
+ action： NETWORK\_LOCATION\_SERVICE_ACTION  
绑定服务时使用的 action， 这里为 "com.android.location.service.v3.NetworkLocationProvider"
注意，这个 Action 在不同版本的 Android 平台上会有所不同。 比如 4.4 上为 v3, 而在 4.2 和 4.3 上为 v2
+ 三个来自 config.xml 的配置项
 1. overlaySwitchResId: 对应 config_enableNetworkLocationOverlay
 2. defaultServicePackageNameResId：对应 config_networkLocationProviderPackageName
 3. initialPackageNamesResId：对应 config_locationProviderPackageNames

这里分析一下上面提到的三个配置项。第一手资料就是配置项的注释。

	    <!-- Whether to enable network location overlay which allows network
	         location provider to be replaced by an app at run-time. When disabled,
	         only the config_networkLocationProviderPackageName package will be
	         searched for network location provider, otherwise packages whose
	         signature matches the signatures of config_locationProviderPackageNames
	         will be searched, and the service with the highest version number will
	         be picked. Anyone who wants to disable the overlay mechanism can set it
	         to false.
	         -->
	    <bool name="config_enableNetworkLocationOverlay" translatable="false">true</bool>
	    <!-- Package name providing network location support. Used only when
	         config_enableNetworkLocationOverlay is false. -->
	    <string name="config_networkLocationProviderPackageName" translatable="false">@null</string>
<font color="red">config\_enableNetworkLocationOverlay</font>：这个参数为 bool 值。 它决定是否开启 network location overlay， 允许 Network Location Provider 可以被别的应用替换。当禁用该功能时， 仅 config\_networkLocationProviderPackageName 指定的应用才能作为 Network Location Provider；当启用该功能时，会在签名跟 config\_locationProviderPackageNames 指定的应用匹配的所有程序包中找到版本号最大的那个用为 Network Location Provider

<font color="red">config_networkLocationProviderPackageName</font>：这个参数为字符串。 它指定了提供 Network Location Provider 功能的程序包名。 仅当 config\_enableNetworkLocationOverlay 为 false 时才起作用。 

    	<!-- Package name(s) containing location provider support.
         These packages can contain services implementing location providers,
         such as the Geocode Provider, Network Location Provider, and
         Fused Location Provider. They will each be searched for
         service components implementing these providers.
         It is strongly recommended that the packages explicitly named
         below are on the system image, so that they will not map to
         a 3rd party application.
         The location framework also has support for installation
         of new location providers at run-time. The new package does not
         have to be explicitly listed here, however it must have a signature
         that matches the signature of at least one package on this list.
         -->
	    <string-array name="config_locationProviderPackageNames" translatable="false">
	        <!-- The standard AOSP fused location provider -->
	        <item>com.android.location.fused</item>
	    </string-array>
<font color="red">config_locationProviderPackageNames</font>：这个参数为字符串数组。它指定了一组实现了 Location Provider 功能的程序包名。 强烈建议这个参数指定的程序包安装在 system image 中，以免映射到第三方应用。 Android Location Framework 还支持运行时安装新的 Location Provider。新包的包名不必在这个参数中指定，但<font color="red">新包的签名必须跟这个参数指定的某个包的签名匹配</font>。

小技巧：实现 Network Location Provider 时一直很困惑。调试时 apk 不仅要安装到 system/app 目录替换原来的， 还需要重启机器。 按照上面的说法， 应该可以不用这么麻烦。 最开始先安装一个实现了 Network Location Provider 功能的 apk 到 system/app 下，以后每次不用安装到 system/app 目录下， 而只要保证签名匹配即可。 如果可行的话， 应该可以大大提高开发调试速度。 

通过下面一段代码，可以理解这三个参数是如何工作的。

        // Whether to enable service overlay.
        boolean enableOverlay = resources.getBoolean(overlaySwitchResId);
        ArrayList<String>  initialPackageNames = new ArrayList<String>();
        if (enableOverlay) {
            // A list of package names used to create the signatures.
            String[] pkgs = resources.getStringArray(initialPackageNamesResId);
            if (pkgs != null) initialPackageNames.addAll(Arrays.asList(pkgs));
            mServicePackageName = null;
            if (D) Log.d(mTag, "Overlay enabled, packages=" + Arrays.toString(pkgs));
        } else {
            // The default package name that is searched for service implementation when overlay is
            // disabled.
            String servicePackageName = resources.getString(defaultServicePackageNameResId);
            if (servicePackageName != null) initialPackageNames.add(servicePackageName);
            mServicePackageName = servicePackageName;
            if (D) Log.d(mTag, "Overlay disabled, default package=" + servicePackageName);
        }
        mSignatureSets = getSignatureSets(context, initialPackageNames);


# 1.1 查找最合适的程序
以上第 6 步：查找最合适的程序包

    /**
     * Searches and binds to the best package, or do nothing
     * if the best package is already bound.
     * Only checks the named package, or checks all packages if it
     * is null.
     * Return true if a new package was found to bind to.
     */
    private boolean bindBestPackageLocked(String justCheckThisPackage) {
        Intent intent = new Intent(mAction);
        if (justCheckThisPackage != null) {
            intent.setPackage(justCheckThisPackage);
        }
        List<ResolveInfo> rInfos = mPm.queryIntentServicesAsUser(intent,
                PackageManager.GET_META_DATA, UserHandle.USER_OWNER);
		...
	}

结合前面的分析，容易知道：

+ config\_enableNetworkLocationOverlay 为 false 时， 仅 config\_networkLocationProviderPackageName 指定的程序包才能作为 Network Location Provider。注意， `bindBestPackageLocked()` 方法的 `justCheckThisPackage` 跟这个程序包名对应。 
+  config\_enableNetworkLocationOverlay 为 true 时， `justCheckThisPackage` 为 `null`

+ `justCheckThisPackage` 为一个确定的包名时， `queryIntentServicesAsUser()` 方法最多找到一个应用
+ `justCheckThisPackage` 为 `null` 时， `queryIntentServicesAsUser()` 方法可能找到多个应用。

而上面 `bindBestPackageLocked()` 方法中省略的代码， 正是处理多个应用的情况。这时需要找到一个最合适的应用。 查找的策略很简单： <font color="red">签名应当与 config_locationProviderPackageNames 指定的某个应用匹配、版本号最大</font>。

# 1.2 绑定
完成上述的查找最合适的程序后， 第 7，8 步会绑定这个程序包中相应的 Service。这个绑定跟我们平常写应用时使用 `bindService()` 非常类似。最终它会绑定到目标服务。

    private void bindToPackageLocked(String packageName, int version, boolean isMultiuser) {
        unbindLocked();
        Intent intent = new Intent(mAction);
        intent.setPackage(packageName);
        mPackageName = packageName;
        mVersion = version;
        mIsMultiuser = isMultiuser;
        if (D) Log.d(mTag, "binding " + packageName + " (version " + version + ") ("
                + (isMultiuser ? "multi" : "single") + "-user)");
        mContext.bindServiceAsUser(intent, this, Context.BIND_AUTO_CREATE | Context.BIND_NOT_FOREGROUND
                | Context.BIND_NOT_VISIBLE, mIsMultiuser ? UserHandle.OWNER : UserHandle.CURRENT);
    }

# 2. 远程 Service
前面分析了加载 Network Location Provider 的过程。其实真正的加载可以理解为 1.2 中的绑定远程服务。 那么这个<font color="red">远程服务到底是谁？它是怎么实现的？绑定成功后会发生什么？</font>后一篇中将讨论。


