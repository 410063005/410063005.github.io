---
title: javac命令source参数的作用
date: 2018-08-27 10:38:13
tags:
---

介绍了javac的source参数和target参数用法。
<!--more-->

javac命令支持`-source`和`-target`参数。

```
➜  blog git:(hexo) ✗ javac
用法: javac <options> <source files>
  -source <发行版>              提供与指定发行版的源兼容性
  -target <发行版>              生成特定 VM 版本的类文件
```

那这两个参数如何使用呢？

先来看一个问题。我开发了一个Java程序，这个Java程序使用了一些第三方库。这个程序在我的电脑上运行得很正常。但公司的服务器上运行出错，提示`Unsupported major.minor version 52.0`。

```
java -jar abc.jar
Exception in thread "main" java.lang.UnsupportedClassVersionError : Unsupported major.minor version 52.0
```

使用`java -version`可以看到我的电脑上装的是JDK 8，而公司的服务器上装的OpenJDK 6。

```
java version "1.8.0_73"
Java(TM) SE Runtime Environment (build 1.8.0_73-b02)
Java HotSpot(TM) 64-Bit Server VM (build 25.73-b02, mixed mode)

java version "1.6.0_22"
OpenJDK Runtime Environment (IcedTea6 1.10.6) (rhel-1.43.1.10.6.el6_2-x86_64)
OpenJDK 64-Bit Server VM (build 20.0-b11, mixed mode)
```

你可能说这个问题很简单呀，给公司的服务器升级到JDK 8不就解决问题了。嗯，这个嘛... 我们还是把关注点放在`Unsupported major.minor version 52.0`

对任何一个java类文件执行`javap -v <class名> | grep version`操作都会有如下输出

```
minor version: 0
major version: 51
```

实际输出根据实际情况可能有所不同。这里列出了各版本JDK使用的major值：

+ Java 1.2 uses major version 46
+ Java 1.3 uses major version 47
+ Java 1.4 uses major version 48
+ Java 5 uses major version 49
+ Java 6 uses major version 50
+ Java 7 uses major version 51
+ Java 8 uses major version 52
+ Java 9 uses major version 53
+ Java 10 uses major version 54

到这里，我们不难明白`Unsupported major.minor version 52.0`的原因。我用JDK 8编译出一个类(Java程序本质上是由一系列类文件打包在一起，简单起见这里假设我们的程序只有一个类)，类的版本是52.0。当你输入`java <class名>`将程序跑起来时，JVM先要将指定的类加载进来。这时类的版本就发生作用了。我的电脑上装的是JDK 8(自带JVM 8)，它认识版本为52.0的类，以及更早的类。而公司服务器上装的是OpenJDK 6(自带JVM 6)，很不幸它只认识版本为50.0的类，以及更早版本的类。

怎么办？`-source`和`-target`就派上用场了，先上图。

{% asset_img javac.png 1418 720 给javac指定不同的source参数和target参数 %}

编译java类时指定`-source 1.6 -target 1.6`，生成的类可以在指定版本的JVM上运行。如果你不喜欢那个警告的话，记得使用`-bootclasspath`指定对应的启动类文件。

```
javac -source 1.6 -target 1.6 Hello.java -bootclasspath /path/to/rt.jar
```

`javac -source 1.6 -target 1.6 Hello.java`编译代码时，如果`Hello.java`中用到了Java 6之后才支持的新语法，会报错。

```java
import java.util.HashMap;

public class Hello {
    private HashMap<String, String> map = new HashMap<>();
}
```

比如上面这个代码会报错 "Hello.java:4: 错误: -source 1.6 中不支持 diamond 运算符  (请使用 -source 7 或更高版本以启用 diamond 运算符)"。

那么是否可以使用`java -source 1.7 -target 1.6 Hello.java`这种形式的呢？验证发现不行，提示"javac: 源发行版 1.8 需要目标发行版 1.8"

在gradle中这样配置source和target参数

```groovy
sourceCompatibility = 1.6
targetCompatibility = 1.6
```