---
title: dive-into-gradle
tags:
---
彻底弄懂Gradle

<!--more-->
[Gradle完全解析](http://km.oa.com/group/22112/articles/show/332186?kmref=home_top10_list)

[构建工具的发展及Android Gradle快速上手](http://blog.csdn.net/yanquan345/article/details/46710869)

[如何通俗地理解Gradle](https://www.zhihu.com/question/30432152)

# Gradle介绍

[构建工具的发展及Android Gradle快速上手](http://blog.csdn.net/yanquan345/article/details/46710869)介绍了构建工具的产生背景及其发展，我用一张图稍加总结一下。
<div align="center">
<img src="构建工具的发展.png" width="60%" height="60%" />
</div>

Gradle脚本使用一种基于Groovy的特定领域语言(DSL)，所以理解Gradle的关键在于理解Groovy和DSL。


# Groovy基础
Groovy是一种JVM动态语言，原生支持Java语法，这对于大部分Java开发人员来说是一个好消息，因为不需要太大的学习成本。但并非不要学习成本。

<div align="center">
<img src="groovy.png" width="40%" height="40%" />
</div>

关键在于理解Groovy的闭包，它是DSL的基础。

<!-- https://coggle.it/diagram/WnwUEFnPjgABpxP1/t/groovy -->

闭包展开。

[TODO]

# Gradle DSL 

https://docs.gradle.org/current/dsl/

## 基础
+ Gradle脚本是配置脚本。当脚本执行时，它会配置一个特定类型的对象。比如构建脚本会配[Project](https://docs.gradle.org/current/dsl/org.gradle.api.Project.html)对象。它是脚本的 _代理对象_
+ 每种Gradle脚本都实现了[Script](https://docs.gradle.org/current/dsl/org.gradle.api.Script.html)接口。

下表显示了每种Gradle脚本的代理对象

|script类型|代理对象|
|---------|-------|
|构建脚本|Project|
|初始化脚本|Gradle|
|设置脚本|Settings|

可以在脚本中使用代码对象的属性和方法。

+ 构建脚本由零个语句或脚本块组成
 + 语句
  + 方法调用
  + 属性赋值
  + 本地变量声明
 + 脚本块，实质上是一个接收闭包作为参数的方法调用。顶层的常用脚本块包括：
  + [allprojects { }](https://docs.gradle.org/current/dsl/org.gradle.api.Project.html#org.gradle.api.Project:allprojects(groovy.lang.Closure)) - 对项目及子项目进行配置
  + [buildscript { }](https://docs.gradle.org/current/dsl/org.gradle.api.Project.html#org.gradle.api.Project:buildscript(groovy.lang.Closure)) - 构建脚本的类路径
  + [dependencies { }](https://docs.gradle.org/current/dsl/org.gradle.api.Project.html#org.gradle.api.Project:dependencies(groovy.lang.Closure)) - 当前项目的依赖
  + [repositories { }](https://docs.gradle.org/current/dsl/org.gradle.api.Project.html#org.gradle.api.Project:repositories(groovy.lang.Closure)) - 当前项目的中央库
+ 构建脚本是Groovy脚本，可以包含方法定义和类定义

核心类型

|类型|描述|
|---------|-------|
|[Project](https://docs.gradle.org/current/dsl/org.gradle.api.Project.html)|Gradle的主要API接口|
|[Task](https://docs.gradle.org/current/dsl/org.gradle.api.Task.html)|构建中最小的工作单元|
|[Gradle](https://docs.gradle.org/current/dsl/org.gradle.api.invocation.Gradle.html)|代表对Gradle的一次调用|
|[Settings](https://docs.gradle.org/current/dsl/org.gradle.api.initialization.Settings.html)|用于配置和初始化Project实例|
|[Script](https://docs.gradle.org/current/dsl/org.gradle.api.Script.html)||
|[SourceSet](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.SourceSet.html)|Java代码和资源|

Task类型

常见的包括 copy, delete, jar, test, upload


---

```groovy
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath group: 'commons-codec', name: 'commons-codec', version: '1.2'
    }
}
```

buildscript()是一个方法  写出Groovy代码的等价形式


---

在脚本中使用jar包中的方法

https://docs.gradle.org/current/userguide/organizing_build_logic.html#sec:build_script_external_dependencies


---


## build logic

[来源](https://docs.gradle.org/current/userguide/organizing_build_logic.html)

+ 使用task闭包定义构建逻辑
+ 如果task有相同逻辑则将相同逻辑提取为方法
+ 如果多个模块有相同逻辑，则在父模块中将其定义为方法
+ 如果构建逻辑过于复杂，则将其封装为类


# android gradle dsl

http://google.github.io/android-gradle-dsl/current/com.android.build.gradle.internal.dsl.DefaultConfig.html
https://developer.android.com/studio/build/index.html?hl=zh-cn


还记得`apply plugin: 'com.android.application'`吗

它是Android Gradle Plugin提供的"extension type"




# 如何开发gradle插件

https://docs.gradle.org/current/userguide/organizing_build_logic.html#sec:build_sources

# 闭包


# 问题
## 如何理解这段代码
```
android.applicationVariants.all { variant ->
    variant.outputs.all{ output ->
        def file = output.outputFile
        output.outputFileName = file.name.replace(".apk", "-modify.apk")
    }
}
```

## gradlew 与 gradle 的区别

## 从不同目录执行gradle有什么区别  
https://docs.gradle.org/current/userguide/build_lifecycle.html#sec:initialization  
https://docs.gradle.org/current/userguide/multi_project_builds.html#sec:execution_rules_for_multi_project_builds
https://docs.gradle.org/current/userguide/multi_project_builds.html hello的例子
The simple rule mentioned already above is: Execute all tasks down the hierarchy which have this name. Only complain if there is no such task!

方法一: 切换目录

方法二: Running tasks by their absolute path

## 如何排除gradle脚本错误?
tip gradle脚本问题举例 tinker插件

## 这些是什么
apply是什么

tasks是什么, plugin是什么, shared scripts是什么, 为什么有方法, 有类 

> 组织构建逻辑的方式而已  [来源](https://docs.gradle.org/current/userguide/organizing_build_logic.html)

ext是什么

## Configuration on demand

如何使用 Configuration on demand mode   编译IGame时为什么还会检查GameLife

## 这里两个hello有什么区别
```groovy
allprojects {
    task hello {
        doLast { task ->
            println "I'm $task.project.name"
        }
    }
}
subprojects {
    hello {
        doLast {
            println "- I depend on water"
        }
    }
}
```


> You may notice that there are two code snippets referencing the “hello” task. The first one, which uses the “task” keyword, constructs the task and provides it’s base configuration. The second piece doesn’t use the “task” keyword, as it is further configuring the existing “hello” task. You may only construct a task once in a project, but you may add any number of code blocks providing additional configuration.

> Other build systems use inheritance as the primary means for defining common behavior. We also offer inheritance for projects as you will see later. But Gradle uses configuration injection as the usual way of defining common behavior. We think it provides a very powerful and flexible way of configuring multiproject builds.

## transitive dependencies
transitive dependencies 问题  https://docs.gradle.org/current/userguide/multi_project_builds.html#sec:project_jar_dependencies

## compile与api和complement

# 参考

http://google.github.io/android-gradle-dsl/current/com.android.build.gradle.internal.dsl.DefaultConfig.html
https://developer.android.com/studio/build/index.html?hl=zh-cn
https://dongchuan.gitbooks.io/gradle-user-guide-/ 

http://km.oa.com/group/18155/articles/show/332296?jumpfrom=RTX