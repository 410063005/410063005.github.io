---
title: Kotlin中thread的用法
tags: Kotlin
toc: true
date: 2018-05-24 20:29:46
---
Kotlin中可以使用`thread()`方法创建新的线程，指定的语句块将在新线程中运行。语法简单，十分易用。
<!--more-->
# Kotlin线程

在Kotlin中很容易创建新的线程。

```kotlin
fun main(args: Array<String>) {

    println("hello from ${Thread.currentThread().name}")

    thread {
        println("hello from ${Thread.currentThread().name}")
    }

}
```

输出如下：

```
hello from main
hello from Thread-0
```

用法够简单吧。你会好奇`thread {}`到底是什么黑科技，其实很简单，语法糖而已。

`thread()`是一个方法，其定义如下：

```kotlin
public fun thread(start: Boolean = true, isDaemon: Boolean = false, contextClassLoader: ClassLoader? = null, name: String? = null, priority: Int = -1, block: () -> Unit): Thread {
    val thread = object : Thread() {
        public override fun run() {
            block()
        }
    }
    if (isDaemon)
        thread.isDaemon = true
    if (priority > 0)
        thread.priority = priority
    if (name != null)
        thread.name = name
    if (contextClassLoader != null)
        thread.contextClassLoader = contextClassLoader
    if (start)
        thread.start()
    return thread
}
```

`thread()`方法具体实现跟我们在Java代码中`new Thread()`方式创建线程本质上没有任何区别。

不过实际Android项目中使用`thread {}`随意创建线程是不合适的，很可能带来混乱。我们最好根据任务的特点来使用不同的线程池。比如，将耗时的DB操作提交到`Executors.newSingleThreadExecutor()`是非常合适的。

那我们能不能定义一个跟`thread()`类似的`ioThread()`方法呢？答案是肯定的。代码如下：

```kotlin
private val IO_EXECUTOR = Executors.newSingleThreadExecutor()

/**
 * Utility method to run blocks on a dedicated background thread, used for io/database work.
 */
fun ioThread(f : () -> Unit) {
    IO_EXECUTOR.execute(f)
}

fun main(args: Array<String>) {

    println("hello from ${Thread.currentThread().name}")

    thread {
        println("hello from ${Thread.currentThread().name}")
    }

    ioThread {
        println("hello from ${Thread.currentThread().name}")
    }
}
```

这一回的输出是：

```
hello from main
hello from Thread-0
hello from pool-1-thread-1
```

从使用上来说，你根本感觉不到`ioThread {}`是用户自定义的，对不对？

# 参考资料
[PagingSample Executors](https://github.com/googlesamples/android-architecture-components/blob/master/PagingSample/app/src/main/java/paging/android/example/com/pagingsample/Executors.kt#L21)