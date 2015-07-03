---
layout: post
title: "Gradle构建"
date: 2015-07-03 11:59:17 +0800
comments: true
categories: android
published: true
---
本文主要讨论如何在实际项目中应用Gradle的Product Flavors特性，实现最大程度的自动化发布。

<!--more-->

越来越多的Android项目使用Gradle构建。相比Ant，[Gradle的主要好处](http://tools.android.com/tech-docs/new-build-system/user-guide)是：

+ 容易重用代码和资源
+ 方便构建多渠道/多版本包

本文主要讨论如何在实际项目中应用Gradle的Product Flavors特性，实现最大程度的自动化发布。当然，我刚加入新项目不久，并不完全了解项目的发布情况。以下只是针对自己发现的问题，尝试进行改进。改进办法可能有待完善。

# 1. 问题背景
我们Android项目中不定期发布APK更新。发布前会对代码或配置进行若干修改，再进行构建。这种发布方式的缺点较明显：

1. 需要修改某几处配置，导致发布门槛高(通常只能专人或熟悉配置情况的同事发布)
2. 发布前修改可能引起潜在风险，比如误操作
3. 不利于自动化构建和持续集成

个人认为"发布前修改代码不如修改配置，修改配置又不如什么都不改"。目前项目我们只做到了不修改代码、但需要修改配置，利用Gradle的Product Flavors特性可以做到不必修改配置，从而有效避免上述发布方式存在的问题。

# 2. Gradle知识

## 2.1 Build Type
缺省时有Release和Debug两种构建类型，通常会有`debug`和`release`两个实例被创建。允许重新配置这两个对象，也可创建自己的构建类型。

通常不用配置`debug`对象，但需要配置`release`以指定keystore、是否混淆等。下面的代码重新配置`release`对象，启用ProGuard：

```
    buildTypes {
        release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard.cfg'
        }
    }
```

## 2.2 Product Flavors
Product Flavors适用于同一个项目要出多个不同的APK包，而每个APK包又很差异又很小的场景，比说仅仅渠道号不同。一个项目可以有不同的flavors，以生成不同的应用。下面的代码定义了`flavor1`和`flavor2`两个flavors：

```
android {
    ....

    productFlavors {
        flavor1 {
            ...
        }

        flavor2 {
            ...
        }
    }
}
```

## 2.3 Build Variants
每个Build Type会产生一个新的APK，每个Product Flavors也会产生新的APK。所以项目的最终输出是Build Type和Product Flavors(如果有的话)的所有组合。每种(Build Type, Product Flavor)组合是一个Build Variant。
比如，使用缺省的`debug`和`release` Build Type，同时定义`flavor1`和`flavor2`两个Product Flavors，会产生四个Build Variants：

+ Flavor1 - debug
+ Flavor1 - release
+ Flavor2 - debug
+ Flavor2 - release

如果不定义Product Flavors，Build Variants将跟Build Type完全一样。

# 3. 简单例子
Product Flavors一种场景是为应用的免费版和付费版创建不同的Product Flavors。在项目的`build.gradle`文件中添加不同的Product Flavors：

```
productFlavors {
    paid {
        applicationId = "com.exam.gradleexamples.paid"
        versionName = "1.0-paid"
    }

    free {
        applicationId = "com.exam.gradleexamples.free"
        versionName = "1.0-free"
    }
} 
```

完整的代码见[这里](https://github.com/410063005/asdemo/blob/master/productflavors/build.gradle)。可以在Android Studio的Build Variants视图中选择自定义的Product Flavors。

![buildvariants](/images/buildvariants.png)

Product Flavors的强大之处在于，可为不同的Flavor提供不同的资源！可以理解为，跟App提供`drawable-hdpi`, `values-v14`等资源非常类似，只不过Android系统是在运行时选择这些资源，而这里是Gradle在构建时选择相应Flavor资源。

假定项目结构如下：

```
src
	main
		java
		res
			values
				strings.xml
				...
			...
		AndroidManifest.xml
	free
		res
			values
				strings.xml
	paid
		res
			values
				strings.xml
 
 
```

构建时，main目录中的`strings.xml`会被free或paid目录中的`strings.xml`覆盖。这样就能在不同的Product Flavors中使用不同的字符串。轻松满足打渠道包这种使用场景！

编译并安装

```
	gradlew assembleFreeDebug assemblePaidDebug
	gradlew installFreeDebug installPaidDebug
```

运行截图如下：

![app-name](/images/app-name.png)

![app-free](/images/app-free.png)

![app-paid](/images/app-paid.png)

# 4. 实践
上面的例子演示了Product Flavors的用法。虽然这个例子很简单，但在实际项目中完全可以借鉴这里Product Flavors的用法。

项目中主要有以下几处配置导致发布前必须手动修改：

1. manifest中名为`APP_ENV`的meta-data，它表示当前的开发环境。不同开发环境会连到不同服务器。开发时修改为`<meta-data android:name="APP_ENV" android:value="1" />`，连接开发服务器。发布前必须修改为`<meta-data android:name="APP_ENV" android:value="5" />`，连接正式服务器
2. manifest中名为`APPKEY_DENGTA`和`TA_APPKEY`的meta-data，分别是两个SDK的key。发布前必须修改为正式的key

另外，项目是从Eclipse迁移过来的，仍然保持以下这种结构。由于不是Android Studio缺省的项目结构，导致后面配置`build.gradle`稍稍麻烦。

```
assets
src
res
	values
		strings.xml
		...
	...
libs
AndroidManifest.xml
```

项目中`Env`的类从manifest中读取`APP_ENV`以得到当前开发环境，并提供`getHost()`, `getHttpHost()`, `isDebuggable()`等方法。代码如下：

```
public class Env {
    private final static int ENV_DEV = 1;
    private final static int ENV_TEST = ENV_DEV + 1;
    private final static int ENV_BETA_DOMAIN = ENV_TEST + 1;
    private final static int ENV_RELEASE_IP = ENV_BETA_DOMAIN + 1;
    private final static int ENV_RELEASE_DOMAIN = ENV_RELEASE_IP + 1;

    // 开发环境
    private final static String DEV_IP = "1.1.1.1";
    private final static int DEV_PORT = 5740;
    // 测试环境
    private final static String TEST_IP = "1.1.1.1";
    private final static int TEST_PORT = 6740;
    // Beta环境
    private final static String BETA_DOMAIN = "2.2.2.2";
    private final static int BETA_PORT = 5740;
    // 正式环境
    private final static String RELEASE_IP = "3.3.3.3";
    private final static String RELEASE_DOMAIN = "our.domain";
    private final static int RELEASE_PORT = 5740;

    private static int currentEnv = ENV_RELEASE_DOMAIN;

    public static void initEnv(Context context) {
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(),
                    PackageManager.GET_META_DATA);
            currentEnv = appInfo.metaData.getInt("APP_ENV");
        } catch (NameNotFoundException e) {
            Logger.e(e);
        }
    }

    public static SocketAddress getHost() {
        SocketAddress address = null;
        switch (currentEnv) {
            case ENV_DEV:
                address = new InetSocketAddress(DEV_IP, DEV_PORT);
                break;
            case ENV_TEST:
                address = new InetSocketAddress(TEST_IP, TEST_PORT);
                break;
            case ENV_BETA_DOMAIN:
                address = new InetSocketAddress(BETA_DOMAIN, BETA_PORT);
                break;
            case ENV_RELEASE_IP:
                address = new InetSocketAddress(RELEASE_IP, RELEASE_PORT);
                break;
            case ENV_RELEASE_DOMAIN:
                address = new InetSocketAddress(RELEASE_DOMAIN, RELEASE_PORT);
                break;
        }
        return address;
    }

    public static String getHttpHost() {
        String host = null;
        switch (currentEnv) {
            case ENV_DEV:
                host = "http://1.1.1.1:8080/app/";
                break;
            case ENV_TEST:
                host = "http://1.1.1.1:8081/app/";
                break;
            case ENV_BETA_DOMAIN:
                host = "http://2.2.2.2:8081/app/";
                break;
            case ENV_RELEASE_IP:
                host = "http://3.3.3.3:8080/app/";
                break;
            case ENV_RELEASE_DOMAIN:
                host = "http://our.domain:8081/app/";
                break;
        }
        return host;
    }

    public static boolean isDebuggable() {
        switch (currentEnv) {
            case ENV_DEV:
                return true;
            case ENV_TEST:
                return true;
            case ENV_BETA_DOMAIN:
                return true;
            case ENV_RELEASE_IP:
                return false;
            case ENV_RELEASE_DOMAIN:
                return false;
            default:
                return false;
        }
    }
}
```

开始修改。步骤如下：

## 4.1 通过配置文件简化`Env`

`Env`这个类本身没有问题，但妨碍我们使用Product Flavors特性。新建`res\values\env_config.xml`文件，文件内容如下：

```
<?xml version="1.0" encoding="utf-8"?>
<!-- 开发环境 -->
<resources>
    <string name="ip">"113.108.78.115"</string>
    <integer name="port">5740</integer>
    <string name="http_host">"http://113.108.78.115:8080/app/"</string>
    <bool name="debuggable">true</bool>
</resources>
```

有了`evn_config.xml`，我们可以将`Env`类简化成这样：

```
public class Env {
    private static Resources sRes;

    public static void initEnv(Context context) {
        sRes = context.getResources();
    }

    public static SocketAddress getHost() {
        String ip = sRes.getString(R.string.ip);
        int port = sRes.getInteger(R.integer.port);
        return new InetSocketAddress(ip, port);
    }

    public static String getHttpHost() {
        return sRes.getString(R.string.http_host);
    }

    public static boolean isDebuggable() {
        return sRes.getBoolean(R.bool.debuggable);
    }
}
```

# 4.2 添加Product Flavors

由于不是缺省的项目结构，为便于理解，这里直接给出添加Product Flavors后完整的`build.gradle`

```
apply plugin: 'android'

dependencies {
    compile fileTree(dir: 'libs', include: '*.jar')
    compile project(':WidgetLib')
}

android {
    compileSdkVersion 21
    buildToolsVersion "20.0.0"

    sourceSets {
        main {
            manifest.srcFile 'AndroidManifest.xml'
            java.srcDirs = ['src']
            resources.srcDirs = ['src']
            aidl.srcDirs = ['src']
            renderscript.srcDirs = ['src']
            res.srcDirs = ['res']
            assets.srcDirs = ['assets']
            jniLibs.srcDirs = ['libs']
        }

        envDev {
        }

        envTest {
            res.srcDirs = ['res-test']
        }

        envBetaDomain {
            res.srcDirs = ['res-beta-domain']
        }

        envReleaseIp {
            res.srcDirs = ['res-release-ip']
            manifest.srcFile 'AndroidManifest-release.xml'
        }

        envReleaseDomain {
            res.srcDirs = ['res-release-domain']
            manifest.srcFile 'AndroidManifest-release.xml'
        }

        // Move the tests to tests/java, tests/res, etc...
        instrumentTest.setRoot('tests')

        // Move the build types to build-types/<type>
        // For instance, build-types/debug/java, build-types/debug/AndroidManifest.xml, ...
        // This moves them out of them default location under src/<type>/... which would
        // conflict with src/ being used by the main source set.
        // Adding new build types or product flavors should be accompanied
        // by a similar customization.
        debug.setRoot('build-types/debug')
        release.setRoot('build-types/release')
    }

    buildTypes {
        release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard.cfg'
        }
    }

    productFlavors {
        envDev {
            applicationId = "com.tencent.campus.dev"
        }

        envTest {
            applicationId = "com.tencent.campus.test"
        }

        envBetaDomain {
            applicationId = "com.tencent.campus.beta_domain"
        }

        envReleaseIp {
            applicationId = "com.tencent.campus.release_ip"
        }

        envReleaseDomain {
            applicationId = "com.tencent.campus"
        }
    }
}
```

注意：

1. 这里我们指定了各个Product Flavors的`applicationId`，这个不是必须的。好处是避免APK包名冲突，允许在同一部手机上同时安装所有Product Flavors生成的APK
2. 这里为`envReleaseIp`和`envReleaseDomain`指定mainifest为`AndroidManifest-release.xml`

# 4.3 添加资源文件

我们已经在上一步使用`sourceSets`指定了不同Product Flavors实际的`res`目录，只需要在相应目录中建立文件即可。

```
res
	values
		env_config.xml
		strings.xml
	...
res-beta-domain
	values
		env_config.xml
res-release-domain
	values
		env_config.xml
res-release-ip
	values
		env_config.xml
res-test
	values
		env_config.xml
AndroidManifest.xml
AndroidManifest-release.xml
```

各个`env_config.xml`中的配置项跟`res\values\env_config.xml`完全一样，根据实际情况设置参数即可。`res-xxx`中的`env_config.xml`会覆盖res中的`env_config.xml`，这是Product Flavors的关键。

新建`AndroidManifest-release.xml`文件，文件内容如下：

```
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
	xmlns:tools="http://schemas.android.com/tools"
    package="com.tencent.campus"
    android:versionCode="1200"
    android:versionName="1.2.0">
    <application
        android:name=".CampusApplication"
        android:allowBackup="true"
        android:hardwareAccelerated="true"
        android:icon="@drawable/logo"
        android:label="@string/app_name"
        android:largeHeap="true">
        <!-- release key -->
        <meta-data
             android:name="TA_APPKEY"
             android:value="AV4ZDV182HN4"
			 tools:node="replace"/>

        <!-- release key -->
        <meta-data
             android:name="APPKEY_DENGTA"
             android:value="0I200UNE171ED9K5"
			 tools:node="replace"/>
	</application>
</manifest>
```

跟资源文件会被覆盖不同，manifest是合并的关系。即`AndroidManifest-release.xml`会跟`AndroidManifest.xml`合并成一个最终的`AndroidManifest.xml`，编译APK时使用这个最终生成的文件。

最终生成的`AndroidManifest.xml`可以在`build\intermediates\manifests\full`中找到，如果合并出错，可以拿这里的文件分析详细错误。

# 4.4 构建并安装

```
// 查看任务列表, 添加了5个Product Flavors, 所以任务那是相当的多
> gradlew tasks

...
Build tasks
-----------
assemble - Assembles all variants of all applications and secondary packages.
assembleAndroidTest - Assembles all the Test applications.
assembleDebug - Assembles all Debug builds.
assembleEnvBetaDomain - Assembles all EnvBetaDomain builds.
assembleEnvBetaDomainDebug - Assembles the DebugEnvBetaDomain build.
assembleEnvBetaDomainDebugAndroidTest - Assembles the android (on device) tests
for the EnvBetaDomainDebug build.
assembleEnvBetaDomainRelease - Assembles the ReleaseEnvBetaDomain build.
assembleEnvDev - Assembles all EnvDev builds.
assembleEnvDevDebug - Assembles the DebugEnvDev build.
...

// 构建所有的Debug APK
> gradlew assembleDebug
...
BUILD SUCCESSFUL

Total time: 6 mins 7.973 secs

// 安装APK
>  gradlew iEDD iETD iEBDD iERID

```

![app-variants](/images/app-variants.png)

注：同时安装了四个APK，它们名字各不相同，原因是为不同的Product Flavors配置了不同的`app_name`。需要的话，完全可以给不同的Product Flavors提供不同的Icon。个人是完全赞同这种做法的，想想项目中服务器配置参数分5种情况，编译个长得一模一样的APK包出来，谁知道到底什么配置。

# 5. 总结
使用Product Flavors的好处是明显的，发布前不必再修改配置了！

1. manifest中不必添加名为`APP_ENV`的meta-data。当前的开发环境相关配置直接跟当前Product Flavor关联
2. 发布前不必修改manifest中名为`APPKEY_DENGTA`和`TA_APPKEY`等SDK的Key。正式Key跟开发用的Key直接跟当前Product Flavor关联

当然，也有以下坏处：

1. 多了几个目录，而且不是缺省结构，略显乱
2. 由于不是缺省项目结构，`build.gradle`配置较麻烦

总的来说，调整后肯定是利大于弊的。另外，建议使用Gralde编译的话最好还是使用缺省的项目结构，"约定优于配置"，可以省不少事。