---
title: Gradle学习笔记
tags: gradle
toc: true
date: 2018-03-20 16:58:08
---

Gradle学习笔记，记录一些零散的知识点。
<!-- more -->

总结如下：

+ project实例跟`build.gradle`是一一对应的
+ settings实例跟`settings.gradle`是一一对应的
+ project是由一系列task组成的
+ task是由一系列action组成的
+ task之间可能有依赖关系和顺序关系
+ 所有的脚本都实现了`Script`接口
+ build script的代理对象是`project`
+ initialization script的代理对象是`gradle`

# [Project][ref]
> There is a one-to-one relationship between a Project and a build.gradle file.  A project is essentially a collection of Task objects.

project实例跟`build.gradle`是一一对应的，使用build.gradle对相应的Project实例进行配置。
project是由一系列task组成的。

## project的依赖

+ 依赖管理
+ 配置管理
+ artifact管理
+ 仓库管理

![](project-dependency.png)

## project的层级

> Projects are arranged into a hierarchy of projects. A project has a name, and a fully qualified path which uniquely identifies it in the hierarchy.

![](project-hierarchy.png)

这个项目的project hierarchy如下所示：

```
➜  demo ./gradlew  projects -q

------------------------------------------------------------
Root project
------------------------------------------------------------

Root project 'demo'
+--- Project ':sub1'
\--- Project ':sub2'
```

## project的属性和方法
> A project has 5 property 'scopes', which it searches for properties. 

project的动态属性有5种不同的scope

![](project-property.png)

> A project has 5 method 'scopes', which it searches for methods

project的动态method有5种不同的scope

![](project-method.png)

Ext属性
> All extra properties must be created through the "ext" namespace

```groovy
project.ext.prop1 = 'test'
```

# [Settings](http://gradledoc.qiniudn.com/1.12/dsl/org.gradle.api.initialization.Settings.html)
> There is a one-to-one correspondence between a Settings instance and a settings.gradle settings file. 

settings实例跟`settings.gradle`是一一对应的，使用settings.gradle对相应的Settings实例进行配置
使用`Settings.include()`方法指定需要构建的project

```groovy
include ':sub1', ':sub2'
```

# [Script](http://gradledoc.qiniudn.com/1.12/dsl/org.gradle.api.Script.html)
> Generally, a Script object will have a delegate object attached to it 

编译后的脚本对应的类都实现了`Script`接口。这个接口定义了Gradle特定的方法，可以直接在脚本中使用这些方法。

`Script`实例都带有一个代理对象。比如build script有`project`代理对象，而initialization script有`gradle`代理对象。

`apply`和`buildscript`这两个我们经常使用的方法其实是来自`Script`接口。

```groovy
apply plugin: 'java'
buildscript {
	
}
```

# [Task](http://gradledoc.qiniudn.com/1.12/dsl/org.gradle.api.Task.html)
> A Task represents a single atomic piece of work for a build. Each task belongs to a Project. A Task is made up of a sequence of Action objects. 

task可能对其他task有依赖，并且对执行顺序有要求
task是由一系列action组成的

# 理解Gradle文档
Gradle的文档结构跟JDK的文档稍稍有所不同。JDK文档中通常只包括`Properties`和`Methods`，而Gradle文档还列出了一些特有的部分，具体如下：

+ Dynamic Properties - 动态属性， 这里的属性可能是插件添加进来的
+ Dynamic Methods - 动态方法，这里的方法可能是插件添加进来的
+ Properties
+ Methods
+ Script blocks - 闭包

另外Gradle的类和接口可以划分成以下四大类：

+ Build script blocks
+ Core types
+ Container types
+ Task types

# 理解Groovy代理

TODO

<!--
# 思考
Gradle官方文档中提到

> Dynamic properties will eventually be removed entirely, meaning that this will be a fatal error in future versions of Gradle. See Extra Properties to learn how to add properties dynamically.

文档中的意思是未来将完全去掉project的动态属性？个

人也感觉动态属性非常复杂，可能引起很多问题。
-->

[ref]: http://gradledoc.qiniudn.com/1.12/dsl/org.gradle.api.Project.html#org.gradle.api.Project:configurations(groovy.lang.Closure)
[ref2]: http://gradledoc.qiniudn.com/1.12/dsl/org.gradle.api.Task.html