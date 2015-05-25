---
layout: post
title: "Gradle学习笔记"
date: 2015-05-21 12:10:51 +0800
comments: true
categories: tools
published: false
---

# Gradle简介
## Projects和tasks
Gradle中一切都由两个基本概念构成：`projects`和`tasks`。

projects - 每个Gradle构建由一个或多个`projects`组成。一个`project`代表软件中可被构建的某种组件。比如，可以是一个JAR包或Web应用。
tasks - 每个`project`由一个或多个`tasks`组成。一个`task`代表构建过程中的某些原子操作。比如，可以是编译某些类，创建一个JAR文件，生成文档，或者是将某些归档文件发布到代码库。

## Hello world
使用`gradle`命令执行Gradle构建。`gradle`命令在当前目录中搜索`build.gradle`文件。这个`build.gradle`文件称为构建脚本。严格来说是构建配置脚本。构建脚本定义了`project`和它的`tasks`。

最简单的构建脚本如下：

```
task hello {
    doLast {
        println 'Hello world!'
    }
}
```

这个构建脚本定义了一个名为`hello`的`task`。运行`gradle hello`时Gradle会执行`hello`任务。该任务简单地执行一行Groovy代码。

Gradle中的`tasks`跟Ant中的targets类似，但更强大更具表现力。`tasks`之间可以有依赖。

```
task hello {
    doLast {
        println 'Hello world!'
    }
}

task intro(dependsOn: hello) << {
    println "I'm cm"
}
```

被依赖的`task`不需要事先定义。

```
task intro(dependsOn: 'hello') << {
    println "I'm cm"
}

task hello {
    doLast {
        println 'Hello world!'
    }
}
```

Gradle中允许定义一个或多个缺省的tasks。

#Gradle编译Java项目
Gradle是通用的构建工具。但是如果不编写相应的代码，Gradle无法真正用于构建。下面看看怎么用Gradle构建Java项目。多数Java项目构建过程基本类似：编译Java源文件，运行单元测试，将class文件打包成JAR文件。你不必重头为每个Java项目编写Gradle构建脚本，Gradle已有专门的`plugins`用于Java项目构建。

> A plugin is an extension to Gradle which configures your project in some way, typically by adding some pre-configured tasks which together do something useful. 

## Java Plugin
要使用Java Plugin，只需要在构建脚本中添加如下代码(简单吧:))：

```
apply plugin 'java'
```

使用Java Plugin应注意：

1. 源码位于`src/main/java`目录
2. 测试代码位于`src/test/java`目录
3. `src/main/resources`目录下的文件作为资源打包到JAR文件
4. `src/test/resources`目录下的文件添加到classpath用于运行单元测试
5. 构建结果输出到`build`目录，JAR文件输出到`build/libs`目录

Java Plugin常用task包括：

1. `build` - 完整构建，编译并单元测试，生成JAR文件
2. `clean` - 删除`build`目录，删除所有编译生成的文件
3. `check` - 编译并单元测试

## 外部依赖
Java项目通常依赖外部JAR文件。应告诉Gradle从哪里能找到项目中引用的JAR文件。这些JAR文件位于`repository`。`repository`用于下载项目依赖的JAR。通常使用公开的Maven仓库。

```
repositories {
    mavenCentral()
}
```

按下面的方式添加项目依赖。这里声明production classes对`commons collections`有编译时依赖，而test classes对`junit`有编译时依赖：

```
dependencies {
    compile group: 'commons-collections', name: 'commons-collections', version: '3.2'
    testCompile group: 'junit', name: 'junit', version: '4.+'
}
```

## 创建Eclipse项目
Gradle要求的目录结构跟Eclipse Java项目有所不同，如何用创建一个既可以用Gradle编译、又可以在Eclipse中开发的Java项目呢？很简单，只需要在构建脚本中添加Eclipse plugin即可。

```
apply plugin: 'eclipse'
```

