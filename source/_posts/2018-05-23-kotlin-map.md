---
title: 谈谈Kotlin中的Map
tags: kotlin
toc: true
date: 2018-05-23 21:05:14
---
本文讨论了Kotlin的Map相对于Java的Map在设计上的优势，以及Kotlin的Map在使用上的便利性。
<!--more-->

# Java中的Map
谈Kotlin的Map前首先看看Java的Map。在Java中Map具体指`java.util.Map`。`java.util.Map`是接口，实际中最常用的类是`java.util.HashMap`。

```java
Map<Integer, String> m = new HashMap<Integer, String>();
m.put(1, "a");
m.put(2, "b");
```

`java.util.HashMap`常被用来作为最简单的内存缓存。一个典型的场景是先检查缓存中是否有结果，没有的话重新计算并更新缓存。

```java
Object value = cache.get(key);
if (value == null) {
    value = doWork();
    cache.put(key, value);
}
```

# Kotlin中的Map
在Kotlin中仍然可以使用`java.util.HashMap`。但要注意Kotlin给它换了个小号，使用时记得用小号就行了。

```kotlin
// 文件 kotlin.collections.TypeAliases.kt
@file:kotlin.jvm.JvmVersion

package kotlin.collections

@SinceKotlin("1.1") public typealias RandomAccess = java.util.RandomAccess


@SinceKotlin("1.1") public typealias ArrayList<E> = java.util.ArrayList<E>
@SinceKotlin("1.1") public typealias LinkedHashMap<K, V> = java.util.LinkedHashMap<K, V>
@SinceKotlin("1.1") public typealias HashMap<K, V> = java.util.HashMap<K, V>
@SinceKotlin("1.1") public typealias LinkedHashSet<E> = java.util.LinkedHashSet<E>
@SinceKotlin("1.1") public typealias HashSet<E> = java.util.HashSet<E>
```

在Kotlin中使用HashMap，示例如下：

```kotlin
fun kotlnJavaMap() {
    val m = HashMap<Int, String>()
    println(m.javaClass.name)
}
```

上面代码输出结果是`java.util.HashMap`


# Java和Kotlin的Map对比
虽然在Java中我们优先使用HashMap，但实际上Kotlin中并不建议直接使用`HashMap`。

我们知道，编程中准确地控制`HashMap`是否可以只读，可以减少意外写操作的风险。这意味着代码中更少的bug。

假如一个`HashMap`是只读的，你不小心更新它，代码编译期就会报错，而不会等到运行期间出现可能非常难以跟踪的bug。世界更加美好。可惜这种看似理解当然的结果，在Java中却并非如此。

**虽然Java不会恶心到让你去找运行期的bug，但也不聪明到编译期就报错，它仅仅是运行期报错。Kotlin对此有改进。**

## 更优的设计
Java以一种迂回的方式提供只读的HashMap。`java.util.Collections`工具类中提供了大量的`unmodifiableXXX()`方法用于生成不可修改的集合，其中包括`unmodifiableMap()`。

```
Map modifiableMap = ...
Map unmodifiableMap = Collections.unmodifiableMap(modifiableMap);
```
Java的解决方式并不完美，它存在两个问题：

1. Java并没有达到上述 *更新只读HashMap编译期报错* 的理想状态，而仅仅是把运行期的bug变成了运行时错误
2. Java的API设计很不友好 (What? modifiableMap和unmodifiableMap都是Map类型，你确认不是开玩笑吗？ 程序员不弄错才怪)

看这段代码，它在运行时抛出异常，`java.lang.UnsupportedOperationException at java.util.Collections$UnmodifiableMap.put(Collections.java:1457)`

```java
public void unsupportedOperationException() {
    // 没法区分 modifiableMap和unmodifiableMap
    Map<Integer, String> modifiableMap = new HashMap<>();
    Map<Integer, String> unmodifiableMap = Collections.unmodifiableMap(modifiableMap);
    // 更新只读HashMap不会在编译期报错
    unmodifiableMap.put(1, "a");
}
```

Kotlin在设计上修复了Java中留下的问题。Kotlin中并不建议直接使用Java中的HashMap。对于不可变的Map，Kotlin中使用`mapOf()`。对于可变的Map，Kotlin中使用`mutableMapOf()`。

这是`unsupportedOperationException()`的Kotlin版本，更新只读HashMap会在编译期报错。

```kotlin
fun noUnsupportedOperationException() {
    val unmodifiableMap = mapOf(1 to "a")
    // 更新只读HashMap会在编译期报错
    // unmodifiableMap[1] = "b"

    val modifiableMap = mutableMapOf(1 to "a")
    modifiableMap[1] = "b"
}
```

此外，`unmodifiableMap`是`Map`接口的实例，而`modifiableMap`是`MutableMap`接口的实例，它们的类型是不同的！前者只提供读操作，后者提供读写操作。明显Kotlin的API设计得更好了，你可以很明确地知道一个Map是可读还是可读写的。

基于此，Kotlin中并不应直接使用Java中的HashMap，而是优先使用Kotlin的Map解决方案：

+ 对于不可变的Map，Kotlin中使用`mapOf()`
+ 对于可变的Map，Kotlin中使用`mutableMapOf()`
+ 坚持要用HashMap的话，Kotlin中使用`hashMapOf()` (`mutableMapOf()`并不是HashMap，而是LinkedHashMap)

## 更简便的方法
[PagingWithNetworkSample](https://github.com/googlesamples/android-architecture-components/tree/master/PagingWithNetworkSample)的[FakeRedditApi.kt](https://github.com/googlesamples/android-architecture-components/blob/master/PagingWithNetworkSample/app/src/test-common/java/com/android/example/paging/pagingwithnetwork/repository/FakeRedditApi.kt#L33)中的一段代码如下：

```kotlin
class FakeRedditApi : RedditApi {
    // subreddits keyed by name
    private val model = mutableMapOf<String, SubReddit>()

    fun addPost(post: RedditPost) {
        val subreddit = model.getOrPut(post.subreddit) {
            SubReddit(items = arrayListOf())
        }
        subreddit.items.add(post)
    }
}
```

还记得本文开头Java中使用HashMap作为缓存的示例代码吗？改写成Kotlin代码大约是这样的。

val value = cache.getOrPut(key) { doWork() }

嗯，是的，没错，**只要一行Kotlin代码**。

相比Java，Kotlin给Map添加了许多非常实用的方法，这里列举几个：

```kotlin
fun mapUsage() {
    val map = mapOf(1 to "a", 2 to "b", 3 to "c")

    println(map.getOrDefault(20, "NONE"))
    println(map.filter { it.key % 2 != 0 })
    map.forEach { k, v -> println("$k -> $v") }
}
```

输出

```
NONE
{1=a, 3=c}
1 -> a
2 -> b
3 -> c
```

更多方法可参考源码或[文档](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/index.html)。

# 参考资料
[Kotlin collections介绍](https://kotlinlang.org/docs/reference/collections.html)
[Kotlin collections文档](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/index.html)
