---
title: 记一个多线程并发读写HashMap时遇到的问题
toc: true
date: 2018-03-30 15:37:38
tags: Java
---

HashMap并非线程安全的。多线程中使用HashMap时，应当注意数据同步问题。本文记录并分析了我在实际项目中遇到的一个关于HashMap数据不同步问题。
<!--more-->

# 问题描述
AppMap用于保存应用的登录态。

+ 请求后台接口时从AppMap读取登录态
+ 登录、退出登录、以及刷新微信access token时会更新登录态

AppMap是单例, 它的内部使用HashMap保存数据。代码如下：

```java
// AppMap.java
public class AppMap {

    private static AppMap instance = new AppMap();

    public static AppMap getInstance() {
        return instance;
    }

    private Map<String, String> m1 = new HashMap<>();

    public Map<String, String> snapshot() {
        return Collections.unmodifiableMap(m1);
    }

    public void update(Map<String, String> map) {
        m1.clear();
        m1.putAll(map);
    }
}

// Demo.java
HashMap m2 = appMap.snapshot();
```

+ `update()`方法用于更新登录态，更新登录态是写操作。`AppMap.update()`直接更改其内部的Map
+ `snapshot()`方法用于获取登录态，获取登录态是读操作。为避免调用方意外修改登录态，`AppMap.snapshot()`返回了一个不可修改的Map

由于某些历史原因，应用运行时可能出现这样一个多线程并发访问HashMap的场景：

![](flow.png)

 **线程A更新登录态，它向`m1`中写入数据；同时，线程B获取登录态以发送网络请求，它从`m2`中读取数据**。

线程B获取登录态发送网络请求的伪代码如下：

```java
void net() {
	if (!m2.containsKey("someKey")) {
		return;
	}	
	String someValue = m2.get("someKey");
	byte[] data = someValue.getBytes();
	....
}
```

很不幸，`byte[] data = someValue.getBytes()`这行代码抛出了NullPointerException，提示`someValue`为`null`。怎么可能？`someKey`明明存在啊，却拿不到`someValue`？

`m2`是通过[Collections.unmodifiableMap](http://grepcode.com/file/repository.grepcode.com/java/root/jdk/openjdk/6-b14/java/util/Collections.java#Collections.unmodifiableMap%28java.util.Map%29)方法得到的，它是一个不可变的map。`m2`本质上是共享`m1`的数据。代码示例如下：

```java
Map<String, String> original = new HashMap<>();
original.put("a", "A");
Map<String, String> unmodifiable = Collections.unmodifiableMap(original);
int oldSize = unmodifiable.size();
original.put("b", "B");
int newSize = unmodifiable.size();
// old size=1, new size=2
// 向original中添加数据, unmodifiable中的数据也跟着变化
// unmodifiable共享original的数据
System.out.println("old size=" + oldSize + ", new size=" + newSize);

// Collections.unmodifiableMap()为原始数据提供了一个不可变的视角
// 下面这一行抛出 UnsupportedOperationException
// unmodifiable.put("b", "B");
```

稍加分析就能明白，上图中多线程并发访问`HashMap`，读写操作序列是不确定的。按以下中这种读写序列执行时，线程A中会出现"someKey"存在，但拿不到"someValue"的奇怪现象，最终引起NullPointerException。

![](thread.png)

# 解决方法
如何避免线程B出现NullPointerException呢？修复方案很简单。

```java
void net() {
	String someValue = m2.get("someKey");
	if (someValue == null) {
		return;
	}
	byte[] data = someValue.getBytes();
	....
}
```

修改后的代码的确避免了NPE。这里给出测试代码，`npe()`是修复前的代码，`noNpe()`是修复后的代码。

```java
public class Demo {
    public static void main(String[] args) {
        Map<String, String> map = new HashMap<>();
        map.put("uid", "cm");

        AppMap appMap = AppMap.getInstance();
        appMap.update(map);

        Thread threadA = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Map<String, String> map = new HashMap<>();
                        map.put("uid", "cm" + new Random().nextInt());
                        appMap.update(map);
                        TimeUnit.MILLISECONDS.sleep(50);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        Thread threadB = new Thread(new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                int loop = 0;
                try {
                    while (true) {
                        loop++;
                        Map<String, String> unmodifiable = appMap.snapshot();
                        npe(unmodifiable);
                        // noNpe(unmodifiable);
                    }
                } catch (NullPointerException e) {
                    System.err.println("NPE occurred after " + (System.currentTimeMillis() - now) + "ms, " + loop + " loops");
                }
            }
        });
        threadA.start();
        threadB.start();
    }

    private static void npe(Map<String, String> unmodifiable) {
        if (unmodifiable.containsKey("uid")) {
            String value = unmodifiable.get("uid");
            byte[] data = value.getBytes();
        }
    }

    private static void noNpe(Map<String, String> unmodifiable) {
        String value = unmodifiable.get("uid");
        if (value != null) {
            byte[] data = value.getBytes();
        }
    }
}
```

执行修复前后的代码发现，`npe()`会输出如下`NPE occurred after 327ms, 4415532 loops`(具体数据可能有所不同)，而`noNpe()`不会输出该日志。证有修复方法是有效的。

# 参考

[Concurrency and HashMap](https://dzone.com/articles/concurrency-and-hashmap)
[Infinite loop in HashMap | JavaByPatel](http://javabypatel.blogspot.in/2016/01/infinite-loop-in-hashmap.html)
[java - Using HashMap in multithreaded environment - Stack Overflow](https://stackoverflow.com/questions/11050539/using-hashmap-in-multithreaded-environment)