然后执行`gradle eclipse`命令生成Eclipse项目文件，生成后该项目就能通过Eclipse的"Import Existing Projects into Workspace"导入进来。导入成功后可使用Eclipse进行项目开发:)。

# 依赖管理
依赖由两部分构成，首先Gradle应知道编译和运行项目需要的依赖项，以找到它们。这些外来的输入文件称为项目的`dependencies`。另一方面，Gradle要编译并上传项目的构建结果。这些向外输出的文件称为项目的`publications`。

可能需要从远程的Maven库或Ivy库，本地目录下载依赖项，或者从其他项目编译产生依赖项。这个过程称为`dependency resolution`。有些依赖项本身也有依赖，它们称为`transitive dependency`。

使用`dependencies` 声明依赖，如下：

```
dependencies {
	compile group: 'org.hibernate', name: 'hibernate-core', version: '3.6.7.Final'
}
```

Gradle依赖分为不同的`configurations`。Java Plugin定义了几类标准的configurations。常见的几种如下

+ compile - 编译项目源码所需的依赖
+ runtime - 运行production classes所需的依赖。缺省情况下也包含compile time dependencies
+ testCompile - 编译项目测试代码所需的依赖。缺省情况下也包含production classes和compile time dependencies
+ testRuntime - 运行测试代码所需的依赖。缺省情况下，也包含上述三种依赖

不同的plugins有不同的标准configurations。也可以定义自己的configurations。

依赖又分不同的情况，一种是`external dependency`。这种依赖是当前构建以外的某种文件，通常保存在某种仓库中，比如Maven中央库或本地目录。使用`group`，`name`和`version`属性来标识依赖。一种简写方式是`"group:name:version"`.

依赖配置也用于发布文件，这些文件称为`publication artifacts`。使用Java Plugin通常不用告诉Gradle发布什么文件，只需要指定`uploadArchives task`的发布路径。

发布地本地目录

```
uploadArchives {
    repositories {
        flatDir {
            dirs 'repos'
        }
    }
}
```

发布到Ivy库

```
uploadArchives {
    repositories {
        ivy {
            credentials {
                username "username"
                password "pw"
            }
            url "http://repo.mycompany.com"
        }
    }
}
```

当运行`gradle uploadArchives`时Gradle将编译并上传JAR文件到`repositories`。

# 常用Gradle命令
+ `gradle <task-name>` - 执行指定的task
+ `gradle <task-a> -x <task-b>` - 在忽略指定的task的情况下执行task
+ 运行Gradle命令时不必指定task的全名，只需要提供可区分的缩写即可。比如使用`di`作为`dist`的缩写，或使用`aD`作为`assembleDebug`的缩写
+ `gradle -b <build-file>` - 指定构建脚本
+ `gradle -p <dir>` - 指定项目目录
+ `gradle -q projects` - 输出项目信息
+ `build.gradle`中添加`description = 'Your Description'` - 提供项目描述
+ `gradle tasks` - 输出tasks信息
+ `gradle tasks -all` - 输出项目中全部的tasks信息
+ `gradle help --task <someTask>` - 输出指定task的详细信息
+ `gradle dependencies app:dependencies` - 输出依赖信息
+ `gradle properties` - 输出properties
+ `gradle -q <module>:properties` - 输出指定module的properties
+ `gradle --profile <task>` - 记录构建消耗的时间并输出到`build/reports/profile`目录。文件名以构建时间命名
+ `gradle -m <task>` - 仅查询tasks执行情况而不真正构建
+ `gradle --gui` - 启动Gradle的GUI界面

# Gradle GUI
Gradle提供一个简单易用的GUI界面。通过`gradle --gui`可以启动Gradle GUI。Gradle GUI主要分为Task Tree、Favorites、Command Line和Setup四个标签页

Task Tree以层级方式显示所有的projects和对应的tasks。双击可以执行相应的task。可以使用filter隐藏不常用的tasks，也可以将将常用的tasks添加到Favorites中。

