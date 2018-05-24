---
title: Gradle之Groovy基础
tags: gradle
date: 2018-03-08 16:53:54
toc: true
---
[Gradle系列之从init.gradle说起 - CSDN博客](http://blog.csdn.net/sbsujjbcy/article/details/52079413)
[Android | 飞雪无情的博客](http://www.flysnow.org/categories/Android/)
[Gradle完全解析 - Android 同学会 - KM平台](http://km.oa.com/group/22112/articles/show/332186?kmref=search&from_page=1&no=6)


Gradle脚本对Java程序员来说非常简单易懂，我们甚至可以直接在Gradle脚本中写Groovy代码甚至Java代码。但不了解Groovy和Gradle DSL的话，你会发现Gradle脚本其实地方让人很疑惑。
<!--more-->
# 关于Gradle脚本的疑问
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

task someTask << {
	
}
```

这是一个典型的Gradle脚本。就这个脚本而言，我有两个疑问：

+ 第一，`apply plugin: 'com.android.application'`的语法形式怪怪的，这里有个":"。Gradle脚本中什么时候会出现":"，什么时候又不需要":"？
+ 第二，`defaultConfig {}`中`minSdkVersion 15`和`vectorDrawables.useSupportLibrary = true`的语法形式并不一致。Gradle脚本中什么时候会出现"="，什么时候又不需要"="？

要清楚地答这两个问题，需要从Groovy语言和Gradle DSL入手。

# Groovy基础
掌握Gradle需要了解的Groovy基础知识如下：

![](groovy-basic.png)

[Groovy基础](http://www.flysnow.org/2016/05/22/groovy-basis.html)一文对Groovy有一个非常简明的介绍，我画了一张图对其内容进行总结。

下面结合上图以及我的问题，有针对性的了解一些Groovy基础内容，包括

+ Groovy的GString
+ 

## String与GString
GString给我们提供了强大的格式化能力。有了GString，完全可以避免使用Java的`String.format()`来格式化输出。看看示例。

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

通过以上例子不难明白普通字符串与GString的区别。现在不妨看看[这段Gradle脚本](https://github.com/square/leakcanary/blob/master/leakcanary-android/build.gradle)中`buildConfigField`这一行，考查下自己是否掌握GString的用法。

```groovy
apply plugin: 'com.android.library'

def gitSha() {
  return 'git rev-parse --short HEAD'.execute().text.trim()
}

android {
  ...
  defaultConfig {
    minSdkVersion versions.minSdk
	...
    buildConfigField "String", "GIT_SHA", "\"${gitSha()}\""
	...
  }
}
```

分析如下：

+ 首先，这个字符串是双引号且有未被转义的$
+ 其次，${gitSha()}将调用`gitSha()`方法并将其结果作为字符串的值。`{}`中可以是任意合法的groovy表达式

## Groovy方法
调用Groovy方法时可以省略括号，并且Groovy方法支持命名参数。

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
Groovy方法支持命名参数。当然，跟Python中的命名参数不同，Groovy命名参数并不是真正意义上的命名参数。实际上，Groovy仅仅是在让方法的map参数的key可以在方法中直接使用而已(以map.key这种方式来访问参数)。

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

## Groovy集合
集合不是我们关注重点，这里只简单提一下。字面量形式的list和map使用非常方便。另外list和map均提供each()方法用于便捷访问。

```groovy
def list = [1, 2, 3]
println list[0]
list.each { println it }

['w':1024, 'h':768].each {
    println "$it.key $it.value"
}
```

## Groovy闭包
为方便对上述第二个问题展开讨论，我们必须Groovy闭包。闭包是Groovy最强大的特性。

### 闭包介绍
什么是闭包呢？简单来讲，Groovy的闭包可以理解为"代码块"，它本质也是一个对象，所以也可以被引用、被传递以及被执行。(可以类比C中的函数指针以及Java 8的方法引用）Groovy中，"代码块"对象具体来说是[Closure](http://docs.groovy-lang.org/latest/html/api/groovy/lang/Closure.html)的对象。

为什么需要闭包呢？我们经常遇到单方法接口，比如Java中的`FileFilter`或Android中的`OnClickListener`，前者只定义了一个方法`accept(File)`，后者只定义了一个方法`onClick(View)`。这种代码组织方式对于单方法接口非常繁琐，我们不得不为一些很简单的操作定义类，导致类数量膨胀。闭包能在一定程度上解决这个问题。

还是上代码，看代码可能会更直观地了解闭包。

```groovy
[1, 2, 3].forEach(new Consumer<Integer>() {
    @Override
    void accept(Integer integer) {
        println "for each " + integer
    }
})

[1, 2, 3].each { println "each " + it }
```

代码的功能是逐个打印列表中的元素。分别使用单方法接口的方式和闭包的方式来完成功能。显而易见后一个方法更可读，尤其当你习惯这种写法之后。该语句的各个元素如下：

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

# Gradle解惑


上面代码中的`add a: 100`不仅不会怪怪的，还很眼熟，对吧。嗯，`apply plugin: 'com.android.library'`就是这么来的。当然`apply()`方法远比我们这里的`add()`方法要复杂，`apply()`方法的完整形式是[`Project.apply(map)`](https://docs.gradle.org/current/dsl/org.gradle.api.plugins.PluginAware.html#org.gradle.api.plugins.PluginAware:apply(java.util.Map))。这里不展开说明，后面的文章中我会详细讨论。

# 总结
回头来看Groovy中关于方法的一些规则其实是有意为之，这些小规则再加上Groovy的闭包委托功能，可以非常方便地实现Gradle DSL。要点总结如下：

+ 闭包的委托优先模式
+ 方法调用可以省略括号；方法的最后一个参数是闭包，则闭包可以放在括号外面

# 参考
[Android | 飞雪无情的博客](http://www.flysnow.org/categories/Android/)