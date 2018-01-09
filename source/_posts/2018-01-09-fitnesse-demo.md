---
title: FitNesse试用
tags:
 - 代码测试
toc: true
date: 2018-01-09 11:11:13
---


FitNesse用于集成测试，非常适用于测试接口。但感觉使用并不广泛。试用一下，并记录用法。
<!--more-->

# 安装和运行
从[FitNesseDownload](http://fitnesse.org/FitNesseDownload)下载jar包。目前最新包是release 20161106。

命令行中使用`java  -jar fitnesse-standalone.jar -p 9999`启动fitnesse server。

![](/images/1515405168646.png)

fitnesse server启动后，我们可以通过浏览器访问，截图如下：

![](/images/1515405277765.png)

# 用法简介
感觉FitNesse文档过于简单，不过它提供了[A Two-Minute Example](http://fitnesse.org/FitNesse.UserGuide.TwoMinuteExample)的例子。我们直接看例子。

例子用于测试一个计算器程序中的除法运算。比如，对于10除以2，你期望返回5。FitNesse中使用 **input** 和 **expected output** 数据表来表示 test。类似这样：

![](/images/1515413862042.png)

FitNesse中这里的表格称为[Decision Table](http://localhost:9999/FitNesse.UserGuide.WritingAcceptanceTests.SliM.DecisionTable)，例子中的每行数据代表一个完整的场景。"numerator"和"denominator"列表示input，而"quotient?"的问号告诉FitNesse这一列表示是 expected output(期望的输出值)。以"10/2 = 5.0"为例，可将其理解为一个问句："如果使用10除以2，我能否得到5?"

如何运行测试呢，很简单。点击FitNesse logo旁边的 **Test** 按钮即可。

![](/images/1515462902359.png)

运行后结果如下：

![](/images/1515462989612.png)

红色表示代码返回了正确的期望值。而红色表示代码返回了跟期望值不同的值，这里可以看到两个值，分别是 **expected value** 和 **actual value**

另外注意这里用于比较的符号：

+ 22/7 ~= 3.14 22/7约等于3.14
+ 9/3 < 5      9/3的结果小于5
+ 11/2 4<_6    11/2的结果在4和6之间

# 工作原理

当用户点击Test按钮时发生什么呢？

+ 首先，FitNesse将test表格提交到[Slim](http://fitnesse.org/FitNesse.UserGuide.WritingAcceptanceTests.SliM)，即底层的测试系统(该系统包含实际执行测试所需要的代码)
+ Slim找到跟test表格对应的fixture代码并运行
+ fixture代码调用应用代码，得到运行结果
+ Slim向FitNesse返回fixture代码的运行结果
+ FitNesse根据运行结果给test表格中标记相应的颜色

那Fixture Code又是什么呢？下面是一个例子。

```java
package eg;

public class Division {
  private double numerator, denominator;
  
  public void setNumerator(double numerator) {
    this.numerator = numerator;
  }
  
  public void setDenominator(double denominator) {
    this.denominator = denominator;
  }
  
  public double quotient() {
    return numerator/denominator;
  }
} 
```

![](/images/1515413862042.png)

+ `eg` - Java包名
+ `Division` - Java类

Slim运行fixture代码的流程是这样的：

+ Slim从左到右处理表格中的每一行数据
+ 对每一行，Slim调用对应的setter将输入数据保存到对应字段中。比如对"numerator"会调用`Division.setNumerator()`
+ 然后，Slim会调用`Decision.quotient()`方法得到输出数据

以第一行测试数据为例，几个方法调用的顺序如下：

1. division.setNumerator(10)
2. division.setDenominator(2)
3. division.quotient()

如果返回值和期望值匹配，FitNesse将格子显示为绿色。如果返回值和期望值不匹配，FitNesse将格子显示为红色并显示期望值和返回值。如果Slim遇到异常或者找不到fixture代码，FitNesse将格子显示为黄色并显示相应的堆栈信息。

实际中的fixture代码不应当做任何实际操作，而是代理到应用代码。即，fixture代码应当 **越"薄"越好** ， 最好是充当FitNesse表格和实际应用代码之间的 **管道** 。更多关于fixture代码的内容请参考[这里](http://fitnesse.org/FitNesse.FullReferenceGuide.UserGuide.WritingAcceptanceTests.FixtureCode)。

# Decision Table
使用一种简单的标记语言创建上文中提到的Decision Table，包括表头，文本加粗，下划线，斜体，无序列表，以及其他一些简单的格式化。具体参考[FitNesse.UserGuide.FitNesseWiki](http://fitnesse.org/FitNesse.UserGuide.FitNesseWiki)。

```
|eg.Division|
|numerator|denominator|quotient?|
|10       |2          |5        |
|12.6     |3          |4.2      |
|100      |4          |33       |
```


上面的标记文本可生成如下表格：

![](/images/1515413862042.png)

# Hello FitNesse
现在我们自己动手添加一个FitNesse测试。

## 应用代码
```java
package demo;

public class Calculator {

    public int add(int i, int j) {
        return i + j;
    }
}
```

## Fixture代码
```java
package demo;

public class CalculatorDemo {

    private int number;
    private int anotherNumber;
    private int sum;

    public void setNumber(int number) {
        this.number = number;
    }

    public void setAnotherNumber(int anotherNumber) {
        this.anotherNumber = anotherNumber;
    }

    public int sum() {
        return new Calculator().add(number, anotherNumber);
    }

    public static void main(String[] args) {
        System.out.println("");
    }
}
```

![](/images/1515465864078.png)

代码路径`/Users/kc/wd/unitTest/lib1/out/production/classes`

## 添加FitNesse测试页面

点击"Add"下拉菜单，选择"Test Page"，并在页面中添加以下内容并保存

```
!path /Users/kc/wd/unitTest/lib1/out/production/classes
!define TEST_SYSTEM {slim}

|demo.CalculatorDemo      |
|number|anotherNumber|sum?|
|1     |1            |2   |
|0     |3            |3   |
|100   |4            |104 |
```

![](/images/1515466040741.png)

+ 第1行`!path`指定了类路径
+ 第2行`!define`指定当前为slim测试
+ 第4行`demo.CalculatorDemo`指定了完整的类名
+ 第5行指定了输入数据和输出数据
+ 第6行及以后为实际测试数据

## 运行测试

<video src='fitnesse.mp4' type='video/mp4' controls='controls'  width='100%' height='100%'>
</video>

# 总结
整个试用感觉FitNesse还不错，wiki形式的测试易理解易于团队成员间沟通，非常适用于后台接口测试。后续我将用FitNesse对后台接口进行测试，看看效果如何。