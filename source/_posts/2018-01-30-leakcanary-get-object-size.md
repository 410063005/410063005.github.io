---
title: 如何在Android中获取对象大小 
date: 2018-01-30 16:57:44
tags:
 - Android
---
[TODO] 如何在Android中获取对象大小。
<!--more-->

# 如何在Java中获取对象大小
## instrumentation
一种方式是使用[instrumentation](http://docs.oracle.com/javase/7/docs/api/java/lang/instrument/Instrumentation.html)，该方法在[Calculate size of Object in Java](https://stackoverflow.com/questions/9368764/calculate-size-of-object-in-java)和[In Java, what is the best way to determine the size of an object?](https://stackoverflow.com/questions/52353/in-java-what-is-the-best-way-to-determine-the-size-of-an-object)中均有介绍

但这种方式最终需要以如下较为繁琐的方式调用：

java -javaagent:ObjectSizeFetcherAgent.jar C

其中jar包包含检查对象大小的代码，而C是待测试的对象。

## ObjectGraphMeasurer

[https://github.com/DimitrisAndreou/memory-measurer](https://github.com/DimitrisAndreou/memory-measurer)

```java
import objectexplorer.ObjectGraphMeasurer;

public class Measurer {

  public static void main(String[] args) {
    Set<Integer> hashset = new HashSet<Integer>();
    Random random = new Random();
    int n = 10000;
    for (int i = 1; i <= n; i++) {
      hashset.add(random.nextInt());
    }
    System.out.println(ObjectGraphMeasurer.measure(hashset));
  }
}
```

## ObjectSizeCalculator

jdk.nashorn.internal.ir.debug.ObjectSizeCalculator

```java
System.out.println(ObjectSizeCalculator.getObjectSize(new gnu.trove.map.hash.TObjectIntHashMap<String>(12000, 0.6f, -1)));
System.out.println(ObjectSizeCalculator.getObjectSize(new HashMap<String, Integer>(100000)));
System.out.println(ObjectSizeCalculator.getObjectSize(3));
System.out.println(ObjectSizeCalculator.getObjectSize(new int[]{1, 2, 3, 4, 5, 6, 7 }));
System.out.println(ObjectSizeCalculator.getObjectSize(new int[100]));
```

164192
48
16
48
416

## 其他方式
### jol
[jol](http://openjdk.java.net/projects/code-tools/jol/)

### sizeof
[sizeof](http://sourceforge.net/projects/sizeof)

[an article on determining the size of composite and potentially nested Java objects](http://www.javaworld.com/javaworld/javaqa/2003-12/02-qa-1226-sizeof.html)

### ObjectSizer

[ObjectSizer](http://www.javapractices.com/topic/TopicAction.do?Id=83)

### 序列化
序列化(但有些人不推荐这种做法)

```
 Serializable ser;
 ByteArrayOutputStream baos = new ByteArrayOutputStream();
 ObjectOutputStream oos = new ObjectOutputStream(baos);
 oos.writeObject(ser);
 oos.close();
 return baos.size();
```

# 如何在Android中获取对象大小
貌似没有好的解决办法。但是LeakCanary中可以输出类似日志。它是如何获取对象大小的呢？

```
In com.sunmoonblog.memperf:1.0:1.
            * com.sunmoonblog.memperf.Box has leaked:
            * GC ROOT static com.sunmoonblog.memperf.Docker.container
            * leaks com.sunmoonblog.memperf.Box instance
            
            * Retaining: 20 B.
            * Reference Key: 04d239b5-2409-48f5-bc54-2432d6eabe61
            * Device: Genymotion Android Google Nexus 5X - 7.0.0 - API 24 - 1080x1920 vbox86p
            * Android Version: 7.0 API: 24 LeakCanary: 1.5.4 74837f0
            * Durations: watch=5001ms, gc=104ms, heap dump=5136ms, analysis=19166ms
```

## 方案二

[来源](https://stackoverflow.com/questions/9009544/android-dalvik-get-the-size-of-an-object)

建议翻一下Dalvik VM源码。对象的大小存储在 `ClassObject::objectSize : size_t`, 源码见 `dalvik/vm/oo/Object.h`

建议从 `dalvik/vm/alloc/Alloc.cpp` 的 `dvmTrackAllocation()`开始看起。 `new`操作符会调用到这个方法。