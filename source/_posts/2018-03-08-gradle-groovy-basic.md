---
title: Gradle之Groovy基础
tags: gradle
date: 2018-03-08 16:53:54
toc: true
---
Gradle脚本看似简单，但深究的话你会发现其实有些地方并不容易理解。
<!--more-->
# 关于Gradle的疑问
```groovy
apply plugin: 'com.android.application'

android {
    compileSdkVersion 26
    defaultConfig {
        applicationId "com.example.kingcmchen.myapplication"
        minSdkVersion 15
        targetSdkVersion 26
        versionCode 1
        versionName "1.0"
        testInstrumentationRunner "android.support.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }
    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        }
    }
}

dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar'])
    implementation 'com.android.support:appcompat-v7:26.1.0'
}
```

这是一个典型的Gradle脚本。简单了解Gradle后我们知道：

+ `apply plugin: 'com.android.application'`表示使用Android应用插件，它会添加Android应用开发相关的Task
+ `android {}`语句块对Android应用插件添加进来的域对象`android`进行各种必要的配置
+ `dependencies`表示应用的依赖，使用`implementation 'group-id:archives-id:version''`为项目添加依赖

通常理解以上几点后就够了，而日常开发过程使用Gradle构建Android应用一般也只是修改`dependencies`和执行`gradle`命令。

但这种似懂非懂的状态下，我始终有几个疑惑。比如，就上面的脚本而言：

+ 语法上看`apply plugin`的形式怪怪的，如何理解？
+ `defaultConfig {}`中`minSdkVersion 15`和`vectorDrawables.useSupportLibrary = true`，一个有"="一个没有"="，为什么形式不一致

要解答这两个问题，得从Groovy语言和Gradle DSL入手。本文主要讨论Groovy

# Groovy基础
掌握Gradle需要了解的Groovy基础知识如下：

![](groovy-basic.png)

