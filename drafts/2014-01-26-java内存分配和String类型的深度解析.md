---
layout: post
title: java内存分配和String类型的深度解析
keywords: Java
description: Java基础
categories: [编程语言]
tags: [Java]
group: archive
icon: globe
---

出处：[http://my.oschina.net/xiaohui249/blog/170013](http://my.oschina.net/xiaohui249/blog/170013)

#一、引题
在java语言的所有数据类型中，String类型是比较特殊的一种类型，同时也是面试的时候经常被问到的一个知识点，本文结合java内存分配深度分析关于String的许多令人迷惑的问题。下面是本文将要涉及到的一些问题，如果读者对这些问题都了如指掌，则可忽略此文。

1、java内存具体指哪块内存？这块内存区域为什么要进行划分？是如何划分的？划分之后每块区域的作用是什么？如何设置各个区域的大小？

2、String类型在执行连接操作时，效率为什么会比StringBuffer或者StringBuilder低？StringBuffer和StringBuilder有什么联系和区别？

3、java中常量是指什么？String s = "s" 和 String s = new String("s") 有什么不一样？

本文经多方资料的收集整理和归纳，最终撰写成文，如果有错误之处，请多多指教！

#二、java内存分配
##1、JVM简介
Java虚拟机（Java Virtual Machine 简称JVM）是运行所有Java程序的抽象计算机，是Java语言的运行环境，它是Java 最具吸引力的特性之一。Java虚拟机有自己完善的硬体架构，如处理器、堆栈、寄存器等，还具有相应的指令系统。JVM屏蔽了与具体操作系统平台相关的信息，使得Java程序只需生成在Java虚拟机上运行的目标代码（字节码），就可以在多种平台上不加修改地运行。

一个运行时的Java虚拟机实例的天职是：负责运行一个java程序。当启动一个Java程序时，一个虚拟机实例也就诞生了。当该程序关闭退出，这个虚拟机实例也就随之消亡。如果同一台计算机上同时运行三个Java程序，将得到三个Java虚拟机实例。每个Java程序都运行于它自己的Java虚拟机实例中。

如下图所示，JVM的体系结构包含几个主要的子系统和内存区：

+ 垃圾回收器（Garbage Collection）：负责回收堆内存（Heap）中没有被使用的对象，即这些对象已经没有被引用了。
+ 类装载子系统（Classloader Sub-System）：除了要定位和导入二进制class文件外，还必须负责验证被导入类的正确性，为类变量分配并初始化内存，以及帮助解析符号引用。
+ 执行引擎（Execution Engine）：负责执行那些包含在被装载类的方法中的指令。
+ 运行时数据区（Java Memory Allocation Area）：又叫虚拟机内存或者Java内存，虚拟机运行时需要从整个计算机内存划分一块内存区域存储许多东西。例如：字节码、从已装载的class文件中得到的其他信息、程序创建的对象、传递给方法的参数，返回值、局部变量等等。

![](http://static.oschina.net/uploads/img/201310/19213307_plFg.jpg)

##2、java内存分区
从上节知道，运行时数据区即是java内存，而且数据区要存储的东西比较多，如果不对这块内存区域进行划分管理，会显得比较杂乱无章。程序喜欢有规律的东西，最讨厌杂乱无章的东西。 根据存储数据的不同，java内存通常被划分为5个区域：程序计数器（Program Count Register）、本地方法栈（Native Stack）、方法区（Methon Area）、栈（Stack）、堆（Heap）。
      
+ 程序计数器（Program Count Register）：又叫程序寄存器。JVM支持多个线程同时运行，当每一个新线程被创建时，它都将得到它自己的PC寄存器（程序计数器）。如果线程正在执行的是一个Java方法（非native），那么PC寄存器的值将总是指向下一条将被执行的指令，如果方法是 native的，程序计数器寄存器的值不会被定义。 JVM的程序计数器寄存器的宽度足够保证可以持有一个返回地址或者native的指针。
           
+ 栈（Stack）：又叫堆栈。JVM为每个新创建的线程都分配一个栈。也就是说,对于一个Java程序来说，它的运行就是通过对栈的操作来完成的。栈以帧为单位保存线程的状态。JVM对栈只进行两种操作：以帧为单位的压栈和出栈操作。我们知道,某个线程正在执行的方法称为此线程的当前方法。我们可能不知道，当前方法使用的帧称为当前帧。当线程激活一个Java方法，JVM就会在线程的 Java堆栈里新压入一个帧，这个帧自然成为了当前帧。在此方法执行期间，这个帧将用来保存参数、局部变量、中间计算过程和其他数据。从Java的这种分配机制来看,堆栈又可以这样理解：栈(Stack)是操作系统在建立某个进程时或者线程(在支持多线程的操作系统中是线程)为这个线程建立的存储区域，该区域具有先进后出的特性。其相关设置参数：

>-Xss --设置方法栈的最大值

+ 本地方法栈（Native Stack）：存储本地方方法的调用状态。

![](http://static.oschina.net/uploads/space/2013/1019/220128_AHoy_1010775.png)

+ 方法区（Method Area）：当虚拟机装载一个class文件时，它会从这个class文件包含的二进制数据中解析类型信息，然后把这些类型信息（包括类信息、常量、静态变量等）放到方法区中，该内存区域被所有线程共享，如下图所示。本地方法区存在一块特殊的内存区域，叫常量池（Constant Pool），这块内存将与String类型的分析密切相关。

![](http://static.oschina.net/uploads/space/2013/1019/220226_3aKe_1010775.png)

+ 堆（Heap）：Java堆（Java Heap）是Java虚拟机所管理的内存中最大的一块。Java堆是被所有线程共享的一块内存区域。在此区域的唯一目的就是存放对象实例，几乎所有的对象实例都是在这里分配内存，但是这个对象的引用却是在栈（Stack）中分配。因此，执行String s = new String("s")时，需要从两个地方分配内存：在堆中为String对象分配内存，在栈中为引用（这个堆对象的内存地址，即指针）分配内存，如下图所示。

![](http://static.oschina.net/uploads/space/2013/1019/220239_LKdX_1010775.png)

 JAVA虚拟机有一条在堆中分配新对象的指令，却没有释放内存的指令，正如你无法用Java代码区明确释放一个对象一样。虚拟机自己负责决定如何以及何时释放不再被运行的程序引用的对象所占据的内存，通常，虚拟机把这个任务交给垃圾收集器（Garbage Collection）。其相关设置参数：

>-Xms -- 设置堆内存初始大小  
-Xmx -- 设置堆内存最大值  
-XX:MaxTenuringThreshold -- 设置对象在新生代中存活的次数  
-XX:PretenureSizeThreshold -- 设置超过指定大小的大对象直接分配在旧生代中

Java堆是垃圾收集器管理的主要区域，因此又称为“GC 堆”（Garbage Collectioned Heap）。现在的垃圾收集器基本都是采用的分代收集算法，所以Java堆还可以细分为：新生代（Young Generation）和老年代（Old Generation），如下图所示。分代收集算法的思想：第一种说法，用较高的频率对年轻的对象(young generation)进行扫描和回收，这种叫做minor collection，而对老对象(old generation)的检查回收频率要低很多，称为major collection。这样就不需要每次GC都将内存中所有对象都检查一遍，以便让出更多的系统资源供应用系统使用；另一种说法，在分配对象遇到内存不足时，先对新生代进行GC（Young GC）；当新生代GC之后仍无法满足内存空间分配需求时， 才会对整个堆空间以及方法区进行GC（Full GC）。

![](http://static.oschina.net/uploads/img/201310/19213307_nWx7.jpg)

在这里可能会有读者表示疑问：记得还有一个什么永久代（Permanent Generation）的啊，难道它不属于Java堆？亲，你答对了！其实传说中的永久代就是上面所说的方法区，存放的都是jvm初始化时加载器加载的一些类型信息（包括类信息、常量、静态变量等），这些信息的生存周期比较长，GC不会在主程序运行期对PermGen Space进行清理，所以如果你的应用中有很多CLASS的话,就很可能出现PermGen Space错误。其相关设置参数：

>-XX:PermSize --设置Perm区的初始大小  
>-XX:MaxPermSize --设置Perm区的最大值

 + 新生代（Young Generation）又分为：Eden区和Survivor区，Survivor区有分为From Space和To Space。Eden区是对象最初分配到的地方；默认情况下，From Space和To Space的区域大小相等。JVM进行Minor GC时，将Eden中还存活的对象拷贝到Survivor区中，还会将Survivor区中还存活的对象拷贝到Tenured区中。在这种GC模式下，JVM为了提升GC效率， 将Survivor区分为From Space和To Space，这样就可以将对象回收和对象晋升分离开来。新生代的大小设置有2个相关参数：

>-Xmn -- 设置新生代内存大小。  
-XX:SurvivorRatio -- 设置Eden与Survivor空间的大小比例

+ 老年代（Old Generation）： 当 OLD 区空间不够时， JVM 会在 OLD 区进行 major collection ；完全垃圾收集后，若Survivor及OLD区仍然无法存放从Eden复制过来的部分对象，导致JVM无法在Eden区为新对象创建内存区域，则出现"Out of memory错误"  。

#三、String类型的深度解析
让我们从Java数据类型开始说起吧！Java数据类型通常（分类方法多种多样）从整体上可以分为两大类：基础类型和引用类型，基础类型的变量持有原始值，引用类型的变量通常表示的是对实际对象的引用，其值通常为对象的内存地址。对于基础类型和引用类型的细分，直接上图吧，大家看了一目了然。当然，下图也仅仅只是其中的一种分类方式。

针对上面的图，有3点需要说明：

+ char类型可以单独出来形成一类，很多基本类型的分类为：数值类型、字符型（char）和bool型。
+ returnAddress类型是一个Java虚拟机在内部使用的类型，被用来实现Java程序中的finally语句。
+ String类型在上图的什么位置？yes，属于引用类型下面的类类型。下面开始对String类型的挖掘！

##1、String的本质
 打开String的源码，类注释中有这么一段话“Strings are constant; their values cannot be changed after they are created. String buffers support mutable strings.Because String objects are immutable they can be shared.”。这句话总结归纳了String的一个最重要的特点：String是值不可变(immutable)的常量，是线程安全的(can be shared)。
 
接下来，String类使用了final修饰符，表明了String类的第二个特点：String类是不可继承的。下面是String类的成员变量定义，从类的实现上阐明了String值是不可变的(immutable)。

            private final char value[];
            private final int count; 

因此，我们看String类的concat方法。实现该方法第一步要做的肯定是扩大成员变量value的容量，扩容的方法重新定义一个大容量的字符数组buf。第二步就是把原来value中的字符copy到buf中来，再把需要concat的字符串值也copy到buf中来，这样子，buf中就包含了concat之后的字符串值。下面就是问题的关键了，如果value不是final的，直接让value指向buf，然后返回this，则大功告成，没有必要返回一个新的String对象。但是。。。可惜。。。由于value是final型的，所以无法指向新定义的大容量数组buf，那怎么办呢？“return new String(0, count + otherLen, buf);”，这是String类concat实现方法的最后一条语句，重新new一个String对象返回。这下真相大白了吧！

总结：String实质是字符数组，两个特点：1、该类不可被继承；2、不可变性(immutable)。

## 2、String的定义方法
在讨论String的定义方法之前，先了解一下常量池的概念，前面在介绍方法区的时候已经提到过了。下面稍微正式的给一个定义吧。

常量池(constant pool)指的是在编译期被确定，并被保存在已编译的.class文件中的一些数据。它包括了关于类、方法、接口等中的常量，也包括字符串常量。常量池还具备动态性，运行期间可以将新的常量放入池中，String类的intern()方法是这一特性的典型应用。不懂吗？后面会介绍intern方法的。虚拟机为每个被装载的类型维护一个常量池，池中为该类型所用常量的一个有序集合，包括直接常量(string、integer和float常量)和对其他类型、字段和方法的符号引用（与对象引用的区别？读者可以自己去了解）。

String的定义方法归纳起来总共为三种方式：

+ 使用关键字new，如：String s1 = new String("myString");
+ 直接定义，如：String s1 = "myString";
+ 串联生成，如：String s1 = "my" + "String";这种方式比较复杂，这里就不赘述了，请参见[java--String常量池问题的几个例子](http://blog.csdn.net/gaopeng0071/article/details/11741027)。

第一种方式通过关键字new定义过程：在程序编译期，编译程序先去字符串常量池检查，是否存在“myString”,如果不存在，则在常量池中开辟一个内存空间存放“myString”；如果存在的话，则不用重新开辟空间，保证常量池中只有一个“myString”常量，节省内存空间。然后在内存堆中开辟一块空间存放new出来的String实例，在栈中开辟一块空间，命名为“s1”，存放的值为堆中String实例的内存地址，这个过程就是将引用s1指向new出来的String实例。各位，最模糊的地方到了！堆中new出来的实例和常量池中的“myString”是什么关系呢？等我们分析完了第二种定义方式之后再回头分析这个问题。

第二种方式直接定义过程：在程序编译期，编译程序先去字符串常量池检查，是否存在“myString”，如果不存在，则在常量池中开辟一个内存空间存放“myString”；如果存在的话，则不用重新开辟空间。然后在栈中开辟一块空间，命名为“s1”，存放的值为常量池中“myString”的内存地址。常量池中的字符串常量与堆中的String对象有什么区别呢？为什么直接定义的字符串同样可以调用String对象的各种方法呢？

带着诸多疑问，我和大家一起探讨一下堆中String对象和常量池中String常量的关系，请大家记住，仅仅是探讨，因为本人对这块也比较模糊。

第一种猜想：因为直接定义的字符串也可以调用String对象的各种方法，那么可以认为其实在常量池中创建的也是一个String实例（对象）。String s1 = new String("myString");先在编译期的时候在常量池创建了一个String实例，然后clone了一个String实例存储在堆中，引用s1指向堆中的这个实例。此时，池中的实例没有被引用。当接着执行String s1 = "myString";时，因为池中已经存在“myString”的实例对象，则s1直接指向翅中的实例对象；否则，在池中先创建一个实例对象，s1再指向它。如下图所示：

![](http://static.oschina.net/uploads/space/2013/1027/082026_NSu9_1010775.png)

这种猜想认为：常量池中的字符串常量实质上是一个String实例，与堆中的String实例是克隆关系。

第二种猜想也是目前网上阐述的最多的，但是思路都不清晰，有些问题解释不通。下面引用[《JAVA String对象和字符串常量的关系解析》](http://blog.csdn.net/sureyonder/article/details/5569366)一段内容。

在解析阶段，虚拟机发现字符串常量"myString"，它会在一个内部字符串常量列表中查找，如果没有找到，那么会在堆里面创建一个包含字符序列[myString]的String对象s1，然后把这个字符序列和对应的String对象作为名值对( [myString], s1 )保存到内部字符串常量列表中。如下图所示：

![](http://static.oschina.net/uploads/space/2013/1027/082727_qdel_1010775.gif)

如果虚拟机后面又发现了一个相同的字符串常量myString，它会在这个内部字符串常量列表内找到相同的字符序列，然后返回对应的String对象的引用。维护这个内部列表的关键是任何特定的字符序列在这个列表上只出现一次。

例如，String s2 = "myString"，运行时s2会从内部字符串常量列表内得到s1的返回值，所以s2和s1都指向同一个String对象。

这个猜想有一个比较明显的问题，红色字体标示的地方就是问题的所在。证明方式很简单，下面这段代码的执行结果，javaer都应该知道。

	String s1 = new String("myString");
	String s2 = "myString";
	System.out.println(s1 == s2);  //按照上面的推测逻辑，那么打印的结果为true；而实际上真实的结果是false，因为s1指向的是堆中String对象，而s2指向的是常量池中的String常量。

![](http://static.oschina.net/uploads/space/2013/1027/090627_GrGk_1010775.png)

虽然这段内容不那么有说服力，但是文章提到了一个东西——字符串常量列表，它可能是解释这个问题的关键。

文中提到的三个问题，本文仅仅给出了猜想，请知道真正内幕的高手帮忙分析分析，谢谢！

+ 堆中new出来的实例和常量池中的“myString”是什么关系呢？
+ 常量池中的字符串常量与堆中的String对象有什么区别呢？
+ 为什么直接定义的字符串同样可以调用String对象的各种方法呢?

##3、String、StringBuffer、StringBuilder的联系与区别 
上面已经分析了String的本质了，下面简单说说StringBuffer和StringBuilder。

StringBuffer和StringBuilder都继承了抽象类AbstractStringBuilder，这个抽象类和String一样也定义了char[] value和int count，但是与String类不同的是，它们没有final修饰符。因此得出结论：String、StringBuffer和StringBuilder在本质上都是字符数组，不同的是，在进行连接操作时，String每次返回一个新的String实例，而StringBuffer和StringBuilder的append方法直接返回this，所以这就是为什么在进行大量字符串连接运算时，不推荐使用String，而推荐StringBuffer和StringBuilder。那么，哪种情况使用StringBuffe？哪种情况使用StringBuilder呢？    

关于StringBuffer和StringBuilder的区别，翻开它们的源码，下面贴出append()方法的实现。

StringBuffer.append()

	public synchronized StringBuffer append(String str) {
		super.append(str);
		return this;
	}

StringBuilder.append()

	public StringBuilder append(String str) {
		super.append(str);
		return this;
	}

上面代码的区别应该一目了然，StringBuffer在方法前加了一个synchronized修饰，起到同步的作用，可以在多线程环境使用。为此付出的代价就是降低了执行效率。因此，如果在多线程环境可以使用StringBuffer进行字符串连接操作，单线程环境使用StringBuilder，它的效率更高。

#四、参考文献

+ [Java虚拟机体系结构](http://www.cnblogs.com/java-my-life/archive/2012/08/01/2615221.html) 
+ [Java内存管理基础篇-Java内存分配](http://hllvm.group.iteye.com/group/wiki/3053-JVM) 
+ [Java堆内存设置优化](http://my.oschina.net/beiyou/blog/122394) 
+ [Java内存管理和垃圾回收](http://hi.baidu.com/leonchunlai/item/cdbed2ff78d3d7c342c36a36) 
+ [Java堆内存的转换和回收](http://www.51testing.com/html/16/310316-237256.html) 
+ [Java虚拟机的JVM垃圾回收机制](http://www.im47.cn/post/2012/10/2012-10-24-jvm) 
+ [浅谈设置JVM内存分配的几个妙招](http://developer.51cto.com/art/200907/135038.htm)
+ [深入Java字符串](http://lavasoft.blog.51cto.com/62575/80034/)
+ [Java性能优化之String篇](http://www.ibm.com/developerworks/cn/java/j-lo-optmizestring/)
+ [java字符串常量池知识](http://blog.sina.com.cn/s/blog_791db047010177w0.html)
+ [Java内存分配及String类型详解](http://fanyo.iteye.com/blog/427097)
+ [Java String的内存机制](http://www.oseye.net/user/kevin/blog/151)
+ [Java之内存分析和String对象]()
+ [String类学习总结](http://www.cnblogs.com/gw811/archive/2012/09/10/2677770.html)