---
layout: post
title: "Android测试"
date: 2015-06-10 10:34:00 +0800
comments: true
categories: android
published: true
---
本文介绍了如何测试Android应用，包括Unit Test和Android Test。原文来自[Android Testing](http://www.vogella.com/tutorials/AndroidTesting/article.html)

<!--more-->

# 为什么Android应用测试很重要
## 被测试应用
> The application which is tested is typically called the *application under test* .

## Android质量保障
Android应用在内存、CPU以及电源有限的设备上运行。设备类型多样，依赖因素也各不相同，如网络连接等等。所以调试、测试和优化Android应用非常重要。Android应用达到合理的测试覆盖率有助于提升和维护。

## Android测试策略
不太可能在所有可能的设备配置上测试Android应用，所以通常的做法是在典型设备上进行Android测试。应该至少在一部最低配置的手机以及一部最高配置的手机上进行测试。比如，不同的像素密度、屏幕分辨率等，以保证可以在这些设备上正常工作。

## Android应用测试历史
Android应用对测试的支持曾经比较糟糕，所以多数Android应用并没有很好的测试覆盖率。幸运的是随着时间的推移，Android测试框架不断改进，现在有很多优秀的框架支持Android测试。

## Android和JUnit 4
过去，Android测试系统是基于JUnit 3的。现在已经升级到JUnit 4。仍然支持老的测试类，但新的测试代码中应避免使用。

# 前提条件
后面的内容要求你对如何创建Android应用有基本的了解。详细内容可以参考[Android Tutorial](http://www.vogella.com/tutorials/Android/article.html)。另外，还要求你对基于Gradle的Android构建系统有基本了解。相关介绍见[Building Android applications with Gradle Tutorial](http://www.vogella.com/tutorials/AndroidBuild/article.html)。

关于Gradle的更多内容见[Gradle Tutorial](http://www.vogella.com/tutorials/Gradlearticle.html)。

# Android自动化测试
## Android单元测试和集成测试
单元测试只测试某个组件的功能。比如，假设某个Android activity上有个按钮用于启动另一个activity。单元测试会检查相应的Intent是否发出，而不管后一个activity是否被启动。

而集成测试还要检查后一个activity是否被正确启动。

## 测试单个应用或多个应用
测试的另一个重要条件是，你只测试自己的应用，还是同时会测试跟另外应用之间的交互。如果只是自己的应用内测试，可以使用需要知道应用信息的测试框架，比如需要知道view ID。

# Android应用测试要点
## Android应用测试什么
下表列出了除常规单元测试外Android应用需要重点测试的内容：

Test area|Description
---------|-----------
Activity life cycle events|You should test if you activity handles the Android life cycle events correctly. You should also test if the configuration change events are handled well and if instance state of your user interface components is restored.
File system and database operations|Write and read access from and to the file system should be tested including the handling of databases.
Different device configurations|You should also test if your application behaves well on different device configurations.

简单来讲，就是这三点： Activity生命周期、IO、不同机型配置。

## 测试前置条件
写一个`testPreconditions()`方法用于测试其他所有测试的前置条件是个好的实践。如果这个方法失败了，马上就知道其他测试需要的条件未满足。

## 在没显示屏的设备上测试
在没有显示屏的情况下运行测试，可以给adb指定`-no-window`参数。

# 哪些测试需要在Android系统运行
## Android测试类型
Android测试基于JUnit。Android测试可以分为两类：

+ 本地测试 - 可以在JVM上运行的测试
+ Instrumented测试 - 依赖Android系统的测试

![categories of android tests](http://www.vogella.com/tutorials/AndroidTesting/images/xandroidtestcategories10.png.pagespeed.ic.kdZY1Na5cb.png)

如果可以的话，你应该优先使用本地测试。因为它的执行比在Android设备上部署并运行测试要快得多。

## 单元测试
单元测试也称为本地测试。如果你的类没有调用Android API，或只对Android API有很少的依赖，可以没有任何限制地使用JUnit测试框架(或其他Java测试框架)。这个办法的好处是可以使用任何Java单元测试框架和工具，而且单元测试的速度比在Android系统上测试速度快得多。

如果对Android API有少量依赖，可以替换掉这些依赖。通常是通过Mockito之类的mocking框架完成替换工作。

## Instrumented测试
Instrumented测试用于测试使用了Android API的Java类。测试使用了Android API的类，需要在Android设备上运行这些测试。不幸地是，这种测试执行时间较长。

因为`android.jar`文件并不包含Android框架代码，而只是对应的Stub(调用`android.jar`中的方法会抛出`RuntimeException("Stub!")`。`android.jar`文件在应用部署到Android设备前用于Java编译器编译代码。它不会跟应用一起打包。一旦应用部署到设备，它会使用Android设备上的`android.jar`。所以如果没有额外库的支持的话，将无法在JVM上测试依赖Android框架的类。

# Android项目结构
## Android项目结构
最好的方式是基于惯例组织测试代码。在应用项目中应当使用基本的目录结构存放代码:

+ `app/src/main/java` - 用于存放主应用的代码
+ `app/src/test/java` - 用于存放任何可以在JVM上运行的测试
+ `app/src/androidTest/java` - 用于存放需要在Android设备上运行的测试

如果你遵守这里的惯例，Android构建系统会自动在JVM上运行单元测试、在Android设备上运行Instruted测试。

## Android Studio中的test目录
切换到 *Project* view，将显示项目的目录结构，可以创建相应的测试目录：

![project view](http://www.vogella.com/tutorials/AndroidTesting/images/xunittestfoldecreation10.png.pagespeed.ic.ZsdkKsZ0ci.png)

选项src目录并右键创建新的test目录。

![create test folder](http://www.vogella.com/tutorials/AndroidTesting/images/xunittestfoldecreation20.png.pagespeed.ic.wmEEMHcrfu.png)

![create test folder](http://www.vogella.com/tutorials/AndroidTesting/images/xunittestfoldecreation30.png.pagespeed.ic.5jv9ceD-Y0.png)

![create test folder](http://www.vogella.com/tutorials/AndroidTesting/images/xunittestfoldecreation40.png.pagespeed.ic.XpuhhQ5-jk.png)

然后在Gradle构建脚本中添加JUnit依赖。

```
dependencies {
    // Unit testing dependencies
    testCompile 'junit:junit:4.12'
    // Set this dependency if you want to use the Hamcrest matcher library
    testCompile 'org.hamcrest:hamcrest-library:1.3'
    // more stuff, e.g., Mockito
} 
```

> 警告：创建Java目录时可能会将新建的test目录也作为源码目录添加到build.gradle文件中。如果发现 app/build.gradle中有如下条目，需要将其去掉。测试代码不应该看作普通源码。
> `sourceSets { main { java.srcDirs = ['src/main/java', 'src/test/java/'] } }`

接下来就可以写测试用例了。

# 运行测试
## 哪些测试被编译
> 警告：想让Android Studio能识别单元测试或Instrumention测试的依赖，需要从Android Studio的Build Variant view中选择"Unit Tests"或"Android Instrumentation Tests"。
> ![build variants](http://www.vogella.com/tutorials/AndroidTesting/images/xunittestfoldecreation50.png.pagespeed.ic.qYKSxI6oVz.png)

如何编写测试取决于你是在做单元测试还是集成测试。

## 查看通过的测试
点击下图中的高亮工具条可以查看所有已通过的测试。

![seeing all passed test](http://www.vogella.com/tutorials/AndroidTesting/images/xandroidstudioseealltests.png.pagespeed.ic.GqtkASu0rc.png)

## 编译错误
遇到"duplicate files in path. Path in archive: LICENSE.txt"错误时在 app/build.gradle 文件中添加以下配置：

```
android {
    packagingOptions {
    exclude 'LICENSE.txt'
    }
}
```

# 单元测试
## Android上的单元测试
Android使用 *unit tests* 这个术语描述那些可以在开发机的本地JVM而非Android Runtime上运行的测试。

这些测试依靠一个修改后的`android.jar`执行，这个jar文件中所有的`final`修饰符都被去掉。修改后允许使用mocking库，如Mockito。这个`android.jar`中的所有方法缺省时抛出异常。这种缺省行为保证单元测试只会测试你自己的代码，而不会依赖Android平台的任何特定行为。如果想使用Android平台的特定行为，可以使用mocking框架替换相应调用。

## 单元测试的位置
前面Android项目结构一节中讲到，单元测试应放到`app/src/test/`目录。

## Gradle构建文件中的依赖
Android项目中要使用JUnit，需添加相应配置到Gradle构建文件中：

```
dependencies {
    // Unit testing dependencies
    testCompile 'junit:junit:4.12'
    // Set this dependency if you want to use the Hamcrest matcher library
    testCompile 'org.hamcrest:hamcrest-library:1.3'
    // more stuff, e.g., Mockito
} 
```

## 从Gradle运行单元测试
使用`gradlew test`命令运行单元测试。

## 从Android Studio运行单元测试
Android Studio的Build Variants窗口中有两种Test Artifact可选择。如果选择Unit Tests，单元测试将在JVM上运行。

![test artifact](http://www.vogella.com/tutorials/AndroidTesting/images/xas_runtests10.png.pagespeed.ic.vPJCrZkzwA.png)

要运行单元测试，应保证选择的是"Unit Test"，右击待测试的类然后选择"Run"。

![run test](http://www.vogella.com/tutorials/AndroidTesting/images/xrunUnitTestInAndroidStudio10.png.pagespeed.ic.mC1SqEP87Y.png)

## 测试报告
测试报告输出到 `app/build/reports/tests/debug/` 目录。`index.html`是测试结果概述，它链接到单个测试的结果页面。

## 从Mocked方法中返回缺省值
也可以配置Gradle构建系统，让`android.jar`中的方法均返回缺省值而不是抛出异常：

```
android {
  // ...
  testOptions { 
    unitTests.returnDefaultValues = true
  }
} 
```

# 练习：创建Android测试项目
[Android temperature converter](http://www.vogella.com/tutorials/Android/article.html#tutorialtemperature)介绍了如何创建Android项目。

# 练习：为Android项目创建JUnit 4测试
## Exercise high level target
这个练习中将学习如何给上一节创建的Android项目添加JUnit4单元测试。

## 创建test目录并添加依赖
按照前面提供的方法，为单元测试创建test目录。然后在 app/build.gradle 中添加JUnit依赖。

```
dependencies {
  // Unit testing dependencies
  testCompile 'junit:junit:4.12'
} 
```

## 创建test
在`app/src/test/`目录中为`ConverterUtil`类创建如下两个测试方法。

```
public class ConverterUtilTest {

  @Test
  public void testConvertFahrenheitToCelsius() {
    float actual = ConverterUtil.convertCelsiusToFahrenheit(100);
    // expected value is 212
    float expected = 212;
    // use this method because float is not precise
    assertEquals("Conversion from celsius to fahrenheit failed", expected,
        actual, 0.001);
  }

  @Test
  public void testConvertCelsiusToFahrenheit() {
    float actual = ConverterUtil.convertFahrenheitToCelsius(212);
    // expected value is 100
    float expected = 100;
    // use this method because float is not precise
    assertEquals("Conversion from celsius to fahrenheit failed", expected,
        actual, 0.001);
  }

} 
```

## 运行单元测试
如果一切正确的话，单元测试应该可以成功执行。

# Instrumentation
Android测试API在Android组件以及应用生命周期中提供hook。这些hook称为`instrumentation API`，允许测试控制生命周期和用户交互事件。

正常条件下你的应用只能跟生命周期及用户交互事件相互作用。比如，Android在activity创建时调用`onCreate()`方法。或者用户按下按钮或按键时相应的代码会执行。通过Instrumentation可以在测试中控制这些事件。

基于instrumentation的测试类才允许向被测试的应用发送key或touch事件。

比如，测试可以调用`getActivity()`方法以启动activity并返回这个待测试的activity。然后可调用`finish()`方法，再次调用`getActivity()`以测试应用是否正确恢复状态。

## Android系统如何执行测试
`InstrumentationTestRunner`是Android测试的基础test runner。这个test runner启动并加载测试方法。它并不启动应用，启动应用是测试方法的责任。测试方法控制应用组件的生命周期。

test runner还启动时还调用应用以及activity的`onCreate()`方法。

## Instrumentation framework的使用
通过直接在JVM上单元测试，以及使用Espresso等流行的UI测试框架，开发者很少直接使用instrumentation API。

# Instrumented测试
## 在Android中使用Instrumented测试
Instrumented测试在Android设备或模拟器上运行而非JVM。这些测试会访问真实设备以及其资源，对那些无法轻易被模拟的功能测试非常有用。还可以用来测试系统功能是否正常，比如验证`Parcelable`的实现是否正确。

Mockito等mocking框架仍然可以被用来模拟那些那测试没影响的Android System部分。(Mockito as mocking framework can still be used to mock the parts of the Android system which are not interesting for the test.)

## Instrumentation测试位置
Instrumentation测试代码位于 `app/src/androidTest/java` 目录。(略)

## Gradle构建文件中的依赖
Android项目中要使用JUnit，需添加相应配置到Gradle构建文件中。还需要指定缺省的 testInstrumentationRunner 为 "android.support.test.runner.AndroidJUnitRunner"。

```
defaultConfig {
       ..... more stuff
        testInstrumentationRunner "android.support.test.runner.AndroidJUnitRunner"
    }

dependencies {
    // Unit testing dependencies
    androidTestCompile 'junit:junit:4.12'
    // Set this dependency if you want to use the Hamcrest matcher library
    androidTestCompile 'org.hamcrest:hamcrest-library:1.3'
    // more stuff, e.g., Mockito
} 
```

## 从Gradle运行测试
使用`gradlew connectedCheck`命令运行单元测试。

## 从Android Studio运行测试
Android Studio的Build Variants窗口中有两种Test Artifact可选择。如果选择Unit Tests，单元测试将在JVM上运行。

![test artifact](http://www.vogella.com/tutorials/AndroidTesting/images/xas_runtestsinstrumented10.png.pagespeed.ic.okkrEwUUqk.png)

要运行单元测试，应保证选择的是"Android Instrumentation Tests"，右击待测试的类然后选择"Run"。

## 测试报告位置
测试报告输出到 `app/build/reports/androidTests/connected/` 目录。`index.html`是测试结果概述，它链接到单个测试的结果页面。

# 练习：创建简单Instrumented测试
略

# 模拟Android对象
对于单元测试，你可以使用mocking框架来模拟Android对象。对Android Runtime上运行的测试也可以这么做。过去，Android框架会提供特殊的mocking classes用于模拟测试，但现在这些类没必要了。

# 使用Mockito
如何使用Mockito框架请参考[Mockito tutorials](http://www.vogella.com/tutorials/Mockito/article.html)。

# Android上使用Mockito
向Gradle构建文件中添加Mockito依赖后就可以直接在Android单元测试中使用Mockito了。如果是在Instrumented测试中使用，还需要添加dexmaker和dexmaker-mockito依赖。

```
dependencies {
    testCompile 'junit:junit:4.12'
    // required if you want to use Mockito for unit tests
    testCompile 'org.mockito:mockito-core:1.+'
    // required if you want to use Mockito for Android instrumentation tests
    androidTestCompile 'org.mockito:mockito-core:1.+'
    androidTestCompile "com.google.dexmaker:dexmaker:1.2"
    androidTestCompile "com.google.dexmaker:dexmaker-mockito:1.2"
} 
```

# 练习：模拟文件访问

## 创建Android测试项目
在已有的或新建的Android应用中添加如下类(包名为`com.vogella.android.testing.mockitocontextmock`)：

```
public class Util {
  public static void writeConfiguration(Context ctx) {
    BufferedWriter writer = null;
    try {
      FileOutputStream openFileOutput = 
       ctx.openFileOutput("config.txt", Context.MODE_PRIVATE);
      openFileOutput.write("This is a test1.".getBytes());
      openFileOutput.write("This is a test2.".getBytes());
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      if (writer != null) {
        try {
          writer.close();
        } catch (IOException e) {
          e.printStackTrace();
        }

      }
    }
  }
} 
```

> 注意：不需要在Android应用中使用这个类，因为我们只是在隔离的环境下对其单元测试。

## 创建单元测试
使用Mockito编写测试验证：

+ `openFileOutput`刚好被调用一次
+ `write()`方法至少被调用两次

```
ublic class TextContextOutputStream {

@Mock
Context context;

@Mock
FileOutputStream fileOutputStream;

@Before
public void init(){
    MockitoAnnotations.initMocks(this);
}

    @Test
public void writeShouldWriteTwiceToFileSystem() {
    try {
        when(context.openFileOutput(anyString(), anyInt())).thenReturn(fileOutputStream);
        Util.writeConfiguration(context);
        verify(context, times(1)).openFileOutput(anyString(), anyInt());
        verify(fileOutputStream, atLeast(2)).write(any(byte[].class));

    } catch (Exception e) {
        e.printStackTrace();
        fail();
    }
}
} 
```

# Android Testing Support Library
Android Testing Support Library可供创建和运行Android测试。这个library包含`AndroidJunitRunner`，Espresso测试框架和UI Automator。

`AndroidJunitRunner`允许创建和运行JUnit 4测试，而Espresso框架可用于应用的UI测试。UI Automator允许跨应用的功能测试。

AndroidJunitRunner允许通过`InstrumentationRegistry`访问instrumentation API，

+ `InstrumentationRegistry.getInstrumentation()` - 返回当前运行的Instrumentation
+ `InstrumentationRegistry.getContext()` - 返回Instrumentation包中的Context对象
+ `InstrumentationRegistry.getTargetContext()` - 返回目标应用的application context
+ `InstrumentationRegistry.getArguments()` - 返回传递给Instrumentation的Bundle参数的一份拷贝。访问命令行中指定的Instrumentat测试参数时很有效

还能通过`ActivityLifecycleMonitorRegistry`访问生命周期。

# 更多
## Android的assertion
除标准的JUnit `Assert`外， Android testing API还提供`MoreAssets`和`ViewAsserts`。

## Test分组
`@SmallTest`, `@MediumTest`, `@LargeTest`注解允许对测试分类。比如，可以运行那些测试时间不会太长的测试。而耗时太长的测试只在持续集成服务器上运行。

通过Gradle插件中配置`InstrumentationTestRunner`来选择需要运行的测试。下面的例子只会运行标记有`@SmallTest`注解的测试。

```
android {
  //....
  defaultConfig {
  //....
    testInstrumentationRunner "android.support.test.runner.AndroidJUnitRunner"
    testInstrumentationRunnerArgument "size", "small"
  }
} 
```

> 注意： 只在 Android Plugin for Gradle 1.3.0 以上版本中有效。目前Android Studio中并不支持。

```
public class ExampleTest {

    @Test
    @SmallTest
    public void validateSecondActivity() {
        // Do something not so long...
    }

    @Test
    @MediumTest
    public void validateSecondActivityAgain() {
        // Do something which takes more time....
    }

} 
```

## 测试过滤

Annotation|Description
----------|-----------
@RequiresDevice|Specifies that the test should run only on physical devices, not on emulators.
@SdkSupress|@SDKSupress(minSdkVersion=18)

## Flaky test
Android中的动作有时跟时间相关。使用`@FlakyTest`注解重复某个测试直到失败为止。Via the `tolerance` attribute of this annotation you can define how often the Android test framework should try to repeat a test before marking it as failed.

# Activity测试
见[Android user interface testing with Espresso](http://www.vogella.com/tutorials/AndroidTestingEspresso/article.html)。

# 跨应用UI测试
## 使用UI Automator
使用UI Automator跨应用测试。功能或黑盒UI测试会测试到整个应用，而不是应用中的单个组件。

Android SDK包含 *uiautomator* Java库用于创建UI测试，并提供用于运行测试的引擎。这两个工具都只在API 16以上才能运行。

uiautomator测试项目是独立的Java项目，JUnit3以及来自`android-sdk/platforms/api-version`中的 uiautomator.jar和android.jar会添加到构建路径中。

Android uiautomator提供`UiDevice`类跟设备交互，`UiSelector`类用于搜索屏幕上的UI元素，而`UiObject`表示UI元素。`UiCollection`类用于一次选择多个UI元素，`UiScrollable`允许在view中滚动以寻找元素。下面的是Android开发者网站的[官方例子](http://developer.android.com/tools/testing/testing_ui.html#sample)。

```
public class LaunchSettings extends UiAutomatorTestCase {

  public void testDemo() throws UiObjectNotFoundException {

    // Simulate a short press on the HOME button.
    getUiDevice().pressHome();

    // We’re now in the home screen. Next, we want to simulate
    // a user bringing up the All Apps screen.
    // If you use the uiautomatorviewer tool to capture a snapshot
    // of the Home screen, notice that the All Apps button’s
    // content-description property has the value “Apps”. We can
    // use this property to create a UiSelector to find the button.
    UiObject allAppsButton = new UiObject(new UiSelector().description("Apps"));

    // Simulate a click to bring up the All Apps screen.
    allAppsButton.clickAndWaitForNewWindow();

    // In the All Apps screen, the Settings app is located in
    // the Apps tab. To simulate the user bringing up the Apps tab,
    // we create a UiSelector to find a tab with the text
    // label “Apps”.
    UiObject appsTab = new UiObject(new UiSelector().text("Apps"));

    // Simulate a click to enter the Apps tab.
    appsTab.click();

    // Next, in the apps tabs, we can simulate a user swiping until
    // they come to the Settings app icon. Since the container view
    // is scrollable, we can use a UiScrollable object.
    UiScrollable appViews = new UiScrollable(new UiSelector().scrollable(true));

    // Set the swiping mode to horizontal (the default is vertical)
    appViews.setAsHorizontalList();

    // create a UiSelector to find the Settings app and simulate
    // a user click to launch the app.
    UiObject settingsApp = appViews
        .getChildByText(new UiSelector()
            .className(android.widget.TextView.class.getName()),
            "Settings");
    settingsApp.clickAndWaitForNewWindow();

    // Validate that the package name is the expected one
    UiObject settingsValidation = new UiObject(new UiSelector()
        .packageName("com.android.settings"));
    assertTrue("Unable to detect Settings", settingsValidation.exists());
  }
} 
```

使用ant构建和部署对应的项目。

```
<android-sdk>/tools/android create uitest-project -n <name> -t 1 -p <path>

# build the test jar
ant build

# push JAR to device
adb push output.jar  /data/local/tmp/

# Run the test
adb shell uiautomator runtest LaunchSettings.jar -c com.uia.example.my.LaunchSettings 
```

## uiautomatorviewer
Android提供 *uiautomatorviewer* 工具，允许分析应用的UI。可以使用这个工具找到应用中的UI index, text或attribute。还允许非程序员分析应用并开发测试用例。uiautomatorviewer截图如下：

![uiautomatorviewer](http://www.vogella.com/tutorials/AndroidTesting/images/xuiautomatorviewer10.png.pagespeed.ic.RoBZ7mNrdL.png)

# Monkey
monkey是用于向设备发送伪随机事件的命令行工具。可以限制Monkey只测试某个应用。比如，下面代码将向`de.vogella.android.test.target`这个应用发送2000个随机事件。

```
adb shell monkey -p de.vogella.android.test.target -v 2000
```

Monkey有时会引起adb server错误。使用如下命令重启adb server。

```
adb kill-server
adb start-server 
```

可以使用 `-s [seed]`参数保证每次产生的事件序列总是相同的。更多关于Monkey的信息见[这里](http://developer.android.com/guide/developing/tools/monkey.html)。

# monkeyrunner
(略)

注：个人感觉很少有人用这个做测试monkeyrunner， 可能是因为本身不太完善。

# Android常见测试需求和解决方案
## 日志记录
测试日志应保存到服务器而不是设备。一个好的实践方式是提供后台，终端通过HTTP POST将测试结果发送到后台。服务器集中管理这些日志并提供统一的访问。

## 通过测试触发系统设置
有时测试中想修改系统状态，比如打开Wifi。这些通常不能直接通过测试完成，因为测试仅具备被测应用的权限。一个好的实践方式是在设备上安装另一个具有相应权限的应用，并在测试中使用Intent触发相关操作。

# 其他开源测试框架
Robotium 是基于Android测试框架的开源测试框架，它让测试API更易用。如何使用Robotium 测试UI见[Robotium](http://www.vogella.com/tutorials/Robotium/article.html)。

Robolectric 是允许依赖于Android API的测试直接在JVM上运行的开源测试框架。更多信息参考[Robolectric tutorial](http://www.vogella.com/tutorials/Robolectric/article.html)。

Roboguice 允许在Android组件中使用依赖注入以简化测试。见[Using Roboguice](http://www.vogella.com/tutorials/RoboGuice/article.html)。

# application测试
<略>

# 练习：测试application
<略>

# 其他

## Service测试

## ContentProvider测试

## Loader测试


