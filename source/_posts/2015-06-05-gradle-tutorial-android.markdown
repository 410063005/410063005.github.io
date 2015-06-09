---
layout: post
title: "Gradle学习笔记(Android)"
date: 2015-06-05 11:53:42 +0800
comments: true
categories: tools android
published: true
---
原文来自[这里](http://tools.android.com/tech-docs/new-build-system/user-guide)。基本上是对其翻译，挑选关键内容以及并加上自己的一些理解。

# 介绍
新编译系统的目标：

+ 代码和资源易重用
+ 易创建应用的多版本
+ 构建过程易配置
+ 良好的IDE集成

## 为什么选择Gradle
Gradle是种高级的构建系统，也是个允许通过插件创建自定义逻辑的构建工具。以下特征让我们选择Gradle：

+ DSL用于描述和操作构建逻辑
+ 构建文件基于Groovy，允许使用DSL描述性元素和使用代码操作DSL元素来提供自定义逻辑
+ 内置的依赖管理(Maven和Ivy)
+ 非常灵活。允许最佳实践，但不限制使用方式
+ 插件可以暴露自己的DSL和API用于构建文件
+ 良好的工具API供IDE集成

# 安装要求
+ Gradle 1.10以上，Gradle插件0.11.1以上
+ SDK with Build Tools 19.0.0以上

# 基本项目
Gradle项目在根目录的`build.gradle`文件中描述其构建。

## 简单构建文件
最简单的Java项目使用以下`build.gradle`，这里使用Java插件，它跟Gradle一起安装。这个插件供Java项目构建和测试。

    apply plugin: 'java'
    
最简单的Android项目使用以下`build.gradle`：

```
buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath 'com.android.tools.build:gradle:0.11.1'
    }
}

apply plugin: 'android'

android {
    compileSdkVersion 19
    buildToolsVersion "19.0.0"
}
```

这个Android构建脚本中有三个主要部分：

`buildscript { ... }` 驱动构建。buildscript声明使用Maven中央库，对Manven库的Gradle插件(版本为v0.11.1)有依赖。注意：这里的依赖仅影响运行构建的代码而不影响项目。项目需要声明自己的库和依赖。

然后是类似之前Java插件的`android`插件。

最后，`android { ... }`设置所有的Android构建参数。它是Android DSL的入口点。缺省时只需要配置编译目标(`compileSdkVersion`)以及构建工具版本(`buildToolsVersion`)。编译目标跟旧的编译系统中`project.properties`的`target`属性是一样的。可以使用整型值(api level)或字符串给新的属性赋值。

**重要：** 你应该只使用`android`插件。同时还使用`java`插件会引起构建错误。

**注意：** 在新的构建系统中同样也需要一个`local.properties`文件，用于`sdk.dir`属性设置SDK的位置。另外，也可以设置`ANDROID_HOME`环境变量。两种方式没有区别。

## 项目结构
上面这种最基本的构建文件需要使用缺省的目录结构。Gradle遵循约定优于配置的原则，尽可能提供合理的默认值。

基本的项目中两个称为"source sets"的组件，分别是源码和测试代码。分别位于：

+ src/main/
+ src/androidTest/

每个目录下存放的是源码组件。对Java和Android插件，Java代码和Java资源文件分别位于：

+ java/
+ resources/

对Android插件，还有些额外的文件：

+ AndroidManifest.xml
+ res/
+ assets/
+ aidl/
+ rs/
+ jni/

注意：`src/androidTest/AndroidManifest.xml`不是必须的，这个文件自动生成。

当缺省的项目结构不满足要求时，可以重新配置。根据Gradle文档，可以使用如下方式重新配置`sourceSets`：

```
sourceSets {
    main {
        java {
            srcDir 'src/java'
        }
        resources {
            srcDir 'src/resources'
        }
    }
}
```

注意：`srcDir`会添加指定的目录到已有的源码目录中(Gradle文档中没有提到这点，但确实是这样)。想替换掉缺省的源码目录，可以使用`srcDirs`，这个属性接收一组路径。下面演示了这种使用方式：

```
sourceSets {
    main.java.srcDirs = ['src/java']
    main.resources.srcDirs = ['src/resources']
}
```

更多详细信息参考Gradle文档中[关于Java插件的部分](http://gradle.org/docs/current/userguide/java_plugin.html)。

Android插件使用类似的语法，但由于是我们自己的`sourceSets`，所以应该对`android`对象执行这些操作。下面是例子，使用旧的项目结构，并将`androidTest`这个sourceSet映射到`tests`目录：

```
android {
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

        androidTest.setRoot('tests')
    }
}
```

注意：由于旧的结构将所有的源码(java, aidl, renderscript以及java resources)放到同一个目录，所以需要将sourceSet中所有这些组件重新映射到同一个`src`目录。

注意：`setRoots()`将整个sourceSet(以及其子目录)移动到新的目录。这里会将`src/androidTest/*`移动到`tests/*`。这里Android特有的，Java项目的sourceSets并不支持。

## 构建任务
### 通用任务
在构建文件中使用插件会自动创建一些构建任务。Java插件和Android插件都如此。

通常的tasks包括：

+ `assemble` - 编译输出
+ `check` - 运行所有校验
+ `build` - 运行`assemble`和`check`
+ `clean` - 清除编译输出

`assemble`，`check`和`build`并没有做任何实际工作。它们是anchor tasks，用于插件来添加实际task来完成工作。

上述anchor tasks允许你无论是什么类型的项目、无论什么插件，都可以使用相同的tasks。比如，使用`findbugs`插件会创建新的task并让`check`依赖于它，无论`check`何时被调用这个新的task都会被调用。

在命令行中可以通过如下命令得到high level tasks：

```
gradle tasks
```

想得到全部的tasks，运行以下命令：

```
gradle tasks --all
```

注意：Gradle自动监视task的输入和输出。重复运行`build`时Gradle会输出`UP-TO-DATE`，指已是最新。这样可以避免没必要地重新构建。

### Java任务
Java插件主要创建两种任务，它们是anchor task的主要依赖：

+ `assemble`
 + `jar` 这个task创建output
+ `check`
 + `test` 这个task运行测试

`jar` task 本身直接和间接依赖于其他 tasks，比如依赖`classes`以编译Java代码。`testClasses`用于编译测试代码，但直接调用这个task没什么意义，`test`依赖于`testClasses`和`classes`。 

通常，你会调用`assemble`或`check`，而忽略其他tasks。可以在[这里](http://gradle.org/docs/current/userguide/java_plugin.html)找到完整的tasks描述。

### Android任务
Android插件使用相同的惯例，保持跟其他插件兼容。Android插件添加以下anchor task：

+ `assemble` - 编译输出
+ `check` - 运行所有校验
+ `connectedCheck` - 运行所有需要有已连接设备或模拟器的检验
+ `deviceCheck` - 使用API连接远程设备(用于持续集成服务器)
+ `build` - 运行`assemble`和`check`
+ `clean` - 清除编译输出

Android插件有必要添加新的anchor tasks，以便能在没有连接设备的情况下运行所有常规校验。注意`build`并不依赖`deviceCheck`或`connectedCheck`。

Android项目至少有两个输出：一个debug APK和一个release APK。每种都有自己的anchor tasks，以独立构建：

+ `assemble`
 + `assembleDebug`
 + `assembleRelease`
 
它们都依赖于其他构建APK必需的tasks。`assemble`依赖于`assembleDebug`和`assembleRelease`。

小贴士：Gradle支持命令行中的驼峰命名法。比如：`gradle aR`跟`gradle assembleRelease`是一样的，只要没有其他的task能跟`aR`匹配。

`check` anchor task有其自己的依赖：

+ `check`
 + `lint`
+ `connectedCheck`
 + `connctedAndroidTest`
 + `connectedUiAutomatorTest` (尚未实现)
+ `deviceCheck` This depends on tasks created when other plugins implement test extension points.

最后，Android插件还针对所有构建类型(`debug`, `release`,`test`)创建了用于安装和卸载的任务，只要它们可被安装(需要签名)。

## 基本自定义
Android插件提供DSL用于从构建系统中直接配置大部分参数。

### Manifest条目
通过DSL可以配置如下Manifest条目：

+ minSdkVersion
+ targetSdkVersion
+ versionCode
+ versionName
+ applicationId (实际就是包名, 见 [ApplicationId versus PackageName](http://tools.android.com/tech-docs/new-build-system/applicationid-vs-packagename))
+ 测试程序的package name
+ Instrumentation test runner

示例：

```
android {
    compileSdkVersion 19
    buildToolsVersion "19.0.0"

    defaultConfig {
        versionCode 12
        versionName "2.0"
        minSdkVersion 16
        targetSdkVersion 16
    }
}
```

`android`元素内的`defaultConfig`元素定义了所有的配置。

前一个版本的Android插件使用 packageName 来配置manifest中的'packageName'属性。从Gradle插件v0.11.0开始，使用 applicationId 来配置manifest中的'packageName'属性。改名主要是为了消除应用包名(即ID)跟Java包名之间的歧义。

在构建文件内描述各种属性的威力在于它是动态的。比如，可以从文件读取或使用某种自定义逻辑计算出version name：

```
def computeVersionName() {
    ...
}

android {
    compileSdkVersion 19
    buildToolsVersion "19.0.0"

    defaultConfig {
        versionCode 12
        versionName computeVersionName()
        minSdkVersion 16
        targetSdkVersion 16
    }
}
```

注意：不要使用可能跟当前范围内已有的 getters 的名字冲突的函数名。比如，`defaultConfig {...}`调用`getVersionName()`，将会自动使用`defaultConfig.getVersionName()`而不是自定义`getVersionName()`函数。

如果没有通过DSL定义属性，将使用缺省值。见下表

Property Name|Default value in DSL object|Default value
-------------|---------------------------|-------------
`versionCode`|-1|如果manifest中有，则使用manifest中的值
`versionName`|null|如果manifest中有，则使用manifest中的值
`minSdkVersion`|-1|如果manifest中有，则使用manifest中的值
`targetSdkVersion`|-1|如果manifest中有，则使用manifest中的值
`applicationId`|null|如果manifest中有，则使用manifest中的值
`testApplicationId`|null|applicationId + ".test"
`testInstrumentationRunner`|null|android.test.InstrumentationTestRunner
`signingConfig`|null|null
`proguardFile`|N/A(set only)|N/A(set only)
`proguardFiles`|N/A(set only)|N/A(set only)

第2列的值在你想在构建脚本中使用自定义逻辑查询这些属性时非常重要，比如，你可以这样写：

```
if (android.defaultConfig.testInstrumentationRunner == null) {
    // assign a better default...
}
```

如果返回null，将在构建时使用第3列的实际缺省值替换。但DSL元素不包含这个缺省值所以不能查询。(注：原文是 If the value remains null, then it is replaced at build time by the actual default from column 3, but the DSL element does not contain this default value so you can't query against it.
This is to prevent parsing the manifest of the application unless it’s really needed. )

### 构建类型
缺省时，Android插件会自动将项目设置为能同时编译debug和release版本。这两种构建类型生成不同版本的应用，它们在非开发手机上的调试能力及APK如何被签名上所区别。

debug版本使用已知的用户名/密码(不必在构建时提示输入)自动创建的key/certificate来签名。构建过程中并不给自动release版本的APK签名，需要后续操作。

通过一个称为`BuildType`的对象进行这些配置。缺省时，两个实例被创建，一个是`debug`，一个是`release`。Android插件允许自定义这两个对象，也允许创建别的构建类型。使用 `buildTypes` DSL容器创建：

```
android {
    buildTypes {
        debug {
            applicationIdSuffix ".debug"
        }

        jnidebug.initWith(buildTypes.debug)
        jnidebug {
            packageNameSuffix ".jnidebug"
            jniDebuggable true
        }
    }
}
```

上面代码达到以下目的：

+ 配置缺省的`debug`构建类型：
 + 将它的包名设置为`<app applicationId>.debug`，允许在一台设备上同时安装debug和release版本的APK
+ 使用debug实例创建一个名为`jnidebug`的新的构建类型，这个新实例使用`debug`相同的配置
+ 继续配置`jnidebug`，打开`jniDebuggable`开关，并添加不同的后缀

调用`initWith()`或使用闭包配置，在`buildTypes`容器内创建新的构建类型跟使用新的元素一样容易。

下面是属性列表及缺省值：

Property Name|Default value for debug|Default value for release/other
-------------|---------------------------|-------------
`debuggable`|true|false
`jniDebuggable`|false|false
`renderscriptDebuggable`|false|false
`renderscriptOptimLevel`|3|3
`applicationIdSuffix`|null|null
`versionNameSuffix`|null|null
`signingConfig`|android.signingConfigs.debug|null
`zipAlignEnabled`|false|true
`minifyEnabled`|false|false
`proguardFile`|N/A(set only)|N/A(set only)
`proguardFiles`|N/A(set only)|N/A(set only)

除了这些属性，构建类型还能提供代码和资源。对每个构建类型，会有相应的 `sourceSet` 创建，其缺省位置是`src/<buildtypename>`。这意味着构建类型的名字不能是main或androidTest，且必须唯一。

跟其他的source sets一样，构建类型的source set也可以使用新的位置：

```
android {
    sourceSets.jnidebug.setRoot('foo/jnidebug')
}
```

另外，对每个构建类型，会有一个`assemble<BuildTypeName>` task被创建。

之前提到过`assembleDebug`和`assembleRelease`，这就是它们的由来。当`debug`和`release`构建类型被创建后，会自动创建对应的`assemble` task。

上面的`build.gradle`代码片断同样会生成一个`assembleJnidebug` task，而`assemble`会像依赖`assembleDebug`及`assembleRelease`一样依赖`assembleJnidebug`。

小贴士：记住，可以输入`gradle aJ`来运行`assembleJnidebug` task。

可能的使用场景：

+ 仅在debug模式下才能使用的权限，而release版本中不行
+ 用于debugging的自定义实现
+ debug模式时提供不同的资源(比如当resource value跟signing certificate绑定时)

BuildType的code和resources有以下使用方式：

+ 合并manifest到app的manifest
+ The code acts as just another source folder
+ resources覆盖main resources，替换已存在的值

### 签名配置
给应用签名需要以下条件：

+ keystore
+ keystore password
+ key alias name
+ key password
+ store type

文件位置，key name以及password和 store type一起组成Signing Configuration(type SigningConfig)

缺省时，有一个`debug`配置用于使用debug keystore，它使用已知的password以及一个使用已知密码的key。debug keystore位于`$HOME/.android/debug.keystore`，不存在时自动创建。

`debug`构建类型自动使用`debug` SigningConfig。也可以创建其他的配置，使用修改缺省内置的配置。通过`signingConfigs` DSL容器完成：

```
android {
    signingConfigs {
        debug {
            storeFile file("debug.keystore")
        }

        myConfig {
            storeFile file("other.keystore")
            storePassword "android"
            keyAlias "androiddebugkey"
            keyPassword "android"
        }
    }

    buildTypes {
        foo {
            debuggable true
            jniDebuggable true
            signingConfig signingConfigs.myConfig
        }
    }
}
```

上述代码修改debug keystore的为项目根目录。这会自动影响任何使用它的构建类型。这里还创建了一个新的 Signing Config 以及一个新的将用到该 Signing Config的构建类型。

注意：只有缺省位置的debug keystores会自动被创建。修改debug keystore的位置后，它不会自动被创建。使用不同的名字创建 一个会用到debug keystore的SigningConfig，会导致debug keystore被自动创建。In other words, it’s tied to the location of the keystore, not the name of the configuration.

注意：keystores的位置通常是相对于项目的根目录，也可以是绝对路径。但不建议使用绝对路径(除非使用自动生成的debug keystore)。

**注意：当把这些文件提交到版本控制系统后，你可能不想将密码保存在文件中。下面这个来自Stack Overflow的帖子演示如何读取控制台输入或从环境变量。[参考](http://stackoverflow.com/questions/18328730/how-to-create-a-release-signed-apk-file-using-gradle)。我们后续会在这份指导中提供更多的详细信息。**

### ProGuard
ProGuard is supported through the Gradle plugin for ProGuard version 4.10。ProGuard插件会自动被使用，如果将`minfyEnabled`属性设置为true，构建类型将会运行ProGuard，相应的tasks也会被创建。

```
android {
    buildTypes {
        release {
            minifyEnabled true
            proguardFile getDefaultProguardFile('proguard-android.txt')
        }
    }

    productFlavors {
        flavor1 {
        }
        flavor2 {
            proguardFile 'some-other-rules.txt'
        }
    }
}
```

不同版本使用各自的规则文件。有两个缺省的规则文件：

+ proguard-android.txt
+ proguard-android-optimize.txt

它们包含在SDK中。使用`getDefaultProguardFile()`会返回完整的文件路径。除是否开启优化外，它们完全相同。

### 压缩资源
也可以在构建时去掉无用的资源。更多信息参见[Resources压缩](http://tools.android.com/tech-docs/new-build-system/resource-shrinking)。

# 依赖管理和库项目/多项目设置
Gradle项目会依赖其他组件。这些组件可能是外部的二进制包或其他Gradle项目。

## 二进制依赖
### 本地包
当依赖外部库时，需要给`compile`配置添加依赖：

```
dependencies {
    compile files('libs/foo.jar')
}

android {
    ...
}
```

注意：`dependencies`DSL元素是标准的Gradle API，不属于`android`元素。

`compile`配置用于编译主应用。 `compile`指定的二进制包会添加到编译时的classpath时且会打包到最终的APK中。其他可能需要依赖的配置包括：

+ `compile` - 主应用
+ `androidTestCompile` - 测试应用
+ `debugCompile` - debug构建类型
+ `releaseCompile` - release构建类型

不可能构建一个不跟构建类型关联的APK，所以APK通常使用`compile`和`<buildtype>Compile`配置。创建新的构建类型将基于它的名字自动创建新的配置。

这个特性在某些时候非常有用，比如在debug版本需要使用自定义库(比如上报异常)而release版本时，或者依赖不同版本的库时。

### 远程文件
Gradle可以从Maven和Ivy库下载文件。首先，必须添加相应的仓库。然后，必须以Maven或Ivy支持的方式声明所依赖的文件。

```
repositories {
    mavenCentral()
}


dependencies {
    compile 'com.google.guava:guava:11.0.2'
}

android {
    ...
}
```

注意：`mavenCentral()`是指定仓库URL的快捷方式。Gradle支持远程和本地仓库。Gradle will follow all dependencies transitively. 即，某个项目依赖又依赖别的文件时，这个文件也会一起被下载。(This means that if a dependency has dependencies of its own, those are pulled in as well.)

关于设置依赖的更多信息，可以参考[Gradle user guide](http://gradle.org/docs/current/userguide/artifact_dependencies_tutorial.html)，和[DSL文档](http://gradle.org/docs/current/dsl/org.gradle.api.artifacts.dsl.DependencyHandler.html)。

## 多项目设置
通过设置'multi-project'，Gradle项目也可以依赖其他Gradle项目。所有项目作为某个指定的根目录的子目录就可以支持multi-project。

比如，下面的目录结构：

```
MyProject/
 + app/
 + libraries/
    + lib1/
    + lib2/
```

我们指定3个项目。Gradle可以通过以下名字引用它们：

```
:app
:libraries:lib1
:libraries:lib2
```

每个项目可以有自己的build.gradle来指定如何构建。另外，根目录下有个叫`settings.gradle`的文件。完整结构如下：

```
MyProject/
 | settings.gradle
 + app/
    | build.gradle
 + libraries/
    + lib1/
       | build.gradle
    + lib2/
       | build.gradle
```

settings.gradle文件内容非常简单：

```
include ':app', ':libraries:lib1', ':libraries:lib2'
```

上述内容定义哪个目录是Gradle项目。

`:app`项目依赖于库项目(在这里是libraries下的lib1)，通过下面的方式声明依赖：

```
dependencies {
    compile project(':libraries:lib1')
}
```

关于multi-project可以参考[这里](http://gradle.org/docs/current/userguide/multi_project_builds.html)

## 库项目
在上面的multi-project设置中，`:libraries:lib1`和`:libraries:lib2`可以是Java项目，而`:app`这个Android项目会使用它们编译输出的jar文件。

如果你想共享需要访问Android API的代码或使用Android风格的资源，则库项目不能是常规的Java项目，它们必须是Android Library项目。

### 创建库项目
库项目跟常规的Android项目有少量不同。构建库跟构建应用不同，所以使用不同的插件。在内部这两个插件共享大部分代码(由相同的`com.android.tools.build.gradle jar`提供)。

```
buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath 'com.android.tools.build:gradle:0.5.6'
    }
}

apply plugin: 'android-library'

android {
    compileSdkVersion 15
}
```

上面的代码创建一个库项目，使用API level 15的SDK来编译。库项目使用相同的方式处理SourceSets以及依赖，也可使用相同的方式自定义。

### 常规项目跟库项目的差异
库项目的主要输出是`.aar`包(代表Android archive)。它由编译后的代码(jar文件或.so文件)以及资源文件(manifest, res, assets)组成。库项目也可以产生一个测试apk用于独立测试库。

库项目使用相同的anchor tasks(`assembleDebug`和`assembleRelease`)，所以在命令行编译库项目跟常规项目没有任何区别。除此之外，库项目跟应用项目完全一样。它们也有构建类型和product flavors，也可以生成不同版本的aar文件。

注意：构建类型的大部分配置都不能应用于库项目。但是你可以根据库项目是被其他项目使用还是在测试，使用自定义的`sourceSet`来修改库的内容。

### Referencing a Library
引用库跟引用其他项目一样：

```
dependencies {
    compile project(':libraries:lib1')
    compile project(':libraries:lib2')
}
```

注意：如果你有多个库项目的话，那么这里的顺序非常重要。跟旧的编译系统中`project.properties`文件中依赖项的顺序很重要类似。

### 发布Library
缺省时只发布库的`release variant`版本。这个版本用于其他项目引用本库，其他版本会被忽略。这是Gradle目前的限制，很快就被解除。

你可以控制发布哪个版本：

```
android {
    defaultPublishConfig "debug"
}
```

注意这里的publishing配置引用完整的variant name。没有其他flavor时只可使用release和debug。如果想使用flavor来修改缺省的发布版本，可以这么写：

```
android {
    defaultPublishConfig "flavor1Debug"
}
```

也可以一次发布所有版本的库。We are planning to allow this while using a normal project-to-project dependency (like shown above), but this is not possible right now due to limitations in Gradle (we are working toward fixing those as well).缺省不开启这个功能，使用如下代码打开：

```
android {
    publishNonDefault true
}
```

应该明白发布多个版本指的是发布多个aar文件，而不是一个aar文件包含多个版本。每个aar文件包含单个版本。发布aar表示让其作为Gradle项目的输出文件。这个文件既可以发布到Maven库，也可以作为另的项目的依赖。

Gradle has a concept of default" artifact. This is the one that is used when writing:

```
compile project(':libraries:lib2')
```

依赖另一个项目的发布版本时，需要指定使用哪个：

```
dependencies {
    flavor1Compile project(path: ':lib1', configuration: 'flavor1Release')
    flavor2Compile project(path: ':lib1', configuration: 'flavor2Release')
}

```

**重要：** Note that the published configuration is a full variant, including the build type, and needs to be referenced as such. 

**重要：** 发布非缺省版本时，Maven发布插件会将其他版本输出额外的包(带修饰名)。它跟发布到Maven库不完全兼容。要么发布一个版本到Maven库，要么为内部项目依赖发布所有版本。

# 测试
构建测试程序也已集成到项目中。没必要再建立专门的独立测试项目。

## 单元测试
Android Studio v1.1中开始支持单元测试(作为试验功能)，具体见[这里](http://tools.android.com/tech-docs/unit-testing-support)。

Android Studio v1.2中正式支持单元测试。 []()

下面部分讲的是"instrmentation test"，它需要在设备或模拟器上运行，也需要构建一个用于测试的APK。

## Instrumentation测试
## 基础
前面提到过，`main` sourceSet后面是 `androidTest` sourceSet，缺省位于`src/androidTest`。这个sourceSet会创建一个用于测试的APK。它可以包含单元测试，instrumentation测试(以后还包括uiautomator测试)。

测试应用manifest中的`<instrmentation>`节点自动生成，但你也可以创建`src/androidTest/AndroidManifest.xml`文件以增加其他组件。

可以为测试应用配置少量参数：

+ `testPackageName`
+ `testInstrumentationRunner`
+ `testHandleProfiling`
+ `testFunctionalTest`

如前面看到的那样，通过`defaultConfig`对象配置：

```
android {
    defaultConfig {
        testPackageName "com.test.foo"
        testInstrumentationRunner "android.test.InstrumentationTestRunner"
        testHandleProfiling true
        testFunctionalTest true
    }
}
```

即使通过`defaultConfig`或Build Type对象配置， 测试应用的manifest中`instrmentation`节点的`targetPackage`属性还是会自动使用被测应用的包名填充。另外，`sourceSet`可以有自己的依赖。缺省时，应用以及它的依赖会添加到测试应用的路径，还可以通过下面的方式扩展

```
dependencies {
    androidTestCompile 'com.google.guava:guava:11.0.2'
}
```

测试应用使用`assembleTest`构建。`assemble`并不依赖于它。`assembleTest`在测试运行时自动执行。目前只会测试一个Build Type。缺省时是`debug` Build Type，也可以重新配置：

```
android {
    ...
    testBuildType "staging"
}
```

## 运行测试
前面提到过，需要在连接的设备上使用`connectedCheck`这个anchor task发起检验。它依赖于`androidTest`。`connectedCheck`执行以下操作：

+ 确保app和test app被构建(分别依赖于`assembleDebug`和`assembleTest`)
+ 安装app和test app
+ 运行测试
+ 卸载app和test app

如果一个以上的设备连接，所有的测试将在已连接的设备上同时运行。如果任一个设备上的测试失败，构建将失败。所有的测试结果以XML文件形式保存在`build/androidTest-results`。(跟常规的JUnit测试结果保存在build/test-results类似)。可以进行配置：

```
android {
    ...

    testOptions {
        resultsDir = "$project.buildDir/foo/results"
    }
}
```

使用`Project.file(String)`对`android.testOptions.resultsDir`进行求值。

## 测试Android库
测试Android库项目跟应用完全一样。唯一不同的是整个库以及其依赖会自动作为test app的库依赖。结果是测试APK不仅包含自己的代码，还包括库本身以及其依赖。库项目的manifest合并到test app的manifest(就跟任何项目引用这个库一样)。

`androidTest`仅安装和卸载test APK(因为没有其他APK可安装)。

## 测试报告
运行单元测试，Gradle输出HTML报告以便查看结果。Android插件构建基于此，并从所有已连接设备统计HTML报告。

### 单个项目
缺省位置是`build/reports/androidTests`。跟JUnit的`build/reports/tests`类似。其他报告通常在`build/reports/<plugin>/`。可以对位置进行配置：

```
android {
    ...

    testOptions {
        reportDir = "$project.buildDir/foo/report"
    }
}
```

报告统计了不同设备的运行结果。

### 多项目
在既有应用也有库的多项目中，当同时运行所有测试时，在同一个报告中统计所有测试结果可能很有用。可以使用Gradle包中的另一个插件实现这一目的：

```
buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath 'com.android.tools.build:gradle:0.5.6'
    }
}

apply plugin: 'android-reporting'
```

在根项目中的build.gradle使用该配置。然后在根项目中，执行以下命令：

```
gradle deviceCheck mergeAndroidReports --continue
```

注意：`--continue`参数保证运行所有测试，就算某个测试失败。如果不使用这个参数，第一个运行失败的测试会中断整个过程。

## Lint报告
从v0.7.0开始，可以为某个特定的版本或所有版本运行lint。也可以通过添加如下`lintOptions`(下面显示了所有的可选项，而通常只需修改其中的某几个)：

```
android {
    lintOptions {
        // set to true to turn off analysis progress reporting by lint
        quiet true
        // if true, stop the gradle build if errors are found
        abortOnError false
        // if true, only report errors
        ignoreWarnings true
        // if true, emit full/absolute paths to files with errors (true by default)
        //absolutePaths true
        // if true, check all issues, including those that are off by default
        checkAllWarnings true
        // if true, treat all warnings as errors
        warningsAsErrors true
        // turn off checking the given issue id's
        disable 'TypographyFractions','TypographyQuotes'
        // turn on the given issue id's
        enable 'RtlHardcoded','RtlCompat', 'RtlEnabled'
        // check *only* the given issue id's
        check 'NewApi', 'InlinedApi'
        // if true, don't include source code lines in the error output
        noLines true
        // if true, show all locations for an error, do not truncate lists, etc.
        showAll true
        // Fallback lint configuration (default severities, etc.)
        lintConfig file("default-lint.xml")
        // if true, generate a text report of issues (false by default)
        textReport true
        // location to write the output; can be a file or 'stdout'
        textOutput 'stdout'
        // if true, generate an XML report for use by for example Jenkins
        xmlReport false
        // file to write report to (if not specified, defaults to lint-results.xml)
        xmlOutput file("lint-report.xml")
        // if true, generate an HTML report (with issue explanations, sourcecode, etc)
        htmlReport true
        // optional path to report (default will be lint-results.html in the builddir)
        htmlOutput file("lint-report.html")

   // set to true to have all release builds run lint on issues with severity=fatal
   // and abort the build (controlled by abortOnError above) if fatal issues are found
   checkReleaseBuilds true
        // Set the severity of the given issues to fatal (which means they will be
        // checked during release builds (even if the lint target is not included)
        fatal 'NewApi', 'InlineApi'
        // Set the severity of the given issues to error
        error 'Wakelock', 'TextViewEdits'
        // Set the severity of the given issues to warning
        warning 'ResourceAsColor'
        // Set the severity of the given issues to ignore (same as disabling the check)
        ignore 'TypographyQuotes'
    }
}
```

# Build Variants
新构建系统的一个目标就是允许创建同一个应用的不同版本。

这里有两种使用场景：

1. 同一个应用的不同版本。比如，分免费/演示版本和专业付费版本。
2. Same application packaged differently for multi-apk in Google Play Store. 更多信息参见[这里](http://developer.android.com/google/play/publishing/multiple-apks.html)

目标是从同一个项目输出不同的APK，而需要使用同一库项目的两个项目。

## Product flavors
product flavor定义同一项目构建的不同版本的应用。一个项目可以用不同的flavors，生成不同的应用。

这个新的概念用于解决版本差异很小的问题。问"是否同一个项目"，如果答案是"是"，那flavor可以就是比库项目更好的方式。

Product flavor使用`productFlavors` DSL容器声明：

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

上面代码创建了两个flavor，分别是`flavor1`和`flavor2`。注意，flavor的名字不能跟已存在的Build Type名字，或`androidTest` sourceSet名字冲突。

## Build Type + Product Flavor = Build Variant
前面知道，每个Build Type会产生一个新的APK。Product Flavor也是：每个项目的输出是Build Type和Product Flavor(如果可用的话)的所有组合。每种(Build Type, Product Flavor)组合是一个Build Variant。比如，使用缺省的`debug`和`release` Build Type，上面的例子将产生四个Build Variants：

+ Flavor1 - debug
+ Flavor1 - release
+ Flavor2 - debug
+ Flavor2 - release

没有flavor的项目同样也有Build Variants，但是会使用缺省的flavor/config，没名字，所以Variants列表跟Build Types列表完全一样。

## Product Flavor配置
每个flavor均使用闭包配置：

```
android {
    ...

    defaultConfig {
        minSdkVersion 8
        versionCode 10
    }

    productFlavors {
        flavor1 {
            packageName "com.example.flavor1"
            versionCode 20
        }

        flavor2 {
            packageName "com.example.flavor2"
            minSdkVersion 14
        }
    }
}
```

注意：`android.productFlavors.*`对象类型跟`android.defaultConfig`对象类型一样，均为ProductFlavor。所有它们有相同的属性。

`defaultConfig`提供所有flavors的基本配置，每个flavor可以覆盖这些值。上面的例子中：

+ `flavor1`
 + `packageName`: com.example.flavor1
 + `minSdkVersion`: 8
 + `versionCode`: 20
+ `flavor2`
 + `packageName`: com.example.flavor2
 + `minSdkVersion`: 14
 + `versionCode`: 10
 
通常Build Type配置会覆盖其他配置。比如，Build Type的`packageNameSuffix`会添加到`packageName`。

有些时候可能一个配置既可以在Build Type也可以在Product Flavor中进行。In this case, it’s is on a case by case basis.

For instance, `signingConfig` is one of these properties.
This enables either having all release packages share the same SigningConfig, by setting `android.buildTypes.release.signingConfig`, or have each release package use their own SigningConfig by setting each `android.productFlavors.*.signingConfig` objects separately.

## SourceSets和依赖
跟Build Types类似，Product Flavors也可以通过sourceSets提供代码和资源。上面的例子创建以下四个sourceSets：

+ `android.sourceSets.flavor1` - 位于 `src/flavor1/`
+ `android.sourceSets.flavor2` - 位于 `src/flavor2/`
+ `android.sourceSets.androidTestFlavor1` - 位于 `src/androidTestFlavor1/`
+ `android.sourceSets.androidTestFlavor2` - 位于 `src/androidTestFlavor2/`

这些sourceSets以及`android.sourceSets.main`和Build Type的sourceSet用于构建APK。以下规则用于处理所有用于构建单个APK的sourceSets：

+ 所有源码(`src/*/java`)用于生成单个输出
+ 所有manifest用于合并到单个manifest。这个特点允许不同Product Flavor有不同组件或权限(跟Build Type类似)。
+ 所有资源(Android res和assets)遵守覆盖优先级，Build Type覆盖Product Flavor，而Product Flavor又可覆盖`main` sourceSet。
+ 每个Build Variant从相应的资源生成自己的R类文件(以及其他的自动生成代码)

最后，跟Build Types很像，Product Flavors也可以有自己的依赖。比如，如果flavor用于生成有广告的免费app和无广告的付费app，那其中有个flavor会依赖Ads SDK，而另一个则不依赖。

```
dependencies {
    flavor1Compile "..."
}
```

这个例子中，`src/flavor1/AndroidManifest.xml`很可能需要包含internet权限。每个variant还会产生其他的sourceSet：

+ `android.sourceSets.flavor1Debug` - 位于 `src/flavor1Debug/`
+ `android.sourceSets.flavor1Release` - 位于 `src/flavor1Release/`
+ `android.sourceSets.flavor2Debug` - 位于 `src/flavor2Debug/`
+ `android.sourceSets.flavor2Release` - 位于 `src/flavor2Release/`

它们的优先级比build type sourceSets高，允许在variant级别上配置。

## 构建和任务
之前看到，每个Build Type创建自己的`assemble<name>`任务，但Build Variants是Build Type和Product Flavor的组合。使用Product Flavors时，会创建多个 assemble-task。它们是：

1. assemble<Variant Name> - 直接构建单个variant，比如`assembleFlavor1Debug`
2. assemble<Build Type Name> - 构建某个Build Type的所有APK，比如`assembleDebug`将构建`Flavor1Debug`和`Flavor2Debug`
3. assemble<Product Flavor Name> - 构建某个Flavor的所有APK，比如`Flavor1Debug`和`Flavor1Release`

而`assemble`将构建所有可能的variant

## 测试
测试multi-flavors项目跟测试简单工程类似。`androidTest` sourceSet用于所有flavor的通用测试，每个flavor可以有自己的测试。前面提到，用于测试每个flavor的sourceSets将创建：

+ `android.sourceSets.androidTestFlavor1` - 位于 `src/androidTestFlavor1/`
+ `android.sourceSets.androidTestFlavor2` - 位于 `src/androidTestFlavor2/`

类似的，它们也可以有自己的依赖：

```
dependencies {
    androidTestFlavor1Compile "..."
}
```

可以通过`deviceCheck`运行测试，或者，当flavor被使用时`androidTest`将作为一个anchor task。每个flavor可以运行自己的测试：`androidTest<VariantName>`。比如：

+ `androidTestFlavor1Debug`
+ `androidTestFlavor2Debug`

类似地，每个variant也有各自的test apk构建任务和install/uninstall任务：

+ `assembleFlavor1Test`
+ `installFlavor1Debug`
+ `installFlavor1Test`
+ `uninstallFlavor1Debug`

最后，HTML报告也可以根据flavor统计。测试结果和报告位置如下，首先是每个flavor版本的，然后是总的统计结果：

+ `build/androidTest-results/flavors/<FlavorName>`
+ `build/androidTest-results/all/`
+ `build/reports/androidTests/flavors<FlavorName>`
+ `build/reports/androidTests/all/`

# 高级构建配置
## 构建选项
###Java编译选项

```
android {
    compileOptions {
        sourceCompatibility = "1.6"
        targetCompatibility = "1.6"
    }
}
```

缺省值是"1.6"。这个会影响所有的编译Java源码的任务。

### appt选项

```
android {
    aaptOptions {
        noCompress 'foo', 'bar'
        ignoreAssetsPattern "!.svn:!.git:!.ds_store:!*.scc:.*:<dir>_*:!CVS:!thumbs.db:!picasa.ini:!*~"
    }
}
```

这个会影响所有使用`aapt`的任务。

### dex选项

```
android {
    dexOptions {
        incremental false
        preDexLibraries = false
        jumboMode = false
        javaMaxHeapSize "2048M"
    }
}
```

## 操作多任务
基本Java项目只有有限的task用于创建输出。`classes` task用于编译Java源码。It’s easy to access from build.gradle by simply using classes in a script. This is a shortcut for project.tasks.classes.

在Android项目中，由于名字基于Build Types和Product Flavor会有大量相同的task。为了避免这个问题，`android`对象有以下属性：

+ `applicationVariants` (仅用于app plugin)
+ `libraryVariants` (仅用于library plugin)
+ `testVariants` (用于app plugin和library plugin)

以上三者分别返回 `ApplicationVariant`, `LibraryVariant`和`TestVariant`的[DomainObjectCollection](http://www.gradle.org/docs/current/javadoc/org/gradle/api/DomainObjectCollection.html)。

(略)

## BuildType和Product Flavor引用
coming soon.

## 使用sourceCompatibility 1.7
从Android KitKat(buildToolsVersion 19)开始可以使用>操作符，multi-catch, strings in switches, try with resources等等。要支持这些新特性，在构建文件中添加如下代码：

```
android {
    compileSdkVersion 19
    buildToolsVersion "19.0.0"

    defaultConfig {
        minSdkVersion 7
        targetSdkVersion 19
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_7
        targetCompatibility JavaVersion.VERSION_1_7
    }
}
```

注意：`minSdkVersion`使用小于19的值时，可以使用除 try with resources 以外的所有语言特性。如果想使用 try with resources，必须让 minSdkVersion至少为19。当然，还必须保证Gradle是使用JDK 1.7或更新版本(Android Gradle插件为0.6.1或更新)



