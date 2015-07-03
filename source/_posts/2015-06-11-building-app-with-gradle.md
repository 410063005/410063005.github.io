---
layout: post
title: "使用Gradle构建Android应用"
date: 2015-06-11 15:13:48 +0800
comments: true
categories: tools
---
本文介绍如何使用Gradle构造Android应用，其最强大的功能在于使用Build flavor支持多版本构建。原文来自[这里](http://www.vogella.com/tutorials/AndroidBuild/article.html)。

<!--more-->

# 简介
## 使用Gradle构建Android应用
Android项目的构建由Gradle构建系统完成。Android项目从结构上可分为两类，一类是2013年前使用Eclipse ADT工具开发的遗留项目结构，另一类是使用新的Gradle构建系统的项目结构。

Gradle经配置后可以支持这两种结构。

> 注意：用于开发Android的Eclipse工具仅支持基于Eclipse的项目结构

## Gradle构建系统的目标
Gradle用于支持创建Android应用时可能出现的复杂场景：

+ 多渠道发布 - 同一个应用可能给不同的公司或客户定制
+ 多APK - 重用部分代码以支持不同设备类型上的多APK
## Gradle的设置
如果直接在Android Studio中创建项目，会自动创建Gradle脚本。Android Studio自带Gradle运行时，所以不必事先安装Gradle。也可以使用Gradle创建的wrapper脚本，它允许直接从命令行运行而不必事先安装Gradle。

# 使用Gradle构建Android应用
想从命令行使用Gradle构建Android项目，从项目根目录执行下面的命令：

```
# build project, runs both the assemble and check task
gradle build

# build project complete from scratch
gradle clean build

# speedup second grandle build by holding it in memory
gradle build --daemon 
```

这个命令会在Gradle构建结果中创建`build`目录。缺省时，Gradle会在`build/outputs/apk`目录中创建两个`.apk`文件。使用`gradle test`构建并在JVM上运行单元测试。使用`gradle connectedCheck`构建并在Android设备上运行instrumented测试。
# 构建不同版本的Android应用
## 构建类型
Android缺省使用两种构建类型：debug和release类型。它们也可以在`build.gradle`中进行不同的配置。
## 定义不同的build flavors
Gradle构建系统也可以管理不同的构建类别。某些情况下，你可能会将应用分成不同类别，Gradle构建可以完成这项工作！比如你可以为不同的设备类型定义不同的构建类别，平板之类的。另一种使用场景就是可以创建应用的免费版本和付费版本。

通过在应用的`build.gradle`文件中使用productFlavor方法，可以添加不同的产品类别：

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

完整的`build.gradle`看起来是这样：

```
apply plugin: 'com.android.application'

android {
    compileSdkVersion 22
    buildToolsVersion "22.0.1"

    defaultConfig {
        applicationId "com.exam.gradleexamples"
        minSdkVersion 19
        targetSdkVersion 22
        versionCode 1
        versionName "1.0"
    }
    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        }
    }

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
}

dependencies {
    compile fileTree(dir: 'libs', include: ['*.jar'])
    compile 'com.android.support:appcompat-v7:22.0.0'
    testCompile 'junit:junit:4.+'
} 
```

定义新的flavors后，可以在Android Studio的Build Variants视图中选择这些flavor。

![build variants](http://www.vogella.com/tutorials/AndroidBuild/images/xbuild_variants_view.png.pagespeed.ic.32KbXR7Y-c.png)
## 为build flavors提供不同的资源
想为不同flavor定义不同的行为，需要在`app/src/`下为已定义的flavors创建适当的目录。

这个例子中，`main`目录中的`string.xml`文件会被`free`或`paid`目录中的`string.xml`覆盖。这样就能在不同的版本中使用不同的字符串。

特定flavor的资源会覆盖主资源，比如你在flavor提供不同的app icon，则构建系统会在该flavor对应的版本中选择这个icon。
## 为build flavors提供不同的代码
代码不会像资源文件那样直接被替换，而是合并。举例来说，你不能在`app/main/java/`目录中有一个`com.example.MainActivity`，而在另一个flavor中有另一个不同的实现。如果你试着这么做，会报类重复的错误。

You can still provide different implementations. Avoid creating the same class in your main source and deploy the class in each flavor. The following screenshot demontrates that.(注：没太明白什么意思)
# 练习：使用不同的flavors构建Android应用
## 目标
创建一个有free和paid构建类别的项目。为简化练习，这里我们只在flavor中替换字符串。
## 创建Android应用
+ 使用Blank Activity template创建新的项目
+ 在`app/build.gradle`文件中定义两个新的flavors，分别为"free"和"paid"
+ 为"free"和"paid"创建对应的目录
+ 将`main`目录中的`string.xml`文件拷贝到"free"和"paid"目录中相应的位置
+ 分别将`string.xml`中的`hello_world`修改为"Free World!"和"Paid World!"

```
<!-- should be located in /app/src/free/res/values/strings.xml -->
<resources>
    <string name="app_name">GradleExamples</string>

    <string name="hello_world">Free World! </string>
    <string name="action_settings">Settings</string>
</resources> 

<!-- should be located in /app/src/paid/res/values/strings.xml -->
<resources>
    <string name="app_name">GradleExamples</string>

    <string name="hello_world">Paid World! </string>
    <string name="action_settings">Settings</string>
</resources>
```
## 验证
在Android Studio的Build Variants视图选择`freeDebug`作为Build Variant并运行应用。
## Gradle命令行构建
使用`./gradlew build`命令构建全部的类别。
# Eclipse项目迁移到Gradle
## 添加Gradle文件
想要给基于Eclipse的Android项目添加Gradle构建支持，只需要将如下`build.gradle`添加到项目的根目录中。

```
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:1.0.0'
    }
}
apply plugin: 'com.android.application'


android {
     lintOptions {
          abortOnError false
      }

    compileSdkVersion 22
    buildToolsVersion "21.1.2"

    defaultConfig {
      targetSdkVersion 22
    }

    sourceSets {
        main {
            manifest.srcFile 'AndroidManifest.xml'
            java.srcDirs = ['src']
            resources.srcDirs = ['src']
            aidl.srcDirs = ['src']
            renderscript.srcDirs = ['src']
            res.srcDirs = ['res']
            assets.srcDirs = ['assets']
        }

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
## 将Eclipse项目导入到Android Studio
一旦给基于Eclipse的Android项目添加了Gradle文件，就可以将其导入到Android Studio中。步骤为：File -> Import，然后选择Gradle文件所在的那个目录。