这张图总结自[Groovy基础 | 飞雪无情的博客](http://www.flysnow.org/2016/05/22/groovy-basis.html)

# Gradle解惑
## String与GString
```groovy
def version = 2.4

// Java String, 单引号中的是普通字符串
def javaString = 'Java'
def javaString2 = 'Java $version'

// Groovy String, 双引号中且有未被转义的$的是GString, 允许使用占位符
def groovyString = "Groovy v$version"
// Java String, 双引号中、有被转义的$是普通字符串
def nonGroovyString = "Groovy v\$version"

// output: Java class java.lang.String
println javaString + ' ' + javaString.getClass().toString()
// output: Java $version class java.lang.String
println javaString2 + ' ' + javaString2.getClass().toString()
// output: Groovy v2.4 class org.codehaus.groovy.runtime.GStringImpl
println groovyString + ' ' + groovyString.getClass().toString()
// output: Groovy v$version class java.lang.String
println nonGroovyString + ' ' + nonGroovyString.getClass().toString()
```

通过以上例子不难明白普通字符串与GString的区别。现在，你能明白以下这段脚本中"\"${gitSha()}\""的用法了吧。注，这段代码来自[LeakCanary](https://github.com/square/leakcanary/blob/master/leakcanary-android/build.gradle)。

```groovy
apply plugin: 'com.android.library'

def gitSha() {
  return 'git rev-parse --short HEAD'.execute().text.trim()
}

android {
  ...
  defaultConfig {
    minSdkVersion versions.minSdk
    buildConfigField "String", "LIBRARY_VERSION", "\"${rootProject.ext.VERSION_NAME}\""
    buildConfigField "String", "GIT_SHA", "\"${gitSha()}\""
	...
  }
}

```

+ 首先，这个字符串是双引号且有未被转义的$
+ 其次，${gitSha()}将调用`gitSha()`方法并将其结果作为字符串的值。`{}`中可以是任意合法的groovy表达式

## Groovy方法
调用Groovy方法时可以省略括号，并且Groovy方法支持命名参数。如果不了解这两点，免不了会觉得`apply plugin: 'com.android.library'`怪怪的。

### 省略括号
省略括号可以让代码更简短，比如`output()`方法：

```groovy
def output(a) {
    println a
}
// 完整写法 Output: 1
output(1)
// 省略括号的写法 Output: 1
output 1
```

如果方法有两个或两个以上的参数呢？

```groovy
def add(a, b) {
    return a + b
}
// 完整写法
sum = add(1, 2)
// 省略括号的写法
sum2 = add 1,2
// true
assert sum == sum2
```
 注意：`add 1,2`的确是`add(1,2)`省略括号的写法， `1,2`表示两个整形参数， 而不是一个包含两个整数的列表或数组，或别的什么东西。

### 命名参数
Groovy方法支持命名参数。当然，并不是真正意义上的支持。实际上Groovy仅仅是在让方法的map参数的key可在方法中作为直接使用而已。

```groovy
def add(map) {
    // 为所有参数设置默认值
    ['a', 'b', 'c'].each { map.get(it, 0)}
    println "a = $map.a, b = $map.b"
    return map.a + map.b
}

// Output: a = 1, b = 2
add(b:2, a:1)
// Output: a = 1, b = 2
add(a:1, b:2)
// Output: a = 1, b = 0
add(a:1)

// 省略括号的写法
add a:1, b:2
add a: 100
```

上面代码中的`add a: 100`不仅不会怪怪的，还很眼熟，对吧。嗯，`apply plugin: 'com.android.library'`就是这么来的。当然`apply()`方法远比我们这里的`add()`方法要复杂，`apply()`方法的完整形式是[`Project.apply(map)`](https://docs.gradle.org/current/dsl/org.gradle.api.plugins.PluginAware.html#org.gradle.api.plugins.PluginAware:apply(java.util.Map))。这里不展开说明，后面的文章中我会详细讨论。

## Groovy闭包
### 什么是闭包
简单来讲，Groovy的闭包可以理解为"代码块"。这个代码块也是一个对象，可以被引用、被传递以及被执行。类似于C中的函数指针以及Java 8的方法引用。

我们经常遇到单方法接口。比如Java中的`FileFilter`，它只定义了一个方法`accept(File)`。又比如Android中的`OnClickListener`，它也只定义了一个方法`onClick(View)`。这种代码组织方式非常繁琐，我们不得不为一些很简单的操作定义类，最后得到非常非常多的类。闭包能在一定程度上解决这个问题。

```groovy
[1, 2, 3].forEach(new Consumer<Integer>() {
    @Override
    void accept(Integer integer) {
        println "for each " + integer
    }
})

[1, 2, 3].each { println "each " + it }
```

这个语句的各个元素如下：

![](what-is-closure.png)

`each()`方法接收一个闭包([Closure](http://docs.groovy-lang.org/latest/html/api/groovy/lang/Closure.html))作为参数。[Closure](http://docs.groovy-lang.org/latest/html/api/groovy/lang/Closure.html)定义如下：

```java
public abstract class Closure<V> {
	...
}
```

Closure表示Groovy中的闭包对象，它是一个普通的Java类。Groovy允许Closure以如下形式被调用：

```groovy
 def a = 1
 def c = { a }
 assert c() == 1
 assert c.call() == 1
```

简单来说，一个闭包是被包装为一个对象的代码块。下面的代码

```groovy
log = ''
// 使用赋值的方式声明闭包
Closure c = { counter -> log += counter}
(1..10).each(c)

// 直接声明闭包
(1..10).each({ counter -> log += counter })

// 闭包的缩写定义，省略了方法调用时的括号
(1..10).each { counter -> log += counter}

// 闭包的缩写定义，使用隐式参数it
(1..10).each { log += it }
```

### 闭包委托
> Groovy闭包的强大之处在于它支持闭包方法的委托。Groovy的闭包有thisObject、owner、delegate三个属性，当你在闭包内调用方法时，由他们来确定使用哪个对象来处理。默认情况下delegate和owner是相等的，但是delegate是可以被修改的，这个功能是非常强大的，Gradle中的很闭包的很多功能都是通过修改delegate实现的 [来源](http://www.flysnow.org/2016/05/22/groovy-basis.html)

更多内容见{% post_link gradle-groovy-closure %}

了解闭包之后再来看本文开头出现的Gradle脚本，这里只选取关键部分

```groovy
android {
    compileSdkVersion 26

    defaultConfig {
        minSdkVersion 15
        vectorDrawables.useSupportLibrary = true
    }
}
```

分析如下

+ `android {}`其实是一个方法调用，方法形式为`android(Closure)` (还记得方法括号可省略，方法最后一个参数如果是闭包，闭包可以放到括号外面的规则吗？)
+ `minSdkVersion 26`也是一个方法调用，方法形式为`minSdkVersion(int)`，所以它的完整形式其实是`minSdkVersion(26)` (仍然是方法括号可省略的规则)
+ `vectorDrawables.useSupportLibrary = true`是对属性赋值

不难使用闭包委托实现自己的DSL。

```groovy
def android(Closure<Android> closure) {
    Android a = new Android()
    closure.delegate = a
    //委托模式优先
    closure.setResolveStrategy(Closure.DELEGATE_FIRST)
    closure(a)
}

class Android {

    def compileSdkVersion(sdkVersion) {
        println "in compileSdkVersion(). compileSdkVersion=$sdkVersion"
    }

    def defaultConfig(Closure<Config> closure) {
        Config c = new Config()
        closure.delegate = c
        //委托模式优先
        closure.setResolveStrategy(Closure.DELEGATE_FIRST)
        closure(c)
    }
}

class Config {
    VD vectorDrawables = new VD()

    def minSdkVersion(sdkVersion) {
        println "in minSdkVersion(). minSdkVersion=$sdkVersion"
    }
}

class  VD {
    boolean useSupportLibrary
}

// 自己实现的DSL
// Output:
// in compileSdkVersion(). compileSdkVersion=26
// in minSdkVersion(). minSdkVersion=15
android {
    compileSdkVersion 26

    defaultConfig {
        vectorDrawables.useSupportLibrary = true
        minSdkVersion 15
    }
}
// 上述DSL的完整形式是这样的
//android({
//
//    a.compileSdkVersion(26)
//
//    a.defaultConfig({
//        c.minSdkVersion(15)
//        c.vectorDrawables.put('useSupportLibrary', true)
//    })
//})

```

至此你应该明白为什么`defaultConfig {}`中`minSdkVersion 15`和`vectorDrawables.useSupportLibrary = true`，一个有"="一个没有"="了吧。

# 总结
回头来看Groovy中关于方法的一些规则其实是有意为之，这些小规则再加上Groovy的闭包委托功能，可以非常方便地实现Gradle DSL。要点总结如下：

+ 闭包的委托优先模式
+ 方法调用可以省略括号；方法的最后一个参数是闭包，则闭包可以放在括号外面

# 参考
[Android | 飞雪无情的博客](http://www.flysnow.org/categories/Android/)