Favorites用于显示常用的命令。可以非常复杂的命令，还能给它设置名字。比如，可以自定义一个构建命令用于快速编译以忽略单元测试、文档生成等步骤。

Command Line标签页用于直接运行单个的Gradle命令。只用输入'gradle'后的部分。可以在添加自定义命令前在这里进行尝试。

# Gradle插件
Gradle自身提供的功能在实际的自动化过程中并不是非常有用。所有的实用功能，比如编译Java源码，是由`plugins`提供的。

一般会说`apply plugin`(使用插件)，对应的方法是`Project.apply()`。

```
apply plugin: 'java'
```

Plugin有缩写名，比如使用'java'作为`JavaPlugin`的缩写名。也可以通过类型来使用插件，比如

```
apply plugin: org.gradle.api.plugins.JavaPlugin
```

由于Gradle有缺省导入，所以也可写作


```
apply plugin: JavaPlugin
```

`plugin`的使用是幂等的(可以多次`apply`)。插件不过是实现了`Plugin`接口的类。


# 疑难

## 代理上网问题
在公司一般通过代理，Gradle没有设置正确的代理的话，很可能无法从repository下载依赖的JAR文件，提示构建失败。Gradle中可以使用标准的JVM系统属性来设置代理。可以直接在构建脚本中设置代理相关的属性，如`System.setProperty('http.proxyHost', 'www.somehost.org')`。也可以在项目根目录或Gradle主目录中的`gradle.properties`文件中指定这些属性。`gradle.properties`内容如下

```
systemProp.http.proxyHost=www.somehost.org
systemProp.http.proxyPort=8080
systemProp.http.proxyUser=userid
systemProp.http.proxyPassword=password
systemProp.http.nonProxyHosts=*.nonproxyrepos.com|localhost
```

## 官方Maven库下载慢
如果当前网络条件下从官方Maven库下载文件过慢，可以通过修改构建脚本切换到国内的Maven镜像仓库，或者换成自建的Maven私服。比如，切换成[开源中国的Maven库](http://maven.oschina.net/index.html)

```
repositories {
    maven{ url 'http://maven.oschina.net/content/groups/public/'}
}
```

也可以在`gradle.properties`中添加`REPO_URL=<url>`，然后在`build.gradle`中使用`project.REPO_URL`引用这个url

## 其他问题
错误：`Connection to https://jcenter.bintray.com refused`

原因：没有配置HTTPS代理，在`gradle.properties`上配置HTTPS代理即可

错误：`Could not create plugin of type 'AppPlugin'`

原因：Gradle版本过低。使用独立安装的Gradle(v1.12)会出现这个问题，使用Android Studio自带的Gradle(v2.2.1)完全正常

错误：JDK编译失败

原因：Android Studio自带JDK 7编译，源码中使用了Java 7的语法。命令行中使用Gradle编译时使用JDK 6进行编译，所以报错。`gradle.properties`使用`org.gradle.java.home`属性指定Gradle编译时使用的JDK路径。[官方文档](https://docs.gradle.org/current/userguide/build_environment.html)

---
参考资料：

+ Gradle Userguide
+ [Gradle 修改 Maven 仓库地址](http://www.yrom.net/blog/2015/02/07/change-gradle-maven-repo-url/)
+ [使用Gradle构建速度慢的问题](http://www.huangyunkun.com/2014/04/26/libgdx-gradle-change-source/)

[source]: https://docs.gradle.org/1.6/userguide/tutorial_using_tasks.html

To Read:

[Gradle Android插件用户指南]: http://avatarqing.gitbooks.io/gradlepluginuserguidechineseverision/content/
[201402 Gradle使用笔记]: http://sinojelly.sinaapp.com/2014/02/201402-gradle-use-notes/
[BUILDING FAST(ER) WITH GRADLE IN ANDROID STUDIO]: http://rileybrewer.com/blog/2013/10/4/building-faster-with-gradle-in-android-studio
