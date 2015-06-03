---
layout: post
title: "Gradle wrapper"
date: 2015-05-29 10:43:34 +0800
comments: true
categories: gradle
published: false
---

[source](https://docs.gradle.org/current/userguide/gradle_wrapper.html)

Gradle Wrapper(后面称为"wrapper")是更好的启动Gradle构建的方式。这个wrapper在Windows上是一个batch脚本，在其他操作系统上是一个shell脚本。当你通过wrapper启动Gradle时，Gradle会自动被下载并用于构建。

应当把wrapper提交到版本控制系统。通过将wrapper跟项目一起发布，所有基于它工作的人事先不必安装Gradle。另一个好处是，可以保证构建者使用指定的Gradle版本进行构建。当然，这对[持续构建](http://en.wikipedia.org/wiki/Continuous_integration)服务器来说也非常有用，因为它不需要进行任何配置。

通过运行`wrapper` task来安装wrapper。(这个task一直存在，即使用你没有在构建中加入)。通过`--gradle-version`来指定Gradle版本。通过`--gradle-distribution-url`指定下载Gradle的URL。如果没有指定这个URL，将下载执行wrapper task时的Gradle版本。所以当你使用Gradle 2.4运行wrapper task时，wrapper会默认配置为Gradle 2.4。

`gradle wrapper --gradle-version 2.0`输出如下：

```
> gradle wrapper --gradle-version 2.0
:wrapper

BUILD SUCCESSFUL

Total time: 1 secs
```

可以通过在构建脚本中添加和配置`Wrapper` task来进一步定制化wrapper。如下

```
task wrapper(type: Wrapper) {
    gradleVersion = '2.0'
}
```

执行`gradle wrapper`后生成的文件如下：

```
simple/
  gradlew
  gradlew.bat
  gradle/wrapper/
    gradle-wrapper.jar
    gradle-wrapper.properties
```

!?? gradlew 出现, `Exception in thread "main" java.lang.RuntimeException: java.net.ConnectException: Connection timed out: connect`


所有的这些文件都应该提交到版本控制系统当中。只需执行一次。当这些文件被加入到项目中，项目应该使用`gradlew`命令构建。`gradlew`命令跟`gradle`命令使用方式完全一样。如果你想切换到新版本的Gradle，你不必重新运行wrapper task。在`gradle-wrapper.properties`中修改相对入口就行了，但如果想使用新版本的Gradle wrapper可提供的功能，就要重新生成wrapper文件了。

# 配置
当你使用`gradlew`运行Gradle时，wrapper会检查对应的Gradle是否可用。如果可用，会将`gradlew`的参数传给`gradle`命令。如果不可用，会先下载。配置Wrapper task时，可以指需要使用的Gradle version。`gradlew`命令首先会从Gradle库中下载。还可以指定下载Gradle的URL，`gradlew`命令使用指定的URL下载Gradle。如果没有指定Gradle版本以及下载的URL，`gradlew`命令会下载用于生成wrapper文件的Gradle。

配置Wrapper的详细方法可以参考[Wrapper](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.wrapper.Wrapper.html)类的API文档。

如果通过`gradlew`构建项目时不想下载Gradle，可以将Gradle zip包提交到版本控制系统，并在配置文件中指定路径。可以使用相对路径。使用wrapper构建时，本地已安装的Gradle会被忽略。

通过修改gradle-wrapper.properties中的`distributionUrl`属性来配置Gradle zip包的下载路径，比如我本地D盘根目录下有一个已下载好的gradle-2.2.1-all.zip文件，使用gradlew时不想从官网下载，可以这样配置(注意转义字符)：

```
distributionUrl=file\:/d:/gradle-2.2.1-all.zip
```

---

